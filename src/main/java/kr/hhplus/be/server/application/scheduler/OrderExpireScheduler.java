package kr.hhplus.be.server.application.scheduler;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireScheduler {

    private final OrderService orderService;
    private final CouponService couponService;
    private final ProductService productService;

    // 5분마다 실행
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void expireUnpaidOrders() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5); // 현재 시간에서 5분 전의 시간
        List<Order> unpaidOrders = orderService.getUnpaidOrdersBefore(fiveMinutesAgo); // 결제 미완료 주문 조회

        for (Order order : unpaidOrders) {
            try {
                // 1. 주문 상태 EXPIRED 처리
                orderService.expireOrder(order);

                // 2. 쿠폰 사용 취소
                couponService.revertCouponIfUsed(order.getUserCouponId());

                // 3. 재고 복구
                productService.revertStockByOrder(order.getId());

                log.info("✅ 미결제 주문 만료 처리 완료 - orderId: {}", order.getId());

            } catch (Exception e) {
                log.warn("❌ 주문 만료 처리 실패 - orderId: {}, 사유: {}", order.getId(), e.getMessage());
            }
        }
    }
}
