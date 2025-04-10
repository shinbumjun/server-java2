package kr.hhplus.be.server.interfaces.product;

import lombok.Getter;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private int code; // 상태 코드
    private String message; // 상태 메시지
    private ProductData data; // 실제 데이터

    @Getter
    @AllArgsConstructor
    public static class ProductData {
        private List<ProductDto> products; // 상품 목록
    }

    @Getter
    @AllArgsConstructor
    public static class ProductDto {  // 엔티티와 충돌을 피하기 위해 이름 변경
        private Long id;
        private String name;
        private Integer price;
        private Integer stock;
    }
}
