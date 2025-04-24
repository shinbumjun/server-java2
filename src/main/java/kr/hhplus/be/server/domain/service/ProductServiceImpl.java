package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getProducts() {
        return productRepository.findAll();  // 상품 목록을 조회
    }

    @Override // 상품 재고 차감
    public void checkAndReduceStock(Long productId, Integer quantity) { // 상품 ID, 주문 수량
        // 1. 상품 조회 (비관적 락 적용)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));

        // 재고 검증 및 차감
        product.validateStock(quantity);  // 재고 검증
        product.reduceStock(quantity);    // 재고 차감

        // 4. 변경된 상품 상태 저장
        productRepository.save(product);
    }
}
