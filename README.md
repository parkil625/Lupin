# 💊 Lupin (루팡) - Premium Medical Goods Auction & Telemedicine Platform

> **"의료 소비의 새로운 패러다임: 실시간 경매부터 비대면 진료까지"**
>
> **Development Period:** 202X.XX ~ 202X.XX (XX Weeks)
> **Team Size:** 3 Engineers

<br/>

## 1. Project Overview (프로젝트 개요)
**Lupin**은 폐쇄적인 의료 용품 시장의 정보 불균형을 해소하고, 의료 소비자의 접근성을 극대화하기 위해 설계된 **수직적 통합 플랫폼(Vertical Platform)**입니다. 희소성 있는 건강기능식품의 **실시간 경매(Real-time Auction)** 시스템과 전문 의료진과의 **비대면 진료(Telemedicine)** 및 상담 기능을 하나의 생태계로 통합했습니다.

> ⚠️ **Note:** 현재 AWS 운영 비용 최적화를 위해 라이브 배포는 중단되었으며, 아래 시연 영상과 코드를 통해 프로젝트의 기술적 성취를 확인하실 수 있습니다.

<br/>

## 2. Key Features & Demonstrations (주요 기능 및 시연)

### 🩺 Telemedicine & Chat (비대면 진료 및 상담)
환자와 의사 간의 실시간 소통을 지원합니다. WebSocket을 활용한 저지연 채팅과 처방전 발급 프로세스를 구현했습니다.
![Telemedicine Demo](./lupin/public/demo_telemedicine.gif)

### 🔨 Real-time Auction (실시간 경매)
동시성 이슈를 제어하며 수천 건의 입찰 요청을 안정적으로 처리하는 고성능 경매 시스템입니다.
![Auction Demo](./lupin/public/demo_auction.gif)

### 👥 Community & Feed (커뮤니티 및 피드)
사용자 경험(UX)을 극대화한 피드 시스템과 알림 서비스를 제공합니다.
![Feed Demo](./lupin/public/demo_feed.gif)

<br/>

## 3. Team Roles & Contribution (팀원 역할 및 기여도)

R&R(Role and Responsibilities)을 명확히 분배하여 각 도메인의 전문성을 강화했습니다.

| Name | Position | Domain Responsibilities | Tech Keywords |
|:---:|:---:|:---|:---|
| **박선일** | **Core Engineer** | **Infrastructure & Core Tech**<br/>- 회원 관리(Auth) 및 소셜 로그인(OAuth 2.0)<br/>- 커뮤니티(Feed/Comment) 및 알림(SSE)<br/>- **Cloud Architecture** (AWS, Cloudflare, Docker)<br/>- **QA & Testing** (k6 Load Test, Playwright E2E)<br/>- CI/CD Pipeline 구축 | `Spring Boot` `Redis` `Cloudflare` `k6` `Docker` `GitHub Actions` |
| **홍세민** | **Team Lead** | **Medical Domain**<br/>- 의사/환자 도메인 설계<br/>- 처방전(Prescription) 발급 로직<br/>- 실시간 채팅(WebSocket) 시스템 구현 | `WebSocket` `JPA` `MySQL` `React` |
| **최재홍** | **Engineer** | **Commerce Domain**<br/>- 경매(Auction) 비즈니스 로직 설계<br/>- 입찰 프로세스 및 타이머 구현<br/>- 상품(Item) 관리 시스템 | `Spring Batch` `Scheduler` `MySQL` |

<br/>

## 4. Technical Architecture (기술 아키텍처)

### 🛠 Tech Stack
| Category | Stack |
|:---:|:---|
| **Frontend** | React, TypeScript, Vite, Tailwind CSS, Storybook, Zustand |
| **Backend** | Java 17, Spring Boot 3.x, Spring Data JPA, QueryDSL |
| **Database** | MySQL 8.0, Redis (Caching & Pub/Sub) |
| **Infra & DevOps** | AWS (EC2, RDS, S3), Docker, Nginx, **Cloudflare**, GitHub Actions |
| **Testing** | k6 (Load Testing), Playwright (E2E), Vitest (Unit) |

### 🏗 Infrastructure Diagram
*(여기에 아키텍처 다이어그램 이미지가 있다면 추가해주세요. 없다면 생략 가능)*

<br/>

## 5. Technical Challenges & Solutions (기술적 도전과 해결)

### 🚀 Performance Optimization with Cloudflare (박선일)
* **Challenge:** 초기 AWS S3에서 직접 정적 리소스를 서빙할 때, 트래픽 급증 시 대역폭 비용 증가와 레이턴시 문제가 발생했습니다.
* **Solution:** **Cloudflare CDN**을 도입하여 정적 자산(이미지, 스크립트)을 엣지 로케이션에서 캐싱하도록 아키텍처를 재설계했습니다.
* **Result:** 콘텐츠 전송 속도(TTFB)를 **약 60% 단축**하고, 원본 서버 부하를 획기적으로 감소시켰습니다.

### ⚡ Handling Concurrency in Auctions (팀 공통/박선일 지원)
* **Challenge:** 인기 경매 마감 직전, 수천 명의 사용자가 동시에 입찰을 시도할 때 **경쟁 상태(Race Condition)** 데이터 정합성 문제가 발생했습니다.
* **Solution:** **Redis Distributed Lock (Redisson)**을 도입하여 입찰 트랜잭션의 원자성(Atomicity)을 보장했습니다. 또한, `k6`를 이용한 부하 테스트 시나리오(`bid-race-test.js`)를 작성하여 임계치를 검증했습니다.

### 🧪 Robust Testing Strategy (박선일)
* **Engineering:** 단순 기능 구현에 그치지 않고, 시스템의 신뢰성을 확보하기 위해 공격적인 테스트 전략을 수립했습니다.
    * **k6:** `stress-test.js`와 `load-test.js`를 통해 API 엔드포인트의 성능 병목을 사전에 식별했습니다.
    * **Playwright:** 사용자 여정(User Journey)을 기반으로 한 E2E 테스트를 자동화하여 배포 전 회귀 버그(Regression Bug)를 방지했습니다.

<br/>

## 6. How to Run (로컬 실행 방법)

서버가 중단되었으므로, 로컬 환경에서 실행하는 방법을 안내합니다.

**Prerequisites:**
* Docker & Docker Compose
* Java 17+
* Node.js 20+

**Backend:**
```bash
cd lupin
./gradlew build
docker-compose up -d
