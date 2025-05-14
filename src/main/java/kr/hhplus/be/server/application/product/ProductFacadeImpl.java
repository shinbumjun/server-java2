package kr.hhplus.be.server.application.product;

import com.fasterxml.jackson.core.type.TypeReference;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.infra.redis.CacheService;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class ProductFacadeImpl implements ProductFacade {

    private final ProductService productService;
    private final CacheService cacheService;

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
            return cached; // 캐시 히트
        }

        // 2. 캐시에 없으면 DB에서 조회
        List<ProductBestDto> bestProducts = productService.getTop5BestSellingProducts();

        // 3. 다시 Redis에 저장 (fallback 캐싱)
        cacheService.set(BEST_PRODUCT_KEY, bestProducts, TTL_SECONDS);

        return bestProducts;
    }
}
