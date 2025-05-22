package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.application.event.PaymentEventPublisher;
import kr.hhplus.be.server.application.event.PointPaymentCompletedEvent;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.PointService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.infra.lock.FairLockManager;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import kr.hhplus.be.server.infra.redis.RedisRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class PointFacadeImpl implements PointFacade {

    // 사용자 포인트 관련 로직 처리 (조회, 충전, 사용)
    private final PointService pointService;
    // 주문 정보 조회 및 상태 변경 처리
    private final OrderService orderService;
    // 재고 복구 등 상품 관련 처리
    private final ProductService productService;
    // 쿠폰 사용 복구 등 쿠폰 관련 처리
    private final CouponService couponService;

    private final PointHandler pointHandler;         // 트랜잭션 단위로 주문 처리 묶음

    private final RedisLockManager redisLockManager; // Redis 기반 분산 락 관리자
    private final FairLockManager fairLockManager;   // 사용자 순서 보장용 Fair Lock 큐 관리자

    private final RedisRankingService redisRankingService;

    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    public PointResult chargePoints(PointCriteria criteria) { // 사용자 ID, 충전금액
        // 포인트 충전 로직 처리: 도메인 서비스 호출
        return pointService.chargePoints(criteria);
    }

    @Override
    public PointResult getPoints(PointCriteria criteria) {

        // 포인트 조회 로직 처리: 도메인 서비스 호출
        return pointService.getPoints(criteria);
    }

    // 사용자 본인만 자신의 포인트를 사용하기 때문에 공정 큐가 필요없다 (userId 단위 락)
    @Override // 포인트 결제 처리 (락은 파사드에서 잡고, 처리 위임은 핸들러에)
    public void processPointPayment(Long orderId) {
        Long userId = orderService.getOrderById(orderId).getUserId();
        String lockKey = "lock:point:" + userId;

        if (!redisLockManager.lockWithRetry(lockKey, 5000)) {
            throw new IllegalStateException("포인트 결제 락 획득 실패");
        }

        try {
            // 트랜잭션은 service에서만 수행 → AOP 분리 유지
            // 관리자 포인트 차감이 추가되면 둘 다 PointService.usePoints(userId, amount) 재사용해야 하기 때문에 수정

            // 파사드는 흐름만 조립
            orderService.validatePayableOrder(orderId);  // 주문 상태 검증 (EXPIRED, PAID 예외)
            int amount = orderService.getOrderById(orderId).getTotalAmount(); // 결제 금액 조회
            pointService.usePoints(userId, amount);  // 포인트 차감 및 내역 저장 (재사용 가능)
            orderService.updateOrderStatusToPaid(orderId);  // 주문 상태를 PAID로 변경

           // Redis ZSet에 오늘 랭킹 집계 누적, 캐시 vs DB(작고 짧은 쿼리)
            List<OrderProduct> products = orderService.getOrderProductsByOrderId(orderId);
            for (OrderProduct op : products) {
                log.info("[주문ID {}] 상품ID={} 수량={} → Redis ZSet 집계 시작", orderId, op.getProductId(), op.getQuantity());
                redisRankingService.incrementDailyProductRanking(op.getProductId(), op.getQuantity());
            }

            // 트랜잭션이 완료되면 (주문-결제 성공) → 이벤트 발행
            // 이벤트 발행 (AFTER_COMMIT에서 실행됨 → 외부 전송 비동기 처리)
            // 문제 : 파사드에 트랜잭션을 감싼게 아니라서 트랜잭션이 풀리면 이 코드는 무쓸모가 될 수 있다
            paymentEventPublisher.publish(new PointPaymentCompletedEvent(orderId, userId));

        } finally {
            redisLockManager.unlock(lockKey);
        }
    }
}
