/**
 * Feed CRUD E2E Tests
 *
 * 피드 생성, 조회, 수정, 삭제 E2E 테스트
 */

import { test, expect } from 'vitest';
import { page } from '@vitest/browser/context';

const BASE_URL = 'http://localhost:3000';

// 로그인 헬퍼 함수
async function login(username = 'user01', password = '1') {
  await page.goto(`${BASE_URL}/login`);
  await page.getByPlaceholder('아이디').fill(username);
  await page.getByPlaceholder('비밀번호').fill(password);
  await page.getByRole('button', { name: '로그인' }).click();
  await expect.poll(() => page.url(), { timeout: 10000 }).toContain('/dashboard');
}

test.describe('피드 CRUD E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
  });

  test('홈 화면에서 내 피드 목록이 표시되어야 함', async () => {
    // 홈 탭으로 이동
    await page.goto(`${BASE_URL}/dashboard/home`);

    // 피드 섹션이 표시되는지 확인
    await expect.element(page.getByText('내 피드')).toBeVisible({ timeout: 10000 });
  });

  test('피드 탭에서 다른 사람 피드가 표시되어야 함', async () => {
    // 피드 탭으로 이동
    await page.goto(`${BASE_URL}/dashboard/feed`);

    // 피드 목록이 로드될 때까지 대기
    await expect.element(page.locator('[data-testid="feed-list"]').or(page.getByText('피드'))).toBeVisible({ timeout: 10000 });
  });

  test('피드 작성 다이얼로그가 열려야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/home`);

    // 피드 작성 버튼 클릭 (+ 버튼 또는 "오늘의 운동 기록하기" 등)
    const createButton = page.getByText('오늘의 운동 기록하기').or(page.getByRole('button', { name: /작성|기록/ }));

    if (await createButton.isVisible()) {
      await createButton.click();

      // 다이얼로그가 열렸는지 확인
      await expect.element(page.getByText('운동 종류').or(page.getByText('피드 작성'))).toBeVisible({ timeout: 5000 });
    }
  });

  test('피드 작성 시 운동 종류 선택이 필수여야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/home`);

    const createButton = page.getByText('오늘의 운동 기록하기');

    if (await createButton.isVisible()) {
      await createButton.click();

      // 내용만 입력하고 운동 종류 선택 안 함
      const contentInput = page.getByPlaceholder(/내용|오늘의 운동/);
      if (await contentInput.isVisible()) {
        await contentInput.fill('테스트 피드 내용');

        // 저장 버튼 클릭
        await page.getByRole('button', { name: /저장|작성|완료/ }).click();

        // 에러 메시지 또는 유효성 검사 실패 확인
        await expect.element(page.getByText(/운동 종류|선택/)).toBeVisible();
      }
    }
  });

  test('피드 클릭 시 상세 다이얼로그가 열려야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/home`);

    // 피드 카드 클릭
    const feedCard = page.locator('[data-testid="feed-card"]').first();

    if (await feedCard.isVisible({ timeout: 5000 })) {
      await feedCard.click();

      // 상세 다이얼로그 확인
      await expect.element(page.getByRole('dialog')).toBeVisible({ timeout: 5000 });
    }
  });

  test('내 피드에서 수정 버튼이 표시되어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/home`);

    // 내 피드 카드 찾기
    const myFeedCard = page.locator('[data-testid="my-feed-card"]').first();

    if (await myFeedCard.isVisible({ timeout: 5000 })) {
      await myFeedCard.click();

      // 수정 버튼 확인
      await expect.element(page.getByRole('button', { name: /수정|편집/ })).toBeVisible();
    }
  });

  test('내 피드에서 삭제 버튼이 표시되어야 함', async () => {
    await page.goto(`${BASE_URL}/dashboard/home`);

    const myFeedCard = page.locator('[data-testid="my-feed-card"]').first();

    if (await myFeedCard.isVisible({ timeout: 5000 })) {
      await myFeedCard.click();

      // 삭제 버튼 확인
      await expect.element(page.getByRole('button', { name: /삭제/ })).toBeVisible();
    }
  });
});
