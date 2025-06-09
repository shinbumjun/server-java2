package kr.hhplus.be.server.application.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Consumer(CouponRequestListener)가 DB에 발급 로직을 성공적으로 커밋한 직후
// Kafka “coupon-issued” 토픽으로 발송하는 발급 완료 메시지
@Getter
@AllArgsConstructor
@NoArgsConstructor  // JSON 역직렬화용
public class CouponIssuedEvent {
    private Long userId;
    private Long couponId;
}
