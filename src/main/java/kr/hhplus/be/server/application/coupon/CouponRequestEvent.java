package kr.hhplus.be.server.application.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Facade(CouponFacadeImpl) → Kafka “coupon-publish-request” 토픽으로 발송하는 발급 요청 메시지
@Getter
@AllArgsConstructor
@NoArgsConstructor  // JSON 역직렬화용 기본 생성자
public class CouponRequestEvent {
    private Long userId;
    private Long couponId;
}
