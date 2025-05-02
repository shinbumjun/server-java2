package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
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
class ProductServiceTest2 {

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
}
