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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest // 통합 테스트
// @Transactional
public class ProductServiceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductServiceImpl productService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("동시성 테스트: 두 스레드가 재고를 차감할 때 발생하는 문제")
    void testCheckAndReduceStock_ConcurrencyIssue() throws InterruptedException {
        // Given
        Long productId;
        Integer quantity = 5;

        Product product = new Product(null, "Product A", "Description", 100, 50, LocalDateTime.now(), LocalDateTime.now());
        productRepository.save(product);
        productRepository.flush();

        productId = product.getId();

        Runnable task = () -> {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setName("tx-" + Thread.currentThread().getName());
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            TransactionStatus status = transactionManager.getTransaction(def);

            try {
                productService.checkAndReduceStock(productId, quantity);
                transactionManager.commit(status);
            } catch (Exception e) {
                transactionManager.rollback(status);
                e.printStackTrace();
            }
        };

        Thread thread1 = new Thread(task, "Thread-1");
        Thread thread2 = new Thread(task, "Thread-2");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Then
        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));
        assertEquals(40, updatedProduct.getStock());
    }
        // ***에러
        // 트랜잭션이 공유되면서 락 경합(lock contention)이 발생, 동시에 비관적 락을 요청하다가 죽음
        // -> 스레드마다 각각 별도의 트랜잭션을 시작


}
