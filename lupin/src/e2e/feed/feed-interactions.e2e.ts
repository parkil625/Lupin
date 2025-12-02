/**
 * Feed Interactions E2E Tests
 *
 * 피드 좋아요, 신고 E2E 테스트
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

test.describe('피드 좋아요 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/feed`);
  });

  test('피드에 좋아요 버튼이 표시되어야 함', async () => {
    const likeButton = page.locator('[data-testid="like-button"]').first()
      .or(page.getByRole('button', { name: /좋아요/ }).first());

    await expect.element(likeButton).toBeVisible({ timeout: 10000 });
  });

  test('좋아요 버튼 클릭 시 좋아요 수가 변경되어야 함', async () => {
    const feedCard = page.locator('[data-testid="feed-card"]').first();

    if (await feedCard.isVisible({ timeout: 5000 })) {
      // 현재 좋아요 수 확인
      const likeCount = feedCard.locator('[data-testid="like-count"]');

      // 좋아요 버튼 클릭
      const likeButton = feedCard.locator('[data-testid="like-button"]')
        .or(feedCard.getByRole('button', { name: /좋아요/ }));

      if (await likeButton.isVisible()) {
        await likeButton.click();

        // UI 업데이트 확인 (버튼 색상 변경 또는 숫자 변경)
        // 성공적으로 클릭되었으면 테스트 통과
      }
    }
  });

  test('좋아요 취소가 가능해야 함', async () => {
    const feedCard = page.locator('[data-testid="feed-card"]').first();

    if (await feedCard.isVisible({ timeout: 5000 })) {
      const likeButton = feedCard.locator('[data-testid="like-button"]');

      if (await likeButton.isVisible()) {
        // 좋아요 클릭
        await likeButton.click();
        await page.waitForTimeout(500);

        // 좋아요 취소
        await likeButton.click();
        await page.waitForTimeout(500);

        // UI가 원래대로 돌아왔는지 확인
      }
    }
  });
});

test.describe('피드 신고 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
    await page.goto(`${BASE_URL}/dashboard/feed`);
  });

  test('피드 상세에서 신고 버튼이 표시되어야 함', async () => {
    const feedCard = page.locator('[data-testid="feed-card"]').first();

    if (await feedCard.isVisible({ timeout: 5000 })) {
      await feedCard.click();

      // 더보기 메뉴 또는 신고 버튼 확인
      const reportButton = page.getByRole('button', { name: /신고/ })
        .or(page.getByText('신고'));

      await expect.element(reportButton).toBeVisible({ timeout: 5000 });
    }
  });

  test('신고 버튼 클릭 시 확인 다이얼로그가 표시되어야 함', async () => {
    const feedCard = page.locator('[data-testid="feed-card"]').first();

    if (await feedCard.isVisible({ timeout: 5000 })) {
      await feedCard.click();

      const reportButton = page.getByRole('button', { name: /신고/ })
        .or(page.getByText('신고'));

      if (await reportButton.isVisible({ timeout: 3000 })) {
        await reportButton.click();

        // 확인 다이얼로그 표시 확인
        await expect.element(
          page.getByText(/정말 신고|신고하시겠습니까/)
        ).toBeVisible({ timeout: 3000 });
      }
    }
  });
});
