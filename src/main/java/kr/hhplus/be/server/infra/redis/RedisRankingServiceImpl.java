package kr.hhplus.be.server.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisRankingServiceImpl implements RedisRankingService {

    private final StringRedisTemplate redisTemplate;

    private static final String DAILY_RANKING_KEY_PREFIX = "ranking:daily:";
    // TTL: 3일 + 병합 작업이 끝날 때까지 보존 여유 1일 (총 4일)
    private static final Duration DAILY_TTL = Duration.ofDays(4);

    @Override // Sorted Set : ZSet(정렬된 집합)에 날짜(key) + 상품ID(member) 기준으로 수량(score)을 누적
    public void incrementDailyProductRanking(Long productId, int quantity) {
        String key = getTodayRankingKey(); // 예: ranking:daily:20240515
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), quantity); // 누적
        redisTemplate.expire(key, DAILY_TTL); // TTL 설정
    }

    @Override // 집합 계열 : ZUNIONSTORE
    public void mergeLast3DaysRanking(String destKey, String... dailyKeys) {
        redisTemplate.opsForZSet().unionAndStore(
                dailyKeys[0],                              // 기준 키
                List.of(dailyKeys).subList(1, dailyKeys.length), // 나머지 키들
                destKey                                    // 결과 저장 키
        );
    }

    private String getTodayRankingKey() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        return DAILY_RANKING_KEY_PREFIX + today;
    }
}
