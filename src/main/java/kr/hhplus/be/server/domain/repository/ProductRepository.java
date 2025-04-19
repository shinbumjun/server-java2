package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 기본적인 CRUD 메서드는 JpaRepository에 구현되어 있으므로, 별도의 구현이 필요하지 않음.
}
