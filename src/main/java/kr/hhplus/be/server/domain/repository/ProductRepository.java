package kr.hhplus.be.server.domain.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 기본적인 CRUD 메서드는 JpaRepository에 구현되어 있으므로, 별도의 구현이 필요하지 않음.
    @Lock(LockModeType.PESSIMISTIC_WRITE)  // 비관적 락 적용
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    // 상품 ID 리스트로 상품 조회
    List<Product> findByIdIn(List<Long> productIds);
}
