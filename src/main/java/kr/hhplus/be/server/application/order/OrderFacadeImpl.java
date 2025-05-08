package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final CouponService couponService;

    private final OrderHandler orderHandler;
    private final RedisLockManager redisLockManager;

    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {
        // 상품 단위로 Redis 락 키 생성 (예: lock:product:1)
        List<String> lockKeys = orderRequest.getOrderItems().stream()
                .map(item -> "lock:product:" + item.getProductId())
                .toList();

        // 1. 모든 상품에 대해 락 획득 시도 (하나라도 실패하면 전체 실패)
        for (String key : lockKeys) {
            if (!redisLockManager.lockWithRetry(key, 3000)) {
                // 지금까지 잡은 락 모두 해제 후 실패 처리
                lockKeys.forEach(redisLockManager::unlock);
                throw new IllegalStateException("상품 재고에 대한 락 획득 실패: " + key);
            }
        }

        try {
            // 2. 락을 전부 획득한 경우에만 트랜잭션 실행 (재고 차감, 쿠폰 적용, 주문 생성)
            return orderHandler.createOrderWithTx(orderRequest);
        } finally {
            // 3. 트랜잭션 성공/실패와 관계없이 락은 반드시 해제 (역순 해제 권장)
            for (int i = lockKeys.size() - 1; i >= 0; i--) {
                redisLockManager.unlock(lockKeys.get(i));
            }
        }
    }




    // 5분 내 미결제 주문을 취소하고, 쿠폰 및 재고를 복구
    @Transactional
    @Scheduled(fixedRate = 300000)
    public void expireUnpaidOrders() {
        // 5분 이상 결제되지 않은 미결제 주문 목록을 가져옴
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Order> unpaidOrders = orderService.getUnpaidOrdersBefore(fiveMinutesAgo);


        for (var order : unpaidOrders) {
            orderService.expireOrder(order); // 상태 EXPIRED로 변경
            couponService.revertCouponIfUsed(order.getUserCouponId()); // 쿠폰 복구
            productService.revertStockByOrder(order.getId()); // 재고 복구
        }
    }
}
