package kr.hhplus.be.server.interfaces.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProductBestResponse { // 상품 베스트 응답 객체
    private int code; // HTTP 상태 코드
    private String message; // 응답 메시지
    private List<ProductBestDto> data; // 베스트 상품 목록
}