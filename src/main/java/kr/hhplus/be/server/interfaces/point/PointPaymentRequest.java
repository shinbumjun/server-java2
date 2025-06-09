package kr.hhplus.be.server.interfaces.point;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointPaymentRequest {

    private Long orderId;  // 결제할 주문 ID
}
