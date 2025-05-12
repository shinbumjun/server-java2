package kr.hhplus.be.server.application.lock;

import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockLockService {

    private final RedisLockManager redisLockManager;
    private final ProductService productService;

    // Redis 락 획득 : 상품별 락 획득 및 재고 차감
    public void lockAndReduceStock(List<OrderRequest.OrderItem> items) {
        List<String> lockKeys = items.stream()
                .map(i -> "lock:product:" + i.getProductId())
                .toList();

        try {
            // 1. 락 획득
            for (String key : lockKeys) {
                if (!redisLockManager.lockWithRetry(key, 5000)) {
                    throw new IllegalStateException("락 획득 실패: " + key);
                }
            }

            // 2. 트랜잭션 기반 재고 차감
            productService.reduceStockWithTx(items);

        } finally {
            // 3. 락 해제 (역순 권장)
            for (int i = lockKeys.size() - 1; i >= 0; i--) {
                redisLockManager.unlock(lockKeys.get(i));
            }
        }
    }
}
