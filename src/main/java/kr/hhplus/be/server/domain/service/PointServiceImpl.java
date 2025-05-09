package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointResult;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.point.PointHistory;
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
    @Transactional
    public PointResult usePoints(Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 2. 주문 상태가 EXPIRED이면 결제 불가
        if ("EXPIRED".equals(order.getStatus())) {
            return new PointResult(order.getUserId(), 0, false, "주문 상태가 EXPIRED(결제 불가 건)입니다.");
        }

        // 3. 포인트 조회 (Point 엔티티에서 직접 처리)
        Point point = Point.findByUserIdOrThrow(order.getUserId(), pointRepository);

        // 4. 결제 금액이 포인트보다 클 경우
        try {
            point.validateBalanceForPayment(order.getTotalAmount());  // 포인트 잔액 검증
        } catch (IllegalArgumentException e) {
            return new PointResult(order.getUserId(), point.getBalance(), false,
                    "포인트 잔액이 부족합니다. 현재 잔액 : " + point.getBalance() + "원, 결제 금액 : " + order.getTotalAmount() + "원");
        }

        // 5. 포인트 차감
        point.deductAmount(order.getTotalAmount());  // 포인트 차감
        pointRepository.save(point);

        // 6. 포인트 사용 내역 저장
        PointHistory pointHistory = new PointHistory();
        pointHistory.setPointId(point.getId());
        pointHistory.setAmount(order.getTotalAmount());
        pointHistory.setBalance(point.getBalance());
        pointHistory.setType("사용");
        pointHistoryRepository.save(pointHistory);

        // 7. 주문 상태 변경 (결제 완료 처리)
        // order.setStatus("PAID");
        // orderRepository.save(order);

        // 8. 결제 성공 시 응답 반환
        return new PointResult(order.getUserId(), point.getBalance(), true, "포인트 결제 완료");
    }




}
