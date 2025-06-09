package kr.hhplus.be.server.interfaces.order;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderRequest {
    private Long userId;  // 사용자 ID
    private Long userCouponId;  // 쿠폰 ID
    private List<OrderItem> orderItems;  // 주문 항목

    @Getter
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;  // 상품 ID
        private Integer quantity;  // 수량
    }
}
