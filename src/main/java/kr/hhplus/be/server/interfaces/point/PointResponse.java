package kr.hhplus.be.server.interfaces.point;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointResponse {

    private Integer code;          // HTTP 상태 코드
    private String message;        // 메시지
    private Data data;             // 응답 데이터 (사용자 ID와 잔액)

    // Data 클래스는 응답 본문을 담는 객체입니다.
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private Long userId;     // 사용자 ID
        private Integer balance; // 충전 후 잔액
    }
}
