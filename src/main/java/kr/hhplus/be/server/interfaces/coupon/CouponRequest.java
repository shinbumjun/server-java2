package kr.hhplus.be.server.interfaces.coupon;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

// REST API 호출 시 브라우저(클라이언트) → 컨트롤러로 전달하는 DTO
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CouponRequest {
    private Long userId;
    private Long couponId;
}
