package kr.hhplus.be.server.interfaces.point;

import kr.hhplus.be.server.application.point.PointCriteria;
import kr.hhplus.be.server.application.point.PointFacade;
import kr.hhplus.be.server.application.point.PointResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointFacade pointFacade;

    // 생성자 주입
    public PointController(PointFacade pointFacade) {
        this.pointFacade = pointFacade;
    }

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
        // 포인트 조회 조건을 설정
        PointCriteria criteria = new PointCriteria(userId, 0); // 조회만 하므로 금액은 0

        // 포인트 조회 로직 처리
        PointResult result = pointFacade.getPoints(criteria);

        // 조회된 포인트 응답 반환
        PointResponse.Data responseData = new PointResponse.Data(result.getUserId(), result.getBalance());
        PointResponse response = new PointResponse(200, "요청이 정상적으로 처리되었습니다.", responseData);
        return ResponseEntity.ok(response);
    }
}
