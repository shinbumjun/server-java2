package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 설정
    private Long id; // 사용자 쿠폰 ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 유저 ID

    @Column(name = "coupon_id", nullable = false)
    private Long couponId; // 쿠폰 ID

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed; // 쿠폰 사용 여부

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt; // 유효기간 시작일

    @Column(name = "expired_at", nullable = false)
    private LocalDate expiredAt; // 유효기간 종료일

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 업데이트일

    // 생성자
    public UserCoupon(Long userId, Long couponId, boolean isUsed, LocalDate issuedAt, LocalDate expiredAt) {
        this.userId = userId;
        this.couponId = couponId;
        this.isUsed = isUsed;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserCoupon(Long userId, Long couponId, boolean b, String couponName, LocalDate startDate, LocalDate endDate) {
    }

    // 정책 검증: 이미 발급된 쿠폰을 다시 발급할 수 없도록 처리
    public void validateDuplicateIssue(UserCouponRepository userCouponRepository) {
        if (userCouponRepository.existsByUserIdAndCouponId(this.userId, this.couponId)) {
            throw new IllegalStateException("이미 쿠폰을 발급 받았습니다.");
        }
    }

    // 유효성 검증: 쿠폰 사용 여부 확인
    public void validateUsage() {
        if (this.isUsed) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
    }
}
