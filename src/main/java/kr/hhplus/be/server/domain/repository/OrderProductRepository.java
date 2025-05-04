package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.order.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {

    // 필드명 ordersId와 정확히 일치해야 Spring Data JPA가 자동으로 인식
    // SELECT op FROM OrderProduct op WHERE op.orderId = :orderId
    List<OrderProduct> findByOrdersId(Long ordersId);


    // 판매가 완료된 건에 한정해 상위 5개 상품 통계
    /*
        상품 ID, 이름, 가격, 재고, 총 판매량(SUM)

     */
    @Query(value = """
    SELECT p.id, p.product_name, p.price, p.stock, SUM(op.quantity) AS sales
    FROM order_product op
    JOIN orders o ON op.orders_id = o.id
    JOIN product p ON op.product_id = p.id
    WHERE o.status = 'PAID'
    GROUP BY p.id, p.product_name, p.price, p.stock
    ORDER BY sales DESC
    LIMIT 5
""", nativeQuery = true)
    List<Object[]> findTop5SellingProductsWithInfo();
}