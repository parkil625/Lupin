import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const feedListTrend = new Trend('feed_list_duration');
const loginTrend = new Trend('login_duration');

// 테스트 설정
export const options = {
    // 부하 시나리오
    stages: [
        { duration: '30s', target: 10 },   // 워밍업: 10명까지 증가
        { duration: '1m', target: 50 },    // 부하: 50명 유지
        { duration: '30s', target: 100 },  // 피크: 100명까지 증가
        { duration: '1m', target: 100 },   // 피크 유지
        { duration: '30s', target: 0 },    // 쿨다운
    ],

    // 성능 임계값
    thresholds: {
        http_req_duration: ['p(95)<500'],     // 95%ile 응답시간 < 500ms
        http_req_failed: ['rate<0.01'],       // 실패율 < 1%
        errors: ['rate<0.05'],                // 에러율 < 5%
        feed_list_duration: ['p(95)<300'],    // 피드 목록 조회 < 300ms
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트 데이터
const testUsers = [
    { email: 'test1@test.com', password: 'password123' },
    { email: 'test2@test.com', password: 'password123' },
    { email: 'test3@test.com', password: 'password123' },
];

export default function () {
    let authToken = null;

    // 1. 헬스 체크
    group('Health Check', function () {
        const res = http.get(`${BASE_URL}/api/health`);
        check(res, {
            'health check status is 200': (r) => r.status === 200,
            'health check response is OK': (r) => r.json().status === 'OK',
        });
        errorRate.add(res.status !== 200);
    });

    // 2. 로그인
    group('Login', function () {
        const user = testUsers[Math.floor(Math.random() * testUsers.length)];
        const loginPayload = JSON.stringify({
            email: user.email,
            password: user.password,
        });

        const res = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
            headers: { 'Content-Type': 'application/json' },
        });

        loginTrend.add(res.timings.duration);

        const success = check(res, {
            'login status is 200': (r) => r.status === 200,
            'login returns token': (r) => r.json().accessToken !== undefined,
        });

        if (success) {
            authToken = res.json().accessToken;
        }
        errorRate.add(!success);
    });

    sleep(1);

    // 3. 피드 목록 조회 (인증 필요)
    if (authToken) {
        group('Feed List', function () {
            const res = http.get(`${BASE_URL}/api/feeds?page=0&size=10`, {
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                    'Content-Type': 'application/json',
                },
            });

            feedListTrend.add(res.timings.duration);

            const success = check(res, {
                'feed list status is 200': (r) => r.status === 200,
                'feed list has content': (r) => r.json().content !== undefined,
            });
            errorRate.add(!success);
        });

        sleep(0.5);

        // 4. 피드 검색
        group('Feed Search', function () {
            const res = http.get(`${BASE_URL}/api/feeds?keyword=운동&page=0&size=10`, {
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                },
            });

            check(res, {
                'search status is 200': (r) => r.status === 200,
            });
            errorRate.add(res.status !== 200);
        });

        sleep(0.5);

        // 5. 인기 피드 조회
        group('Popular Feeds', function () {
            const res = http.get(`${BASE_URL}/api/feeds/popular`, {
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                },
            });

            check(res, {
                'popular feeds status is 200': (r) => r.status === 200,
            });
            errorRate.add(res.status !== 200);
        });
    }

    sleep(1);
}

// 테스트 완료 후 요약
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'k6/summary.json': JSON.stringify(data),
    };
}

function textSummary(data, options) {
    const indent = options.indent || '';
    let summary = '\n';
    summary += `${indent}===============================================\n`;
    summary += `${indent}  Lupin API 부하 테스트 결과\n`;
    summary += `${indent}===============================================\n\n`;

    // 주요 메트릭
    if (data.metrics.http_req_duration) {
        const duration = data.metrics.http_req_duration;
        summary += `${indent}응답 시간:\n`;
        summary += `${indent}  - 평균: ${duration.values.avg.toFixed(2)}ms\n`;
        summary += `${indent}  - 중앙값: ${duration.values.med.toFixed(2)}ms\n`;
        summary += `${indent}  - p(95): ${duration.values['p(95)'].toFixed(2)}ms\n`;
        summary += `${indent}  - p(99): ${duration.values['p(99)'].toFixed(2)}ms\n\n`;
    }

    if (data.metrics.http_reqs) {
        summary += `${indent}요청 수: ${data.metrics.http_reqs.values.count}\n`;
        summary += `${indent}초당 요청: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n\n`;
    }

    if (data.metrics.http_req_failed) {
        const failRate = data.metrics.http_req_failed.values.rate * 100;
        summary += `${indent}실패율: ${failRate.toFixed(2)}%\n`;
    }

    return summary;
}
