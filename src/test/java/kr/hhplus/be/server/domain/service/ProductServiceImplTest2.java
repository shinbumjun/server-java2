package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceImplTest2 {

    private ProductRepository productRepository;
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductServiceImpl(productRepository);
    }

    @Test
    @DisplayName("상품 목록 조회 시, 상품 목록이 정상적으로 반환되어야 한다")
    void getProducts_ReturnsProductList() {
        // Given: 상품 목록이 존재하는 경우
        Product product1 = new Product(1L, "Macbook Pro", "Description", 2000000, 10, null, null);
        Product product2 = new Product(2L, "iPhone 12", "Description", 1200000, 20, null, null);
        List<Product> productList = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(productList);

        // When: 상품 목록을 조회할 때
        List<Product> result = productService.getProducts();

        // Then: 상품 목록이 정상적으로 반환되어야 한다
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Macbook Pro", result.get(0).getProductName());
        assertEquals("iPhone 12", result.get(1).getProductName());
    }



}
