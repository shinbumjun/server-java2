package kr.hhplus.be.server.infra.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CacheService { // Redis에 데이터를 JSON 형태로 저장하고 조회하는 서비스

    // Redis에 문자열 데이터를 저장/조회할 수 있도록 도와주는 Spring 제공 도구
    private final StringRedisTemplate redisTemplate;

    // 자바 객체 ↔ JSON 문자열 변환을 위한 Jackson 라이브러리
    private final ObjectMapper objectMapper;

    /**
     * Redis에 데이터를 저장 (JSON 문자열 형태로 저장됨)
     *
     * @param key         Redis에 저장할 키
     * @param value       저장할 자바 객체 (자동으로 JSON 변환됨)
     * @param ttlSeconds  캐시 만료 시간(초 단위)
     */
    public <T> void set(String key, T value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value); // 객체 → JSON 문자열
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds)); // Redis에 저장
        } catch (Exception e) {
            throw new RuntimeException("❌ 캐시 저장 실패", e);
        }
    }

    /**
     * Redis에서 데이터를 조회하고 자바 객체로 역직렬화
     *
     * @param key   Redis 키
     * @param type  반환 타입 정보 (List<ProductDto> 등)
     * @return      Redis에 저장된 JSON을 자바 객체로 변환하여 반환
     */
    public <T> T get(String key, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key); // Redis에서 JSON 문자열 가져옴
            return json != null ? objectMapper.readValue(json, type) : null; // JSON → 객체
        } catch (Exception e) {
            throw new RuntimeException("❌ 캐시 조회 실패", e);
        }
    }
}
