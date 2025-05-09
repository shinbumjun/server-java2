package kr.hhplus.be.server.domain.integration;

import kr.hhplus.be.server.application.coupon.CouponFacade;
import kr.hhplus.be.server.application.coupon.CouponResult;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.repository.CouponRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class CouponRedisLockIntegrationTest {

    @Autowired
    private CouponFacade couponFacade;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Long testCouponId;
    private final Long userId = 1L;
    private final int COUPON_STOCK = 3;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setupAndClearRedis() {
        // Redis: 큐 및 락 키 초기화
        redisTemplate.delete("fair:order:queue");
        Set<String> keys = redisTemplate.keys("lock:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // DB: 기존 쿠폰 및 사용자 쿠폰 초기화
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();

        // 테스트용 쿠폰 생성
        Coupon coupon = new Coupon(
                null,
                "동시성 테스트 쿠폰",
                "PERCENT",
                BigDecimal.valueOf(10),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                3 // 재고 3
        );

        Coupon saved = couponRepository.save(coupon);
        testCouponId = saved.getId();
    }

    @Test
    @DisplayName("동일 유저가 동시에 여러 번 쿠폰 발급 시도 - 단 한 번만 성공")
    void testSingleUserMultipleIssueAttempts() throws InterruptedException, ExecutionException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<CouponResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int attempt = i + 1;
            futures.add(executorService.submit(() -> {
                CouponResult result = couponFacade.issueCoupon(userId, testCouponId);
                log.info("[시도 {}] 응답 코드: {}, 상세 메시지: {}", attempt, result.getCode(), result.getDetail());
                return result;
            }));
        }

        executorService.shutdown();
        Thread.sleep(1000);  // 모든 쓰레드가 종료되길 대기

        int successCount = 0;
        int conflictCount = 0;

        for (int i = 0; i < futures.size(); i++) {
            CouponResult result = futures.get(i).get();
            if (result.getCode() == 201) {
                successCount++;
                log.info("[결과 {}] 성공", i + 1);
            } else if (result.getCode() == 409) {
                conflictCount++;
                log.info("[결과 {}] 실패 - {}", i + 1, result.getDetail());
            }
        }

        log.info("성공 수: {}", successCount);
        log.info("실패 수: {}", conflictCount);

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("여러 유저가 동시에 쿠폰 발급 시도 - 재고 수량만큼만 정확히 선착순 성공 (정확한 선착순)")
    void testMultipleUsersWithLimitedStock() throws InterruptedException, ExecutionException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<CouponResult>> futures = new ArrayList<>();

        List<Long> userIds = List.of(1L, 2L, 3L, 4L, 5L);

        for (Long userId : userIds) {
            futures.add(executorService.submit(() -> couponFacade.issueCoupon(userId, testCouponId)));
        }

        executorService.shutdown();
        Thread.sleep(1000);  // 모든 쓰레드가 종료되길 대기

        int successCount = 0;
        int conflictCount = 0;

        for (int i = 0; i < futures.size(); i++) {
            CouponResult result = futures.get(i).get();
            if (result.getCode() == 201) {
                successCount++;
                log.info("[결과 {}] 성공 - userId: {}", i + 1, result.getUserId());
            } else if (result.getCode() == 409) {
                conflictCount++;
                log.info("[결과 {}] 실패 - userId: {}, 메시지: {}", i + 1, result.getUserId(), result.getDetail());
            }
        }

        log.info("성공 수: {}", successCount);
        log.info("실패 수: {}", conflictCount);

        assertThat(successCount).isEqualTo(COUPON_STOCK);
        assertThat(conflictCount).isEqualTo(threadCount - COUPON_STOCK);
    }
}
