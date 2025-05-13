package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.service.CouponResponseService;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.infra.lock.FairLockManager;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import kr.hhplus.be.server.interfaces.coupon.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class CouponFacadeImpl implements CouponFacade {

    private final CouponService couponService; // 쿠폰 검증 및 적용 서비스
    private final CouponResponseService couponResponseService; // UserCoupon을 API 응답 형식으로 변환하는 역할

    private final RedisLockManager redisLockManager; // Redis 기반 분산 락 관리자
    private final FairLockManager fairLockManager;   // 사용자 순서 보장용 Fair Lock 큐 관리자

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

    /*
        Redis 락만으로도 선착순 처리가 가능하다.
        Redis는 단일 스레드 기반으로 명령을 순차 처리하므로,
        락을 먼저 획득한 사용자가 쿠폰을 먼저 발급받게 된다.

        [수정 이유]
        - 공정 큐는 요청 순서를 보장하지만, 실무에서는 오히려 성능 저하를 유발할 수 있다.
        - Redis 분산 락만으로도 충분히 선착순 발급 처리를 할 수 있다.

        [최종 흐름]
        1. Redis 락 획득 시도
        2. 성공 시 → 쿠폰 발급
        3. 실패 시 → 선착순 실패 처리
     */
    @Override // 선착순 쿠폰 발급
    public CouponResult issueCoupon(Long userId, Long couponId) { // 쿠폰을 발급받는 사용자 ID, 발급받을 쿠폰 ID
        // 동일한 락 키 사용 → 쿠폰 단위
        String lockKey = "lock:coupon:" + couponId; // 동일한 쿠폰(같은 couponId)에 대해서는 하나만 락을 가질 수 있음 → 동시성 제어

        // 락 획득 시도 (예: 최대 5초 대기)
        if (!redisLockManager.lockWithRetry(lockKey, 5000)) { // 락은 쿠폰 하나당 하나만 존재
            log.warn("[락 실패] userId: {}, key: {}", userId, lockKey);
            throw new IllegalStateException("쿠폰 발급 락 획득 실패: " + lockKey);
        }

        log.info("[락 획득 성공] userId: {}, key: {}", userId, lockKey);

        try {
            // 락을 획득한 경우에만 발급 시도
            return couponService.issueCoupon(userId, couponId);
        } finally {
            // 락 해제
            redisLockManager.unlock(lockKey);
            log.info("[락 해제 및 큐 해제 완료] userId: {}", userId);
        }
    }
}
