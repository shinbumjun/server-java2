package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.domain.repository.UserCouponRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;               // 주문 저장 및 조회
    private final ProductRepository productRepository;           // 상품 정보 조회 (가격, 재고 등)
    private final OrderProductRepository orderProductRepository; // 주문 항목(상품과 수량) 저장
    private final UserCouponRepository userCouponRepository;     // 유저 쿠폰 정보 조회 (사용 여부 확인)
    private final CouponService couponService;                   // 쿠폰 사용 취소 처리


    @Override // orderRequest에서 사용자 ID, 주문 항목(상품ID, 수량), 쿠폰 ID를 추출하여 주문을 생성
    public Long createOrder(OrderRequest orderRequest) {
        // 1. 주문 총액 계산
        int totalAmount = calculateTotalAmount(orderRequest);  // 주문 총액 계산

        // 2. 주문 생성
        Order order = new Order(
                orderRequest.getUserId(),
                orderRequest.getUserCouponId(),
                false,  // 쿠폰 사용 여부 (여기선 가정, 실제로 쿠폰 적용 후 설정)
                totalAmount,  // 주문 총액 계산된 값
                "NOT_PAID",  // 주문 상태
                null, null  // 생성 시간은 자동으로 설정될 것
        );

        // 3. 주문 내 상품 재고 체크 및 차감
        order.validateOrder(orderRequest.getOrderItems(), productRepository); // 재고 및 상품 존재 여부 확인

        // 4. 주문 저장 (ID는 자동으로 생성됨)
        orderRepository.save(order);  // DB에 주문 저장

        // 5. 주문 항목 저장
        saveOrderItems(order, orderRequest.getOrderItems()); // 주문 항목 저장

        // 6. 주문 ID 반환
        return order.getId();  // 생성된 주문 ID 반환
    }

    // 5분마다 실행되는 스케줄러: 결제되지 않은 주문을 EXPIRED 상태로 처리
    // 5분 내 미결제 주문 취소 + 쿠폰 & 재고 복구
    @Transactional
    @Scheduled(fixedRate = 300000)  // 5분마다 실행
    public void checkOrderStatusAndExpireIfNecessary() { // 주문 상태 변경 + 쿠폰 복구 + 재고 복구
        // 5분 전에 생성된 결제 대기 주문을 조회
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        // 결제 대기 상태의 주문 조회
        List<Order> orders = orderRepository.findByStatusAndCreatedAtBefore("NOT_PAID", fiveMinutesAgo);

        for (Order order : orders) {
            try {
                // 1. 주문 상태를 EXPIRED로 변경
                order.setStatus("EXPIRED");
                orderRepository.save(order);  // 상태 업데이트

                // 2. 쿠폰 복구 처리
                if (order.getUserCouponId() != null) { // 주문에 쿠폰이 적용된 경우
                    UserCoupon userCoupon = userCouponRepository.findById(order.getUserCouponId())
                            .orElse(null);

                    if (userCoupon != null && Boolean.TRUE.equals(userCoupon.getIsUsed())) {
                        // 쿠폰이 실제로 사용된 경우에만 사용 취소
                        couponService.cancelCouponUsage(order.getUserCouponId());
                    }
                }

                // 3. 재고 복구 처리
                List<OrderProduct> orderProducts = orderProductRepository.findByOrdersId(order.getId());

                for (OrderProduct op : orderProducts) {
                    Product product = productRepository.findById(op.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

                    // 주문 수량만큼 재고 되돌리기
                    product.increaseStock(op.getQuantity());
                    productRepository.save(product);  // 재고 복구 저장
                }

            } catch (Exception e) {
                // 하나의 주문 처리 중 오류 발생 시 로깅
                e.printStackTrace();
            }
        }
    }


    private void saveOrderItems(Order order, List<OrderRequest.OrderItem> orderItems) {
        for (OrderRequest.OrderItem item : orderItems) {
            // 주문 항목 저장 로직을 서비스에서 처리
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            // 재고 부족 처리
            if (item.getQuantity() > product.getStock()) {
                throw new IllegalArgumentException("상품의 재고가 부족합니다.");
            }

            // 주문 항목 저장
            OrderProduct orderProduct = new OrderProduct(
                    item.getProductId(),
                    order.getId(),  // 현재 주문의 ID를 사용
                    product.getPrice() * item.getQuantity(),  // 주문 금액 (상품 가격 * 수량)
                    item.getQuantity()
            );

            // 실제 저장 구현
            orderProductRepository.save(orderProduct);  // 실제 데이터베이스에 저장
        }
    }

    private int calculateTotalAmount(OrderRequest orderRequest) {
        // 총액 계산 로직 (예: 각 상품의 가격 * 수량)
        int totalAmount = 0;
        for (OrderRequest.OrderItem item : orderRequest.getOrderItems()) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            totalAmount += product.getPrice() * item.getQuantity();
        }
        return totalAmount;
    }
}
