package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderHandler {

    private final ProductService productService;
    private final OrderService orderService;
    private final CouponService couponService;

    @Transactional // 주문 생성
    public OrderResponse createOrderWithTx(OrderRequest orderRequest) {
        // 1. 주문된 상품의 재고 확인 및 차감
//        for (OrderRequest.OrderItem item : orderRequest.getOrderItems()) {
//            productService.checkAndReduceStock(item.getProductId(), item.getQuantity());
//        }
        // 1. 주문된 상품의 재고 확인 및 차감 (일괄 처리)
        productService.checkAndReduceStock(orderRequest.getOrderItems());

        // 2. 쿠폰이 있을 경우, 쿠폰 검증 및 적용
        if (orderRequest.getUserCouponId() != null) {
            couponService.applyCoupon(orderRequest.getUserId(), orderRequest.getUserCouponId());
        }

        // 3. 최종 주문 생성
        Long orderId = orderService.createOrder(orderRequest);

        // 4. 응답 반환
        return new OrderResponse(201, "주문이 정상적으로 처리되었습니다.", new OrderResponse.OrderData(orderId));
    }

}
