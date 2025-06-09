package kr.hhplus.be.server.interfaces.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductBestDto { // 베스트 상품 정보 DTO
    private Long id;        // 상품 ID
    private String name;    // 상품 이름
    private Integer price;  // 상품 가격
    private Integer sales;  // 판매량
    private Integer stock;  // 재고 수량
}
