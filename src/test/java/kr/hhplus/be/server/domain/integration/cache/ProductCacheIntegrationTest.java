package kr.hhplus.be.server.domain.integration.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCacheIntegrationTest {

    private static final String KEY = "best:daily";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderProductRepository orderProductRepository;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @BeforeEach
    void setup() {
        redisTemplate.delete(KEY); // 캐시 초기화

        for (int i = 1; i <= 3; i++) {
            // 1. 샘플 상품
            Product product = new Product(null, "상품" + i, "설명" + i, 1000 * i, 10, LocalDateTime.now(), LocalDateTime.now());
            productRepository.save(product);

            // 2. 주문
            Order order = new Order((long) i, null, false, 1000 * i, "PAID", LocalDateTime.now(), LocalDateTime.now());
            orderRepository.save(order);

            // 3. 주문 상품 (판매량: i * 2개씩)
            OrderProduct orderProduct = new OrderProduct(product.getId(), order.getId(), 1000 * i * (i * 2), i * 2);
            orderProductRepository.save(orderProduct);
        }
    }

    @Test
    @DisplayName("캐시가 없을 때 DB 조회 후 Redis에 캐시 저장된다")
    void testFallbackToDbWhenCacheMiss() throws Exception {
        // 1. Redis에 캐시가 없음을 보장
        assertThat(redisTemplate.hasKey(KEY)).isFalse();

        // 2. API 호출 → 캐시가 없으므로 DB에서 조회되고 Redis에 저장됨
        mockMvc.perform(get("/api/v1/products/best"))
                .andExpect(status().isOk());

        // 3. Redis에 캐시가 생겼는지 확인
        String cached = redisTemplate.opsForValue().get(KEY);
        assertThat(cached).isNotNull();

        // 4. JSON → 객체 역직렬화 확인 (option)
        List<ProductBestDto> result = cacheService.get(KEY, new TypeReference<>() {});
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("캐시가 존재할 때 Redis에서 바로 조회된다")
    void testCacheHitReturnsCachedData() throws Exception {
        // given
        List<ProductBestDto> fakeData = List.of(
                new ProductBestDto(99L, "캐시상품", 1000, 10, 50)
        );

        // 1. Redis에 가짜 데이터 저장
        cacheService.set(KEY, fakeData, 60 * 60); // TTL 1시간

        // when - API 호출 후 응답 바디 추출 : 컨트롤러 → 파사드 → 캐시 확인 → DB fallback → Redis 저장
        String jsonResponse = mockMvc.perform(get("/api/v1/products/best"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 응답 JSON → 객체로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode dataNode = root.get("data");
        JsonNode apiFirst = dataNode.get(0);

        // 캐시에서 꺼낸 값
        List<ProductBestDto> cached = cacheService.get(KEY, new TypeReference<>() {});
        ProductBestDto cachedDto = cached.get(0);

        // 첫번째것만 로그 찍어보기
        System.out.println("✅ [API 응답 데이터]");
        System.out.println("ID: " + apiFirst.get("id").asLong());
        System.out.println("이름: " + apiFirst.get("name").asText());
        System.out.println("가격: " + apiFirst.get("price").asInt());
        System.out.println("재고: " + apiFirst.get("stock").asInt());
        System.out.println("판매량: " + apiFirst.get("sales").asInt());

        System.out.println("✅ [Redis 캐시 데이터]");
        System.out.println("ID: " + cachedDto.getId());
        System.out.println("이름: " + cachedDto.getName());
        System.out.println("가격: " + cachedDto.getPrice());
        System.out.println("재고: " + cachedDto.getStock());
        System.out.println("판매량: " + cachedDto.getSales());

        // 응답의 첫 번째 상품과 비교
        assertThat(dataNode.get(0).get("name").asText()).isEqualTo(cachedDto.getName());
        assertThat(dataNode.get(0).get("price").asInt()).isEqualTo(cachedDto.getPrice());
        assertThat(dataNode.get(0).get("sales").asInt()).isEqualTo(cachedDto.getSales());
    }
}
