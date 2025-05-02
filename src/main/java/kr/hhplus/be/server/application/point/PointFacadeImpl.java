package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.common.exception.PointErrorCode;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.PointService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.common.exception.CustomBusinessException;
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

    @Override // 포인트 결제 처리
    @Transactional
    public void processPointPayment(Long orderId) {
        // 1. 주문 조회
        Order order = orderService.getOrderById(orderId);

        // 2. 주문 상태 검증 (EXPIRED면 예외 발생 → 도메인 내부에서 validate 호출)
        order.validatePayable(); // 도메인에 정의되어 있어야 함

        // 3. 사용자 포인트 차감 처리
        pointService.usePoints(orderId);
    }
}
