package kr.hhplus.be.server.common.exception;

public class PointErrorCode {

    public static final String INVALID_USER_ID = "INVALID_USER_ID";  // 잘못된 사용자 ID
    public static final String INVALID_CHARGE_AMOUNT = "INVALID_CHARGE_AMOUNT";  // 잘못된 충전 금액
    public static final String EXCEED_ONE_TIME_LIMIT = "EXCEED_ONE_TIME_LIMIT";  // 1회 충전 금액 초과
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";  // 잔액 부족

}
