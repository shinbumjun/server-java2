package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;

import java.util.List;

public interface ProductFacade {
    List<Product> getProducts();  // 상품 목록 조회 (Product 엔티티 반환)

    List<ProductBestDto> getBestSellingProducts(); // 판매량 상위 5개 상품 조회

    List<ProductBestDto> getRealTimeRankings(); // Redis 기반 랭킹 조회
}
