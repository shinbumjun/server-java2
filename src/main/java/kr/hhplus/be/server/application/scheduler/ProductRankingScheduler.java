package kr.hhplus.be.server.application.scheduler;

import kr.hhplus.be.server.infra.redis.RedisRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingScheduler { // ranking:3days → Redis ZSet 기반 누적 랭킹 (실시간 응답용)

    private final RedisRankingService redisRankingService;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void merge3DayRanking() {
        // 오늘은 자정이라 데이터가 없음 → 오늘은 제외
        String day1 = LocalDate.now().minusDays(3).format(DateTimeFormatter.BASIC_ISO_DATE); // 3일 전
        String day2 = LocalDate.now().minusDays(2).format(DateTimeFormatter.BASIC_ISO_DATE); // 2일 전
        String day3 = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE); // 어제

        String destKey = "ranking:3days";
        String key1 = "ranking:daily:" + day1;
        String key2 = "ranking:daily:" + day2;
        String key3 = "ranking:daily:" + day3;

        redisRankingService.mergeLast3DaysRanking(destKey, key1, key2, key3);

        log.info("최근 3일 랭킹 병합 완료 → [{}] from {}, {}, {}", destKey, key1, key2, key3);
    }
}
