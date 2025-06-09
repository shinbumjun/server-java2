package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.order.OrderHistory;
import kr.hhplus.be.server.domain.repository.OrderHistoryRepository;
import kr.hhplus.be.server.domain.service.OrderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private final OrderHistoryRepository historyRepository;

    /**
     * 주문 단계별 이벤트를 기록합니다.
     *
     * @param orderId 기록할 주문 ID
     * @param event   이벤트 코드 (e.g. "STOCK_DEDUCTED")
     */
    @Override
    // 전체 트랜잭션이 롤백돼도 이벤트 기록은 남기기 위해 별도 트랜잭션으로 커밋
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long orderId, String event) {
        OrderHistory entry = new OrderHistory(orderId, event);
        historyRepository.save(entry);
    }
}
