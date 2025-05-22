package kr.hhplus.be.server.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor // 생성자 자동 생성
@Component // 2. 이벤트 발행기 (Publisher), application layer의 흐름 제어
public class PaymentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(PointPaymentCompletedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}