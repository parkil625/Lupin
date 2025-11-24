import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

// 스트레스 테스트 설정 - 한계점 찾기
export const options = {
    stages: [
        { duration: '2m', target: 100 },   // 100명까지 증가
        { duration: '5m', target: 100 },   // 100명 유지
        { duration: '2m', target: 200 },   // 200명까지 증가
        { duration: '5m', target: 200 },   // 200명 유지
        { duration: '2m', target: 300 },   // 300명까지 증가 (스트레스)
        { duration: '5m', target: 300 },   // 300명 유지
        { duration: '2m', target: 0 },     // 쿨다운
    ],

    thresholds: {
        http_req_duration: ['p(99)<1500'], // 99%ile < 1.5초
        http_req_failed: ['rate<0.10'],    // 실패율 < 10%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    // 헬스 체크 (가장 가벼운 엔드포인트)
    const healthRes = http.get(`${BASE_URL}/api/health`);
    check(healthRes, {
        'health is 200': (r) => r.status === 200,
    });
    errorRate.add(healthRes.status !== 200);

    // 피드 목록 조회 (인증 없이 실패해도 괜찮음 - 서버 부하 테스트 목적)
    const feedRes = http.get(`${BASE_URL}/api/feeds?page=0&size=10`);
    check(feedRes, {
        'feed response received': (r) => r.status === 200 || r.status === 401,
    });

    sleep(0.1); // 최소 대기
}
