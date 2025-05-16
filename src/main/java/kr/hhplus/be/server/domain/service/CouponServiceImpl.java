package kr.hhplus.be.server.domain.service;


import kr.hhplus.be.server.application.coupon.CouponResult;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.repository.CouponRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import kr.hhplus.be.server.interfaces.coupon.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override // 사용자 보유 쿠폰 조회
    public List<UserCoupon> getUserCoupons(Long userId) {
        return userCouponRepository.findByUserId(userId);
    }


    @Override // 선착순 쿠폰 발급
    public CouponResult issueCoupon(Long userId, Long couponId) {
        // 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));

        // 쿠폰 재고 확인 및 유효성 검증
        try {
            coupon.validateStock(); // 재고가 부족한 경우 예외가 발생
        } catch (IllegalStateException e) {
            return new CouponResult(409, "비즈니스 정책을 위반한 요청입니다.", userId, "issued", e.getMessage());
        }
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

    @Override // 쿠폰 검증 및 적용
    public void applyCoupon(Long userId, Long userCouponId) {
        // 1. 사용자 쿠폰 조회
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));

        // 2. 쿠폰이 유효한지 검증
        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));

        // 3. 쿠폰 사용 여부 검증
        userCoupon.validateUsage(); // 쿠폰이 이미 사용되었는지 확인 (엔티티 내 메서드)

        // 4. 쿠폰의 유효 기간 검증
        coupon.validateDates(); // 쿠폰이 유효 기간 내에 있는지 확인 (엔티티 내 메서드)

        // 5. 쿠폰을 적용하여 주문에 반영 -> 할인 적용은 쿠폰 검증에서 이루어지지 않는다
        // 예: 할인을 적용하는 로직 (쿠폰의 할인 금액을 주문에 반영)
        // applyDiscountToOrder() 메서드 등을 호출하여 할인 적용

        // 6. 쿠폰 사용 처리 (쿠폰을 사용했다고 표시)
        userCoupon.setIsUsed(true); // 쿠폰 사용 처리
        userCouponRepository.save(userCoupon); // 저장
    }

    // 쿠폰 복구
    @Override
    public void revertCouponIfUsed(Long userCouponId) {
        if (userCouponId == null) return; // 주문에 쿠폰이 적용된 경우

        UserCoupon userCoupon = userCouponRepository.findById(userCouponId).orElse(null);

        if (userCoupon != null && Boolean.TRUE.equals(userCoupon.getIsUsed())) { // 쿠폰이 실제로 사용된 경우에만 사용 취소
            userCoupon.setIsUsed(false);
            userCouponRepository.save(userCoupon);
            log.info("✅ 쿠폰 복구 완료 - couponId: {}", userCoupon.getCouponId());
        } else {
            log.info("❌ 쿠폰이 사용되지 않았거나 이미 복구된 상태입니다. couponId: {}", userCoupon.getCouponId());
        }
    }

    @Override
    public Coupon findCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));
    }

//    @Override
//    public void cancelCouponUsage(Long userCouponId) {
//        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
//                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다."));
//
//        userCoupon.setIsUsed(false);  // 쿠폰 사용 상태를 false로 되돌림
//        userCouponRepository.save(userCoupon); // DB에 저장
//    }


}
