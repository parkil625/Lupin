import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const latencyTrend = new Trend('latency');

// Test configuration
export const options = {
  stages: [
    { duration: '10s', target: 10 },  // Ramp up to 10 users
    { duration: '30s', target: 10 },  // Stay at 10 users for 30s
    { duration: '10s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    errors: ['rate<0.1'],             // Error rate should be below 10%
  },
};

const BASE_URL = 'https://api.lupin-care.com';

// Optional: Set your JWT token here for authenticated endpoints
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export default function () {
  const headers = AUTH_TOKEN ? {
    'Authorization': `Bearer ${AUTH_TOKEN}`,
    'Content-Type': 'application/json',
  } : {
    'Content-Type': 'application/json',
  };

  // Test 1: Public ranking endpoint (no auth required)
  const rankingRes = http.get(`${BASE_URL}/api/users/ranking?limit=10`, { headers });
  check(rankingRes, {
    'ranking status is 200': (r) => r.status === 200,
    'ranking response time < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(rankingRes.status !== 200);
  latencyTrend.add(rankingRes.timings.duration);

  sleep(1);

  // Test 2: User statistics endpoint
  const statsRes = http.get(`${BASE_URL}/api/users/statistics`, { headers });
  check(statsRes, {
    'statistics status is 200': (r) => r.status === 200,
  });
  errorRate.add(statsRes.status !== 200);
  latencyTrend.add(statsRes.timings.duration);

  sleep(1);

  // Test 3: Authenticated endpoint (if token provided)
  if (AUTH_TOKEN) {
    const meRes = http.get(`${BASE_URL}/api/users/me`, { headers });
    check(meRes, {
      'me status is 200': (r) => r.status === 200,
      'me has user data': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id !== undefined;
        } catch {
          return false;
        }
      },
    });
    errorRate.add(meRes.status !== 200);
    latencyTrend.add(meRes.timings.duration);

    sleep(1);

    // Test 4: Get feeds with pagination
    const feedsRes = http.get(`${BASE_URL}/api/feeds?page=0&size=10`, { headers });
    check(feedsRes, {
      'feeds status is 200': (r) => r.status === 200,
      'feeds has content': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.content !== undefined;
        } catch {
          return false;
        }
      },
    });
    errorRate.add(feedsRes.status !== 200);
    latencyTrend.add(feedsRes.timings.duration);

    sleep(1);
  }
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, opts) {
  const indent = opts.indent || '';

  let out = '\n=== K6 Load Test Summary ===\n\n';

  out += `${indent}Scenarios: ${data.root_group.name}\n`;
  out += `${indent}VUs: ${data.metrics.vus?.values?.value || 0}\n`;
  out += `${indent}Iterations: ${data.metrics.iterations?.values?.count || 0}\n\n`;

  out += `${indent}HTTP Request Duration:\n`;
  out += `${indent}  avg: ${(data.metrics.http_req_duration?.values?.avg || 0).toFixed(2)}ms\n`;
  out += `${indent}  min: ${(data.metrics.http_req_duration?.values?.min || 0).toFixed(2)}ms\n`;
  out += `${indent}  max: ${(data.metrics.http_req_duration?.values?.max || 0).toFixed(2)}ms\n`;
  out += `${indent}  p(90): ${(data.metrics.http_req_duration?.values?.['p(90)'] || 0).toFixed(2)}ms\n`;
  out += `${indent}  p(95): ${(data.metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms\n\n`;

  out += `${indent}HTTP Requests:\n`;
  out += `${indent}  total: ${data.metrics.http_reqs?.values?.count || 0}\n`;
  out += `${indent}  rate: ${(data.metrics.http_reqs?.values?.rate || 0).toFixed(2)}/s\n\n`;

  out += `${indent}Errors: ${(data.metrics.errors?.values?.rate * 100 || 0).toFixed(2)}%\n`;

  return out;
}
