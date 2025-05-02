package kr.hhplus.be.server.interfaces.point;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointFacade;
import kr.hhplus.be.server.application.point.PointResult;
import kr.hhplus.be.server.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor // 생성자 자동 생성
public class PointController {

    private final PointFacade pointFacade;
    private final OrderService orderService; // 주문 서비스 추가

    // 포인트 충전 API
    @PostMapping("/charge")
    public ResponseEntity<PointResponse> chargePoints(@RequestBody PointRequest pointRequest) {
        // 포인트 충전 조건을 설정
        PointCriteria criteria = new PointCriteria(pointRequest.getUserId(), pointRequest.getChargeAmount());

        // 포인트 충전 로직 처리
        PointResult result = pointFacade.chargePoints(criteria);

        // 결과에 따른 응답 처리
        if (result.isSuccess()) {
            PointResponse.Data responseData = new PointResponse.Data(result.getUserId(), result.getBalance());
            PointResponse response = new PointResponse(200, "요청이 정상적으로 처리되었습니다.", responseData);
            return ResponseEntity.ok(response);
        } else {
            PointResponse.Data responseData = new PointResponse.Data(result.getUserId(), result.getBalance());
            PointResponse response = new PointResponse(409, "비즈니스 정책을 위반한 요청입니다.", responseData);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    // 포인트 조회 API
    @GetMapping
    public ResponseEntity<PointResponse> getPoints(@RequestParam Long userId) {
        // 잘못된 정보가 넘어오면 컨트롤러에서 처리
        if (userId == null || userId <= 0) {
            // 사용자 ID가 null이거나 0 이하일 경우, 클라이언트에게는 일반적인 오류 메시지 제공
            return ResponseEntity.badRequest().body(
                    new PointResponse(400, "잘못된 사용자 ID", new PointResponse.Data(null, 0))
            );
        }
        // 포인트 조회 조건을 설정
        PointCriteria criteria = new PointCriteria(userId, 0); // 조회만 하므로 금액은 0

        // 포인트 조회 로직 처리
        PointResult result = pointFacade.getPoints(criteria);

        // 조회된 포인트 응답 반환
        PointResponse.Data responseData = new PointResponse.Data(result.getUserId(), result.getBalance());
        PointResponse response = new PointResponse(200, "요청이 정상적으로 처리되었습니다.", responseData);
        return ResponseEntity.ok(response);
    }

//    // 포인트 결제 API
//    @PostMapping("/use")
//    public ResponseEntity<PointResponse> usePointsForPayment(@RequestBody PointPaymentRequest request) { // 결제할 주문 ID
//        try {
//            // 포인트 결제 처리
//            pointFacade.processPointPayment(request.getOrderId());
//
//            // 주문 상태 변경: 결제 성공시 상태를 PAID로 업데이트
//            orderService.updateOrderStatusToPaid(request.getOrderId());
//
//            // 성공적인 응답 반환
//            return ResponseEntity.noContent().build();
//        } catch (InsufficientPointsException e) {
//            // 포인트 부족 오류 처리
//            return ResponseEntity.status(HttpStatus.CONFLICT).body(
//                    new PointResponse(409, "비즈니스 정책을 위반한 요청입니다.", e.getMessage())
//            );
//        } catch (OrderExpiredException e) {
//            // 주문 상태가 EXPIRED인 경우 처리
//            return ResponseEntity.status(HttpStatus.CONFLICT).body(
//                    new PointResponse(409, "비즈니스 정책을 위반한 요청입니다.", e.getMessage())
//            );
//        } catch (Exception e) {
//            // 기타 예외 처리
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
//                    new PointResponse(500, "서버 오류", "알 수 없는 오류가 발생했습니다.")
//            );
//        }
//    }
}
