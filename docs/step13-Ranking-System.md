# STEP13 - Redis 실시간 상품 랭킹 시스템

## 목표
- 최근 3일간 가장 많이 팔린 상품 TOP 5 조회 API 구현
- Redis ZSet + ZINCRBY + ZUNIONSTORE 활용

## 구성 요약

### 1. 판매 집계
- 결제 완료(PAID) 시 상품별 수량 → Redis ZSet(`ranking:daily:yyyyMMdd`)에 `ZINCRBY`

### 2. TTL 관리
- 일별 키에 TTL 4일 설정 → 3일 데이터 유지

### 3. 자정 병합
- 매일 00:00 `@Scheduled`
- 최근 3일치 키를 `ZUNIONSTORE`로 `ranking:3days`에 병합

### 4. 랭킹 조회 API
- `/api/v1/products/ranking`
- Redis `ranking:3days`에서 TOP 5 조회 → DB로 상세 정보 조회 후 응답

## 기술 포인트
- ZSet 기반 점수 정렬
- Redis 캐시로 실시간 응답 속도 향상
- TTL + 스케줄러로 정확성 유지

## 테스트 체크
- 결제 성공 → Redis 누적 확인
- 자정 병합 → `ranking:3days` 갱신 확인
- API 응답 → Redis 기반으로 빠르게 반환
