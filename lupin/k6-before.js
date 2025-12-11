import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// 각 엔드포인트별 응답시간 측정
const rankingLatency = new Trend('ranking_latency');
const statisticsLatency = new Trend('statistics_latency');
const userInfoLatency = new Trend('user_info_latency');
const feedDetailLatency = new Trend('feed_detail_latency');

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = 'https://api.lupin-care.com';

export default function () {
  // 1. GET /api/users/ranking
  const ranking = http.get(`${BASE_URL}/api/users/ranking?limit=10`);
  check(ranking, { 'ranking 200': (r) => r.status === 200 });
  rankingLatency.add(ranking.timings.duration);
  sleep(0.5);

  // 2. GET /api/users/statistics
  const stats = http.get(`${BASE_URL}/api/users/statistics`);
  check(stats, { 'statistics 200': (r) => r.status === 200 });
  statisticsLatency.add(stats.timings.duration);
  sleep(0.5);

  // 3. GET /api/users/{userId} (userId=1 테스트)
  const userInfo = http.get(`${BASE_URL}/api/users/1`);
  check(userInfo, { 'userInfo 200': (r) => r.status === 200 });
  userInfoLatency.add(userInfo.timings.duration);
  sleep(0.5);

  // 4. GET /api/feeds/{feedId} (feedId=1 테스트)
  const feedDetail = http.get(`${BASE_URL}/api/feeds/1`);
  check(feedDetail, { 'feedDetail 200 or 404': (r) => r.status === 200 || r.status === 404 });
  feedDetailLatency.add(feedDetail.timings.duration);
  sleep(0.5);
}
