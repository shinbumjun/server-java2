package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 기본적인 CRUD 메서드는 JpaRepository에 구현되어 있으므로 별도의 구현이 필요하지 않음

    // 5분 전에 생성된 NOT_PAID 상태의 주문 조회
    // SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :createdAt
    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdAt);
}
