package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.common.exception.PointErrorCode;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.PointService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.common.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class PointFacadeImpl implements PointFacade {

    private final PointService pointService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CouponService couponService;

    @Override
    public PointResult chargePoints(PointCriteria criteria) { // 사용자 ID, 충전금액
        // 포인트 충전 로직 처리: 도메인 서비스 호출
        return pointService.chargePoints(criteria);
    }

    @Override
    public PointResult getPoints(PointCriteria criteria) {

        // 포인트 조회 로직 처리: 도메인 서비스 호출
        return pointService.getPoints(criteria);
    }
}
