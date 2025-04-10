package kr.hhplus.be.server.domain.point;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "point")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)  // 외래키 컬럼
    private Long userId;  // User 엔티티를 참조하는 외래키

    @Column(name = "balance", nullable = false)
    private Integer balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 불변성 검증 메서드
    public void validate() {
        if (userId == null || balance == null || balance < 0) {
            throw new IllegalStateException("유효하지 않은 포인트 상태입니다.");
        }
    }

    // 정책적 검증
    public void addAmount(int chargeAmount) {
        // 1회 충전 금액 검증
        if (chargeAmount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        if (chargeAmount > 1000000) {
            throw new IllegalArgumentException("1회 충전 금액은 1,000,000원을 초과할 수 없습니다. 입력값 : " + chargeAmount);
        }

        // 누적 충전 금액 검증
        long totalChargeAmount = this.balance + chargeAmount; // 기존 잔액 + 충전 금액
        if (totalChargeAmount > 5000000) {
            throw new IllegalArgumentException("누적 충전 금액은 5,000,000원을 초과할 수 없습니다. 현재 누적 충전 금액 : " + totalChargeAmount);
        }

        // 충전 금액이 검증을 통과하면 잔액을 업데이트
        this.balance += chargeAmount;
    }
}

