package kr.hhplus.be.server.application.product;

import com.fasterxml.jackson.core.type.TypeReference;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.infra.redis.CacheService;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class ProductFacadeImpl implements ProductFacade {

    private final ProductService productService;
    private final CacheService cacheService;

    private final RedisTemplate redisTemplate;

    private static final String BEST_PRODUCT_KEY = "best:daily";
    private static final long TTL_SECONDS = 60 * 60 * 25; // 25시간

    @Override
    public List<Product> getProducts() {
        return productService.getProducts();  // 상품 목록을 Product 엔티티로 반환
    }

    @Override
    public List<ProductBestDto> getBestSellingProducts() { // 판매량 상위 5개 상품 조회
        // 1. Redis에서 먼저 캐시 조회
        List<ProductBestDto> cached = cacheService.get(BEST_PRODUCT_KEY, new TypeReference<>() {});
        if (cached != null) {
            log.info("[CACHE HIT] Redis 캐시에서 인기 상품을 조회했습니다.");
            return cached; // 캐시 히트
        }

        // 2. 캐시에 없으면 DB에서 조회
        List<ProductBestDto> bestProducts = productService.getTop5BestSellingProducts();
        log.info("[CACHE MISS] DB에서 인기 상품을 조회하고 Redis에 저장합니다.");

        // 3. 다시 Redis에 저장 (fallback 캐싱)
        cacheService.set(BEST_PRODUCT_KEY, bestProducts, TTL_SECONDS);

        return bestProducts;
    }

    @Override
    public List<ProductBestDto> getRealTimeRankings() {
        // 1. Redis에서 정렬된 랭킹 TOP 5 조회 (상품 ID + 판매량)
        Set<ZSetOperations.TypedTuple<String>> topRanks =
                redisTemplate.opsForZSet().reverseRangeWithScores("ranking:3days", 0, 4);

        // 2. 상품 ID만 뽑아서 DB에서 상품 상세 정보 조회
        List<Long> productIds = topRanks.stream()
                .map(t -> Long.valueOf(t.getValue()))
                .toList();

        List<Product> products = productService.getProductsByIds(productIds);

        // 3. 상품 정보 + 판매량(score)을 묶어서 응답용 DTO로 변환
        return products.stream()
                .map(p -> {
                    double score = topRanks.stream()
                            .filter(t -> t.getValue().equals(p.getId().toString()))
                            .findFirst()
                            .map(ZSetOperations.TypedTuple::getScore)
                            .orElse(0.0);

                    return new ProductBestDto(
                            p.getId(),
                            p.getProductName(),
                            p.getPrice(),
                            (int) score,     // 판매량
                            p.getStock()
                    );
                })
                .toList();
    }


}
