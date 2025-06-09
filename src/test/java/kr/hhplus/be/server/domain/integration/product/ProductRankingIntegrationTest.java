package kr.hhplus.be.server.domain.integration.product;

import kr.hhplus.be.server.application.point.PointFacade;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class ProductRankingIntegrationTest {

    @Autowired private PointFacade pointFacade;

    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderProductRepository orderProductRepository;
    @Autowired private PointRepository pointRepository;

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
    @DisplayName("productId=101인 상품이 총 3건 주문(수량=2)되었을 때, Redis ZSet에 score=6.0이 누적된다")
    void should_AccumulateProductSalesToZSet_When_PaymentSucceeds() {
        // then
        String todayKey = "ranking:daily:" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Double score = redisTemplate.opsForZSet().score(todayKey, productId); // 101

        // 로그 : [Redis 랭킹 집계] key=ranking:daily:20250515, productId=101, quantity=3, TTL=4일
        assertThat(score).isNotNull();
        assertThat(score).isEqualTo(6.0);
    }

}
