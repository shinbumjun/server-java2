package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.application.event.PaymentEventPublisher;
import kr.hhplus.be.server.application.event.PointPaymentCompletedEvent;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

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

    @SpyBean // 실제 로직 수행, verify() 가능
    private PaymentEventPublisher paymentEventPublisher;

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
        PointResult result = pointService.usePoints(order.getId(), order.getTotalAmount());

        // then
        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());  // 실패 시 반환 코드 명확히 검증
        assertEquals("포인트 잔액이 부족합니다. 현재 잔액 : 10000원, 결제 금액 : 20000원", result.getDetail());  // 상세 메시지 검증

    }

    @Test
    @DisplayName("실패: 주문 상태가 EXPIRED일 경우 결제 불가")
    void processPointPayment_fail_expiredOrder() {
        // given
        pointRepository.save(new Point(null, 1L, 50000, now(), now()));
        Order order = orderRepository.save(new Order(1L, null, false, 20000, "EXPIRED", now(), now()));

        // when
        PointResult result = pointService.usePoints(order.getId(), order.getTotalAmount());

        // then
        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        assertEquals("주문 상태가 EXPIRED(결제 불가 건)입니다.", result.getDetail());
    }

    @Test
    @DisplayName("성공: 포인트 결제 후 이벤트가 발행된다")
    void processPointPayment_eventPublished() {
        // given
        pointRepository.save(new Point(null, 1L, 50000, now(), now())); // 해당 사용자 포인트 정보
        Order order = orderRepository.save(new Order(1L, null, false, 20000, "NOT_PAID", now(), now()));

        // when
        pointFacade.processPointPayment(order.getId());

        // then
        verify(paymentEventPublisher, times(1))
                .publish(any(PointPaymentCompletedEvent.class));
        // 로그 확인
        // [MOCK] 외부 데이터 플랫폼에 주문 1 전송 완료
        // [성공] 주문 1 외부 플랫폼 전송 완료
    }
}
