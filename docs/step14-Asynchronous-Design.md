# STEP14 - Redis 비동기 서버 선착순 쿠폰 발급 시스템

## 목표

- 선착순 쿠폰 발급 기능을 Redis 기반 비동기 설계로 개정
- 도중에 많은 사용자가 요청해도 중복 발급 방지, uc7ac고 초견 제어, ub79d 기반 동시성 제어가 가능하도록 구현

## 구성 요약

### 1. 락 기반 선착순 처리
- Redis (lock:coupon:{couponId}) 사용 → 하나의 쿠폰에 대해 ud55c 유저만 처리 진입 가능

### 2. Redis Set 활용한 중복 제어 + 재고 확인
- 쿠폰별 발급 사용자 목록: coupon:issued:{couponId}
- SADD 명령어로 중복 발급 방지
- SCARD(Set size)로 현재 발급 수량 확인 → 재고 초견 시 발급 차단

### 3. TTL 설정
- 발급 사용자 Set (coupon:issued:{couponId})에 1일 TTL 설정 → Redis 메모리 누수 방지

### 4. 최종 DB 발급 처리
- Redis 조건을 모두 통과한 경우에만 실제 쿠폰 DB에 발급 (user_coupon 저장)

### 주요 환경 (코드 기준)

1. Redis 락 → "lock:coupon:{couponId}"
2. SADD → "coupon:issued:{couponId}" 중복 방지
3. SCARD → 재고 초견 위험 판단
4. 쿠폰 DB 발급 처리
5. 락 해제

### 기술 포인트
- 동시성 제어: Redis 락 (lockWithRetry) 사용
- 중복 방지: Redis Set + SADD
- 재고 제어: Redis Set size (SCARD)
- 리소스 관리: TTL 1일 설정 → 메모리 자동 만료

### 테스트 체크
- 중복 발급 방지: 동일 유저가 2번 요청 시 1번만 성공
- 재고 초과 차단: 재고보다 많은 유저 요청 시 일부 실패
- 성공 시 DB 저장 확인: user_coupon 테이블에 정상 저장 여부 확인