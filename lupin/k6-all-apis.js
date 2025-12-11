import http from 'k6/http';
import { Trend } from 'k6/metrics';

// 각 API별 응답시간 Trend
const feedsLatency = new Trend('feeds_latency');
const feedDetailLatency = new Trend('feed_detail_latency');
const feedsMyLatency = new Trend('feeds_my_latency');
const usersMeLatency = new Trend('users_me_latency');
const usersRankingLatency = new Trend('users_ranking_latency');
const usersStatisticsLatency = new Trend('users_statistics_latency');
const usersPointsLatency = new Trend('users_points_latency');
const feedCommentsLatency = new Trend('feed_comments_latency');

export const options = {
  vus: 10,
  duration: '30s',
};

const BASE_URL = 'https://api.lupin-care.com';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export default function () {
  const headers = AUTH_TOKEN ? {
    'Authorization': `Bearer ${AUTH_TOKEN}`,
    'Content-Type': 'application/json',
  } : { 'Content-Type': 'application/json' };

  // 1. GET /api/feeds?page=0&size=10
  const feeds = http.get(`${BASE_URL}/api/feeds?page=0&size=10`, { headers });
  feedsLatency.add(feeds.timings.duration);

  // 2. GET /api/feeds/{feedId}
  const feedDetail = http.get(`${BASE_URL}/api/feeds/1`, { headers });
  feedDetailLatency.add(feedDetail.timings.duration);

  // 3. GET /api/feeds/my?page=0&size=10
  const feedsMy = http.get(`${BASE_URL}/api/feeds/my?page=0&size=10`, { headers });
  feedsMyLatency.add(feedsMy.timings.duration);

  // 4. GET /api/users/me
  const usersMe = http.get(`${BASE_URL}/api/users/me`, { headers });
  usersMeLatency.add(usersMe.timings.duration);

  // 5. GET /api/users/ranking?limit=10
  const usersRanking = http.get(`${BASE_URL}/api/users/ranking?limit=10`, { headers });
  usersRankingLatency.add(usersRanking.timings.duration);

  // 6. GET /api/users/statistics
  const usersStatistics = http.get(`${BASE_URL}/api/users/statistics`, { headers });
  usersStatisticsLatency.add(usersStatistics.timings.duration);

  // 7. GET /api/users/points
  const usersPoints = http.get(`${BASE_URL}/api/users/points`, { headers });
  usersPointsLatency.add(usersPoints.timings.duration);

  // 8. GET /api/feeds/{feedId}/comments
  const feedComments = http.get(`${BASE_URL}/api/feeds/1/comments`, { headers });
  feedCommentsLatency.add(feedComments.timings.duration);
}
