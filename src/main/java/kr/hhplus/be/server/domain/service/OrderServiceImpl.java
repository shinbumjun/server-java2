package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동 생성
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;

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
