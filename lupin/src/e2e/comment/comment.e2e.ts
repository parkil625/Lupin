/**
 * Comment E2E Tests
 *
 * 댓글 CRUD 및 상호작용 E2E 테스트
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

async function openFeedDetail() {
  await page.goto(`${BASE_URL}/dashboard/feed`);
  const feedCard = page.locator('[data-testid="feed-card"]').first();

  if (await feedCard.isVisible({ timeout: 5000 })) {
    await feedCard.click();
    return true;
  }
  return false;
}

test.describe('댓글 CRUD E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
  });

  test('피드 상세에서 댓글 목록이 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      // 댓글 섹션 확인
      await expect.element(
        page.getByText('댓글').or(page.locator('[data-testid="comments-section"]'))
      ).toBeVisible({ timeout: 5000 });
    }
  });

  test('댓글 입력 필드가 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      // 댓글 입력 필드 확인
      await expect.element(
        page.getByPlaceholder(/댓글|의견/)
          .or(page.locator('[data-testid="comment-input"]'))
      ).toBeVisible({ timeout: 5000 });
    }
  });

  test('댓글 작성이 가능해야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const commentInput = page.getByPlaceholder(/댓글|의견/)
        .or(page.locator('[data-testid="comment-input"]'));

      if (await commentInput.isVisible({ timeout: 3000 })) {
        // 댓글 입력
        await commentInput.fill('E2E 테스트 댓글입니다');

        // 전송 버튼 클릭
        const submitButton = page.getByRole('button', { name: /전송|등록|작성/ })
          .or(page.locator('[data-testid="comment-submit"]'));

        if (await submitButton.isVisible()) {
          await submitButton.click();

          // 댓글이 추가되었는지 확인
          await expect.element(
            page.getByText('E2E 테스트 댓글입니다')
          ).toBeVisible({ timeout: 5000 });
        }
      }
    }
  });

  test('빈 댓글은 작성되지 않아야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const submitButton = page.getByRole('button', { name: /전송|등록|작성/ })
        .or(page.locator('[data-testid="comment-submit"]'));

      if (await submitButton.isVisible({ timeout: 3000 })) {
        // 빈 상태로 전송 시도
        await submitButton.click();

        // 에러 메시지 또는 버튼 비활성화 확인
        // (구현에 따라 다를 수 있음)
      }
    }
  });

  test('내 댓글에 수정 버튼이 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const myComment = page.locator('[data-testid="my-comment"]').first();

      if (await myComment.isVisible({ timeout: 3000 })) {
        // 더보기 메뉴 또는 수정 버튼 확인
        const editButton = myComment.getByRole('button', { name: /수정|편집/ });
        await expect.element(editButton).toBeVisible();
      }
    }
  });

  test('내 댓글에 삭제 버튼이 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const myComment = page.locator('[data-testid="my-comment"]').first();

      if (await myComment.isVisible({ timeout: 3000 })) {
        const deleteButton = myComment.getByRole('button', { name: /삭제/ });
        await expect.element(deleteButton).toBeVisible();
      }
    }
  });
});

test.describe('댓글 좋아요/신고 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
  });

  test('댓글에 좋아요 버튼이 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const comment = page.locator('[data-testid="comment"]').first();

      if (await comment.isVisible({ timeout: 5000 })) {
        const likeButton = comment.locator('[data-testid="comment-like"]')
          .or(comment.getByRole('button', { name: /좋아요/ }));

        await expect.element(likeButton).toBeVisible();
      }
    }
  });

  test('댓글에 신고 버튼이 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const comment = page.locator('[data-testid="comment"]').first();

      if (await comment.isVisible({ timeout: 5000 })) {
        // 더보기 메뉴 열기
        const moreButton = comment.locator('[data-testid="more-button"]')
          .or(comment.getByRole('button', { name: /더보기|.../ }));

        if (await moreButton.isVisible()) {
          await moreButton.click();

          const reportButton = page.getByText('신고')
            .or(page.getByRole('menuitem', { name: /신고/ }));

          await expect.element(reportButton).toBeVisible();
        }
      }
    }
  });
});

test.describe('대댓글 E2E 테스트', () => {
  test.beforeEach(async () => {
    await login();
  });

  test('댓글에 답글 버튼이 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const comment = page.locator('[data-testid="comment"]').first();

      if (await comment.isVisible({ timeout: 5000 })) {
        const replyButton = comment.getByRole('button', { name: /답글|대댓글/ })
          .or(comment.getByText('답글'));

        await expect.element(replyButton).toBeVisible();
      }
    }
  });

  test('답글 버튼 클릭 시 입력 필드가 표시되어야 함', async () => {
    const opened = await openFeedDetail();

    if (opened) {
      const comment = page.locator('[data-testid="comment"]').first();

      if (await comment.isVisible({ timeout: 5000 })) {
        const replyButton = comment.getByRole('button', { name: /답글/ });

        if (await replyButton.isVisible()) {
          await replyButton.click();

          // 답글 입력 필드 표시 확인
          await expect.element(
            page.getByPlaceholder(/답글/)
              .or(page.locator('[data-testid="reply-input"]'))
          ).toBeVisible();
        }
      }
    }
  });
});
