package kr.hhplus.be.server.domain.service;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import kr.hhplus.be.server.domain.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {
    private final UserCouponRepository userCouponRepository;

    @Override
    public UserCoupon getUserCoupon(Long couponId) {
        // 예외 조회(Repository) → 서비스 계층
        return userCouponRepository.findById(couponId)
                .orElseThrow(() -> new EntityNotFoundException("UserCoupon not found: " + couponId));
    }
}
