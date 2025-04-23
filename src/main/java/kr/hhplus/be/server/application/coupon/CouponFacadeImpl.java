package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class CouponFacadeImpl implements CouponFacade {

    private final CouponService couponService;

    @Override
    public CouponResult getUserCoupons(Long userId) {
        return couponService.getUserCoupons(userId);
    }

    @Override // 선착순 쿠폰 발급
    public CouponResult issueCoupon(Long userId, Long couponId) { // 쿠폰을 발급받는 사용자 ID, 발급받을 쿠폰 ID
        return couponService.issueCoupon(userId, couponId);
    }
}
