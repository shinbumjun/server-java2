package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.product.Product;

import java.util.List;

public interface ProductService {
    List<Product> getProducts();  // 상품 목록 조회

    void checkAndReduceStock(Long productId, Integer quantity); // 상품 재고 차감
}
