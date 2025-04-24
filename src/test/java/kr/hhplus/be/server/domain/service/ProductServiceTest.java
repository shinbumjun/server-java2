package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest // 통합 테스트
@Transactional
public class ProductServiceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductServiceImpl productService;

    @Test
    @DisplayName("동시성 테스트: 두 스레드가 재고를 차감할 때 발생하는 문제")
    void testCheckAndReduceStock_ConcurrencyIssue() throws InterruptedException {
        // Given: 테스트를 위한 상품 ID와 주문 수량 설정
        Long productId = 1L;
        Integer quantity = 5;

        // 상품 객체 생성 (상품 ID, 상품명, 설명, 가격, 재고, 생성시간, 수정시간)
        Product product = new Product(1L, "Product A", "Description", 100, 50, LocalDateTime.now(), LocalDateTime.now());
        productRepository.save(product);

        // 상품이 저장소에서 조회될 때 해당 상품을 반환하도록 mock 설정
        // findByIdForUpdate는 실제 DB에서 락을 걸어서 처리
        productRepository.findByIdForUpdate(productId);

        // 두 스레드가 동시에 동일한 상품을 주문하려고 시도하는 시나리오 생성
        Thread thread1 = new Thread(() -> {
            try {
                // 첫 번째 스레드가 재고 차감 메서드 호출
                productService.checkAndReduceStock(productId, quantity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                // 두 번째 스레드가 재고 차감 메서드 호출
                productService.checkAndReduceStock(productId, quantity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 두 스레드를 동시에 시작 (동시 재고 차감 시도)
        thread1.start();
        thread2.start();

        // 두 스레드가 끝날 때까지 기다림
        thread1.join();
        thread2.join();

        // 검증: 재고가 두 번의 차감을 마친 후 40이어야 함 (초기 재고 50에서 각 5씩 차감)
        assertEquals(40, product.getStock()); // 예상되는 재고는 40 (50 - 5 - 5)
    }
}
