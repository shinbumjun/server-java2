package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.order.OrderResponse;

public interface OrderFacade {
    OrderResponse createOrder(OrderRequest orderRequest);  // 주문 생성
}
