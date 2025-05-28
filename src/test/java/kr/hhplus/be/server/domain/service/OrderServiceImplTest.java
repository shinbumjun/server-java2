package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.application.order.OrderTransactionHandler;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderProductRepository orderProductRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @InjectMocks
    private OrderTransactionHandler orderTransactionHandler;

    private OrderRequest orderRequest;

    private User user;
    private UserCoupon userCoupon;
    private Order order;
    private List<OrderItemCommand> items;
    @BeforeEach
    void setUp() {
        // 사용자 ID와 쿠폰 ID
        // 1) User 객체 세팅
        user = new User();
        user.setId(1L);

        // 2) UserCoupon 객체 세팅
        userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setUserId(user.getId());
        userCoupon.setCouponId(1L);
        userCoupon.setIsUsed(false);

        // 3) 주문 항목 세팅
        items = List.of(new OrderItemCommand(1L, 2));

        // 4) Order 객체 세팅
        order = new Order(
                user.getId(),
                userCoupon != null ? userCoupon.getCouponId() : null,
                false,                            // 쿠폰 사용 여부
                10000,                            // 테스트용 총액
                "NOT_PAID",                       // 초기 상태
                LocalDateTime.now(),              // createdAt
                LocalDateTime.now()               // updatedAt
        );
        order.setId(1L);                      // Mockito가 save 후 ID를 붙인다고 가정

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

        // when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);  // save가 order를 반환하도록 설정
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);  // 테스트용 ID 설정
            return saved;
        });

        // When: 주문 생성 호출
        Order orderResult = orderService.createOrder(user, userCoupon, items);

        // Then: 주문 ID가 반환되어야 함
        assertNotNull(orderResult.getId());
        assertEquals(1L, orderResult.getId());  // 반환된 주문 ID가 1L이어야 함
        verify(orderRepository, times(1)).save(any(Order.class));  // save 메서드가 한 번 호출되었는지 확인
    }




    // 주문 실패 케이스: 재고가 부족한 경우
    @Test
    @DisplayName("주문 생성 실패 - 재고 부족 (단위 테스트)")
    void createOrder_Failure_With_Stock() {
        // Given: 재고 부족 상황을 가정
        doThrow(new IllegalStateException("상품의 재고가 부족합니다."))
                .when(productService)
                .reduceStockWithTx(any());

        // 주문 수량이 재고보다 많은 경우
        items = List.of(new OrderItemCommand(1L, 3));

        // When & Then: 예외 발생 여부 확인
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderTransactionHandler.processOrder(order, items, null); // 쿠폰 없음
        });

        assertEquals("상품의 재고가 부족합니다.", exception.getMessage());
    }


    @Test
    @DisplayName("성공: 주문 상태를 EXPIRED로 변경한다")
    void expireOrder_success() {
        // given
        Order order = new Order();
        order.setStatus("NOT_PAID");

        // when
        orderService.expireOrder(order);

        // then
        assertEquals("EXPIRED", order.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("실패: order가 null이면 예외 발생")
    void expireOrder_fail_whenOrderIsNull() {
        // expect
        assertThrows(NullPointerException.class, () -> {
            orderService.expireOrder(null);
        });
    }

    @Test
    @DisplayName("성공: 주문 ID로 주문을 조회한다")
    void getOrderById_success() {
        // given
        Order order = new Order();
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // when
        Order foundOrder = orderService.getOrderById(1L);

        // then
        assertNotNull(foundOrder);
        assertEquals(1L, foundOrder.getId());
    }

    @Test
    @DisplayName("실패: 주문 ID가 존재하지 않으면 예외 발생")
    void getOrderById_fail() {
        // given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // expect
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.getOrderById(1L);
        });
    }

    @Test
    @DisplayName("성공: 주문 상태를 PAID로 변경")
    void updateOrderStatusToPaid_success() {
        // given
        Order order = new Order();
        order.setId(1L);
        order.setStatus("NOT_PAID");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // when
        orderService.updateOrderStatusToPaid(1L);

        // then
        assertEquals("PAID", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("실패: EXPIRED 상태의 주문은 PAID로 변경 불가")
    void updateOrderStatusToPaid_fail_expired() {
        // given
        Order order = new Order();
        order.setId(1L);
        order.setStatus("EXPIRED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // expect
        assertThrows(IllegalStateException.class, () -> {
            orderService.updateOrderStatusToPaid(1L);
        });
    }

}
