package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import java.util.List;

public interface ProductFacade {
    List<Product> getProducts();  // 상품 목록 조회 (Product 엔티티 반환)
}
