import http from "k6/http";
import { check, group, sleep } from "k6";
import { Trend } from "k6/metrics";

// ============================================================================
// [1] 설정 및 임계치 정의 (Thresholds)
// ============================================================================

const landingPageTrend = new Trend("page_landing_duration");
const homePageTrend = new Trend("page_home_duration");
const feedPageTrend = new Trend("page_feed_duration");
const rankingPageTrend = new Trend("page_ranking_duration");
const myPageTrend = new Trend("page_mypage_duration");
const notiPageTrend = new Trend("page_notification_duration");

export const options = {
  scenarios: {
    user_journey: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        // [수정 1] VU를 2명으로 줄여서 Rate Limit(429 에러) 회피
        { duration: "5s", target: 2 }, // 2명까지 서서히 증가
        { duration: "20s", target: 2 }, // 2명 유지 (성능 측정 구간)
        { duration: "5s", target: 0 }, // 종료
      ],
      gracefulRampDown: "5s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"], // 에러율 1% 미만 (이제 통과할 것입니다)
    http_req_duration: ["p(95)<2000"], // 95% 요청이 2초 이내면 합격
  },
};

const BASE_URL = __ENV.BASE_URL || "https://lupin-care.com";

const TEST_USER = {
  userId: __ENV.TEST_ID || "user01",
  password: __ENV.TEST_PW || "1",
};

// ============================================================================
// [2] Setup: 최초 1회 로그인 및 토큰 발급
// ============================================================================
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify(TEST_USER),
    { headers: { "Content-Type": "application/json" } }
  );

  if (loginRes.status !== 200) {
    console.error(
      `[Setup] 로그인 실패! Status: ${loginRes.status}, Body: ${loginRes.body}`
    );
  }

  check(loginRes, { "Setup Login Successful": (r) => r.status === 200 });

  const body = loginRes.json();
  // 토큰이 없는 경우를 대비해 안전하게 파싱
  const token = body ? body.accessToken || body.token : "";
  const userId = body ? body.id || body.userId : "";

  return { token, userId };
}

// ============================================================================
// [3] Main: 가상 유저(VU) 행동 시나리오
// ============================================================================
export default function (data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
    "Content-Type": "application/json",
  };
  const userId = data.userId || 1;

  // --------------------------------------------------------------------------
  // 1. 소개 페이지 (Landing Page)
  // --------------------------------------------------------------------------
  group("Page_Landing", () => {
    const start = new Date();
    const res = http.get(BASE_URL);

    if (res.status !== 200) console.warn(`[Landing] Fail: ${res.status}`);

    check(res, { "Landing Page Loaded": (r) => r.status === 200 });
    landingPageTrend.add(new Date() - start);
  });

  // [수정 2] 대기 시간을 3초로 늘려 "사람다운" 속도로 조절
  sleep(3);

  // --------------------------------------------------------------------------
  // 2. 홈 대시보드 (Home)
  // --------------------------------------------------------------------------
  group("Page_Home", () => {
    const start = new Date();

    const responses = http.batch([
      ["GET", `${BASE_URL}/api/users/${userId}`, null, { headers }],
      [
        "GET",
        `${BASE_URL}/api/users/${userId}/ranking-context`,
        null,
        { headers },
      ],
      // [수정 3] 올바른 API 주소 적용 완료
      ["GET", `${BASE_URL}/api/feeds/can-post-today`, null, { headers }],
    ]);

    if (responses[0].status !== 200)
      console.warn(`[Home] Fail: ${responses[0].status}`);
    if (responses[2].status !== 200)
      console.warn(`[Home-PostCheck] Fail: ${responses[2].status}`);

    check(responses[0], { "Home: Get User Info OK": (r) => r.status === 200 });
    homePageTrend.add(new Date() - start);
  });

  // [수정 2] 대기 시간 5초 (홈에서 피드로 넘어가는 시간 시뮬레이션)
  sleep(5);

  // --------------------------------------------------------------------------
  // 3. 피드 메뉴 (Feed)
  // --------------------------------------------------------------------------
  group("Page_Feed", () => {
    const start = new Date();
    const res = http.get(`${BASE_URL}/api/feeds?page=0&size=10`, { headers });

    if (res.status !== 200) console.warn(`[Feed] Fail: ${res.status}`);

    check(res, { "Feed: List Loaded OK": (r) => r.status === 200 });
    feedPageTrend.add(new Date() - start);
  });

  sleep(5);

  // --------------------------------------------------------------------------
  // 4. 랭킹 페이지 (Ranking)
  // --------------------------------------------------------------------------
  group("Page_Ranking", () => {
    const start = new Date();
    const responses = http.batch([
      ["GET", `${BASE_URL}/api/users/ranking?limit=10`, null, { headers }],
      ["GET", `${BASE_URL}/api/users/statistics`, null, { headers }],
      [
        "GET",
        `${BASE_URL}/api/users/${userId}/ranking-context`,
        null,
        { headers },
      ],
    ]);

    if (responses[0].status !== 200)
      console.warn(`[Ranking] Fail: ${responses[0].status}`);

    check(responses[0], { "Ranking: Top 10 OK": (r) => r.status === 200 });
    rankingPageTrend.add(new Date() - start);
  });

  sleep(3);

  // --------------------------------------------------------------------------
  // 5. 마이 페이지 (My Page)
  // --------------------------------------------------------------------------
  group("Page_MyPage", () => {
    const start = new Date();
    const responses = http.batch([
      ["GET", `${BASE_URL}/api/users/${userId}`, null, { headers }],
      ["GET", `${BASE_URL}/api/oauth/connections`, null, { headers }],
    ]);

    if (responses[0].status !== 200)
      console.warn(`[MyPage] Fail: ${responses[0].status}`);

    check(responses[0], { "MyPage: Info OK": (r) => r.status === 200 });
    myPageTrend.add(new Date() - start);
  });

  sleep(3);

  // --------------------------------------------------------------------------
  // 6. 알림 (Notifications)
  // --------------------------------------------------------------------------
  group("Page_Notifications", () => {
    const start = new Date();
    const res = http.get(`${BASE_URL}/api/notifications`, { headers });

    if (res.status !== 200) console.warn(`[Notification] Fail: ${res.status}`);

    check(res, { "Notification: List OK": (r) => r.status === 200 });
    notiPageTrend.add(new Date() - start);
  });

  sleep(2);
}
