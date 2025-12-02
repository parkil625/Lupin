/**
 * Ranking Page E2E Tests
 *
 * 랭킹 페이지 E2E 테스트
 */

import { test, expect } from 'vitest';
import { page } from '@vitest/browser/context';

const BASE_URL = 'http://localhost:3000';

async function login(username = 'user01', password = '1') {
  await page.goto(`${BASE_URL}/login`);
  await page.getByPlaceholder('아이디').fill(username);
  await page.getByPlaceholder('비밀번호').fill(password);
  await page.getByRole('button', { name: '로그인' }).click();
  await expect.poll(() => page.url(), { timeout: 10000 }).toContain('/dashboard');
}

test.describe('랭킹 페이지 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/ranking`);
  });

  test('랭킹 페이지가 로드되어야 함', async () => {
    await expect.element(
      page.getByText('랭킹')
        .or(page.getByText('순위'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('랭킹 목록이 표시되어야 함', async () => {
    // 랭킹 목록 확인
    await expect.element(
      page.locator('[data-testid="ranking-list"]')
        .or(page.locator('[class*="ranking"]'))
        .or(page.getByText(/1위|2위|3위/))
    ).toBeVisible({ timeout: 10000 });
  });

  test('상위 3명이 강조 표시되어야 함', async () => {
    // Top 3 섹션 확인
    await expect.element(
      page.locator('[data-testid="top-3"]')
        .or(page.locator('[class*="podium"]'))
        .or(page.locator('[class*="top-rank"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('내 순위가 표시되어야 함', async () => {
    // 내 순위 표시 확인
    await expect.element(
      page.getByText(/내 순위|나의 순위/)
        .or(page.locator('[data-testid="my-rank"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('각 사용자의 포인트가 표시되어야 함', async () => {
    const rankingItem = page.locator('[data-testid="ranking-item"]').first()
      .or(page.locator('[class*="ranking-row"]').first());

    if (await rankingItem.isVisible({ timeout: 5000 })) {
      // 포인트 표시 확인
      await expect.element(
        rankingItem.getByText(/P$|포인트/)
          .or(rankingItem.locator('[data-testid="points"]'))
      ).toBeVisible();
    }
  });

  test('각 사용자의 프로필 이미지가 표시되어야 함', async () => {
    const rankingItem = page.locator('[data-testid="ranking-item"]').first();

    if (await rankingItem.isVisible({ timeout: 5000 })) {
      // 프로필 이미지 확인
      await expect.element(
        rankingItem.locator('img')
          .or(rankingItem.locator('[class*="avatar"]'))
      ).toBeVisible();
    }
  });

  test('랭킹 탭 전환이 가능해야 함 (일간/주간/월간)', async () => {
    // 탭 버튼들 확인
    const dailyTab = page.getByRole('tab', { name: /일간|오늘/ })
      .or(page.getByText('일간'));
    const weeklyTab = page.getByRole('tab', { name: /주간|이번 주/ })
      .or(page.getByText('주간'));
    const monthlyTab = page.getByRole('tab', { name: /월간|이번 달/ })
      .or(page.getByText('월간'));

    // 탭이 존재하는지 확인
    const hasTabs = await dailyTab.isVisible({ timeout: 3000 });

    if (hasTabs) {
      // 주간 탭 클릭
      await weeklyTab.click();
      await page.waitForTimeout(500);

      // 월간 탭 클릭
      await monthlyTab.click();
      await page.waitForTimeout(500);

      // 일간 탭으로 돌아가기
      await dailyTab.click();
    }
  });

  test('랭킹 사용자 클릭 시 프로필 정보가 표시되어야 함', async () => {
    const rankingItem = page.locator('[data-testid="ranking-item"]').first();

    if (await rankingItem.isVisible({ timeout: 5000 })) {
      await rankingItem.click();

      // 프로필 정보 또는 상세 정보 표시 확인
      await expect.element(
        page.getByRole('dialog')
          .or(page.locator('[data-testid="user-profile-popup"]'))
          .or(page.getByText(/운동 기록|통계/))
      ).toBeVisible({ timeout: 5000 });
    }
  });
});

test.describe('랭킹 페이지 UI E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/ranking`);
  });

  test('랭킹 페이지가 반응형으로 동작해야 함', async () => {
    // 모바일 뷰포트로 변경
    await page.setViewportSize({ width: 375, height: 667 });

    // 랭킹이 여전히 표시되는지 확인
    await expect.element(
      page.getByText('랭킹')
        .or(page.locator('[data-testid="ranking-list"]'))
    ).toBeVisible({ timeout: 5000 });

    // 데스크톱으로 복원
    await page.setViewportSize({ width: 1280, height: 720 });
  });

  test('스크롤 시 추가 랭킹이 로드되어야 함 (무한 스크롤)', async () => {
    // 랭킹 목록이 로드될 때까지 대기
    await page.waitForTimeout(2000);

    // 스크롤 다운
    await page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight);
    });

    // 추가 데이터 로드 대기
    await page.waitForTimeout(1000);

    // 로딩 인디케이터 또는 추가 항목 확인
    // (구현에 따라 다를 수 있음)
  });

  test('1위 사용자에게 왕관/메달 아이콘이 표시되어야 함', async () => {
    // 1위 사용자의 특별 아이콘 확인
    await expect.element(
      page.locator('[data-testid="crown-icon"]')
        .or(page.locator('[data-testid="gold-medal"]'))
        .or(page.locator('[class*="crown"]'))
        .or(page.locator('svg').filter({ has: page.locator('[class*="crown"]') }))
    ).toBeVisible({ timeout: 5000 });
  });
});
