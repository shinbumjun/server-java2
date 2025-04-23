package kr.hhplus.be.server.common.exception;

public class CustomBusinessException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    // 생성자
    public CustomBusinessException(String errorCode) {
        super(getErrorMessageByCode(errorCode));  // errorCode에 해당하는 메시지를 전달
        this.errorCode = errorCode;
        this.errorMessage = getErrorMessageByCode(errorCode);
    }

    // errorCode에 맞는 에러 메시지를 반환하는 메서드
    private static String getErrorMessageByCode(String errorCode) {
        switch (errorCode) {
            case PointErrorCode.INVALID_USER_ID:
                return "잘못된 사용자 ID";
            case PointErrorCode.INVALID_CHARGE_AMOUNT:
                return "충전 금액은 0보다 커야 합니다.";
            case PointErrorCode.EXCEED_ONE_TIME_LIMIT:
                return "1회 충전 금액은 1,000,000원을 초과할 수 없습니다.";
            case PointErrorCode.INSUFFICIENT_BALANCE:
                return "잔액 부족";
            default:
                return "알 수 없는 오류";
        }
    }

    // errorCode 반환
    public String getErrorCode() {
        return errorCode;
    }

    // errorMessage 반환
    public String getErrorMessage() {
        return errorMessage;
    }
}
