package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.order.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

}
