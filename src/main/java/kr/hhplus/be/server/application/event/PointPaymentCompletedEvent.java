package kr.hhplus.be.server.application.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 1. 이벤트 클래스, application layer, 이벤트 정의 (비즈니스 유스케이스 결과를 전달하는 역할)
@Getter
@AllArgsConstructor
public class PointPaymentCompletedEvent {
    private final Long orderId;
    private final Long userId;
}
