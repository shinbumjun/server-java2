package kr.hhplus.be.server.domain.integration.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import kr.hhplus.be.server.application.scheduler.ProductBestCacheScheduler;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.infra.redis.CacheService;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductBestCacheSchedulerTest {

    private static final String KEY = "best:daily";

    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderProductRepository orderProductRepository;

    @Autowired private CacheService cacheService;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ProductBestCacheScheduler scheduler;

    @BeforeEach
    void setup() {
        redisTemplate.delete(KEY);

        // 테스트용 데이터 삽입
        Product p1 = productRepository.save(new Product(null, "상품A", "설명", 1000, 10, now(), now()));
        Product p2 = productRepository.save(new Product(null, "상품B", "설명", 2000, 5, now(), now()));

        Order order = orderRepository.save(new Order(1L, null, false, 3000, "PAID", now(), now()));

        orderProductRepository.save(new OrderProduct(p1.getId(), order.getId(), 2000, 2));
        orderProductRepository.save(new OrderProduct(p2.getId(), order.getId(), 1000, 1));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Test
    @DisplayName("스케줄러가 실행되면 Redis에 일간 인기상품이 저장된다")
    void testSchedulerStoresDataInRedis() {
        // when - 자정에 실행되는 스케줄러 메서드 수동 호출
        scheduler.cacheDailyBestProducts();

        // then - 캐시에 값이 들어갔는지 확인
        List<ProductBestDto> cached = cacheService.get(KEY, new TypeReference<>() {});
        assertThat(cached).isNotNull();
        assertThat(cached).isNotEmpty();

        // 로그 출력 (선택)
        cached.forEach(dto ->
                System.out.println("✅ 캐시 저장됨: " + dto.getName() + ", 판매량: " + dto.getSales())
        );
    }
}
