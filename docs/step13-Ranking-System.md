🔩 STEP13 - Redis 기반 실시간 상품 랭킹 시스템

✅ 구현 목표

최근 3일간 가장 많이 팔림 상품을 실시간으로 조회하는 랭킹 API 제공

Redis의 Sorted Set (ZSet) 구조와 ZINCRBY, ZUNIONSTORE 명령을 활용

🏗️ 시스템 구성

1. 주문 성공 시 랭킹 누적

결제 완료(PAID)된 주문에 한해 OrderProduct에서 상품 ID, 수량 조회

Redis ZSet에 ZINCRBY 사용하여 해당 상품 판매 수량 누적

redisTemplate.opsForZSet().incrementScore("ranking:daily:yyyyMMdd", productId, quantity);

2. 일일 누적 TTL 관리

ranking:daily:yyyyMMdd 키에 TTL 4일 설정 → 정확성 + 3일 총계 유지 목적

3. 자정 스케줄러로 3일 랭킹 병합

@Scheduled(cron = "0 0 0 * * *")

3일간의 ZSet 키를 ZUNIONSTORE로 병합하여 ranking:3days에 저장

unionAndStore(d1, [d2, d3], "ranking:3days")

4. 실시간 랭킹 API 구현

/api/v1/products/ranking

RedisTemplate으로 ranking:3days ZSet에서 TOP 5 추출

추출된 상품 ID를 기반으로 DB에서 상품 상세 조회 후 응답

🧠 기술 포인트

ZSet의 정렬 특성을 활용한 점수 기반 실시간 정렬

DB → Redis 총계 → 캐시 조회의 흉년 완성

TTL과 스케줄리를 통해 정확성과 실시간성 관리

🧪 검증 사항

주문 → 결제 → Redis 총계 누적 동작 확인

자정만분 3일치 랭킹 자동 병합됨 확인

API 호출 시 Redis 캐시만을 조회해 빠른 응답 확인