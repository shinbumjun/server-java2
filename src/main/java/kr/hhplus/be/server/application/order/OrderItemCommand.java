package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.interfaces.order.OrderRequest;

// DTO → 도메인 명령으로 변환된 불변 객체
public record OrderItemCommand(Long productId, Integer quantity) { // 상품 ID, 수량

    // DTO → Command 변환
    public static OrderItemCommand from(OrderRequest.OrderItem dto) {
        return new OrderItemCommand(dto.getProductId(), dto.getQuantity());
    }
}
