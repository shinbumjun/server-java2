package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    // 5분 내 미결제 주문을 취소하고, 쿠폰 및 재고를 복구
    @Transactional
    @Scheduled(fixedRate = 300000)
    public void expireUnpaidOrders() {
        // 5분 이상 결제되지 않은 미결제 주문 목록을 가져옴
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Order> unpaidOrders = orderService.getUnpaidOrdersBefore(fiveMinutesAgo);


        for (var order : unpaidOrders) {
            orderService.expireOrder(order); // 상태 EXPIRED로 변경
            couponService.revertCouponIfUsed(order.getUserCouponId()); // 쿠폰 복구
            productService.revertStockByOrder(order.getId()); // 재고 복구
        }
    }
}
