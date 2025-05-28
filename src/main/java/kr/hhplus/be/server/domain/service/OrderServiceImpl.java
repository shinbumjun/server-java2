package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.application.order.OrderItemCommand;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;               // 주문 저장 및 조회
    private final ProductRepository productRepository;           // 상품 정보 조회 (가격, 재고 등)
    private final OrderProductRepository orderProductRepository; // 주문 항목(상품과 수량) 저장
    private final UserCouponRepository userCouponRepository;     // 유저 쿠폰 정보 조회 (사용 여부 확인)
    private final CouponService couponService;                   // 쿠폰 사용 취소 처리


    @Override // orderRequest에서 사용자 ID, 주문 항목(상품ID, 수량), 쿠폰 ID를 추출하여 주문을 생성
    public Order createOrder(User user, UserCoupon userCoupon, List<OrderItemCommand> items) {
        // 1. 주문 총액 계산
        int totalAmount = calculateTotalAmount(items);  // 주문 총액 계산

        // 2. 주문 생성
        Order order = new Order(
                user.getId(),
                userCoupon != null ? userCoupon.getCouponId() : null, // 있으면 넣기
                false,  // 쿠폰 사용 여부 (여기선 가정, 실제로 쿠폰 적용 후 설정)
                totalAmount,  // 주문 총액 계산된 값
                "NOT_PAID",  // 주문 상태
                null, null  // 생성 시간은 자동으로 설정될 것
        );

        // 3. 주문 내 상품 재고 체크
        order.validateOrder(items, productRepository); // 재고 및 상품 존재 여부 확인

        // 4. 주문 저장 (ID는 자동으로 생성됨)
        orderRepository.save(order);  // DB에 주문 저장

        // 5. 주문 항목 저장
        saveOrderItems(order.getId(), items); // 주문 항목 저장

        // 6. 주문 ID 반환
        return order;  // 생성된 주문 ID 반환
    }

    // 5분 이상 결제 안 된 주문 조회
    @Override
    public List<Order> getUnpaidOrdersBefore(LocalDateTime time) {
        List<Order> orders = orderRepository.findByStatusAndCreatedAtBefore("NOT_PAID", time); // 결제 대기 상태의 주문 조회
        log.info("[조회] 미결제 주문 수: {}, 기준 시각: {}", orders.size(), time);
        return orders;
    }

    // 주문 상태를 EXPIRED로 변경
    @Override
    public void expireOrder(Order order) {
        order.setStatus("EXPIRED"); // 주문 상태를 EXPIRED로 변경
        orderRepository.save(order); // 상태 업데이트
        log.info("주문 상태 변경 → EXPIRED 처리 완료 - UserId: {}", order.getUserId());
    }

    @Override // 주문 조회
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));
    }

    @Transactional // 실패처리에도 트랜잭션을 걸어야함
    @Override
    public void updateOrderStatusToFail(Long orderId) { // 실패 시 상태 FAIL
        Order order = getOrderById(orderId);
        order.updateStatusToFail(); // 상태 변경 메서드는 엔티티에서 정의
        orderRepository.save(order);
    }

    @Override
    public void validatePayableOrder(Long orderId) {
        Order order = getOrderById(orderId); // 기존에 존재하는 조회 메서드 재사용
        order.validatePayable(); // 도메인 엔티티가 상태 검증 수행 (EXPIRED, PAID 검사)
    }

    @Override // Redis ZSet에 오늘 랭킹 집계 누적
    public List<OrderProduct> getOrderProductsByOrderId(Long orderId) {
        return orderProductRepository.findByOrdersId(orderId);
    }

    @Override // 주문 상태 변경: 결제 성공시 상태를 PAID로 업데이트
    public void updateOrderStatusToPaid(Long orderId) {
        Order order = getOrderById(orderId); // 주문 가져오기
        order.updateStatusToPaid(); // 도메인에서 상태 변경 (EXPIRED일 경우 내부에서 막게 할 수 있음)
        orderRepository.save(order); // 변경 사항 저장
    }

    @Override
    public void saveOrderItems(Long orderId, List<OrderItemCommand> items) {
        for (OrderItemCommand item : items) {
            // 주문 항목 저장 로직을 서비스에서 처리
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));


            // 주문 항목 저장
            OrderProduct orderProduct = new OrderProduct(
                    item.productId(),
                    orderId,  // 현재 주문의 ID를 사용
                    product.getPrice() * item.quantity(),  // 주문 금액 (상품 가격 * 수량)
                    item.quantity()
            );

            // 실제 저장 구현
            orderProductRepository.save(orderProduct);  // 실제 데이터베이스에 저장
        }
    }

    private int calculateTotalAmount(List<OrderItemCommand> items) {
        // 총액 계산 로직 (예: 각 상품의 가격 * 수량)
        int totalAmount = 0;
        for (OrderItemCommand item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            totalAmount += product.getPrice() * item.quantity();
        }
        return totalAmount;
    }
}
