package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class UserCoupon {
    @Id
    private Long id;
    private Long userId;
    private Long couponId;
    private Boolean isUsed;
    private String issuedAt;
    private String expiredAt;

    // 생성자
    public UserCoupon(Long userId, Long couponId, boolean isUsed, String issuedAt, String expiredAt, String endDate) {
        this.userId = userId;
        this.couponId = couponId;
        this.isUsed = isUsed;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
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
