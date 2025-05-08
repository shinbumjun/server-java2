package kr.hhplus.be.server.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component // 스프링 빈 등록
@RequiredArgsConstructor
public class RedisLockManagerImpl implements RedisLockManager {

    private final StringRedisTemplate redisTemplate; // Redis 문자열 기반 템플릿
    private static final long LOCK_EXPIRE_MILLIS = 10000; // 락 유지 시간 (10초)

    @Override
    public boolean lockWithRetry(String key, long timeoutMs) {
        long start = System.currentTimeMillis();

        // timeoutMs가 끝날 때까지 락을 계속 시도
        while (System.currentTimeMillis() - start < timeoutMs) {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, "locked", Duration.ofMillis(LOCK_EXPIRE_MILLIS)); // 락 시도
            if (Boolean.TRUE.equals(success)) {
                return true; // 락 획득 성공
            }

            try {
                Thread.sleep(100); // 락 실패 시 100ms 기다렸다가 재시도
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 인터럽트 발생 시 종료
                return false;
            }
        }

        return false; // 제한 시간 내에 락을 획득하지 못함
    }

    @Override
    public void unlock(String key) {
        redisTemplate.delete(key); // 락 해제
    }
}
