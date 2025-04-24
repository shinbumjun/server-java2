package kr.hhplus.be.server.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;  // 상품명

    @Column(name = "description")
    private String description;  // 상품 상세정보

    @Column(name = "price", nullable = false)
    private Integer price;  // 상품 가격

    @Column(name = "stock", nullable = false)
    private Integer stock;  // 상품 재고

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 생성일

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;  // 업데이트일

    // 재고가 부족한지 확인
    public void validateStock(Integer quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException("상품의 재고가 부족합니다.");
        }
    }

    // 재고 차감
    public void reduceStock(Integer quantity) {
        this.stock -= quantity;
    }
}
