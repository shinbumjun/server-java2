package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // 쿠폰을 조회하는 메서드
}
