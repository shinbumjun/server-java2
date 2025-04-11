package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointResult;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.PointHistoryRepository;
import kr.hhplus.be.server.domain.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// MockitoExtension을 통해 Mockito를 사용할 수 있도록 설정
@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    /*
    1. 포인트 충전 (PointServiceImpl)
    유효성 검사: chargeAmount가 0보다 크고, 1,000,000을 초과하지 않도록.
    정책 검증: 1회 충전 금액 초과, 누적 충전 금액 초과.
    */

    /*
    2. 포인트 조회 (PointServiceImpl)
    유효성 검사: userId가 양의 정수인지 확인.
    */

    @Mock
    private PointRepository mockPointRepository;

    @Mock
    private OrderRepository mockOrderRepository;

    @Mock
    private PointHistoryRepository mockPointHistoryRepository;

    @Test
    @DisplayName("포인트 충전: 1회 충전 금액 초과 시 409 오류")
    void chargePoints_ExceedSingleChargeAmount() {
        // Given: 초기 포인트가 100인 경우
        Point point = new Point(1L, null, 100, null, null); // 초기 포인트 100
        when(mockPointRepository.findByUserId(1L)).thenReturn(point); // 이 함수 호출되면 무조건 expected 줘! (Stub)

        PointService pointService = new PointServiceImpl(mockPointRepository, mockOrderRepository, mockPointHistoryRepository);

        // PointService pointService = new PointServiceImpl(mockPointRepository);

        // When: 1회 충전 금액이 1,500,000인 경우
        PointCriteria criteria = new PointCriteria(1L, 1500000); // 1,500,000원 충전 시도
        PointResult result = pointService.chargePoints(criteria);

        // Then: 충전 금액이 비즈니스 정책을 위반한 경우 409 오류가 발생
        assertEquals(409, result.getCode());
//        assertEquals("비즈니스 정책을 위반한 요청입니다.", result.getMessage());
        assertEquals("1회 충전 금액은 1,000,000원을 초과할 수 없습니다. 입력값 : 1500000", result.getDetail());
    }

    @Test
    @DisplayName("포인트 충전: 누적 충전 금액 초과 시 409 오류")
    void chargePoints_ExceedTotalChargeAmount() {
        // Given: 누적 충전 금액이 5,000,000원인 경우
        Point point = new Point(1L, null, 5000000, null, null); // 누적 포인트 5000000
        when(mockPointRepository.findByUserId(1L)).thenReturn(point);

        PointService pointService = new PointServiceImpl(mockPointRepository, mockOrderRepository, mockPointHistoryRepository);
        //PointService pointService = new PointServiceImpl(mockPointRepository);

        // When: 누적 충전 금액이 5,500,000인 경우
        PointCriteria criteria = new PointCriteria(1L, 500000);
        PointResult result = pointService.chargePoints(criteria);

        // Then: 누적 충전 금액이 5,000,000원을 초과하면 409 오류가 발생
        assertEquals(409, result.getCode());
//        assertEquals("비즈니스 정책을 위반한 요청입니다.", result.getMessage());
        assertEquals("누적 충전 금액은 5,000,000원을 초과할 수 없습니다. 현재 누적 충전 금액 : 5500000", result.getDetail());
    }

    @Test
    @DisplayName("포인트 조회: 사용자 ID로 포인트 조회")
    void getPoints_Success() {
        // Given: 조회할 사용자와 관련된 포인트 데이터
        Point point = new Point(1L, 1L, 10000, null, null); // 사용자 1의 포인트 10000
        when(mockPointRepository.findByUserId(1L)).thenReturn(point);

        PointService pointService = new PointServiceImpl(mockPointRepository, mockOrderRepository, mockPointHistoryRepository);
        // PointService pointService = new PointServiceImpl(mockPointRepository);

        // When: 포인트 조회
        PointCriteria criteria = new PointCriteria(1L, 0);  // 조회만 하므로 금액은 0
        PointResult result = pointService.getPoints(criteria);

        // Then: 조회된 포인트 응답 검증
        assertEquals(200, result.getCode());
//        assertEquals("요청이 정상적으로 처리되었습니다.", result.getMessage());
        assertEquals(10000, result.getBalance());
    }

    @Test
    @DisplayName("포인트 조회: 유효하지 않은 사용자 ID로 조회")
    void getPoints_InvalidUserId() {
        // Given: 유효하지 않은 사용자 ID로 조회 시
        when(mockPointRepository.findByUserId(100L)).thenReturn(null);  // 없는 사용자

        PointService pointService = new PointServiceImpl(mockPointRepository, mockOrderRepository, mockPointHistoryRepository);
        // PointService pointService = new PointServiceImpl(mockPointRepository);

        // When: 잘못된 사용자 ID로 조회
        PointCriteria criteria = new PointCriteria(100L, 0);  // 조회만 하므로 금액은 0
        PointResult result = pointService.getPoints(criteria);

        // Then: 조회 결과가 없으면 오류 응답
        assertEquals(409, result.getCode());
    }

    @Test
    @DisplayName("포인트 결제: 결제 성공")
    void usePoints_Success() {
        // Given: 주문과 관련된 포인트 데이터
        LocalDateTime now = LocalDateTime.now();  // 현재 시간으로 설정
        Order order = new Order(1L, null, false, 5000, "NOT_PAID", now, now);  // 주문 금액 5000, 적절한 값으로 수정
        Point point = new Point(1L, 1L, 10000, now, now);  // 포인트 잔액 10000, 적절한 값으로 수정
        when(mockPointRepository.findByUserId(1L)).thenReturn(point);
        when(mockOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PointService pointService = new PointServiceImpl(mockPointRepository, mockOrderRepository, mockPointHistoryRepository);

        // When: 포인트 결제
        PointResult result = pointService.usePoints(1L);

        // Then: 결제 성공 시
        assertEquals(200, result.getCode());  // 성공 코드 200
        assertEquals(5000, point.getBalance());  // 포인트 5000 차감 후 잔액 5000
        assertEquals("포인트 결제 완료", result.getDetail());  // 성공 메시지
    }

    @Test
    @DisplayName("포인트 결제: 포인트 잔액 부족")
    void usePoints_InsufficientBalance() {
        // Given: 주문과 관련된 포인트 데이터
        LocalDateTime now = LocalDateTime.now();  // 현재 시간으로 설정
        Order order = new Order(1L, null, false, 10000, "NOT_PAID", now, now);  // 주문 금액 10000
        Point point = new Point(1L, 1L, 5000, now, now);  // 포인트 잔액 5000
        when(mockPointRepository.findByUserId(1L)).thenReturn(point);
        when(mockOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PointService pointService = new PointServiceImpl(mockPointRepository, mockOrderRepository, mockPointHistoryRepository);

        // When: 포인트 결제 (잔
        // 액 부족)
        PointResult result = pointService.usePoints(1L);

        // Then: 결제 실패 시 (잔액 부족)
        assertEquals(409, result.getCode());  // 오류 코드 409
        assertEquals("포인트 잔액이 부족합니다. 현재 잔액 : 5000원, 결제 금액 : 10000원", result.getDetail());  // 오류 메시지
    }


    @Test
    @DisplayName("포인트 결제: 주문 상태가 EXPIRED일 때")
    void usePoints_OrderExpired() {
        // Given: 주문과 관련된 포인트 데이터
        LocalDateTime now = LocalDateTime.now();  // 현재 시간으로 설정
        Order order = new Order(1L, null, false, 5000, "EXPIRED", now, now);  // 주문 상태가 EXPIRED
        Point point = new Point(1L, 1L, 10000, now, now);  // 포인트 잔액 10000
        // when(mockPointRepository.findByUserId(1L)).thenReturn(point);  // 이 코드가 불필요한 경우 제거

        when(mockOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PointService pointService = new PointServiceImpl(mockPointRepository, mockOrderRepository, mockPointHistoryRepository);

        // When: 포인트 결제 (주문 상태 EXPIRED)
        PointResult result = pointService.usePoints(1L);

        // Then: 주문 상태가 EXPIRED인 경우
        assertEquals(409, result.getCode());  // 오류 코드 409
        assertEquals("주문 상태가 EXPIRED(결제 불가 건)입니다.", result.getDetail());  // 오류 메시지
    }




}
