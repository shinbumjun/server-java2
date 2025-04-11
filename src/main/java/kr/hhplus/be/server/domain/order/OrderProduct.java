package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_product")
@Getter
@Setter
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;  // 상품 ID

    @Column(name = "orders_id", nullable = false)
    private Long ordersId;  // 주문 ID (주문과 연관)

    @Column(name = "amount", nullable = false)
    private int amount;  // 주문 금액 (상품 가격 * 수량)

    @Column(name = "quantity", nullable = false)
    private int quantity;  // 주문 수량

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 생성자
    public OrderProduct(Long productId, Long ordersId, int amount, int quantity) {
        this.productId = productId;
        this.ordersId = ordersId;
        this.amount = amount;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();  // 생성 시간
        this.updatedAt = LocalDateTime.now();  // 업데이트 시간
    }

    // 기본 생성자 (JPA 요구사항)
    public OrderProduct() {}
}
