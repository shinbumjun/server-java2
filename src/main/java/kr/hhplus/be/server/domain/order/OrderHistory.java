package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class OrderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String event;            // e.g. "STOCK_DEDUCTED", "COUPON_APPLIED"
    private LocalDateTime createdAt; // 이벤트 발생 시각

    protected OrderHistory() {}

    public OrderHistory(Long orderId, String event) {
        this.orderId   = orderId;
        this.event     = event;
        this.createdAt = LocalDateTime.now();
    }

    // getters...
}