package kr.hhplus.be.server.domain.integration;

import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
public class OrderRedisLockIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Redis 락 기반 동시 주문 테스트: 실패(사용자3)???, 성공(사용자1), 실패(사용자2)")
    void testConcurrentOrderWithRedisLock() throws InterruptedException {

        // 상품 A, B, C 생성 (재고 3개씩)
        // 주의: saveAllAndFlush()로 저장된 객체에서 ID를 가져와야 중복 저장/ID 미반영 문제 방지
        Product a = new Product(null, "A", "A 설명", 1000, 3, LocalDateTime.now(), LocalDateTime.now());
        Product b = new Product(null, "B", "B 설명", 1000, 3, LocalDateTime.now(), LocalDateTime.now());
        Product c = new Product(null, "C", "C 설명", 1000, 3, LocalDateTime.now(), LocalDateTime.now());

        List<Product> savedProducts = productRepository.saveAllAndFlush(List.of(a, b, c)); // 한 번에 저장 + flush
        Product savedA = savedProducts.get(0); // 저장된 객체에서 ID를 정확히 획득
        Product savedB = savedProducts.get(1);
        Product savedC = savedProducts.get(2);

        // 사용자 주문 요청 생성
        // 반드시 savedA/savedB/savedC의 ID를 사용해야 올바른 재고 감산이 적용됨
        OrderRequest req1 = new OrderRequest(1L, null, List.of(
                new OrderRequest.OrderItem(savedA.getId(), 1),
                new OrderRequest.OrderItem(savedB.getId(), 1),
                new OrderRequest.OrderItem(savedC.getId(), 1)
        ));
        OrderRequest req2 = new OrderRequest(2L, null, List.of(
                new OrderRequest.OrderItem(savedA.getId(), 3),
                new OrderRequest.OrderItem(savedC.getId(), 3)
        ));
        OrderRequest req3 = new OrderRequest(3L, null, List.of(
                new OrderRequest.OrderItem(savedB.getId(), 2),
                new OrderRequest.OrderItem(savedC.getId(), 2)
        ));


        List<OrderRequest> requests = List.of(req1, req2, req3);
        CountDownLatch latch = new CountDownLatch(requests.size());

        for (OrderRequest req : requests) {
            new Thread(() -> {
                try {
                    log.info("[테스트] 사용자 {} - 주문 시작", req.getUserId());
                    orderFacade.createOrder(req);
                    log.info("[테스트] 사용자 {} - 주문 성공", req.getUserId());
                } catch (Exception e) {
                    log.warn("[테스트] 사용자 {} - 주문 실패: {}", req.getUserId(), e.getMessage());
                } finally {
                    log.info("[테스트] 사용자 {} - 쓰레드 종료", req.getUserId());
                    latch.countDown();
                }
            }).start();
        }

        latch.await(); // 모든 쓰레드 종료 대기

        // 재고 확인
        Product resultA = productRepository.findById(savedA.getId()).orElseThrow();
        Product resultB = productRepository.findById(savedB.getId()).orElseThrow();
        Product resultC = productRepository.findById(savedC.getId()).orElseThrow();

        log.info("재고 결과 A: {}", resultA.getStock());
        log.info("재고 결과 B: {}", resultB.getStock());
        log.info("재고 결과 C: {}", resultC.getStock());

        // 검증: 재고가 음수가 아니고, 3개를 넘지 않아야 함
        assertTrue(resultA.getStock() >= 0 && resultA.getStock() <= 3);
        assertTrue(resultB.getStock() >= 0 && resultB.getStock() <= 3);
        assertTrue(resultC.getStock() >= 0 && resultC.getStock() <= 3);
    }

    @Test
    @DisplayName("Redis 락 기반 동시 주문 테스트: 코드가 문제인가? 재고가 있는데도 주문이 실패")
    void testConcurrentOrderWithRedisLock2() throws InterruptedException {

        // 상품 A, B, C 생성 (재고 3개씩)
        // 주의: saveAllAndFlush()로 저장된 객체에서 ID를 가져와야 중복 저장/ID 미반영 문제 방지
        Product a = new Product(null, "A", "A 설명", 1000, 3, LocalDateTime.now(), LocalDateTime.now());
        Product b = new Product(null, "B", "B 설명", 1000, 3, LocalDateTime.now(), LocalDateTime.now());
        Product c = new Product(null, "C", "C 설명", 1000, 3, LocalDateTime.now(), LocalDateTime.now());

        List<Product> savedProducts = productRepository.saveAllAndFlush(List.of(a, b, c)); // 한 번에 저장 + flush
        Product savedA = savedProducts.get(0); // 저장된 객체에서 ID를 정확히 획득
        Product savedB = savedProducts.get(1);
        Product savedC = savedProducts.get(2);

        // 사용자 주문 요청 생성
        // 반드시 savedA/savedB/savedC의 ID를 사용해야 올바른 재고 감산이 적용됨
        OrderRequest req1 = new OrderRequest(1L, null, List.of(
                new OrderRequest.OrderItem(savedA.getId(), 1),
                new OrderRequest.OrderItem(savedB.getId(), 1),
                new OrderRequest.OrderItem(savedC.getId(), 1)
        ));
        OrderRequest req2 = new OrderRequest(2L, null, List.of(
                new OrderRequest.OrderItem(savedA.getId(), 3),
                new OrderRequest.OrderItem(savedC.getId(), 3)
        ));
        OrderRequest req3 = new OrderRequest(3L, null, List.of(
                new OrderRequest.OrderItem(savedB.getId(), 2),
                new OrderRequest.OrderItem(savedC.getId(), 2)
        ));


        List<OrderRequest> requests = List.of(req1, req2, req3);
        CountDownLatch latch = new CountDownLatch(requests.size());

        for (OrderRequest req : requests) {
            new Thread(() -> {
                try {
                    log.info("[테스트] 사용자 {} - 주문 시작", req.getUserId());
                    orderFacade.createOrder(req);
                    log.info("[테스트] 사용자 {} - 주문 성공", req.getUserId());
                } catch (Exception e) {
                    log.warn("[테스트] 사용자 {} - 주문 실패: {}", req.getUserId(), e.getMessage());
                } finally {
                    log.info("[테스트] 사용자 {} - 쓰레드 종료", req.getUserId());
                    latch.countDown();
                }
            }).start();
        }

        latch.await(); // 모든 쓰레드 종료 대기

        // 재고 확인
        Product resultA = productRepository.findById(savedA.getId()).orElseThrow();
        Product resultB = productRepository.findById(savedB.getId()).orElseThrow();
        Product resultC = productRepository.findById(savedC.getId()).orElseThrow();

        log.info("재고 결과 A: {}", resultA.getStock());
        log.info("재고 결과 B: {}", resultB.getStock());
        log.info("재고 결과 C: {}", resultC.getStock());

        // 검증: 재고가 음수가 아니고, 3개를 넘지 않아야 함
        assertTrue(resultA.getStock() >= 0 && resultA.getStock() <= 3);
        assertTrue(resultB.getStock() >= 0 && resultB.getStock() <= 3);
        assertTrue(resultC.getStock() >= 0 && resultC.getStock() <= 3);
    }
}
