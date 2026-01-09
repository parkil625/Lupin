# 💊 Lupin (루팡) - Premium Medical Goods Auction & Telemedicine Platform

> "의료 소비의 새로운 패러다임: 실시간 경매부터 비대면 진료까지"
>
> Development Period: 2025.11.06 ~ 2026.01.09 (9 Weeks)
> Team Size: 3 Engineers

<br/>

## 1. Project Overview (프로젝트 개요)
Lupin은 폐쇄적인 의료 용품 시장의 정보 불균형을 해소하고, 의료 소비자의 접근성을 극대화하기 위해 설계된 **수직적 통합 플랫폼(Vertical Platform)**입니다. 희소성 있는 건강기능식품의 **실시간 경매(Real-time Auction)** 시스템과 전문 의료진과의 **비대면 진료(Telemedicine)** 및 상담 기능을 하나의 생태계로 통합했습니다.

> ⚠️ Note: 현재 AWS 운영 비용 최적화를 위해 라이브 배포는 중단되었으며, 아래 시연 영상과 코드, 그리고 성능 테스트 결과를 통해 프로젝트의 기술적 성취를 확인하실 수 있습니다.

<br/>

## 2. Key Features & Demonstrations (주요 기능 및 시연)

### 🩺 Telemedicine & Chat (비대면 진료 및 상담)
환자와 의사 간의 실시간 소통을 지원합니다. WebSocket을 활용한 저지연 채팅과 처방전 발급 프로세스를 구현했습니다.
![Telemedicine Demo](./lupin/public/demo_telemedicine.gif)

### 🔨 Real-time Auction (실시간 경매)
동시성 이슈를 제어하며 수천 건의 입찰 요청을 안정적으로 처리하는 고성능 경매 시스템입니다.



### 👥 Community & Feed (커뮤니티 및 피드)
사용자 경험(UX)을 극대화한 피드 시스템과 알림 서비스를 제공합니다.
![Feed Demo](./lupin/public/demo_feed.gif)

<br/>

## 3. Team Roles & Contribution (팀원 역할 및 기여도)

R&R(Role and Responsibilities)을 명확히 분배하여 각 도메인의 전문성을 강화했습니다.

| Team Member | Position | Domain Responsibilities | Tech Keywords |
|:---:|:---:|:---|:---|
| 박선일 | Core Engineer | Infrastructure & Core Tech<br/>- 회원 관리(Auth) 및 소셜 로그인(OAuth 2.0)<br/>- 커뮤니티(Feed/Comment) 및 알림(SSE)<br/>- Cloud Architecture (AWS, Cloudflare, Docker)<br/>- Performance Engineering (k6 Load Test)<br/>- CI/CD Pipeline 구축 | Spring Boot, Redis, Cloudflare, k6, Docker, GitHub Actions |
| 홍세민 | Team Lead | Medical Domain<br/>- 의사/환자 도메인 설계<br/>- 처방전(Prescription) 발급 로직<br/>- 실시간 채팅(WebSocket) 시스템 구현 | WebSocket, JPA, MySQL, React |
| 최재홍 | Engineer | Commerce Domain<br/>- 경매(Auction) 비즈니스 로직 설계<br/>- 입찰 프로세스 및 타이머 구현 | Spring Batch, Scheduler, MySQL |

<br/>

## 4. Technical Architecture (기술 아키텍처)

### 🛠 Tech Stack
| Category | Stack |
|:---:|:---|
| Frontend | React, TypeScript, Vite, Tailwind CSS, Storybook, Zustand |
| Backend | Java 17, Spring Boot 3.x, Spring Data JPA, QueryDSL |
| Database | MySQL 8.0, Redis (Caching & Pub/Sub) |
| Infra & DevOps | AWS (EC2, RDS, S3), Docker, Nginx, Cloudflare, GitHub Actions |
| Testing | k6 (Load Testing), Playwright (E2E), Vitest (Unit) |

<br/>

## 5. Technical Challenges & Solutions (기술적 도전과 해결)

### 🚀 1. Extreme Performance Optimization (극한의 성능 최적화)
**[Problem]** 초기 AWS S3 직접 서빙 방식은 고화질 의료 용품 이미지 로딩 시 **LCP(Largest Contentful Paint)가 35.2초**에 달해 사용자 이탈이 우려되었습니다.
**[Solution]**
* Edge Caching: Cloudflare CDN을 도입하여 정적 리소스를 엣지 로케이션에서 캐싱했습니다.
* Image Processing: 업로드된 이미지를 5.0MB에서 **50KB(WebP/AVIF)**로 압축 및 포맷 변환하는 파이프라인을 구축했습니다.
* Lazy Loading: `lazyWithPreload` 유틸리티를 구현하여 초기 번들 사이즈를 최소화했습니다.
**[Result]**
* Payload Reduction: 이미지 크기 **99% 감소** (5.0MB → 50KB)
* Lighthouse Score: 모바일 성능 점수 **52점 → 86점** (홈 기준), LCP **0.8초** 달성

