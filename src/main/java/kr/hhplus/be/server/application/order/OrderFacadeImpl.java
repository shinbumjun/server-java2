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
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final CouponService couponService;

    @Override // 파사드에서는 호출만 !!! (상품 재고 차감, 쿠폰 적용, 주문 생성) 서비스 조합
    @Transactional // 주문 생성
    public OrderResponse createOrder(OrderRequest orderRequest) {
        // 1. 주문된 상품의 재고 확인 및 차감
        for (OrderRequest.OrderItem item : orderRequest.getOrderItems()) { // 한 사람의 주문에 포함된 여러 상품을 처리
            productService.checkAndReduceStock(item.getProductId(), item.getQuantity()); // 재고 확인 및 차감
        }

        // 2. 쿠폰이 있을 경우, 쿠폰 검증 및 적용
        if (orderRequest.getUserCouponId() != null) {
            couponService.applyCoupon(orderRequest.getUserId(), orderRequest.getUserCouponId());  // 쿠폰 검증 및 적용
        }

        // 3. 최종 주문 생성
        Long orderId = orderService.createOrder(orderRequest);  // 주문 생성

        // 4. 응답 반환
        return new OrderResponse(201, "주문이 정상적으로 처리되었습니다.", new OrderResponse.OrderData(orderId));
    }
}
