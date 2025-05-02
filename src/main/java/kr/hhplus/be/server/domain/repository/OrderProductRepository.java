package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.order.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {

    // 필드명 ordersId와 정확히 일치해야 Spring Data JPA가 자동으로 인식
    // SELECT op FROM OrderProduct op WHERE op.orderId = :orderId
    List<OrderProduct> findByOrdersId(Long ordersId);
}