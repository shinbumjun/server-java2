package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.repository.OrderProductRepository;
import kr.hhplus.be.server.domain.repository.ProductRepository;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService { // 1

    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;

    @Transactional
    @Override
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional // ← 여기에 AOP 프록시가 붙음, 재고차감
    public void reduceStockWithTx(List<OrderRequest.OrderItem> items) {
        for (OrderRequest.OrderItem item : items) {
            checkAndReduceStock(item.getProductId(), item.getQuantity()); // 유틸처럼 사용
        }
    }

    // @Transactional(propagation = Propagation.REQUIRES_NEW) // 새 트랜잭션을 새로 열어줌, 실패한 상품만 따로 처리하고 싶을때
    @Transactional  // 기본 전파 속성(REQUIRED), 트랜잭션 안에서 비관적 락을 적용
    @Override
    public void checkAndReduceStock(Long productId, Integer quantity) {
        // 1. 상품 조회 (비관적 락 적용)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));

        log.info("[{}] {} 상품 - 검증 전 재고: {}, 요청 수량: {}",
                Thread.currentThread().getName(), product.getProductName(), product.getStock(), quantity);

        // 2. 재고 검증 및 차감
        product.validateStock(quantity);  // 재고 검증
        product.reduceStock(quantity);    // 재고 차감

        // 3. 변경된 상품 상태 저장
        productRepository.saveAndFlush(product); // 변경된 내용 즉시 DB에 반영
    }

//    @Transactional  // 트랜잭션 내에서 비관적 락과 일괄 저장 처리
//    @Override
//    public void checkAndReduceStock(List<OrderRequest.OrderItem> items) {
//        Map<Long, Product> productMap = new HashMap<>();
//
//        // 1. 모든 상품 비관적 락 조회 + 재고 검증
//        for (OrderRequest.OrderItem item : items) {
//            Product product = productRepository.findByIdForUpdate(item.getProductId())
//                    .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));
//
//            log.info("[{}] {} 상품 - 검증 전 재고: {}, 요청 수량: {}",
//                    Thread.currentThread().getName(), product.getProductName(), product.getStock(), item.getQuantity());
//
//            product.validateStock(item.getQuantity());
//            // productMap.put(product, item.getQuantity());
//            productMap.put(product.getId(), product);
//        }
//
//        // 2. 모든 상품 재고 차감
//        for (OrderRequest.OrderItem item : items) {
//            Product product = productMap.get(item.getProductId());
//            product.validateStock(item.getQuantity());
//            product.reduceStock(item.getQuantity());
//        }
//
//        // 3. 일괄 저장 (flush는 트랜잭션 커밋 시 자동)
//        productRepository.saveAll(productMap.values());
//    }


    // 재고 복구
    @Override
    public void revertStockByOrder(Long orderId) {
        // 재고 복구 처리
        List<OrderProduct> orderProducts = orderProductRepository.findByOrdersId(orderId); // 해당 주문에 속한 모든 상품 항목

        for (OrderProduct op : orderProducts) {
            Product product = productRepository.findById(op.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            // 주문 수량만큼 재고 되돌리기
            product.increaseStock(op.getQuantity());
            // 재고 복구 저장
            productRepository.save(product);
        }
    }


    @Override
    public List<ProductBestDto> getTop5BestSellingProducts() { // 판매량 상위 5개 상품 조회
        // DB에서 판매량 상위 5개 상품 정보 조회 (상품 ID, 이름, 가격, 재고, 판매량)
        List<Object[]> rawData = orderProductRepository.findTop5SellingProductsWithInfo();

        // DTO 리스트로 변환하여 반환
        return toProductBestDtoList(rawData);
    }

    private List<ProductBestDto> toProductBestDtoList(List<Object[]> rawData) {
        List<ProductBestDto> result = new ArrayList<>();

        for (Object[] row : rawData) {
            // 컬럼 순서대로 데이터 추출 및 형 변환
            Long id = ((Number) row[0]).longValue();       // 상품 ID
            String name = (String) row[1];                 // 상품 이름
            Integer price = ((Number) row[2]).intValue();  // 상품 가격
            Integer stock = ((Number) row[3]).intValue();  // 재고 수량
            Integer sales = ((Number) row[4]).intValue();  // 판매량

            // DTO 생성 및 결과 리스트에 추가
            result.add(new ProductBestDto(id, name, price, sales, stock));
        }

        return result;
    }



}
