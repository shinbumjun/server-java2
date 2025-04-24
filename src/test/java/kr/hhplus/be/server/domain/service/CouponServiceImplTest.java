package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.coupon.CouponResult;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.repository.CouponRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl(couponRepository, userCouponRepository);
    }

//    @Test
//    @DisplayName("쿠폰 조회: 사용자 쿠폰 조회 성공")
//    void getUserCoupons_Success() {
//        // Given: 쿠폰 조회
//        Long userId = 1L;
//        CouponResult expected = new CouponResult(200, "요청이 정상적으로 처리되었습니다.", userId, "coupons", null);
//
//        // When: 쿠폰 조회
//        CouponResult result = couponService.getUserCoupons(userId);
//
//        // Then: 성공적으로 처리되었는지 확인
//        assertEquals(expected.getCode(), result.getCode());
//        assertEquals(expected.getMessage(), result.getMessage());
//    }

    @Test
    @DisplayName("쿠폰 발급: 선착순 쿠폰 발급 성공")
    void issueCoupon_Success() {
        // Given: 유효한 쿠폰
        Long userId = 1L;
        Long couponId = 1L;
        // Coupon coupon = new Coupon(couponId, "10% 할인 쿠폰", "RATE", 10.0, "2025-08-01", "2025-08-31", 10);
        // Coupon coupon = new Coupon(couponId, "10% 할인 쿠폰", "RATE", BigDecimal.valueOf(10.0), LocalDate.parse("2025-08-01"), LocalDate.parse("2025-08-31"), 10);
        Coupon coupon = new Coupon(couponId, "10% 할인 쿠폰", "RATE", BigDecimal.valueOf(10.0), LocalDate.parse("2025-08-01"), LocalDate.parse("2025-08-31"), 10);

        when(couponRepository.findById(couponId)).thenReturn(java.util.Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);  // 발급된 적 없는 상태

        // When: 쿠폰 발급 요청
        CouponResult result = couponService.issueCoupon(userId, couponId);

        // Then: 발급 성공 여부 확인
        assertEquals(201, result.getCode());
        assertEquals("요청이 정상적으로 처리되었습니다.", result.getMessage());
    }

    @Test
    @DisplayName("쿠폰 발급: 쿠폰 재고 부족으로 발급 실패")
    void issueCoupon_InsufficientStock() {
        // Given: 재고가 부족한 쿠폰
        Long userId = 1L;
        Long couponId = 1L;
        // Coupon coupon = new Coupon(couponId, "10% 할인 쿠폰", "RATE", 10.0, "2025-08-01", "2025-08-31", 0);  // 재고 0
        Coupon coupon = new Coupon(couponId, "10% 할인 쿠폰", "RATE", BigDecimal.valueOf(10.0), LocalDate.parse("2025-08-01"), LocalDate.parse("2025-08-31"), 10);

        when(couponRepository.findById(couponId)).thenReturn(java.util.Optional.of(coupon));

        // When: 쿠폰 발급 요청
        CouponResult result = couponService.issueCoupon(userId, couponId);

        // Then: 재고 부족으로 발급 실패
        assertEquals(409, result.getCode());
        assertEquals("비즈니스 정책을 위반한 요청입니다.", result.getMessage());
        assertEquals("쿠폰의 잔여 수량이 부족합니다.", result.getDetail());
    }

    @Test
    @DisplayName("쿠폰 발급: 이미 발급된 쿠폰으로 발급 실패")
    void issueCoupon_AlreadyIssued() {
        // Given: 이미 발급된 쿠폰
        Long userId = 1L;
        Long couponId = 1L;
        // Coupon coupon = new Coupon(couponId, "10% 할인 쿠폰", "RATE", 10.0, "2025-08-01", "2025-08-31", 10);
        Coupon coupon = new Coupon(couponId, "10% 할인 쿠폰", "RATE", BigDecimal.valueOf(10.0), LocalDate.parse("2025-08-01"), LocalDate.parse("2025-08-31"), 10);

        when(couponRepository.findById(couponId)).thenReturn(java.util.Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(true);  // 이미 발급된 상태

        // When: 쿠폰 발급 요청
        CouponResult result = couponService.issueCoupon(userId, couponId);

        // Then: 이미 발급된 쿠폰으로 실패
        assertEquals(409, result.getCode());
        assertEquals("비즈니스 정책을 위반한 요청입니다.", result.getMessage());
        assertEquals("이미 쿠폰을 발급 받았습니다.", result.getDetail());
    }
}
