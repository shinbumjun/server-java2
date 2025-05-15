package kr.hhplus.be.server.infra.redis;

public interface RedisRankingService {

    /**
     * 오늘 날짜 기준 랭킹에 상품 주문 수를 누적합니다.
     *
     * @param productId 상품 ID
     * @param quantity 주문 수량 (score 증가치)
     */
    void incrementDailyProductRanking(Long productId, int quantity);

    /**
     * 최근 3일의 일별 랭킹 키를 합산하여 새로운 랭킹 키에 저장합니다.
     *
     * @param destinationKey 병합 결과가 저장될 키 (ex: ranking:merged:3days)
     * @param dailyKeys 합산할 일간 랭킹 키들 (ex: ranking:daily:20240513 ...)
     */
    void mergeLast3DaysRanking(String destinationKey, String... dailyKeys);
}
