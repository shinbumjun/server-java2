package kr.hhplus.be.server.interfaces.coupon;

import kr.hhplus.be.server.application.coupon.CouponFacade;
import kr.hhplus.be.server.application.coupon.CouponResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponFacade couponFacade;

    public CouponController(CouponFacade couponFacade) {
        this.couponFacade = couponFacade;
    }

    // 사용자 보유 쿠폰 조회
    @GetMapping
    public ResponseEntity<CouponResult> getUserCoupons(@RequestParam Long userId) {
        CouponResult result = couponFacade.getUserCoupons(userId);
        return ResponseEntity.status(result.getCode()).body(result);
    }

    // 선착순 쿠폰 발급
    @PostMapping("/issue")
    public ResponseEntity<CouponResult> issueCoupon(@RequestBody CouponRequest couponRequest) {
        CouponResult result = couponFacade.issueCoupon(couponRequest.getUserId(), couponRequest.getCouponId());
        return ResponseEntity.status(result.getCode()).body(result);
    }
}
