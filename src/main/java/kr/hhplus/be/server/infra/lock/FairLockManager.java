package kr.hhplus.be.server.infra.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FairLockManager {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String QUEUE_KEY = "fair:order:queue";

    /**
     * 큐에 사용자 ID를 등록하고, 순서가 될 때까지 대기
     */
    public void waitMyTurn(Long userId, long timeoutMillis) {
        String userIdStr = userId.toString();
        redisTemplate.opsForList().rightPush(QUEUE_KEY, userIdStr);

        long waitStart = System.currentTimeMillis();
        while (true) {
            String head = redisTemplate.opsForList().index(QUEUE_KEY, 0);
            if (userIdStr.equals(head)) {
                return; // 내 순서가 됨
            }

            if (System.currentTimeMillis() - waitStart > timeoutMillis) {
                redisTemplate.opsForList().remove(QUEUE_KEY, 1, userIdStr);
                throw new IllegalStateException("공정 락 큐 대기 시간 초과");
            }

            try {
                Thread.sleep(100); // 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("락 대기 중 인터럽트 발생", e);
            }
        }
    }

    /**
     * 락 큐에서 제거
     */
    public void releaseTurn(Long userId) {
        redisTemplate.opsForList().remove(QUEUE_KEY, 1, userId.toString());
    }
}
