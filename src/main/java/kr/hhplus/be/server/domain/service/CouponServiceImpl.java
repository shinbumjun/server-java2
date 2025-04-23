package kr.hhplus.be.server.domain.service;


import kr.hhplus.be.server.application.coupon.CouponResult;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.repository.CouponRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override
    public CouponResult getUserCoupons(Long userId) {
        // 사용자 쿠폰 조회 로직
        // 예: userCouponRepository.findByUserId(userId);
        // 이를 CouponResult에 담아서 반환
        return new CouponResult(200, "요청이 정상적으로 처리되었습니다.", userId, "coupons", null); // 임시값
    }

    @Override
    public CouponResult issueCoupon(Long userId, Long couponId) {
        // 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));

        // 쿠폰 재고 확인 및 유효성 검증
        coupon.validateStock();  // 재고가 부족한 경우 예외가 발생
        coupon.validateDates();  // 유효기간 검증

        // 이미 발급된 쿠폰 확인
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            return new CouponResult(409, "비즈니스 정책을 위반한 요청입니다.", userId, "issued", "이미 쿠폰을 발급 받았습니다.");
        }

        // 쿠폰 발급 처리
        coupon.decreaseStock();  // 재고 차감
        couponRepository.save(coupon);  // 쿠폰 상태 저장

        // UserCoupon 생성 및 저장 (발급 처리)
        // UserCoupon userCoupon = new UserCoupon(userId, couponId, false, coupon.getCouponName(), coupon.getStartDate(), coupon.getEndDate());
        // UserCoupon 생성 (저장은 UserCouponRepository에 위임), 쿠폰 이름 제외 -> UserCoupon 엔티티의 생성자에서 couponName과 같은 Coupon 엔티티에만 존재하는 필드를 받는 것이 문제
        UserCoupon userCoupon = new UserCoupon(userId, couponId, false, coupon.getStartDate(), coupon.getEndDate());
        userCoupon.validateUsage();  // 쿠폰 사용 여부 검증
        userCouponRepository.save(userCoupon);

        return new CouponResult(201, "요청이 정상적으로 처리되었습니다.", userId, "issued", null);
    }
}
