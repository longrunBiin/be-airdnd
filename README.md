# Air-Bob
에어비앤비 클론 백엔드 - Spring Boot 기반 숙소 예약 플랫폼

## 주요 기능

- 회원가입 / 로그인 (세션 기반 인증)
- 숙소 검색, 예약, 리뷰 등록
- 호스트용 숙소 등록 / 수정 / 삭제
- 쿠폰 발급 (선착순, Redis 이벤트 큐)

---

## 기술 스택

| 분류      | 기술 |
|-----------|------|
| Language  | Java 21 |
| Framework | Spring Boot 3.5.0, Spring Data JPA, QueryDsl |
| DB        | MySQL (RDS, Redis) |
| Infra     | AWS EC2, RDS, S3, Code Deploy, ALB |
| Tooling   | GitHub Actions, Swagger, RestDocs, flyway |
| Test   | K6, Prometheus, Grafana, Test container |

## ERD
<img width="2572" height="1388" alt="image" src="https://github.com/user-attachments/assets/3698f68c-a7bb-413d-a6c7-89e8b25af506" />

## 인프라 아키텍처
<img width="1526" height="1122" alt="image" src="https://github.com/user-attachments/assets/ceeaba6f-d109-457a-bcc0-a4b116638253" />

## 핵심 구현 포인트

- **Redis 기반 이벤트 큐**
  - ZSet + Lua 스크립트를 이용해 **선착순 쿠폰 발급 시 Race Condition 방지**
  - 동시에 여러 요청이 들어와도 **원자적 처리 보장**, 실패 시 응답 즉시 반환

- **Redis 분산락을 활용한 예약 동시성 제어**
  - `@DistributedLock` 커스텀 어노테이션 + AOP로 구현
  - 동일 숙소에 대해 중복 예약 발생하지 않도록 **Lock 획득 후 예약 트랜잭션 수행**
  - 구현 예:
    ```java
    @DistributedLock(key = "#accommodationId", lockName = "reservation")
    public Long createReservation(...) { ... }
    ```

- **Redis 캐시 활용 (최근 숙소 캐싱)**
  - 최근 등록 숙소 목록을 Redis에 캐싱하여 조회 성능 개선
  - 커스텀 어노테이션 + AOP로 분리, 명확한 책임 분산
  - 구현 예:
    ```java
        @RedisListCacheable(
            key = "recent:accommodations",
            type = AccommodationSearchResponseDto.class
    )
    public List<AccommodationSearchResponseDto> searchAccommodations(AccommodationSearchConditionDto request, Pageable pageable) {
        return accommodationRepository.searchByFilter(request, pageable);
    }
    ```

- **세션 공유 문제 해결**
  - EC2 다중 인스턴스 환경에서 Redis 기반 Session 적용
  - 세션 일관성 유지 + Sticky Session 불필요

- **무중단 배포 전략**
  - CodeDeploy Blue/Green 배포 방식 채택
  - 배포 시 ALB Target Group 스위칭으로 Zero Downtime 실현

- **성능 테스트 및 모니터링**
  - `K6` 부하 테스트 스크립트 작성 (예약/쿠폰 발급 등 시나리오)
  - `Prometheus + Grafana` 연동하여 자원 사용률, 요청 처리시간 시각화

## 기타

- API 문서: Swagger + Spring REST Docs 기반 자동 생성  
- 데이터 마이그레이션: Flyway를 이용한 스키마 버전 관리  
- 테스트 환경: Testcontainers를 이용한 통합 테스트
  
