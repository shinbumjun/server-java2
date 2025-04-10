package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointResult;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.PointRepository;
import org.springframework.stereotype.Service;

@Service
public class PointServiceImpl implements PointService {

    private final PointRepository pointRepository;  // 레파지스토리 주입

    // 생성자 주입
    public PointServiceImpl(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    @Override
    public PointResult chargePoints(PointCriteria criteria) {
        // 1. 포인트 조회
        Point point = pointRepository.findByUserId(criteria.getUserId());

        // 2. 포인트 충전: 정책 검증은 Point 엔티티의 addAmount 메서드에서 수행
        try {
            point.addAmount(criteria.getChargeAmount());  // 충전 금액 검증 및 잔액 업데이트
        } catch (IllegalArgumentException e) {
            // 예외가 발생하면 비즈니스 정책을 위반한 것으로 처리
            return new PointResult(criteria.getUserId(), point.getBalance(), false, e.getMessage());
        }

        // 3. 포인트 저장
        pointRepository.save(point);

        // 4. 성공적으로 충전된 경우
        return new PointResult(criteria.getUserId(), point.getBalance(), true, "충전 완료");
    }

    @Override
    public PointResult getPoints(PointCriteria criteria) {
        // 1. 포인트 조회
        Point point = pointRepository.findByUserId(criteria.getUserId());

        // 2. 포인트 조회 실패 처리
        if (point == null) {
            return new PointResult(criteria.getUserId(), 0, false, "해당 사용자를 찾을 수 없습니다.");
        }

        // 3. 포인트 불변성 검증
        try {
            point.validate();  // 엔티티에서 불변성 검증
        } catch (IllegalStateException e) {
            // 불변성 검증 실패 시 예외 처리
            return new PointResult(criteria.getUserId(), 0, false, e.getMessage());
        }

        // 4. 조회된 포인트 반환
        return new PointResult(criteria.getUserId(), point.getBalance(), true, "조회 완료");
    }


}
