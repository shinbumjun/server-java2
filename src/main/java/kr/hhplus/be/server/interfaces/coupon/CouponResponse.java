package kr.hhplus.be.server.interfaces.coupon;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

// 개별 쿠폰 정보
@Getter
@Setter
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String title;
    private String discountType;
    private String startDate;
    private String endDate;
}
