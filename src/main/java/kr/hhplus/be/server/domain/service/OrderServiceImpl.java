package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // 생성자 주입
    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Long createOrder(OrderRequest orderRequest) {
        // 주문 생성 로직
        Order order = new Order(
                orderRequest.getUserId(),
                orderRequest.getUserCouponId(),
                false,  // 쿠폰 사용 여부 (여기선 가정)
                calculateTotalAmount(orderRequest),  // 주문 총액 계산
                "NOT_PAID",  // 주문 상태
                null, null  // 시간은 자동으로 설정될 것
        );

        // 엔티티 내에서 재고 체크 및 상품 존재 여부 확인
        order.validateOrder(orderRequest.getOrderItems(), productRepository);

        // 인메모리 방식 (하드코딩된 order ID)
        order.setId(1L);  // 하드코딩으로 ID 설정

        // 실제 저장 로직을 사용하지 않고, 단순히 인메모리에서 주문을 저장하는 형태로 처리
        orderRepository.save(order);  // save가 정상적으로 실행되어야 함

        // 주문 항목 처리
        saveOrderItems(order, orderRequest.getOrderItems());

        return order.getId();  // 생성된 주문 ID 반환
    }

    private void saveOrderItems(Order order, List<OrderRequest.OrderItem> orderItems) {
        // 주문 항목 저장 로직을 Order 엔티티 내에서 처리하도록 변경
        order.saveOrderItems(orderItems, productRepository);
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
