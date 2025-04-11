package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.coupon.CouponResult;

public interface CouponService {
    CouponResult getUserCoupons(Long userId);
    CouponResult issueCoupon(Long userId, Long couponId);
}
