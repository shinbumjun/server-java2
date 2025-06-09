package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.repository.CouponRepository;
import kr.hhplus.be.server.interfaces.coupon.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 데이터를 응답 형식으로 변환
public class CouponResponseService {

    private final CouponRepository couponRepository;

    public List<CouponResponse> convertToCouponResponse(List<UserCoupon> userCoupons) {
        return userCoupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                            .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));
                    return new CouponResponse(
                            coupon.getId(),
                            coupon.getCouponName(),
                            coupon.getDiscountType(),
                            coupon.getStartDate().toString(),
                            coupon.getEndDate().toString()
                    );
                })
                .collect(Collectors.toList());
    }
}
