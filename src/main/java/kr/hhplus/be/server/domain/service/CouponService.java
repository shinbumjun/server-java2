package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.coupon.CouponResult;
import kr.hhplus.be.server.domain.coupon.UserCoupon;

import java.util.List;

public interface CouponService {
    List<UserCoupon> getUserCoupons(Long userId);
    CouponResult issueCoupon(Long userId, Long couponId);
}
