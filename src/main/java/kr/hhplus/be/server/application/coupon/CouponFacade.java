package kr.hhplus.be.server.application.coupon;

public interface CouponFacade {
    CouponResult getUserCoupons(Long userId);
    CouponResult issueCoupon(Long userId, Long couponId);
}
