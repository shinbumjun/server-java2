package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.application.event.PaymentEventPublisher;
import kr.hhplus.be.server.application.event.PointPaymentCompletedEvent;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.service.*;
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

    private final PointPaymentHandler pointPaymentHandler;         // 트랜잭션 단위로 주문 처리 묶음

    private final RedisLockManager redisLockManager; // Redis 기반 분산 락 관리자
    private final FairLockManager fairLockManager;   // 사용자 순서 보장용 Fair Lock 큐 관리자

    private final RedisRankingService redisRankingService;

    private final PaymentEventPublisher paymentEventPublisher;

    private final UserService userService;

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

    // 사용자 본인만 자신의 포인트를 사용하기 때문에 공정 큐가 필요없다
    @Override  // 포인트 결제 처리 (락은 Facade, 트랜잭션은 Handler)
    public void processPointPayment(Long orderId) {
        // 1) Order & User 조회
        Order order = orderService.getOrderById(orderId);
        User user  = userService.getUser(order.getUserId()); // User 레코드 남기지 않으면 빈 결과값 에러

        String lockKey = "lock:point:" + user.getId();
        if (!redisLockManager.lockWithRetry(lockKey, 5000)) {
            throw new IllegalStateException("포인트 결제 락 획득 실패");
        }

        try {
            // 1) 주문 상태 검증 (EXPIRED 또는 이미 PAID 된 경우 예외 발생)
            orderService.validatePayableOrder(orderId);

            // 2) 포인트 차감 & 주문 상태 변경
            //    → PointPaymentHandler.payWithPoints() 에 @Transactional 로 묶여 있어,
            //       내부에서 usePoints() 와 updateOrderStatusToPaid() 가 하나의 트랜잭션으로 실행됩니다.
            //    ※ 관리자용 포인트 조정이 필요하다면, 별도의 adjustPoints(user, delta, reason) 메서드를 PointService에 구현하
            pointPaymentHandler.payWithPoints(order, user);

            // 3) 결제 성공 후 이벤트 발행 예약
            //    → 트랜잭션 커밋 직후(afterCommit) publisher.publishEvent() 호출
            paymentEventPublisher.publish(new PointPaymentCompletedEvent(order.getId(), user.getId()));

            // 3) 랭킹 집계 (트랜잭션 외부, 비경쟁)
            // Redis ZSet에 오늘 랭킹 집계 누적, 캐시 vs DB(작고 짧은 쿼리)
            List<OrderProduct> products = orderService.getOrderProductsByOrderId(orderId);
            for (OrderProduct op : products) {
                log.info("[주문ID {}] 상품ID={} 수량={} → Redis ZSet 집계 시작", orderId, op.getProductId(), op.getQuantity());
                redisRankingService.incrementDailyProductRanking(op.getProductId(), op.getQuantity());
            }

        } finally {
            // 5) 락 해제 (경쟁 구간 종료)
            redisLockManager.unlock(lockKey);
        }
    }
}
