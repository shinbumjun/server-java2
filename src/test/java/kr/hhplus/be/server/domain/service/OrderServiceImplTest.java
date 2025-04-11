package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        // 주문 요청 초기화 (상품 ID, 수량)
        orderRequest = new OrderRequest(1L, 1L, Arrays.asList(new OrderRequest.OrderItem(1L, 2)));
    }

    @Test
    @DisplayName("주문 생성 성공 - 재고가 충분한 경우")
    void createOrder_Success_With_Stock() {
        // Given: 상품이 충분히 재고가 있는 경우
        Product product = new Product(1L, "Macbook Pro", "Description", 2000000, 10, null, null);  // 재고 10
        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));

        // 주문 생성할 Order 객체
        Order order = new Order(1L, 1L, false, 10000, "NOT_PAID", null, null);

        // When: orderRepository.save 호출 시, 반환되는 ID 값을 명시적으로 설정
        Order savedOrder = new Order(1L, 1L, false, 10000, "NOT_PAID", null, null);
        savedOrder.setId(1L);  // ID를 1L로 설정

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);  // save가 order를 반환하도록 설정

        // When: 주문 생성 호출
        Long orderId = orderService.createOrder(orderRequest);

        // Then: 주문 ID가 반환되어야 함
        assertNotNull(orderId);
        assertEquals(1L, orderId);  // 반환된 주문 ID가 1L이어야 함
        verify(orderRepository, times(1)).save(any(Order.class));  // save 메서드가 한 번 호출되었는지 확인
    }




    // 주문 실패 케이스: 재고가 부족한 경우
    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_Failure_With_Stock() {
        // Given: 상품의 재고가 부족한 경우
        Product product = new Product(1L, "Macbook Pro", "Description", 2000000, 2, null, null);  // 재고 2
        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));

        // 주문 요청 객체 생성 (주문 수량이 재고보다 많은 경우)
        orderRequest = new OrderRequest(1L, 1L, Arrays.asList(new OrderRequest.OrderItem(1L, 3)));  // 재고 2, 주문 수량 3

        // When: 주문 생성 호출
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(orderRequest);  // 주문 생성
        });

        // Then: 예외가 발생해야 함 (재고 부족)
        assertEquals("상품의 재고가 부족합니다.", exception.getMessage());  // 예외 메시지가 정확한지 확인
    }

}
