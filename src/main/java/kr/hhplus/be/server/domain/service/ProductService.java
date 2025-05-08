package kr.hhplus.be.server.domain.service;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.interfaces.order.OrderRequest;
import kr.hhplus.be.server.interfaces.product.ProductBestDto;

import java.util.List;

public interface ProductService {
    List<Product> getProducts();  // 상품 목록 조회

    void checkAndReduceStock(Long productId, Integer quantity); // 상품 재고 차감

    void revertStockByOrder(Long id); // 재고 복구

    List<ProductBestDto> getTop5BestSellingProducts(); // 판매량 상위 5개 상품 조회

    void checkAndReduceStock(List<OrderRequest.OrderItem> orderItems); // 주문된 상품의 재고 확인 및 차감 (일괄 처리)
}
