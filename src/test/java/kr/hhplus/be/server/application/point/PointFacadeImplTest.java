package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.PointRepository;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PointFacadeImplTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private PointService pointService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointRepository pointRepository;

    @Test
    @DisplayName("성공: 포인트 결제 성공 시 주문 상태가 PAID로 변경된다")
    void processPointPayment_success() {
        // given
        Point point = pointRepository.save(new Point(null, 1L, 50000, now(), now()));
        Order order = orderRepository.save(new Order(1L, null, false, 20000, "NOT_PAID", now(), now()));

        // when
        pointFacade.processPointPayment(order.getId());

        // then
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        Point updatedPoint = pointRepository.findByUserId(1L);

        assertEquals("PAID", updatedOrder.getStatus());
        assertEquals(30000, updatedPoint.getBalance()); // 50000 - 20000
    }

    @Test
    @DisplayName("실패: 포인트 부족으로 결제 실패")
    void processPointPayment_fail_insufficient() {
        // given
        pointRepository.save(new Point(null, 1L, 10000, now(), now()));
        Order order = orderRepository.save(new Order(1L, null, false, 20000, "NOT_PAID", now(), now()));

        // when
        PointResult result = pointService.usePoints(order.getId());

        // then
        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());  // 실패 시 반환 코드 명확히 검증
        assertEquals("포인트 잔액이 부족합니다. 현재 잔액 : 10000원, 결제 금액 : 20000원", result.getDetail());  // 상세 메시지 검증

    }

}
