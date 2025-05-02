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
}
