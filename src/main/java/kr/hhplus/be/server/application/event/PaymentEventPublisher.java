package kr.hhplus.be.server.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@RequiredArgsConstructor // 생성자 자동 생성
@Component // 2. 이벤트 발행기 (Publisher), application layer의 흐름 제어
public class PaymentEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(PointPaymentCompletedEvent event) {
        // 즉시 이벤트 발행
        // publisher.publishEvent(event);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        // 트랜잭션 커밋이 성공적으로 완료된 이후에만 이 코드가 실행됩니다.
                        publisher.publishEvent(event);
                    }
                }
        );
    }
}