package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_name", nullable = false)
    private String couponName; // 쿠폰 이름

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue; // 퍼센트 또는 값

    @Column(name = "discount_type", nullable = false)
    private String discountType; // 할인 정책 (정률/정액)

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // 유효기간 시작일

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // 유효기간 종료일

    @Column(name = "stock", nullable = false)
    private Integer stock; // 쿠폰 재고

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 업데이트일

    public Coupon(Long id, String couponName, String discountType, BigDecimal discountValue, LocalDate startDate, LocalDate endDate, Integer stock) {
        this.id = id;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.stock = stock;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    // 쿠폰 재고가 0 이하일 경우 예외 처리
    public void validateStock() {
        if (this.stock <= 0) {
            throw new IllegalStateException("쿠폰 재고가 부족합니다.");
        }
    }

    // 쿠폰 유효 기간 검사
    public void validateDates() {
        if (this.startDate.isAfter(this.endDate)) {
            throw new IllegalStateException("유효기간이 잘못 설정되었습니다.");
        }
    }

    // 재고 차감 메서드
    public void decreaseStock() {
        if (this.stock <= 0) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stock -= 1;  // 재고 차감
    }
}

