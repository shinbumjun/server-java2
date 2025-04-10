package kr.hhplus.be.server.application.point;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class PointResult {

    private Long userId;
    private int balance;
    private boolean success;
    private String message;

    // 유효성 검증: 상태 코드 반환
    public int getCode() {
        if (success) {
            return 200;  // 성공적인 충전
        } else {
            return 409;  // 비즈니스 정책 위반 (충전 금액 초과 등)
        }
    }

    // 상세 메시지 반환
    public String getDetail() {
        // 실패 시, message에 담긴 내용을 그대로 detail로 사용
        return message;
    }
}
