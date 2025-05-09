package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.application.order.OrderHandler;
import kr.hhplus.be.server.common.exception.PointErrorCode;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.PointService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.common.exception.CustomBusinessException;
import kr.hhplus.be.server.infra.lock.FairLockManager;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            // 실질적 포인트 처리 로직은 트랜잭션이 적용된 핸들러에 위임
            pointHandler.processPointPaymentTx(orderId);
        } finally {
            redisLockManager.unlock(lockKey);
        }
    }
}
