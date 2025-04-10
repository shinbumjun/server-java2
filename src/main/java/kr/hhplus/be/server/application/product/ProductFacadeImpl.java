package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductFacadeImpl implements ProductFacade {

    private final ProductService productService;

    // 생성자 주입
    public ProductFacadeImpl(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public List<Product> getProducts() {
        return productService.getProducts();  // 상품 목록을 Product 엔티티로 반환
    }
}
