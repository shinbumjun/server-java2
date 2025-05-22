package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.lock.StockLockService;
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

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;         // 주문 생성 로직 (DB 저장용)
    private final ProductService productService;     // 상품 재고 관련 서비스
    private final CouponService couponService;       // 쿠폰 검증 및 적용 서비스

    private final OrderTransactionHandler orderHandler;         // 트랜잭션 단위로 주문 처리 묶음

    private final RedisLockManager redisLockManager; // Redis 기반 분산 락 관리자
    private final FairLockManager fairLockManager;   // 사용자 순서 보장용 Fair Lock 큐 관리자

    private final StockLockService stockLockService;

    private final OrderTransactionHandler orderTransactionHandler;

    /*
        [피드백] 재고 감소 – 락을 너무 오래 잡고 있어요
        1. 주문 먼저 생성
        2. 그 후 상품별로 락 획득 & 재고 차감
        3. 실패 시 주문 상태만 FAIL
        4. 락을 짧게 사용하고 다른 유저도 병렬로 시도 가능
        5. 공정 큐와 전체 락 선점 방식은 제거

        [피드백2] 쿠폰 적용 중 실패 시 재고도 롤백되어야 하지 않나?
        1. 주문 먼저 생성
        2. 상품별 Redis 락만 획득 (재고는 경쟁 자원이므로)
        3. 재고 차감 + 쿠폰 적용을 하나의 트랜잭션으로 처리 (실패 시 모두 롤백)
        4. 실패 시 주문 상태 FAIL 처리 후 락 해제
     */
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 주문 먼저 생성
        Long orderId = orderService.createOrder(request);

        List<OrderRequest.OrderItem> items = request.getOrderItems(); // 주문 항목(상품 ID, 수량)
        Long userId = request.getUserId(); // 사용자 ID
        Long couponId = request.getUserCouponId(); // 쿠폰 ID

        List<String> lockKeys = stockLockService.lockProductItems(items); // 1. 락만 잡음

        try {
            // 2. 트랜잭션으로 재고 차감 + 쿠폰 적용
            orderTransactionHandler.processOrder(orderId, items, userId, couponId);

        } catch (Exception e) {
            // 1. 주문 상태를 FAIL로 변경
            orderService.updateOrderStatusToFail(orderId);
            // 2. 재고 복구
            productService.revertStockByOrder(orderId);
            // 3. 쿠폰 복구 (사용된 경우에만)
            if (couponId != null) {
                couponService.revertCouponIfUsed(couponId);
            }

            throw new IllegalStateException("주문 처리 실패: 주문 상태 FAIL 및 자원 복구 처리됨", e);
        } finally {
            // 3. 락 해제
            stockLockService.unlockProductItems(lockKeys);
        }

        return new OrderResponse(201, "주문이 정상적으로 처리되었습니다.", new OrderResponse.OrderData(orderId));
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
