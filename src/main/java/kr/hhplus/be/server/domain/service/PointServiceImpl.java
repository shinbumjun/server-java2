package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointResult;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.pointhistory.PointHistoryFactory;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.PointHistoryRepository;
import kr.hhplus.be.server.domain.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class PointServiceImpl implements PointService {

    private final PointRepository pointRepository;
    private final OrderRepository orderRepository;
    private final PointHistoryRepository pointHistoryRepository;

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
    public PointResult getPoints(PointCriteria criteria) { // 사용자 ID, 충전금액
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

    @Override
    public PointResult usePoints(User user, int amount) {
//        // 1. 주문 조회
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 1. Point 엔티티 조회
        Point point = Point.findByUserIdOrThrow(user.getId(), pointRepository);

        // 2. 잔액 검증 + 차감 및 저장
        point.deductAmount(amount);
        pointRepository.save(point);

        // 3. 히스토리 기록
        PointHistory history = PointHistoryFactory.createUsageHistory(point, amount);
        pointHistoryRepository.save(history);

        // 7. 주문 상태 변경 (결제 완료 처리) -> 포인트가 주문을 직접 수정하는 것 안되고 포인트핸들러가
        // order.setStatus("PAID");
        // orderRepository.save(order);

        // 8. 결제 성공 시 응답 반환
        return new PointResult(user.getId(), point.getBalance(), true, "포인트 결제 완료");
    }
}
