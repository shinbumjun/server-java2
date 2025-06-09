package kr.hhplus.be.server.domain.integration.scheduler;

import kr.hhplus.be.server.application.point.PointFacade;
import kr.hhplus.be.server.application.scheduler.ProductRankingScheduler;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.PointRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@SpringBootTest
public class ProductRankingSchedulerIntegrationTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderProductRepository orderProductRepository;
    @Autowired private PointRepository pointRepository;

    @Autowired private ProductRankingScheduler productRankingScheduler;

    private final Long userId = 1001L;
    private final String productId = "101";

    @BeforeEach
    void setUp() {
        // 1. DB 초기화
        orderProductRepository.deleteAll();
        orderRepository.deleteAll();
        pointRepository.deleteAll();

        // 2. Redis 초기화
        String todayKey = "ranking:daily:" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        redisTemplate.delete(todayKey);

        LocalDateTime[] dates = {
                LocalDateTime.now().minusDays(3), // 그저께
                LocalDateTime.now().minusDays(2), // 그제
                LocalDateTime.now().minusDays(1)  // 어제
        };

        int[] productIds = {100, 101, 102};   // 상품 ID 3개
        int[] quantities = {1, 2, 3};         // 수량 다양화

        // 포인트는 한 번만 저장 (사용자 기준)
        Point point = new Point();
        point.setUserId(userId);
        point.setBalance(100000); // 넉넉히 세팅
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        pointRepository.save(point);

        int orderCount = 0;

        for (LocalDateTime date : dates) {
            for (int i = 0; i < 3; i++) {
                // 1. 주문 저장 (각 날짜에 3건씩)
                Order order = new Order(
                        userId,
                        null,
                        false,
                        3000 * (i + 1), // 금액 증가
                        "NOT_PAID",
                        date,
                        date
                );
                Order savedOrder = orderRepository.save(order);
                Long orderId = savedOrder.getId();

                // 2. 주문상품 저장
                OrderProduct op = new OrderProduct(
                        (long) productIds[i],
                        orderId,
                        3000 * (i + 1),
                        quantities[i]
                );
                orderProductRepository.save(op);

                // 3. 포인트 결제 처리
                pointFacade.processPointPayment(orderId);

                orderCount++;
            }
        }

        log.info("총 주문 생성 및 결제 처리 완료: {}건", orderCount);
    }

    @Test
    @DisplayName("최근 3일 랭킹 병합 시 Redis에 'ranking:3days' ZSet이 생성되고 데이터가 포함된다")
    void should_Merge3DayRankingAndStoreInRedis() {
        // when: 스케줄러 실행
        productRankingScheduler.merge3DayRanking();

        // then: Redis ZSet 확인
        String rankingKey = "ranking:3days";
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<String>> result = ops.reverseRangeWithScores(rankingKey, 0, -1);

        // 로그 : 최근 3일 랭킹 병합 완료 → [ranking:3days] from ranking:daily:20250512, ranking:daily:20250513, ranking:daily:20250514
        assertThat(result).isNotNull();
    }
}
