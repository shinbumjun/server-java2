package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
class OrderFacadeImplTest {

    @Autowired
    private OrderFacadeImpl orderFacade;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Test
    @DisplayName("성공: 미결제 주문이 만료되고 쿠폰/재고가 복구된다")
    void expireUnpaidOrders_success() {
        // given
        Product product = productRepository.save(new Product(null, "상품", "설명", 1000, 5, LocalDateTime.now(), LocalDateTime.now()));
        UserCoupon userCoupon = userCouponRepository.save(new UserCoupon(1L, 1L, true, LocalDate.now(), LocalDate.now().plusDays(1)));

        Order order = orderRepository.save(new Order(1L, userCoupon.getId(), true, 1000, "NOT_PAID", LocalDateTime.now().minusMinutes(6), LocalDateTime.now()));
        orderProductRepository.save(new OrderProduct(product.getId(), order.getId(), 1000, 2));

        int prevStock = product.getStock();

        // when
        orderFacade.expireUnpaidOrders();

        // then
        Order expired = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals("EXPIRED", expired.getStatus());

        UserCoupon updatedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertFalse(updatedCoupon.getIsUsed());

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(prevStock + 2, updatedProduct.getStock());
    }

    @Test
    @DisplayName("실패: 결제 완료된 주문은 만료되지 않는다")
    void expireUnpaidOrders_fail_whenStatusNotNotPaid() {
        // given
        Product product = productRepository.save(new Product(null, "상품", "설명", 1000, 5, LocalDateTime.now(), LocalDateTime.now()));
        UserCoupon userCoupon = userCouponRepository.save(new UserCoupon(1L, 1L, true, LocalDate.now(), LocalDate.now().plusDays(1)));

        Order paidOrder = orderRepository.save(new Order(1L, userCoupon.getId(), true, 1000, "PAID", LocalDateTime.now().minusMinutes(6), LocalDateTime.now()));
        orderProductRepository.save(new OrderProduct(product.getId(), paidOrder.getId(), 1000, 2));

        int stockBefore = product.getStock();

        // when
        orderFacade.expireUnpaidOrders(); // 실행해도 이 주문은 대상이 아님

        // then
        Order notExpiredOrder = orderRepository.findById(paidOrder.getId()).orElseThrow();
        assertEquals("PAID", notExpiredOrder.getStatus(), "결제 완료된 주문은 EXPIRED 되면 안 됨");

        UserCoupon unchangedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertTrue(unchangedCoupon.getIsUsed(), "쿠폰은 여전히 사용 상태여야 함");

        Product unchangedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(stockBefore, unchangedProduct.getStock(), "재고는 변하지 않아야 함");
    }

}
