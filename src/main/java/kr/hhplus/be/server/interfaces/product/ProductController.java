package kr.hhplus.be.server.interfaces.product;

import kr.hhplus.be.server.application.product.ProductFacade;
import kr.hhplus.be.server.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor // 생성자 자동 생성
public class ProductController {

    private final ProductFacade productFacade;

    @GetMapping("/api/v1/products")
    public ProductResponse getProducts() {
        List<Product> products = productFacade.getProducts(); // 상품 목록 조회

        // Product 엔티티를 ProductDto로 변환
        List<ProductResponse.ProductDto> productDtos = products.stream()
                .map(product -> new ProductResponse.ProductDto(
                        product.getId(),
                        product.getProductName(),
                        product.getPrice(),
                        product.getStock()))
                .collect(Collectors.toList());

        // ProductResponse를 생성하여 반환
        return new ProductResponse(200, "요청이 정상적으로 처리되었습니다.", new ProductResponse.ProductData(productDtos));
    }

    @GetMapping("/api/v1/products/best")
    public ResponseEntity<ProductBestResponse> getBestSellingProducts() { // 판매량 상위 5개 상품 조회
        List<ProductBestDto> bestProducts = productFacade.getBestSellingProducts();
        ProductBestResponse response = new ProductBestResponse(200, "요청이 정상적으로 처리되었습니다.", bestProducts);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/products/ranking")
    public ResponseEntity<ProductBestResponse> getRealTimeRanking() {
        List<ProductBestDto> rankedProducts = productFacade.getRealTimeRankings(); // Redis 기반 랭킹 조회
        ProductBestResponse response = new ProductBestResponse(200, "실시간 랭킹 조회 성공", rankedProducts);
        return ResponseEntity.ok(response);
    }
}
