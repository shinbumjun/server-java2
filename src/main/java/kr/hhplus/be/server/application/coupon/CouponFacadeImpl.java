package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.service.CouponResponseService;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.infra.lock.FairLockManager;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import kr.hhplus.be.server.interfaces.coupon.CouponResponse;
import kr.hhplus.be.server.application.coupon.CouponRequestEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class CouponFacadeImpl implements CouponFacade {

    private final CouponService couponService; // 쿠폰 검증 및 적용 서비스
    private final CouponResponseService couponResponseService; // UserCoupon을 API 응답 형식으로 변환하는 역할

    private final KafkaTemplate<String, CouponRequestEvent> kafkaTemplate; // 카프카
    // private final RedisLockManager redisLockManager; // Redis 기반 분산 락 관리자
    // private final FairLockManager fairLockManager;   // 사용자 순서 보장용 Fair Lock 큐 관리자
    // private final RedisTemplate redisTemplate;

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

        [카프카 도입]
        1. 락을 전부 삭제
        2. 발급 요청 이벤트만 발행
     */
    @Override
    public CouponResult issueCoupon(Long userId, Long couponId) {
        // 1. (선택) 가벼운 사전검증만 가능
        // if (couponId == null) { ... }

        // 2. Kafka로 이벤트 발행
        CouponRequestEvent event = new CouponRequestEvent(userId, couponId);
        kafkaTemplate.send("coupon-publish-request", // 토픽 (어떤 토픽에 보낼지)
                couponId.toString(),                      // 메시지 키 (파티션 결정에 사용)
                event);                                   // 페이로드 (메시지 본문)

        // 3. 즉시 “202 Accepted” 응답
        return new CouponResult(
                202,
                "발급 요청이 접수되었습니다.",
                userId,
                "couponId",
                couponId
        );
    }
//    @Override // 선착순 쿠폰 발급
//    public CouponResult issueCoupon(Long userId, Long couponId) { // 쿠폰을 발급받는 사용자 ID, 발급받을 쿠폰 ID
//
//        // 락 키: 쿠폰 발급 동시성 제어용
//        String lockKey = "lock:coupon:" + couponId; // 동일한 쿠폰(같은 couponId)에 대해서는 하나만 락을 가질 수 있음 → 동시성 제어
//
//        // 락 획득 시도 (예: 최대 5초 대기)
//        if (!redisLockManager.lockWithRetry(lockKey, 5000)) { // 락은 쿠폰 하나당 하나만 존재
//            log.warn("[락 실패] userId: {}, key: {}", userId, lockKey);
//            throw new IllegalStateException("쿠폰 발급 락 획득 실패: " + lockKey);
//        }
//
//        log.info("[락 획득 성공] userId: {}, key: {}", userId, lockKey);
//
//        // Redis 락 획득 → Redis Set 중복 체크 → Redis Set 수량 확인 → DB 발급 처리 → 락 해제
//        try {
//            // 1. Redis Set 키: 해당 쿠폰에 대해 발급된 사용자 목록 추적용
//            String issuedSetKey = "coupon:issued:" + couponId;
//
//            // 2. 중복 발급 제어: 이미 발급된 사용자면 SADD 결과가 false (0)
//            Boolean added = redisTemplate.opsForSet().add(issuedSetKey, userId.toString()) == 1;
//            // TTL 설정
//            redisTemplate.expire(issuedSetKey, Duration.ofDays(1));  // 하루 뒤 자동 만료
//
//            if (!added) {
//                // 이미 발급한 사용자
//                log.warn("[중복 발급 차단] userId: {}, couponId: {}", userId, couponId);
//                return new CouponResult(409, "이미 쿠폰을 발급 받았습니다.", userId, "issued", "중복 요청");
//            }
//
//            // 3. 선착순 재고 초과 제어: Redis Set 크기 > 쿠폰 재고면 차단
//            Long totalIssued = redisTemplate.opsForSet().size(issuedSetKey);
//
//            Coupon coupon = couponService.findCouponOrThrow(couponId);  // 재고 조회용 메서드 추가 필요
//            if (totalIssued > coupon.getStock()) {
//                log.warn("[재고 초과] userId: {}, couponId: {}, issued: {}", userId, couponId, totalIssued);
//                return new CouponResult(409, "쿠폰 재고가 소진되었습니다.", userId, "issued", "재고 초과");
//            }
//
//            // 4. 실제 발급 처리(DB 저장)
//            return couponService.issueCoupon(userId, couponId);
//        } finally {
//            // 5. 락 해제
//            redisLockManager.unlock(lockKey);
//            log.info("[락 해제 및 큐 해제 완료] userId: {}", userId);
//        }
//    }
}
