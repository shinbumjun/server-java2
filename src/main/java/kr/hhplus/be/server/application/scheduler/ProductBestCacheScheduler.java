package kr.hhplus.be.server.application.scheduler;

import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.infra.redis.CacheService;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBestCacheScheduler {
    // 매일 자정마다 인기 상품 데이터를 Redis에 저장
    private final ProductService productService;
    private final CacheService cacheService;

    @Scheduled(cron = "0 0 0 * * *") // 자정마다 실행
    public void cacheDailyBestProducts() {
        // 1. DB에서 인기상품 5개 조회
        List<ProductBestDto> bestProducts = productService.getTop5BestSellingProducts();

        // 2. 그 결과를 Redis에 캐시 저장
        cacheService.set("best:daily", bestProducts, 60 * 60 * 25); // TTL 25시간

        log.info("✅ 인기 상품 캐시 저장 완료!");
    }
}
