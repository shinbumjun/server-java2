package kr.hhplus.be.server.domain.integration.scheduler;

import kr.hhplus.be.server.application.scheduler.OrderExpireScheduler;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class OrderExpireSchedulerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderExpireScheduler orderExpireScheduler;


    @Test
    @DisplayName("5분 이상 지난 미결제 주문만 조회된다")
    void testGetUnpaidOrdersBefore() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 1. 6분 전 주문 (조회되어야 함) : UTC 기준이라서 9시간 느림
        Order oldOrder = new Order(1L, null, false, 1000, "NOT_PAID", now.minusMinutes(6), now.minusMinutes(6));
        orderRepository.save(oldOrder);

        // 2. 2분 전 주문 (조회되면 안 됨)
        Order recentOrder = new Order(2L, null, false, 1000, "NOT_PAID", now.minusMinutes(2), now.minusMinutes(2));
        orderRepository.save(recentOrder);

        // when
        List<Order> result = orderService.getUnpaidOrdersBefore(now.minusMinutes(5));

        // then
        assertThat(result).extracting("id")
                .contains(oldOrder.getId())
                .doesNotContain(recentOrder.getId());
    }

    @Test
    @DisplayName("expireOrder 호출 시 주문 상태가 EXPIRED로 변경된다")
    void testExpireOrderChangesStatusToExpired() {
        // given
        Order order = new Order(
                100L,           // userId
                null,         // userCouponId
                false,        // isCouponApplied
                1000,         // totalAmount
                "NOT_PAID",   // 초기 상태
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(10)
        );
        orderRepository.save(order);

        // when
        orderService.expireOrder(order);

        // then
        Order updated = orderService.getOrderById(order.getId());
        assertThat(updated.getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("사용된 쿠폰이면 revertCouponIfUsed 호출 시 isUsed = false로 변경된다")
    void testRevertCouponIfUsed() {
        // given
        UserCoupon coupon = new UserCoupon(
                1L,                    // userId
                1000L,                    // couponId
                true, // false,                 // isUsed
                LocalDate.now(),      // issuedAt
                LocalDate.now().plusDays(7)  // expiredAt
        );
        userCouponRepository.save(coupon);

        // when
        couponService.revertCouponIfUsed(coupon.getId());

        // then
        UserCoupon updated = userCouponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updated.getIsUsed()).isFalse(); // 쿠폰 사용 취소 확인
    }

    @Test
    @DisplayName("주문 상품의 수량만큼 재고가 복구된다")
    void testRevertStockByOrder() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(null, "상품 A", "설명", 1000, 5, now, now);
        productRepository.save(product);

        Order order = new Order(1L, null, false, 1000, "NOT_PAID", LocalDateTime.now(), LocalDateTime.now());
        orderRepository.save(order);

        // 주문 상품: 수량 3
        OrderProduct orderProduct = new OrderProduct(product.getId(), order.getId(), 3000, 3);
        orderProductRepository.save(orderProduct);

        // when
        productService.revertStockByOrder(order.getId());

        // then
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(8); // 5 + 3
    }

    @Test
    @DisplayName("스케줄러가 미결제 주문을 EXPIRED로 변경하고 쿠폰과 재고를 복구한다")
    void testExpireUnpaidOrdersIntegration() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 상품 생성
        Product product = new Product(null, "상품 A", "설명", 1000, 5, now, now);
        productRepository.save(product);

        // 쿠폰 생성 및 저장 (사용됨 상태)
        UserCoupon coupon = new UserCoupon(1L, 1000L, true, LocalDate.now(), LocalDate.now().plusDays(7));
        userCouponRepository.save(coupon);

        // 주문 생성 (6분 전)
        Order order = new Order(1L, coupon.getId(), true, 3000, "NOT_PAID", now.minusMinutes(6), now.minusMinutes(6));
        orderRepository.save(order);

        // 주문 상품 생성
        OrderProduct orderProduct = new OrderProduct(product.getId(), order.getId(), 3000, 3);
        orderProductRepository.save(orderProduct);

        // when - 스케줄러 직접 호출
        orderExpireScheduler.expireUnpaidOrders();

        // then - 주문 상태 확인
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo("EXPIRED");

        // 쿠폰 상태 확인
        UserCoupon updatedCoupon = userCouponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIsUsed()).isFalse();

        // 재고 확인
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(8); // 5 + 3
    }
}
