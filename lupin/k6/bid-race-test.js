import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// 1. 100명이 동시에 1번씩만 공격!
export const options = {
    vus: 100,        // 가상 유저 100명
    iterations: 100  // 총 100번 실행 (각자 1번)
};

const successCount = new Counter('successful_bids');
const failCount = new Counter('failed_bids');

const BASE_URL = 'http://localhost:8081'; // 포트 8081 확인
const AUCTION_ID = 7;
const BID_AMOUNT = 58;

// 2. 아까 DB에 만든 tester1 ~ tester100 유저 정보 생성
const testUsers = [];
for (let i = 1; i <= 100; i++) {
    testUsers.push({
        email: `tester${i}`, // 아이디 (email 변수명이지만 실제론 아이디 입력)
        password: '1' // 아까 해시값의 원본 비밀번호 (test1234)
    });
}

export default function () {
    // 3. 현재 유저 번호에 맞는 아이디 가져오기
    // (VU 번호가 1이면 tester1, 100이면 tester100 사용)
    const userIndex = (__VU - 1) % testUsers.length;
    const currentUser = testUsers[userIndex];

    // --- 로그인 시작 ---
    // console.log(`[로그인 시도] ID: ${currentUser.email}`); // 로그 너무 많아서 주석 처리

    const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
        email: currentUser.email,
        password: currentUser.password,
    }), { headers: { 'Content-Type': 'application/json' } });

    if (loginRes.status !== 200) {
        console.error(`[로그인 실패] ID: ${currentUser.email}, Status: ${loginRes.status}`);
        return;
    }

    const authToken = loginRes.json().accessToken;

    // --- 입찰 시작 ---
    console.log(`[입찰 시도] ${currentUser.email}`); // 로그 너무 많아서 주석 처리

    const bidRes = http.post(`${BASE_URL}/api/auction/${AUCTION_ID}/bid`,
        JSON.stringify({
            bidAmount: BID_AMOUNT
        }),
        {
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json',
            },
        }
    );

    if (bidRes.status === 200) {
        console.log(`[입찰 성공] ID: ${currentUser.email} 가 성공했습니다!`);
        successCount.add(1);
    } else {
        // 실패 로그는 너무 많으니 주석 처리하거나 필요하면 켬
        console.log(`[입찰 실패] Status: ${bidRes.status}`);
        failCount.add(1);
    }
}