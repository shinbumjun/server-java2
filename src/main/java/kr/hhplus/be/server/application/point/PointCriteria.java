package kr.hhplus.be.server.application.point;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class PointCriteria {

    private Long userId;  // 사용자 ID
    private int chargeAmount;  // 충전 금액
}
