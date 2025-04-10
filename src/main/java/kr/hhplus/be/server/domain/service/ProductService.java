package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.product.Product;

import java.util.List;

public interface ProductService {
    List<Product> getProducts();  // 상품 목록 조회
}