### 📊 2. Identifying & Resolving Bottlenecks (데이터 기반 병목 해결)
**[Problem]** 복잡한 도메인 로직(경매/피드)으로 인해 API 응답 속도가 저하되는 현상을 발견했습니다.
**[Solution]**
* Proactive Testing (개인 주도): 팀 필수 사항이 아니었으나, 시스템 안정성을 위해 자발적으로 **k6**를 도입하여 부하 테스트를 수행했습니다.
* Query Optimization: Hibernate 통계 로그를 분석하여 **N+1 문제**가 발생하는 지점을 식별, Fetch Join 및 QueryDSL로 쿼리를 튜닝했습니다.
**[Result]**
* DB Query Reduction: 트랜잭션 당 쿼리 수 **89회 → 15회 (83.1% 감소)**
* Latency Improvement: 피드 API 응답 속도 **850ms → 184ms (4.6배 단축)**

### 🛡️ 3. Concurrency Control in Auctions (경매 동시성 제어)
**[Problem]** 인기 경매 마감 직전, 수천 건의 입찰이 동시에 발생할 때 **Race Condition(경쟁 상태)**으로 인한 데이터 불일치 위험이 있었습니다.
**[Solution]** Redis Distributed Lock (Redisson)을 적용하여 입찰 트랜잭션의 원자성(Atomicity)을 보장하고, `bid-race-test.js` 시나리오를 통해 임계치를 검증했습니다.

<br/>

## 6. Quantitative Performance Report (정량적 성능 지표)

박선일(Core Engineer) 주도로 측정한 성능 개선 결과입니다.

### 📉 Backend & Database Performance (k6 & Hibernate)
단위 테스트 및 통합 테스트를 넘어, 실제 사용자 시나리오 기반의 부하 테스트를 통해 병목을 제거했습니다.

| Measurement Criteria | Before | After | Improvement | Note |
|:---:|:---:|:---:|:---:|:---|
| Feed API Latency | 850ms | **184ms** | **4.6x Faster** 🚀 | 조회 성능 최적화 |
| Ranking API Latency | 680ms | **274ms** | **2.5x Faster** | 캐싱 전략 적용 |
| Home API Latency | 520ms | **269ms** | **1.9x Faster** | |
| DB Queries per Req | 89 queries | **15 queries** | **83% Reduced** 📉 | N+1 문제 해결 |

### ⚡ Frontend Core Web Vitals (Lighthouse)
모바일 환경에서의 사용자 경험(UX)을 최우선으로 개선했습니다.

| Page | Type | Performance Score (Before) | Score (After) | LCP Speed |
|:---|:---:|:---:|:---:|:---:|
| Main Home | Mobile | 52 | **86** | 35.2s → 4.0s |
| | Web | - | **100** | 0.8s |
| Feed Page | Mobile | 55 | **87** | 7.6s → 3.9s |
| Intro Page | Mobile | 58 | **93** | 8.2s → 3.2s |


## 🔐 Concurrency Performance Benchmark

실시간 경매 환경(다중 사용자 동시 입찰)을 가정하고,  
동일한 비즈니스 로직에서 **동시성 제어 전략별 성능 비교 테스트**를 수행했습니다.

### 📊 Benchmark Results

| **구분**  | **핵심 지표 (Metric)**     | 비관적 락     | 분산 락    | Redis Lua Script (최적화) | **비고**    |
| ------- | ---------------------- | --------- | ------- | ---------------------- | --------- |
| **속도**  | **평균 응답 시간 (avg)**     | 2.91s     | 2.98s   | **2.54s**              | ↓ 낮을수록 좋음 |
| **속도**  | **하위 95% 응답 시간 (p95)** | **5.42s** | 5.78s   | 5.62s                  | ↓ 안정성 지표 |
| **처리량** | **초당 처리 요청 (TPS)**     |  28/s    | 27.69/s | **34.34/s**            | ↑ 높을수록 좋음 |

<br/>

## 7. Robust Testing Strategy (테스트 전략)

단순한 기능 구현을 넘어, **신뢰할 수 있는 소프트웨어**를 만들기 위해 공격적인 테스트 커버리지를 확보했습니다.

| Category | Count | Tools Used | Description |
|:---:|:---:|:---|:---|
| Backend Unit/Integration | 138 | JUnit5, Mockito | 비즈니스 로직 및 예외 케이스 검증 |
| Backend E2E | 30 | RestAssured | API 엔드포인트의 전체 흐름 검증 |
| Frontend Unit | 79 | Vitest | 컴포넌트 렌더링 및 훅 로직 검증 |
| Frontend E2E | 93 | Playwright | User Journey 기반의 시나리오 테스트 |
| Load Testing | 6 | k6 | `stress-test.js` 등 임계치 성능 검증 |
| Total Tests | 346 | - | 견고한 무중단 배포(CI/CD)의 기반 마련 |

<br/>

## 8. How to Run (로컬 실행 방법)

AWS 배포가 중단되었으므로, 아래 명령어를 통해 로컬 환경에서 전체 서비스를 실행할 수 있습니다.

**Prerequisites:**
* Docker & Docker Compose
* Java 17+
* Node.js 20+

**Backend:**
```bash
cd lupin
./gradlew build
docker-compose up -d
