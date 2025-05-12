package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.interfaces.order.OrderRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    Long createOrder(OrderRequest orderRequest);  // 주문 생성

    // 5분 이상 결제되지 않은 미결제 주문 목록을 가져
    List<Order> getUnpaidOrdersBefore(LocalDateTime fiveMinutesAgo);

    void expireOrder(Order order); // 상태 EXPIRED로 변경

    // 주문 상태 변경: 결제 성공시 상태를 PAID로 업데이트
    void updateOrderStatusToPaid(Long orderId);

    // 주문 조회
    Order getOrderById(Long orderId);

    void updateOrderStatusToFail(Long orderId); // 실패 시 상태 FAIL
}
