package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointResult;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    @DisplayName("포인트 충전: 1회 충전 금액 초과 시 409 오류")
    void chargePoints_ExceedSingleChargeAmount() {
        // Given: 초기 포인트가 100인 경우
        Point point = new Point(1L, null, 100, null, null); // 초기 포인트 100
        when(mockPointRepository.findByUserId(1L)).thenReturn(point); // 이 함수 호출되면 무조건 expected 줘! (Stub)

        PointService pointService = new PointServiceImpl(mockPointRepository);

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

        PointService pointService = new PointServiceImpl(mockPointRepository);

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

        PointService pointService = new PointServiceImpl(mockPointRepository);

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

        PointService pointService = new PointServiceImpl(mockPointRepository);

        // When: 잘못된 사용자 ID로 조회
        PointCriteria criteria = new PointCriteria(100L, 0);  // 조회만 하므로 금액은 0
        PointResult result = pointService.getPoints(criteria);

        // Then: 조회 결과가 없으면 오류 응답
        assertEquals(409, result.getCode());
    }
}
