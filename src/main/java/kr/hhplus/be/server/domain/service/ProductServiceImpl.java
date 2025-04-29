package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Transactional
    @Override
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    // @Transactional // 트랜잭션 안에서 비관적 락을 적용
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새 트랜잭션을 새로 열어줌
    @Override
    public void checkAndReduceStock(Long productId, Integer quantity) {
        // 1. 상품 조회 (비관적 락 적용)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));

        // 2. 재고 검증 및 차감
        product.validateStock(quantity);  // 재고 검증
        product.reduceStock(quantity);    // 재고 차감

        // 3. 변경된 상품 상태 저장
        productRepository.saveAndFlush(product); // 변경된 내용 즉시 DB에 반영
    }
}
