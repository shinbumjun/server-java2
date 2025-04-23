package kr.hhplus.be.server.application.coupon;

import lombok.Getter;
import lombok.Setter;

// 쿠폰 전체 응답 관리
@Getter
@Setter
public class CouponResult {

    private int code;         // HTTP 상태 코드
    private String message;   // 메시지
    private String detail;    // 오류 메시지 세부 사항
    private Long userId;      // 사용자 ID
    private String dataType;  // 예: "coupons"
    private Object data;      // 실제 데이터 (쿠폰 목록 등)

    // 생성자
    public CouponResult(int code, String message, Long userId, String dataType, Object data) {
        this.code = code;
        this.message = message;
        this.userId = userId;
        this.dataType = dataType;
        this.data = data;
    }

    // 오류 메시지 세부 사항을 반환하는 메서드 추가
    public String getDetail() {
        return this.detail;
    }

    // 오류 메시지 세부 사항을 설정하는 메서드 추가
    public void setDetail(String detail) {
        this.detail = detail;
    }
}
