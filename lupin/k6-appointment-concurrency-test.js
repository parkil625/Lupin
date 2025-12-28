import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const successfulBookings = new Counter('successful_bookings');
const failedBookings = new Counter('failed_bookings');
const duplicateErrors = new Counter('duplicate_errors');
const errorRate = new Rate('errors');
const latencyTrend = new Trend('latency');

// Test configuration - 1000 VUs trying to book the same time slot
export const options = {
  scenarios: {
    concurrent_booking: {
      executor: 'shared-iterations',
      vus: 1000,              // 1000 virtual users
      iterations: 1000,       // Total 1000 iterations
      maxDuration: '1m',      // Max 1 minute
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],     // 95% of requests should be below 2s
    errors: ['rate<0.95'],                  // Less than 95% errors (only 1 should succeed)
    successful_bookings: ['count==1'],      // Only 1 booking should succeed
    duplicate_errors: ['count>0'],          // Should have duplicate errors
  },
};

const BASE_URL = __ENV.BASE_URL || 'https://api.lupin-care.com';

// You need to provide valid authentication tokens for different users
// Generate these tokens beforehand and set them as environment variables
const AUTH_TOKENS = __ENV.AUTH_TOKENS ?
  JSON.parse(__ENV.AUTH_TOKENS) :
  generateMockTokens(1000);

// Generate mock tokens for testing (replace with real tokens)
function generateMockTokens(count) {
  const tokens = [];
  for (let i = 0; i < count; i++) {
    tokens.push(`mock_token_${i}`);
  }
  return tokens;
}

// Get VU-specific token
function getAuthToken() {
  const vuId = __VU - 1; // VU IDs start from 1
  return AUTH_TOKENS[vuId % AUTH_TOKENS.length];
}

// Target appointment details - all VUs will try to book this exact time slot
const TARGET_APPOINTMENT = {
  patientId: 1,                              // Same patient trying to book
  doctorId: 21,                              // Same doctor
  date: '2026-01-15T15:00:00',              // Same exact time slot
};

export default function () {
  const token = getAuthToken();

  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  const payload = JSON.stringify(TARGET_APPOINTMENT);

  // All VUs try to create appointment at the same time
  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/api/appointment`, payload, { headers });
  const duration = Date.now() - startTime;

  latencyTrend.add(duration);

  // Check response
  const success = check(res, {
    'status is 200 (booking succeeded)': (r) => r.status === 200,
    'status is 409 (duplicate booking conflict)': (r) => r.status === 409,
    'status is 400 (bad request)': (r) => r.status === 400,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
  });

  // Count results
  if (res.status === 200) {
    successfulBookings.add(1);
    console.log(`[${__VU}] 예약성공 - ID: ${res.body}`);
  } else if (res.status === 409 || res.status === 400) {
    duplicateErrors.add(1);
    failedBookings.add(1);
    if (Math.random() < 0.01) {
      console.log(`[${__VU}] 중복차단 - ${res.status}`);
    }
  } else {
    failedBookings.add(1);
    console.log(`[${__VU}] 에러 - ${res.status} ${res.status_text}`);
  }

  errorRate.add(res.status !== 200 && res.status !== 409);

  // Small random sleep to simulate real-world variance
  sleep(Math.random() * 0.1);
}

export function handleSummary(data) {
  const successful = data.metrics.successful_bookings?.values?.count || 0;
  const failed = data.metrics.failed_bookings?.values?.count || 0;
  const duplicates = data.metrics.duplicate_errors?.values?.count || 0;
  const totalRequests = successful + failed;

  console.log('\n' + '-'.repeat(60));
  console.log('예약 동시성 테스트 결과');
  console.log('-'.repeat(60));

  console.log(`\n[예약 처리 결과]`);
  console.log(`성공: ${successful}건 / 실패: ${failed}건 (총 ${totalRequests}건)`);
  console.log(`중복 차단: ${duplicates}건`);

  const avg = (data.metrics.http_req_duration?.values?.avg || 0).toFixed(0);
  const min = (data.metrics.http_req_duration?.values?.min || 0).toFixed(0);
  const max = (data.metrics.http_req_duration?.values?.max || 0).toFixed(0);
  const p95 = (data.metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(0);

  console.log(`\n[응답시간]`);
  console.log(`평균 ${avg}ms (최소 ${min}ms / 최대 ${max}ms / P95 ${p95}ms)`);

  console.log(`\n[판정]`);
  if (successful === 1) {
    console.log(`OK - 1건만 예약됨 (중복방지 정상)`);
  } else if (successful === 0) {
    console.log(`FAIL - 예약 실패`);
  } else {
    console.log(`FAIL - ${successful}건 중복예약 발생!`);
  }

  if (duplicates > 0) {
    console.log(`OK - 중복시도 차단됨`);
  }

  const avgLatency = data.metrics.http_req_duration?.values?.avg || 0;
  if (avgLatency < 1000) {
    console.log(`OK - 응답시간 양호`);
  } else {
    console.log(`WARNING - 응답시간 느림`);
  }

  console.log('-'.repeat(60) + '\n');

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'summary.json': JSON.stringify(data, null, 2),
  };
}

function textSummary(data) {
  const vus = data.metrics.vus?.values?.value || 0;
  const iterations = data.metrics.iterations?.values?.count || 0;
  const duration = (data.state?.testRunDurationMs / 1000 || 0).toFixed(1);
  const totalReqs = data.metrics.http_reqs?.values?.count || 0;
  const rate = (data.metrics.http_reqs?.values?.rate || 0).toFixed(1);

  let out = `\n상세 통계\n`;
  out += `동시사용자: ${vus}명, 반복: ${iterations}회, 소요시간: ${duration}초\n`;
  out += `요청: ${totalReqs}건 (${rate}건/초)\n\n`;

  return out;
}

// Setup function - runs once before the test
export function setup() {
  console.log('\n테스트 시작');
  console.log(`예약시간: ${TARGET_APPOINTMENT.date}`);
  console.log(`의사 ID: ${TARGET_APPOINTMENT.doctorId}, 환자 ID: ${TARGET_APPOINTMENT.patientId}`);
  console.log(`동시접속: 1000명\n`);
}

// Teardown function - runs once after the test
export function teardown(data) {
  console.log('테스트 종료\n');
}
