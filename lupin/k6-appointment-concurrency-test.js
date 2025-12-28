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
    console.log(`✅ VU ${__VU}: Booking succeeded! Appointment ID: ${res.body}`);
  } else if (res.status === 409 || res.status === 400) {
    duplicateErrors.add(1);
    failedBookings.add(1);
    // Don't log every failure to avoid cluttering output
    if (Math.random() < 0.01) { // Log ~1% of failures
      console.log(`❌ VU ${__VU}: Booking failed - ${res.status} ${res.status_text}`);
    }
  } else {
    failedBookings.add(1);
    console.log(`⚠️ VU ${__VU}: Unexpected error - ${res.status} ${res.status_text}`);
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

  console.log('\n' + '='.repeat(80));
  console.log('🏥 예약 중복 동시성 테스트 결과');
  console.log('='.repeat(80));
  console.log(`\n📊 예약 결과:`);
  console.log(`  ✅ 성공한 예약: ${successful}개`);
  console.log(`  ❌ 실패한 예약: ${failed}개`);
  console.log(`  🔁 중복 에러: ${duplicates}개`);
  console.log(`  📈 총 요청 수: ${totalRequests}개`);

  console.log(`\n⏱️  성능 지표:`);
  console.log(`  평균 응답 시간: ${(data.metrics.http_req_duration?.values?.avg || 0).toFixed(2)}ms`);
  console.log(`  최소 응답 시간: ${(data.metrics.http_req_duration?.values?.min || 0).toFixed(2)}ms`);
  console.log(`  최대 응답 시간: ${(data.metrics.http_req_duration?.values?.max || 0).toFixed(2)}ms`);
  console.log(`  P90 응답 시간: ${(data.metrics.http_req_duration?.values?.['p(90)'] || 0).toFixed(2)}ms`);
  console.log(`  P95 응답 시간: ${(data.metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms`);
  console.log(`  P99 응답 시간: ${(data.metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2)}ms`);

  console.log(`\n🎯 테스트 결과 판정:`);
  if (successful === 1) {
    console.log(`  ✅ PASS: 정확히 1개의 예약만 성공했습니다. (중복 방지 성공)`);
  } else if (successful === 0) {
    console.log(`  ❌ FAIL: 예약이 하나도 성공하지 못했습니다.`);
  } else {
    console.log(`  ❌ FAIL: ${successful}개의 중복 예약이 발생했습니다! (중복 방지 실패)`);
  }

  if (duplicates > 0) {
    console.log(`  ✅ PASS: 중복 시도가 적절히 거부되었습니다.`);
  }

  const avgLatency = data.metrics.http_req_duration?.values?.avg || 0;
  const p95Latency = data.metrics.http_req_duration?.values?.['p(95)'] || 0;

  if (avgLatency < 1000 && p95Latency < 2000) {
    console.log(`  ✅ PASS: 응답 시간이 기준 내에 있습니다.`);
  } else {
    console.log(`  ⚠️  WARNING: 응답 시간이 다소 느립니다.`);
  }

  console.log('\n' + '='.repeat(80) + '\n');

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'summary.json': JSON.stringify(data, null, 2),
  };
}

function textSummary(data, opts) {
  const indent = opts.indent || '';

  let out = '\n=== 상세 통계 ===\n\n';

  out += `${indent}Virtual Users: ${data.metrics.vus?.values?.value || 0}\n`;
  out += `${indent}Iterations: ${data.metrics.iterations?.values?.count || 0}\n`;
  out += `${indent}Duration: ${(data.state?.testRunDurationMs / 1000 || 0).toFixed(2)}s\n\n`;

  out += `${indent}HTTP Requests:\n`;
  out += `${indent}  Total: ${data.metrics.http_reqs?.values?.count || 0}\n`;
  out += `${indent}  Rate: ${(data.metrics.http_reqs?.values?.rate || 0).toFixed(2)}/s\n\n`;

  out += `${indent}HTTP Request Duration:\n`;
  out += `${indent}  avg: ${(data.metrics.http_req_duration?.values?.avg || 0).toFixed(2)}ms\n`;
  out += `${indent}  min: ${(data.metrics.http_req_duration?.values?.min || 0).toFixed(2)}ms\n`;
  out += `${indent}  med: ${(data.metrics.http_req_duration?.values?.med || 0).toFixed(2)}ms\n`;
  out += `${indent}  max: ${(data.metrics.http_req_duration?.values?.max || 0).toFixed(2)}ms\n`;
  out += `${indent}  p(90): ${(data.metrics.http_req_duration?.values?.['p(90)'] || 0).toFixed(2)}ms\n`;
  out += `${indent}  p(95): ${(data.metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms\n`;
  out += `${indent}  p(99): ${(data.metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2)}ms\n\n`;

  return out;
}

// Setup function - runs once before the test
export function setup() {
  console.log('\n🚀 예약 중복 테스트 시작');
  console.log(`📅 목표 예약 시간: ${TARGET_APPOINTMENT.date}`);
  console.log(`👨‍⚕️ 의사 ID: ${TARGET_APPOINTMENT.doctorId}`);
  console.log(`👤 환자 ID: ${TARGET_APPOINTMENT.patientId}`);
  console.log(`👥 동시 접속자 수: 1000명`);
  console.log('='.repeat(80) + '\n');
}

// Teardown function - runs once after the test
export function teardown(data) {
  console.log('\n✅ 테스트 완료\n');
}
