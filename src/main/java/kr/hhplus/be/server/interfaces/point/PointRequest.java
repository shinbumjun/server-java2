package kr.hhplus.be.server.interfaces.point;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointRequest {

    private Long userId;           // 사용자 ID
    private Integer chargeAmount;  // 충전할 금액
}
