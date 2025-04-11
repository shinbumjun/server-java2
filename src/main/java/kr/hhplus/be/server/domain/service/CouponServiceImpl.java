package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.coupon.CouponResult;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.repository.CouponRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import org.springframework.stereotype.Service;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponServiceImpl(CouponRepository couponRepository, UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @Override
    public CouponResult getUserCoupons(Long userId) {
        // 사용자 쿠폰 조회 로직
        // 예: userCouponRepository.findByUserId(userId);
        // 이를 CouponResult에 담아서 반환
        return new CouponResult(200, "요청이 정상적으로 처리되었습니다.", userId, "coupons", null); // 임시값
    }

    @Override
    public CouponResult issueCoupon(Long userId, Long couponId) {
        // 선착순 쿠폰 발급 로직
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));

        // 쿠폰 재고 확인
        if (coupon.getStock() <= 0) {
            return new CouponResult(409, "비즈니스 정책을 위반한 요청입니다.", userId, "issued", "쿠폰의 잔여 수량이 부족합니다.");
        }

        // 이미 발급된 쿠폰 확인
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            return new CouponResult(409, "비즈니스 정책을 위반한 요청입니다.", userId, "issued", "이미 쿠폰을 발급 받았습니다.");
        }

        // 발급된 쿠폰 처리 (발급 로직)
        coupon.setStock(coupon.getStock() - 1);  // 재고 차감
        couponRepository.save(coupon);

        // UserCoupon 생성 및 저장 (발급 처리)
        UserCoupon userCoupon = new UserCoupon(userId, couponId, false, coupon.getCouponName(), coupon.getStartDate(), coupon.getEndDate());
        userCouponRepository.save(userCoupon);

        return new CouponResult(201, "요청이 정상적으로 처리되었습니다.", userId, "issued", null);
    }
}
