package kr.hhplus.be.server.domain.point;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.repository.PointRepository;
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

    // 정책적 검증: 충전 금액 검증
    public void addAmount(int chargeAmount) {
        if (chargeAmount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        if (chargeAmount > 1000000) {
            throw new IllegalArgumentException("1회 충전 금액은 1,000,000원을 초과할 수 없습니다. 입력값 : " + chargeAmount);
        }

        long totalChargeAmount = this.balance + chargeAmount;
        if (totalChargeAmount > 5000000) {
            throw new IllegalArgumentException("누적 충전 금액은 5,000,000원을 초과할 수 없습니다. 현재 누적 충전 금액 : " + totalChargeAmount);
        }

        this.balance += chargeAmount;
    }

    // 포인트 결제 (차감) 메서드
    public void deductAmount(int amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다. 현재 잔액: " + this.balance + ", 결제 금액: " + amount);
        }
        this.balance -= amount;
    }

    // 포인트 잔액 검증 (결제 전에 사용)
    public void validateBalanceForPayment(int amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다. 현재 잔액: " + this.balance + ", 결제 금액: " + amount);
        }
    }

    // 사용자 ID로 포인트 조회 (Optional 없이 예외 던지기)
    public static Point findByUserIdOrThrow(Long userId, PointRepository pointRepository) {
        // 사용자 ID로 포인트 조회
        Point point = pointRepository.findByUserId(userId);
        if (point == null) {
            // 포인트가 존재하지 않으면 IllegalArgumentException을 던짐
            throw new IllegalArgumentException("해당 사용자의 포인트 정보를 찾을 수 없습니다.");
        }
        return point;
    }

}
