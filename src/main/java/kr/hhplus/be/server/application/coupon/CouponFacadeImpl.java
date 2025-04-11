package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.service.CouponService;
import org.springframework.stereotype.Service;

@Service
public class CouponFacadeImpl implements CouponFacade {

    private final CouponService couponService;

    public CouponFacadeImpl(CouponService couponService) {
        this.couponService = couponService;
    }

    @Override
    public CouponResult getUserCoupons(Long userId) {
        return couponService.getUserCoupons(userId);
    }

    @Override
    public CouponResult issueCoupon(Long userId, Long couponId) {
        return couponService.issueCoupon(userId, couponId);
    }
}
