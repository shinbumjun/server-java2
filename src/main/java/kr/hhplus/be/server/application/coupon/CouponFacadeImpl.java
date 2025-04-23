package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.service.CouponResponseService;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.interfaces.coupon.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class CouponFacadeImpl implements CouponFacade {

    private final CouponService couponService;
    private final CouponResponseService couponResponseService;

    @Override // 사용자 보유 쿠폰 조회
    public CouponResult getUserCoupons(Long userId) {
        // 1. 사용자 쿠폰 조회
        List<UserCoupon> userCoupons = couponService.getUserCoupons(userId);

        // 2. 쿠폰이 없으면 결과 반환
        if (userCoupons.isEmpty()) {
            return new CouponResult(200, "사용자 보유 쿠폰이 없습니다.", userId, "coupons", null);
        }

        // 3. 쿠폰 응답 형식으로 변환
        List<CouponResponse> couponResponses = couponResponseService.convertToCouponResponse(userCoupons);

        // 4. 최종 결과 반환
        return new CouponResult(200, "요청이 정상적으로 처리되었습니다.", userId, "coupons", couponResponses);
    }

    @Override // 선착순 쿠폰 발급
    public CouponResult issueCoupon(Long userId, Long couponId) { // 쿠폰을 발급받는 사용자 ID, 발급받을 쿠폰 ID
        return couponService.issueCoupon(userId, couponId);
    }
}
