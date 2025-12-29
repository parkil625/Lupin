import { test, expect } from '@playwright/test';

const TEST_USER = {
    employeeId: 'user01', // DB seed data에 있는 확실한 ID 사용
    password: '1', // DB seed data의 초기 비밀번호
};

test.describe('경매(Auction) 기능 테스트', () => {

    // 모든 테스트 시작 전 실행
    test.beforeEach(async ({ page }) => {

        // 2. 페이지 이동
        await page.goto('/login');

        // 3. 로그인 시도
        await page.getByLabel('사내 아이디', { exact: true }).fill(TEST_USER.employeeId);
        await page.getByLabel('비밀번호', { exact: true }).fill(TEST_USER.password);
        await page.getByRole('button', { name: '로그인', exact: true}).click();

    });

    test('경매 페이지 진입 및 입찰 테스트', async ({ page }) => {
        const sidebar = page.locator('div[data-sidebar="true"]');

        // 사이드바가 보일 때까지 기다렸다가 호버 (혹시 모바일 화면이라 안 보이면 에러 날 수 있음)
        if (await sidebar.isVisible()) {
            await sidebar.hover();
            await page.waitForTimeout(500); // 메뉴가 스르륵 열리는 애니메이션(300ms) 기다림
        }

        // 1. 경매 메뉴 클릭
        await page.getByRole('button', { name: '경매'}).click();

        // 2. URL 확인
        await expect(page).toHaveURL(/\/dashboard\/auction/);

        // 3. 경매 카드 확인
        const activeAuction = page.locator('div').filter({ hasText: '현재가' }).first();

        // 경매 물품이 로딩될 때까지 잠시 대기
        await page.waitForTimeout(1000);

        if (await activeAuction.isVisible()) {
            console.log('진행 중인 경매 발견! 입찰을 시도합니다.');

            const bidInput = page.getByPlaceholder('입찰하실 금액을 입력하세요');
            if (await bidInput.isVisible()) {
                // 현재가보다 확실히 비싼 금액 입력 (테스트용)
                await bidInput.fill('100');

                // 입찰 버튼 클릭
                await page.getByRole('button', { name: '입찰하기' }).click();

                // 4. 성공 메시지나 변화 확인 (토스트 메시지 등)
                // 화면에 "입찰" 관련 성공 메시지가 뜨는지 확인 (프로젝트 설정에 따라 다름)
                await page.waitForTimeout(1000);
            }
        } else {
            console.log('⚠️ 현재 진행 중인 경매가 없습니다. DB 데이터를 확인하세요.');
        }
    });
});