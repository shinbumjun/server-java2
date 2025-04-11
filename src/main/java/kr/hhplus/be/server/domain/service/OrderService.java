package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.interfaces.order.OrderRequest;

public interface OrderService {
    Long createOrder(OrderRequest orderRequest);  // 주문 생성
}
