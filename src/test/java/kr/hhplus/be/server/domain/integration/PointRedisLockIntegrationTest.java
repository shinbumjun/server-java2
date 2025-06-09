package kr.hhplus.be.server.domain.integration;

import kr.hhplus.be.server.application.point.PointFacade;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.PointRepository;
import kr.hhplus.be.server.domain.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class PointRedisLockIntegrationTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointRepository pointRepository;

    private Long testOrderId;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 피드백 : "같은 유저의 포인트는 한 번만 저장하고, for문에서는 주문을 여러 개 만들어서 동시 수행"
    @BeforeEach
    void setUp() {
        // Redis 초기화
        Set<String> keys = redisTemplate.keys("lock:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 포인트는 단 1회 저장 (userId: 1)
        Long userId = 1L;
        Point testPoint = new Point(null, userId, 1000, LocalDateTime.now(), LocalDateTime.now());
        pointRepository.save(testPoint);
    }

    @Test
    @DisplayName("동일 사용자 여러 주문 동시 결제 시도 - 포인트는 1개, 주문은 여러 개")
    void testSingleUserMultipleOrdersPayment() throws Exception {
        int orderCount = 5;
        Long userId = 1L;
        ExecutorService executor = Executors.newFixedThreadPool(orderCount);
        List<Long> orderIds = new ArrayList<>();

        // 동일 사용자에게 주문 5건 생성 (결제금액 300원씩)
        for (int i = 0; i < orderCount; i++) {
            Order order = new Order(userId, null, false, 300, "NOT_PAID", LocalDateTime.now(), LocalDateTime.now());
            orderRepository.save(order);
            orderIds.add(order.getId());
        }

        // 각 주문에 대해 동시에 포인트 결제 시도
        List<Future<Boolean>> results = new ArrayList<>();
        for (Long orderId : orderIds) {
            results.add(executor.submit(() -> {
                try {
                    pointFacade.processPointPayment(orderId);
                    log.info("✅ 결제 성공 - orderId: {}", orderId); // 성공 로그
                    return true;
                } catch (Exception e) {
                    log.warn("❌ 결제 실패 - orderId: {}, 사유: {}", orderId, e.getMessage()); // 실패 로그
                    return false;
                }
            }));
        }

        executor.shutdown();
        Thread.sleep(3000); // 쓰레드 종료 대기

        // 성공한 결제 수 집계
        long successCount = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) successCount++;
        }

        log.info("성공한 결제 수: {}", successCount);
        // 총 포인트 1000원, 주문당 300원 → 최대 3건까지 결제 성공 가능
        assertThat(successCount).isLessThanOrEqualTo(3);
    }


    @Test
    @DisplayName("서로 다른 사용자 각각 포인트 결제 요청 → 모두 성공")
    void testMultipleUsersIndependentPointPayment() throws InterruptedException, ExecutionException {
        int userCount = 3; // 사용자 3명
        ExecutorService executor = Executors.newFixedThreadPool(userCount); // 3개의 쓰레드로 동시에 실행
        List<Future<Boolean>> results = new ArrayList<>();

        // 3명의 사용자 각각에 대해 주문과 포인트 생성
        for (long userId = 100L; userId < 100L + userCount; userId++) {
            // 1. 주문 생성 (각 사용자당 하나씩)
            Order order = new Order(
                    userId,
                    null,
                    false,
                    500, // 결제 금액
                    "NOT_PAID",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            orderRepository.save(order); // DB 저장

            // 2. 포인트 생성 (각 사용자당 하나씩)
            Point point = new Point(
                    null,
                    userId,
                    1000, // 충분한 잔액
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            pointRepository.save(point); // DB 저장

            Long orderId = order.getId(); // 주문 ID 추출

            // 3. 주문별 결제 로직을 쓰레드로 실행
            results.add(executor.submit(() -> {
                try {
                    pointFacade.processPointPayment(orderId); // 포인트 결제 시도
                    return true; // 성공
                } catch (Exception e) {
                    return false; // 실패
                }
            }));
        }

        executor.shutdown(); // 쓰레드 종료 요청
        Thread.sleep(5000);  // 모든 작업 완료 대기 (간단한 대기 방식)

        // 모든 사용자 결제가 성공했는지 검증
        for (Future<Boolean> result : results) {
            assertThat(result.get()).isTrue(); // 하나라도 실패하면 테스트 실패
        }

        log.info("모든 사용자 포인트 결제 성공"); // 테스트 로그
    }


}
