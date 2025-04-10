package kr.hhplus.be.server.interfaces.product;

import kr.hhplus.be.server.application.product.ProductFacade;
import kr.hhplus.be.server.domain.product.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    private final ProductFacade productFacade;

    public ProductController(ProductFacade productFacade) {
        this.productFacade = productFacade;
    }

    @GetMapping("/api/v1/products")
    public ProductResponse getProducts() {
        List<Product> products = productFacade.getProducts(); // 상품 목록 조회
        return new ProductResponse(200, "요청이 정상적으로 처리되었습니다.", (ProductResponse.ProductData) products);
    }
}
