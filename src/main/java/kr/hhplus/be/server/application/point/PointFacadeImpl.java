package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.domain.service.PointService;
import org.springframework.stereotype.Service;

@Service
public class PointFacadeImpl implements PointFacade {

    private final PointService pointService;  // 도메인 서비스

    // 생성자 주입
    public PointFacadeImpl(PointService pointService) {
        this.pointService = pointService;
    }

    @Override
    public PointResult chargePoints(PointCriteria criteria) {
        // 도메인 서비스 호출
        return pointService.chargePoints(criteria);
    }

    @Override
    public PointResult getPoints(PointCriteria criteria) {
        // 도메인 서비스 호출
        return pointService.getPoints(criteria);
    }
}
