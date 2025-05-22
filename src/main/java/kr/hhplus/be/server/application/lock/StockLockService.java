package kr.hhplus.be.server.application.lock;

import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockLockService { //  락만 잡고 해제만 담당

    private final RedisLockManager redisLockManager;

    /**
     * 상품별로 Redis 락을 획득합니다.
     * 재고 차감 로직 실행 전에 락을 걸어야 데이터 정합성을 보장할 수 있습니다.
     *
     * @param items 주문 항목
     * @return 획득한 락 키 목록 (나중에 해제용)
     */
    // Redis 락 획득 : 상품별 락 획득 및 재고 차감
    public List<String> lockProductItems(List<OrderItemCommand> items) {
        List<String> lockKeys = items.stream()
                .map(i -> "lock:product:" + i.productId()) // OrderItemCommand는 record 이므로 getter 대신 필드명 호출
                .toList();

        for (String key : lockKeys) {
            if (!redisLockManager.lockWithRetry(key, 5000)) {
                throw new IllegalStateException("락 획득 실패: " + key);
            }
        }
        // 2. 트랜잭션 기반 재고 차감
        // productService.reduceStockWithTx(items);

        return lockKeys;
    }

    /**
     * 락을 해제합니다. 락 해제는 반대 순서(역순)로 수행하는 것이 안전합니다.
     *
     * @param lockKeys 락을 걸었던 키 목록
     */
    public void unlockProductItems(List<String> lockKeys) {
        for (int i = lockKeys.size() - 1; i >= 0; i--) {
            redisLockManager.unlock(lockKeys.get(i));
        }
    }
}
