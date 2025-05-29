package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.application.coupon.CouponRequestEvent;
import kr.hhplus.be.server.application.coupon.CouponIssuedEvent;
import kr.hhplus.be.server.application.coupon.CouponResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponRequestListener {

    private final CouponService couponService;
    private final KafkaTemplate<String, CouponIssuedEvent> kafkaTemplate;

    @KafkaListener(
            topics = "coupon-publish-request",
            groupId = "coupon-issuers",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(CouponRequestEvent evt, Acknowledgment ack) {
        Long userId = evt.getUserId();
        Long couponId = evt.getCouponId();

        // 1) 서비스 호출: 중복·재고·유효기간 검사 + 발급 처리
        CouponResult result = couponService.issueCoupon(userId, couponId);

        if (result.getCode() == 201) {
            // 2) 트랜잭션 커밋 직후에만 완료 이벤트 발행 + 오프셋 커밋
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send(
                            "coupon-issued",
                            couponId.toString(),
                            new CouponIssuedEvent(userId, couponId)
                    );
                    ack.acknowledge();
                }

                @Override
                public void beforeCommit(boolean readOnly) {
                }

                @Override
                public void afterCompletion(int status) {
                }
            });
        } else {
            log.warn("쿠폰 발급 실패(user={}, coupon={}): {}", userId, couponId, result.getMessage());
            // 실패 시에도 오프셋 커밋
            ack.acknowledge();
        }
    }
}