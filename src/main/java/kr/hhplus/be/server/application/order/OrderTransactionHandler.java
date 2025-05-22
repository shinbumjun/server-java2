package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTransactionHandler {

    private final ProductService productService;
    private final OrderService orderService;
    private final CouponService couponService;

    /**
     * 재고 차감과 쿠폰 적용을 하나의 트랜잭션으로 묶어 처리한다.
     * 이 메서드가 실패하면 호출 측에서 주문 상태를 FAIL로 변경한다.
     *
     * @param orderId 생성된 주문 ID
     * @param items 주문 항목 (상품 ID + 수량)
     * @param userId 사용자 ID
     * @param couponId 적용할 사용자 쿠폰 ID
     */
    @Transactional
    public void processOrder(Long orderId, List<OrderItemCommand> items, Long userId, Long couponId) {
        // 1. 재고 차감
        productService.reduceStockWithTx(items);

        // 2. 쿠폰 적용 (있을 경우만)
        if (couponId != null) {
            couponService.applyCoupon(userId, couponId);
        }

        // 주문 상태(PAID)는 결제 단계에서 별도로 처리됨
        // 진짜 성공한 경우에만 주문 항목 저장
        orderService.saveOrderItems(orderId, items);
    }
}
