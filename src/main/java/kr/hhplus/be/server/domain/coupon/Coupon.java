package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Coupon {
    @Id
    private Long id;
    private String couponName;
    private String discountType; // RATE, AMOUNT
    private Double discountValue;
    private String startDate;
    private String endDate;
    private Integer stock;

    public Coupon(Long id, String couponName, String discountType, Double discountValue, String startDate, String endDate, Integer stock) {
        this.id = id;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.stock = stock;
    }

    // 정책 검증: 쿠폰 재고가 0 이하일 경우 예외
    public void validateStock() {
        if (this.stock <= 0) {
            throw new IllegalStateException("쿠폰 재고가 부족합니다.");
        }
    }

    // 유효성 검증: 쿠폰 유효 기간 검사
    public void validateDates() {
        // 날짜 검증 로직
        // 예를 들어 startDate와 endDate를 비교하는 로직 추가
        if (this.startDate.compareTo(this.endDate) > 0) {
            throw new IllegalStateException("유효기간이 잘못 설정되었습니다.");
        }
    }
}
