package kr.hhplus.be.server.interfaces.order;

import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public OrderResponse createOrder(@RequestBody OrderRequest orderRequest) {
        // 주문 처리 요청을 주문 파사드에 전달
        OrderResponse result = orderFacade.createOrder(orderRequest);
        return result;  // 결과를 반환
    }
}
