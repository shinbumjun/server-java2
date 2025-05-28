package kr.hhplus.be.server.application.order;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.application.lock.StockLockService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import kr.hhplus.be.server.domain.repository.UserRepository;
import kr.hhplus.be.server.domain.service.*;
import kr.hhplus.be.server.infra.lock.FairLockManager;
import lombok.extern.slf4j.Slf4j;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.infra.redis.RedisLockManager;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
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

    private final OrderTransactionHandler orderHandler;  // 트랜잭션 단위로 주문 처리 묶음
    private final RedisLockManager redisLockManager; // Redis 기반 분산 락 관리자
    private final FairLockManager fairLockManager;   // 사용자 순서 보장용 Fair Lock 큐 관리자

    private final StockLockService stockLockService; // Redis 분산 락 서비스
    private final OrderTransactionHandler orderTransactionHandler; // 재고·쿠폰·상태 업데이트를 묶는 트랜잭션 핸들러

    private final UserService userService;
    private final UserCouponService userCouponService;
    
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

        [피드백3]
        1. 주문 생성·항목 저장 (비-경쟁, 빠르게 처리)
        2. 분산 락 획득 (경쟁 구간 진입)
        3. 트랜잭션 시작 (재고 차감, 쿠폰 적용)
        4. 트랜잭션 커밋 또는 롤백
        5. 분산 락 해제 (경쟁 구간 종료)
     */
    public OrderResponse createOrder(OrderRequest request) {
        // 1) 요청에서 주문 항목 변환
        List<OrderItemCommand> items = request.getOrderItems().stream()
                .map(OrderItemCommand::from)
                .toList(); // 주문 항목(상품 ID, 수량)

        // 2) Facade에서 User·UserCoupon 엔티티를 한 번만 조회
        User user = userService.getUser(request.getUserId());
        UserCoupon userCoupon = request.getUserCouponId() != null
                ? userCouponService.getUserCoupon(request.getUserCouponId())
                : null;

        // 3) 주문 생성 호출: Facade에서 조회한 User·UserCoupon 엔티티를 직접 전달
        //    -> 서비스는 ID 조회 없이, 전달받은 엔티티의 상태(유효성, 사용 여부 등)만 검증·처리합니다
        Order order = orderService.createOrder(user, userCoupon, items);

        // 4) 락 획득 (비-트랜잭션)
        List<String> lockKeys = stockLockService.lockProductItems(items);

        try {
            // 5) 트랜잭션 시작: 재고 차감 + 쿠폰 적용 + 주문 상태 변경
            orderTransactionHandler.processOrder(order, items, userCoupon);

        } catch (Exception e) {
            // 트랜잭션 A에서 실패 시 자동 롤백 → 여기서는 주문 상태만 FAIL로 업데이트
            orderService.updateOrderStatusToFail(order.getId());
            throw new IllegalStateException("주문 처리 실패: 주문 상태 FAIL 및 자원 복구 처리됨", e);
        } finally {
            // 6) 락 해제
            stockLockService.unlockProductItems(lockKeys);
        }

        return new OrderResponse(201, "주문이 정상적으로 처리되었습니다.", new OrderResponse.OrderData(order.getId()));
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
