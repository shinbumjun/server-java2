package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointResult;

public interface PointService {
    PointResult chargePoints(PointCriteria criteria);  // 포인트 충전
    PointResult getPoints(PointCriteria criteria);  // 포인트 조회
}
