package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductFacadeImplTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Test
    @DisplayName("성공: 판매량 상위 5개 상품 통합 조회")
    void getBestSellingProducts_success() {
        // given: 상품 2개, 주문 1개 생성
        Product p1 = productRepository.save(new Product(null, "상품A", "desc", 1000, 10, now(), now()));
        Product p2 = productRepository.save(new Product(null, "상품B", "desc", 2000, 20, now(), now()));

        Order order = orderRepository.save(new Order(1L, null, false, 3000, "PAID", now(), now()));

        orderProductRepository.save(new OrderProduct(p1.getId(), order.getId(), 1000 * 3, 3)); // 상품A 3개
        orderProductRepository.save(new OrderProduct(p2.getId(), order.getId(), 2000 * 5, 5)); // 상품B 5개

        // when
        List<ProductBestDto> result = productFacade.getBestSellingProducts();

        // then
        assertEquals(2, result.size());

        // 판매량 많은 상품B가 먼저
        assertEquals("상품B", result.get(0).getName());
        assertEquals(5, result.get(0).getSales());

        assertEquals("상품A", result.get(1).getName());
        assertEquals(3, result.get(1).getSales());
    }

    @Test
    @DisplayName("성공: 10개 상품 중 판매량 기준 상위 5개 반환")
    void getBestSellingProducts_with10Products_success() {
        // given - 10개 상품 저장
        for (int i = 1; i <= 10; i++) {
            productRepository.save(new Product(null, "상품" + i, "desc", 1000 * i, 100, now(), now()));
        }

        List<Product> allProducts = productRepository.findAll();

        // 주문 생성
        Order order = orderRepository.save(new Order(1L, null, false, 0, "PAID", now(), now()));

        // 주문상품 10개 생성 (상품1은 1개, 상품2는 2개, ..., 상품10은 10개 판매)
        for (int i = 0; i < 10; i++) {
            Product product = allProducts.get(i);
            int quantity = i + 1; // 판매 수량 증가

            OrderProduct op = new OrderProduct(product.getId(), order.getId(), product.getPrice() * quantity, quantity);
            orderProductRepository.save(op);
        }

        // when
        List<ProductBestDto> result = productFacade.getBestSellingProducts();

        // then
        assertEquals(5, result.size());

        // 로그 출력: 결과 확인용
        System.out.println("=== 판매량 상위 5개 상품 ===");
        for (ProductBestDto dto : result) {
            System.out.println(dto.getName() + " / 판매량: " + dto.getSales());
        }

        // 판매량 10~6 순으로 나와야 함
        for (int i = 0; i < 5; i++) {
            ProductBestDto dto = result.get(i);
            assertEquals("상품" + (10 - i), dto.getName());
            assertEquals(10 - i, dto.getSales());
        }
    }

}
