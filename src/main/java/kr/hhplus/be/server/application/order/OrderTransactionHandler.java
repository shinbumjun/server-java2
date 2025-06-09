package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.repository.CouponRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import kr.hhplus.be.server.domain.service.CouponService;
import kr.hhplus.be.server.domain.service.OrderHistoryService;
import kr.hhplus.be.server.domain.service.OrderService;
import kr.hhplus.be.server.domain.service.ProductService;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTransactionHandler {


    private final ProductService productService;
    private final CouponService couponService;
    private final OrderService orderService;      // 상태 변경용
    private final OrderHistoryService historyService;    // 이력 기록용

    /**
     * 재고 차감과 쿠폰 적용을 하나의 트랜잭션으로 묶어 처리합니다.
     * 실패 시 자동 롤백되며, 호출측에서는 주문 상태만 FAIL로 업데이트하면 됩니다.
     *
     * @param order      생성된 주문 엔티티
     * @param items      주문 항목 리스트
     * @param userCoupon 적용할 UserCoupon 엔티티 (없으면 null)
     */
    @Transactional
    public void processOrder(Order order, List<OrderItemCommand> items, UserCoupon userCoupon) {

        // 1) 재고 차감
        productService.reduceStockWithTx(items);
        historyService.record(order.getId(), "STOCK_DEDUCTED");

        // 2) 주문 항목 저장
        orderService.saveOrderItems(order.getId(), items);
        historyService.record(order.getId(), "ORDER_ITEMS_SAVED");

        // 3) 쿠폰 적용 (있으면)
        if (userCoupon != null) {
            couponService.applyCoupon(userCoupon, order);
            historyService.record(order.getId(), "COUPON_APPLIED");
        }

        // 주문 상태 변경(PAID)은 결제 이후에 처리됩니다.
    }
}
