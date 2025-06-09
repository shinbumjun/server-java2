package kr.hhplus.be.server.infra.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service // 외부 시스템과 통신, infra layer
public class DataPlatformSendService {

    public void sendOrderData(Long orderId) {
        // ❌ DB 조회 없이
        // 외부 API 호출 or mock 처리
        log.info("[MOCK] 외부 데이터 플랫폼에 주문 {} 전송 완료", orderId);
    }
}
