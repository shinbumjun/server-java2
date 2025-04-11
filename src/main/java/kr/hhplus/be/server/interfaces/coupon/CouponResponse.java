package kr.hhplus.be.server.interfaces.coupon;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

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
