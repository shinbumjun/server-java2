package kr.hhplus.be.server.domain.integration;

import kr.hhplus.be.server.ServerApplication;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.domain.service.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ServerApplication.class)  // ServerApplication 클래스를 지정해주어야 한다
@Transactional  // 테스트 후 DB 롤백
@Sql("classpath:/data.sql")  // 테스트 전용 SQL 스크립트
class ProductServiceIntegrationTest {


    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductServiceImpl productService;

    // 테스트 전에 데이터를 삽입
    @BeforeEach
    void setUp() {
        Product product1 = new Product(null, "Macbook Pro", "Apple Laptop", 2000000, 50, LocalDateTime.now(), LocalDateTime.now());
        Product product2 = new Product(null, "iPhone 12", "Apple Smartphone", 1200000, 100, LocalDateTime.now(), LocalDateTime.now());
        productRepository.save(product1);
        productRepository.save(product2);
    }

    @Test
    @DisplayName("상품 목록 조회 시, 상품 목록이 정상적으로 반환되어야 한다")
    void getProducts_ReturnsProductList() {
        // 상품 목록을 조회
        List<Product> result = productService.getProducts();

        // 결과 검증
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Macbook Pro", result.get(0).getProductName());
        assertEquals("iPhone 12", result.get(1).getProductName());
    }
}
