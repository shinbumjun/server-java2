package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);  // 이미 발급된 쿠폰 여부 확인

    List<UserCoupon> findByUserId(Long userId);
    // 다른 필요한 메서드 추가 가능
}
