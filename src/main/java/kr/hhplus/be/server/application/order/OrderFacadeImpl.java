package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import org.springframework.stereotype.Service;

@Service
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;

    public OrderFacadeImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {
        // 주문을 처리하고 결과를 반환
        Long orderId = orderService.createOrder(orderRequest);
        return new OrderResponse(201, "주문이 정상적으로 처리되었습니다.", new OrderResponse.OrderData(orderId));
    }
}
