package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.infra.lock.FairLockManager;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;         // 주문 생성 로직 (DB 저장용)
    private final ProductService productService;     // 상품 재고 관련 서비스
    private final CouponService couponService;       // 쿠폰 검증 및 적용 서비스

    private final OrderHandler orderHandler;         // 트랜잭션 단위로 주문 처리 묶음

    private final RedisLockManager redisLockManager; // Redis 기반 분산 락 관리자
    private final FairLockManager fairLockManager;   // 사용자 순서 보장용 Fair Lock 큐 관리자


    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {

        log.info("[{}] 주문 시작", orderRequest.getUserId());

        orderRequest.getOrderItems().stream()
                .collect(Collectors.groupingBy(OrderRequest.OrderItem::getProductId))
                .forEach((id, list) -> {
                    int totalQty = list.stream()
                            .mapToInt(OrderRequest.OrderItem::getQuantity)
                            .sum();
                    System.out.println("[사용자 " + orderRequest.getUserId() + "] 상품 ID: " + id + ", 요청 총 수량: " + totalQty);
                });

        // 상품 단위로 Redis 락 키 생성 (예: lock:product:1)
        List<String> lockKeys = orderRequest.getOrderItems().stream()
                .map(item -> "lock:product:" + item.getProductId())
                .toList();

        // (1) 공정 락 큐 진입
        fairLockManager.waitMyTurn(orderRequest.getUserId(), 20000L); // 20초 대기

        log.info("[{}] 락 시도 - {}", orderRequest.getUserId(), lockKeys);

        // 1. 모든 상품에 대해 락 획득 시도 (하나라도 실패하면 전체 실패)
        for (String key : lockKeys) {
            if (!redisLockManager.lockWithRetry(key, 20000)) { // 재시도 가능한 최대 대기 시간 10초

                log.warn("[{}] 락 획득 실패 - {}", orderRequest.getUserId(), key);

                // 지금까지 잡은 락 모두 해제 후 실패 처리
                lockKeys.forEach(redisLockManager::unlock);

                // (2) 락 획득 실패 시 큐에서 제거
                fairLockManager.releaseTurn(orderRequest.getUserId()); // 락 실패 시도 후 제거

                throw new IllegalStateException("상품 재고에 대한 락 획득 실패: " + key);
            }
        }

        log.info("[{}] 락 획득 성공 - {}", orderRequest.getUserId(), lockKeys);

        try {

            log.info("[{}] 트랜잭션 실행 시작", orderRequest.getUserId());

            // 2. 락을 전부 획득한 경우에만 트랜잭션 실행 (재고 차감, 쿠폰 적용, 주문 생성)
            return orderHandler.createOrderWithTx(orderRequest);
        } finally {
            // 3. 트랜잭션 성공/실패와 관계없이 락은 반드시 해제 (역순 해제 권장)
            for (int i = lockKeys.size() - 1; i >= 0; i--) {
                redisLockManager.unlock(lockKeys.get(i));
            }

            // (3) 트랜잭션 종료 후 무조건 큐에서 제거
            fairLockManager.releaseTurn(orderRequest.getUserId()); // 성공/실패 상관없이 큐에서 제거

            log.info("[{}] 락 해제 완료 - {}", orderRequest.getUserId(), lockKeys);

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
