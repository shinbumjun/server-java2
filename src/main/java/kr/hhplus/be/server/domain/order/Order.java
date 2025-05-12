package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order { // 주문

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long userCouponId;
    private boolean isCouponApplied;
    private int totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order(Long userId, Long userCouponId, boolean isCouponApplied, int totalAmount, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.isCouponApplied = isCouponApplied;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validateOrder(List<OrderRequest.OrderItem> orderItems, ProductRepository productRepository) {
        // 재고 부족 체크
        for (OrderRequest.OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
//            if (item.getQuantity() > product.getStock()) {
//                throw new IllegalArgumentException("상품의 재고가 부족합니다.");
//            }
        }
    }

    // 주문 상태 검증 (EXPIRED, PAID면 결제 불가 예외 발생)
    public void validatePayable() {
        if ("EXPIRED".equals(this.status)) {
            throw new IllegalStateException("주문 상태가 EXPIRED입니다. 결제가 불가능합니다.");
        }
        if ("PAID".equals(this.status)) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다.");
        }
    }

    // 주문 상태를 PAID로 변경
    public void updateStatusToPaid() {
        if ("EXPIRED".equals(this.status)) {
            throw new IllegalStateException("EXPIRED 상태의 주문은 결제 완료로 변경할 수 없습니다.");
        }
        this.status = "PAID";
    }

    public void updateStatusToFail() {
        this.status = "FAIL"; // 주문 취소
    }
}
