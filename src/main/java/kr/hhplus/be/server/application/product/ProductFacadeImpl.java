package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class ProductFacadeImpl implements ProductFacade {

    private final ProductService productService;

    @Override
    public List<Product> getProducts() {
        return productService.getProducts();  // 상품 목록을 Product 엔티티로 반환
    }
}
