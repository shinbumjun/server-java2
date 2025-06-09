package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.application.event.PaymentEventPublisher;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.PointRepository;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.PointService;
import kr.hhplus.be.server.infra.external.DataPlatformSendService;
import kr.hhplus.be.server.interfaces.event.PaymentCompletedEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class PointPaymentEventFailureTest {

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private PointService pointService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private OrderService orderService;

    @SpyBean // 실제 로직 수행, verify() 가능
    private DataPlatformSendService dataPlatformSendService;

    @Test
    @DisplayName("이벤트 리스너 실패해도 주문-결제는 정상적으로 완료되어야 한다")
    void processPointPayment_eventListenerFails_butPaymentSucceeds() {
        // given
        Point point = pointRepository.save(new Point(null, 1L, 50000, now(), now()));
        Order order = orderRepository.save(new Order(1L, null, false, 20000, "NOT_PAID", now(), now()));

        // 외부 전송 서비스에서 예외 발생시키기 (리스너는 실제 실행됨)
        doThrow(new RuntimeException("외부 전송 실패"))
                .when(dataPlatformSendService)
                .sendOrderData(anyLong());

        // when
        pointFacade.processPointPayment(order.getId());

        // then
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals("PAID", updatedOrder.getStatus()); // 결제는 정상 처리
        // 로그 ->
        // [실패] 주문 1 외부 전송 실패 → 재처리 대상
    }
}
