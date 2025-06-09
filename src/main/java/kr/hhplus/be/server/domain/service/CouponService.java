package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.coupon.CouponResult;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;

import java.util.List;

public interface CouponService {
    List<UserCoupon> getUserCoupons(Long userId);
    CouponResult issueCoupon(Long userId, Long couponId);

    // 쿠폰 검증 및 적용
    void applyCoupon(UserCoupon userCoupon, Order order);

    // 쿠폰 취소
    // void cancelCouponUsage(Long userCouponId);

    // 쿠폰 복구
    void revertCouponIfUsed(Long userCouponId);

    Coupon findCouponOrThrow(Long couponId);
}
