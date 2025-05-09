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

    @BeforeEach
    void setUp() {
        // Redis 상태 초기화
        redisTemplate.delete("fair:order:queue");
        Set<String> keys = redisTemplate.keys("lock:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 테스트용 주문 및 포인트 초기화
        Long userId = 1L;

        Order testOrder = new Order(
                userId,
                null,
                false,
                500,
                "NOT_PAID",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        orderRepository.save(testOrder);
        this.testOrderId = testOrder.getId(); // 필드로 저장해서 테스트 메서드에서 사용

        Point testPoint = new Point(
                null,
                userId,
                1000,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        pointRepository.save(testPoint);
    }

    @Test
    @DisplayName("동일 사용자 동시에 여러 포인트 결제 요청 → 단 1개만 성공")
    void testSingleUserConcurrentPointPayment() throws InterruptedException, ExecutionException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executor.submit(() -> {
                try {
                    pointFacade.processPointPayment(testOrderId); // 필드 사용
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        executor.shutdown();
        Thread.sleep(5000);

        long successCount = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) successCount++;
        }

        assertThat(successCount).isEqualTo(1);
        log.info("성공한 결제 수: {}", successCount);
    }

    @Test
    @DisplayName("서로 다른 사용자 각각 포인트 결제 요청 → 모두 성공")
    void testMultipleUsersIndependentPointPayment() throws InterruptedException, ExecutionException {
        int userCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        List<Future<Boolean>> results = new ArrayList<>();

        // 사용자별 주문 및 포인트 세팅
        for (long userId = 100L; userId < 100L + userCount; userId++) {
            Order order = new Order(
                    userId,
                    null,
                    false,
                    500,
                    "NOT_PAID",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            orderRepository.save(order);

            Point point = new Point(
                    null,
                    userId,
                    1000,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            pointRepository.save(point);

            Long orderId = order.getId();

            results.add(executor.submit(() -> {
                try {
                    pointFacade.processPointPayment(orderId);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        executor.shutdown();
        Thread.sleep(5000);

        for (Future<Boolean> result : results) {
            assertThat(result.get()).isTrue(); // 전부 성공해야 함
        }

        log.info("모든 사용자 포인트 결제 성공");
    }

}
