package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest2 { // 단위 테스트

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderProductRepository orderProductRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("성공: 주문 상품 목록 기반으로 재고 복구")
    void revertStockByOrder_success() {
        // given
        Long orderId = 1L;

        OrderProduct op1 = new OrderProduct(1L, orderId, 10000, 3);
        OrderProduct op2 = new OrderProduct(2L, orderId, 20000, 2);

        Product p1 = new Product(1L, "상품A", "desc", 1000, 10, null, null); // 기존 재고 10
        Product p2 = new Product(2L, "상품B", "desc", 2000, 5, null, null);  // 기존 재고 5

        when(orderProductRepository.findByOrdersId(orderId)).thenReturn(List.of(op1, op2));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));

        // when
        productService.revertStockByOrder(orderId);

        // then
        assertEquals(13, p1.getStock());
        assertEquals(7, p2.getStock());
        verify(productRepository, times(1)).save(p1);
        verify(productRepository, times(1)).save(p2);
    }

    @Test
    @DisplayName("실패: 주문 상품의 Product 가 존재하지 않으면 예외 발생")
    void revertStockByOrder_fail_whenProductMissing() {
        // given
        Long orderId = 1L;
        OrderProduct op = new OrderProduct(99L, orderId, 5000, 1); // 존재하지 않는 상품

        when(orderProductRepository.findByOrdersId(orderId)).thenReturn(List.of(op));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            productService.revertStockByOrder(orderId);
        });
    }

    @Test
    @DisplayName("성공: 판매량 상위 5개 상품 반환")
    void getTop5BestSellingProducts_success() { // 단위 테스트이기 때문에 정렬을 직접 해줘야 한다
        // given
        Object[] row1 = new Object[]{1L, "상품A", 1000, 10, 50}; // id, name, price, stock, sales
        Object[] row2 = new Object[]{2L, "상품B", 2000, 20, 40};

        when(orderProductRepository.findTop5SellingProductsWithInfo()).thenReturn(List.of(row1, row2));

        // when
        List<ProductBestDto> result = productService.getTop5BestSellingProducts();

        // then
        assertEquals(2, result.size());
        assertEquals("상품A", result.get(0).getName());
        assertEquals(50, result.get(0).getSales());

        assertEquals("상품B", result.get(1).getName());
        assertEquals(40, result.get(1).getSales());
    }

    @Test
    @DisplayName("성공: 판매량 0개인 경우 빈 리스트 반환")
    void getTop5BestSellingProducts_empty() {
        // given
        when(orderProductRepository.findTop5SellingProductsWithInfo()).thenReturn(List.of());

        // when
        List<ProductBestDto> result = productService.getTop5BestSellingProducts();

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("성공: 재고가 충분하면 차감됨")
    void checkAndReduceStock_success() {
        // given
        Product product = new Product(1L, "상품A", "desc", 1000, 5, null, null); // 재고 5
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        // when
        productService.checkAndReduceStock(1L, 3);

        // then
        assertEquals(2, product.getStock());
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    @DisplayName("실패: 재고 부족하면 예외 발생")
    void checkAndReduceStock_fail_whenStockTooLow() {
        // given
        Product product = new Product(1L, "상품A", "desc", 1000, 1, null, null); // 재고 1
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        // then
        assertThrows(IllegalStateException.class, () -> {
            productService.checkAndReduceStock(1L, 2); // 2개 주문 시도
        });
    }


}
