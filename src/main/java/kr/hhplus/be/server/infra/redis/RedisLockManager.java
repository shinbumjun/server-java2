package kr.hhplus.be.server.infra.redis;

// Redis 기반 분산 락 기능을 위한 인터페이스 정의
public interface RedisLockManager {
    // 락을 획득하려고 timeoutMs 시간 동안 재시도함
    boolean lockWithRetry(String key, long timeoutMs);

    // 락 해제
    void unlock(String key);
}

