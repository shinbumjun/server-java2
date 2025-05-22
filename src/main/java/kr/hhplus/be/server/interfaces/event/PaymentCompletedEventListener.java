package kr.hhplus.be.server.interfaces.event;

import jakarta.annotation.PostConstruct;
import kr.hhplus.be.server.infra.external.DataPlatformSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import kr.hhplus.be.server.application.event.PointPaymentCompletedEvent;

@Slf4j
@Component // 3. 이벤트 리스너 (Listener), interface layer (입출력 처리)
public class PaymentCompletedEventListener {

    private final DataPlatformSendService dataPlatformSendService;

    public PaymentCompletedEventListener(DataPlatformSendService dataPlatformSendService) {
        this.dataPlatformSendService = dataPlatformSendService;
    }

    @Async // 비동기 처리
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // "현재 트랜잭션이 커밋된 이후에 실행된다
    @EventListener
    public void handle(PointPaymentCompletedEvent event) {
        try {
            dataPlatformSendService.sendOrderData(event.getOrderId());
            log.info("[성공] 주문 {} 외부 플랫폼 전송 완료", event.getOrderId());
        } catch (Exception e) {
            log.error("[실패] 주문 {} 외부 전송 실패 → 재처리 대상", event.getOrderId(), e);
            // TODO: 실패 이력 저장 or 재처리 메시지 발행
        }
    }
}
