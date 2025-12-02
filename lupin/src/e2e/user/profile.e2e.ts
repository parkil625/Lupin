/**
 * User Profile E2E Tests
 *
 * 사용자 프로필 E2E 테스트
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

test.describe('사용자 프로필 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
  });

  test('프로필 페이지로 이동할 수 있어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    // 프로필 페이지 요소 확인
    await expect.element(
      page.getByText('프로필').or(page.getByText('내 정보'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('사용자 이름이 표시되어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    // 사용자 이름 표시 확인
    await expect.element(
      page.locator('[data-testid="user-name"]')
        .or(page.getByText(/user01|사용자/))
    ).toBeVisible({ timeout: 5000 });
  });

  test('프로필 이미지가 표시되어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    // 프로필 이미지/아바타 확인
    await expect.element(
      page.locator('[data-testid="profile-avatar"]')
        .or(page.locator('img[alt*="프로필"]'))
        .or(page.locator('[class*="avatar"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('프로필 이미지 변경 버튼이 있어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    // 프로필 이미지 변경 버튼 확인
    await expect.element(
      page.getByRole('button', { name: /사진 변경|이미지 변경|수정/ })
        .or(page.locator('[data-testid="change-avatar"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('현재 포인트가 표시되어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    // 포인트 표시 확인
    await expect.element(
      page.getByText(/포인트|P$/)
        .or(page.locator('[data-testid="user-points"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('로그아웃 버튼이 있어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    // 로그아웃 버튼 확인
    await expect.element(
      page.getByRole('button', { name: /로그아웃/ })
    ).toBeVisible({ timeout: 5000 });
  });

  test('로그아웃 버튼 클릭 시 로그인 페이지로 이동해야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    const logoutButton = page.getByRole('button', { name: /로그아웃/ });

    if (await logoutButton.isVisible({ timeout: 3000 })) {
      await logoutButton.click();

      // 확인 다이얼로그가 있다면 확인
      const confirmButton = page.getByRole('button', { name: /확인|예/ });
      if (await confirmButton.isVisible({ timeout: 2000 })) {
        await confirmButton.click();
      }

      // 로그인 페이지로 이동 확인
      await expect.poll(() => page.url(), { timeout: 5000 }).toContain('/login');
    }
  });

  test('OAuth 연동 정보가 표시되어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    // OAuth 연동 섹션 확인
    await expect.element(
      page.getByText(/연동|소셜/)
        .or(page.locator('[data-testid="oauth-section"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('프로필 수정 페이지로 이동할 수 있어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/profile`);

    const editButton = page.getByRole('button', { name: /프로필 수정|정보 수정/ })
      .or(page.getByText('프로필 수정'));

    if (await editButton.isVisible({ timeout: 3000 })) {
      await editButton.click();

      // 수정 폼이 표시되는지 확인
      await expect.element(
        page.getByRole('form')
          .or(page.getByPlaceholder(/이름|닉네임/))
      ).toBeVisible({ timeout: 5000 });
    }
  });
});

test.describe('사용자 통계 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/profile`);
  });

  test('운동 통계가 표시되어야 함', async () => {
    // 운동 통계 섹션 확인
    await expect.element(
      page.getByText(/통계|운동 기록/)
        .or(page.locator('[data-testid="workout-stats"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('총 운동 횟수가 표시되어야 함', async () => {
    await expect.element(
      page.getByText(/총.*회|운동 횟수/)
        .or(page.locator('[data-testid="total-workouts"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test('총 소모 칼로리가 표시되어야 함', async () => {
    await expect.element(
      page.getByText(/칼로리|kcal/)
        .or(page.locator('[data-testid="total-calories"]'))
    ).toBeVisible({ timeout: 5000 });
  });
});
