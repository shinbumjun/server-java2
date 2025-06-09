package kr.hhplus.be.server.application.point;

public interface PointFacade {

    // 포인트 충전
    PointResult chargePoints(PointCriteria criteria);

    // 포인트 조회
    PointResult getPoints(PointCriteria criteria);

    // 포인트 결제 처리
    void processPointPayment(Long orderId);
}
