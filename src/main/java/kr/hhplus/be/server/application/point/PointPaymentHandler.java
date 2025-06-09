package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.PointService;
import kr.hhplus.be.server.domain.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class PointPaymentHandler {
    private final PointService pointService;
    private final OrderService orderService;

    @Transactional
    public void payWithPoints(Order order, User user) {
        int amount = order.getTotalAmount();                // 결제 금액
        pointService.usePoints(user, amount);               // User 엔티티로 넘김
        orderService.updateOrderStatusToPaid(order);        // Order 엔티티로 넘김
    }
}
