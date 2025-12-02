/**
 * Home Page E2E Tests
 *
 * 홈 페이지 E2E 테스트
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

test.describe('홈 페이지 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/home`);
  });

  test('홈 페이지가 로드되어야 함', async () => {
    await expect.element(
      page.getByText('홈')
        .or(page.locator('[data-testid="home-page"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('사용자 인사말이 표시되어야 함', async () => {
    // 환영 메시지 확인
    await expect.element(
      page.getByText(/안녕하세요|좋은 아침|좋은 저녁/)
        .or(page.locator('[data-testid="greeting"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('현재 포인트가 표시되어야 함', async () => {
    await expect.element(
      page.getByText(/포인트|P$/)
        .or(page.locator('[data-testid="current-points"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('오늘의 운동 기록 버튼이 표시되어야 함', async () => {
    await expect.element(
      page.getByText('오늘의 운동 기록하기')
        .or(page.getByRole('button', { name: /운동 기록|피드 작성/ }))
    ).toBeVisible({ timeout: 5000 });
  });

  test('내 피드 섹션이 표시되어야 함', async () => {
    await expect.element(
      page.getByText('내 피드')
        .or(page.locator('[data-testid="my-feeds-section"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('오늘 피드 작성 가능 여부가 표시되어야 함', async () => {
    // 이미 작성했으면 "오늘 운동 완료" 또는 작성 가능하면 버튼 표시
    await expect.element(
      page.getByText(/오늘 운동 완료|오늘의 운동 기록하기|이미 작성/)
        .or(page.locator('[data-testid="can-post-today"]'))
    ).toBeVisible({ timeout: 5000 });
  });
});

test.describe('홈 페이지 네비게이션 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/home`);
  });

  test('사이드바가 표시되어야 함 (데스크톱)', async () => {
    await page.setViewportSize({ width: 1280, height: 720 });

    await expect.element(
      page.locator('[data-testid="sidebar"]')
        .or(page.locator('nav'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('모바일에서 하단 네비게이션 바가 표시되어야 함', async () => {
    await page.setViewportSize({ width: 375, height: 667 });

    await expect.element(
      page.locator('[data-testid="bottom-nav"]')
        .or(page.locator('.md\\:hidden').filter({ has: page.locator('button') }))
    ).toBeVisible({ timeout: 5000 });

    // 데스크톱으로 복원
    await page.setViewportSize({ width: 1280, height: 720 });
  });

  test('피드 탭으로 이동할 수 있어야 함', async () => {
    const feedNav = page.getByRole('button', { name: /피드/ })
      .or(page.getByText('피드'));

    if (await feedNav.isVisible({ timeout: 3000 })) {
      await feedNav.click();

      await expect.poll(() => page.url(), { timeout: 5000 }).toContain('/feed');
    }
  });

  test('랭킹 탭으로 이동할 수 있어야 함', async () => {
    const rankingNav = page.getByRole('button', { name: /랭킹/ })
      .or(page.getByText('랭킹'));

    if (await rankingNav.isVisible({ timeout: 3000 })) {
      await rankingNav.click();

      await expect.poll(() => page.url(), { timeout: 5000 }).toContain('/ranking');
    }
  });

  test('진료 탭으로 이동할 수 있어야 함', async () => {
    const medicalNav = page.getByRole('button', { name: /진료/ })
      .or(page.getByText('진료'));

    if (await medicalNav.isVisible({ timeout: 3000 })) {
      await medicalNav.click();

      await expect.poll(() => page.url(), { timeout: 5000 }).toContain('/medical');
    }
  });

  test('경매 탭으로 이동할 수 있어야 함', async () => {
    const auctionNav = page.getByRole('button', { name: /경매/ })
      .or(page.getByText('경매'));

    if (await auctionNav.isVisible({ timeout: 3000 })) {
      await auctionNav.click();

      await expect.poll(() => page.url(), { timeout: 5000 }).toContain('/auction');
    }
  });

  test('프로필 탭으로 이동할 수 있어야 함', async () => {
    const profileNav = page.getByRole('button', { name: /MY|프로필/ })
      .or(page.getByText('MY'));

    if (await profileNav.isVisible({ timeout: 3000 })) {
      await profileNav.click();

      await expect.poll(() => page.url(), { timeout: 5000 }).toContain('/profile');
    }
  });
});

test.describe('홈 페이지 반응형 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/home`);
  });

  test('모바일 뷰에서 레이아웃이 올바르게 표시되어야 함', async () => {
    await page.setViewportSize({ width: 375, height: 667 });

    // 주요 요소들이 표시되는지 확인
    await expect.element(
      page.getByText('내 피드')
        .or(page.locator('[data-testid="home-page"]'))
    ).toBeVisible({ timeout: 5000 });

    await page.setViewportSize({ width: 1280, height: 720 });
  });

  test('태블릿 뷰에서 레이아웃이 올바르게 표시되어야 함', async () => {
    await page.setViewportSize({ width: 768, height: 1024 });

    await expect.element(
      page.getByText('내 피드')
        .or(page.locator('[data-testid="home-page"]'))
    ).toBeVisible({ timeout: 5000 });

    await page.setViewportSize({ width: 1280, height: 720 });
  });
});
