package kr.hhplus.be.server.interfaces.order;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private int code;  // HTTP 상태 코드
    private String message;  // 상태 메시지
    private OrderData data;  // 주문 ID

    @Getter
    @AllArgsConstructor
    public static class OrderData {
        private Long orderId;  // 주문 ID
    }
}
