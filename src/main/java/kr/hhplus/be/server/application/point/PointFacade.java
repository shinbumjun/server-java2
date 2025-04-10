package kr.hhplus.be.server.application.point;

public interface PointFacade {

    PointResult chargePoints(PointCriteria criteria);

    PointResult getPoints(PointCriteria criteria);
}
