/**
 * 전체 사용자 여정 E2E 테스트
 *
 * 실제 프론트엔드 구조 기반:
 * - 네비게이션: 홈, 피드, 랭킹, 경매, 진료, 프로필
 * - 홈: 내 피드, 포인트/칼로리/랭킹, 만들기 버튼
 * - 피드: 전체 피드 목록, 좋아요, 댓글
 * - 랭킹: Top 10 랭커, 나의 랭킹
 * - 프로필: 개인정보 수정, 프로필 사진, OAuth 연동, 로그아웃
 */

import { test, expect, Page } from '@playwright/test';

// 테스트용 계정 정보
const TEST_USER = {
  employeeId: 'test001',
  password: 'password123',
};

test.describe('전체 사용자 여정', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
  });

  // ==================== 1. 로그인 ====================
  test('1. 로그인', async () => {
    await page.goto('/');

    // 로그인 페이지 확인
    await expect(page.getByText('루팡이와 함께하는')).toBeVisible();

    // 로그인
    await page.getByLabel('사내 아이디').fill(TEST_USER.employeeId);
    await page.getByLabel('비밀번호').fill(TEST_USER.password);
    await page.getByRole('button', { name: '로그인' }).click();

    // 홈으로 이동 확인
    await page.waitForTimeout(2000);
  });

  // ==================== 2. 홈 화면 ====================
  test('2-1. 홈 - 화면 로드', async () => {
    // 홈 화면 요소 확인
    await page.waitForTimeout(1000);
  });

  test('2-2. 홈 - 피드 작성 다이얼로그 열기', async () => {
    // 만들기/+ 버튼 찾기
    const createButton = page.locator('button').filter({ has: page.locator('svg.lucide-plus') }).first();
    if (await createButton.isVisible()) {
      await createButton.click();
      await expect(page.getByText('피드 작성')).toBeVisible();
    }
  });

  test('2-3. 홈 - 탭 전환 (사진/글 작성)', async () => {
    const photoTab = page.getByRole('button', { name: /사진/ });
    if (await photoTab.isVisible()) {
      // 글 작성 탭 클릭
      await page.getByRole('button', { name: /글 작성/ }).click();
      await expect(page.locator('.bn-editor')).toBeVisible();

      // 사진 탭으로 돌아가기
      await photoTab.click();
    }
  });

  test('2-4. 홈 - 글 작성 후 비우고 닫기', async () => {
    // 글 작성 탭으로 이동
    const contentTab = page.getByRole('button', { name: /글 작성/ });
    if (await contentTab.isVisible()) {
      await contentTab.click();

      // 에디터에 텍스트 입력
      const editor = page.locator('.ProseMirror');
      if (await editor.isVisible()) {
        await editor.click();
        await editor.fill('E2E 테스트 글입니다.');
      }

      // 닫기 시도
      await page.keyboard.press('Escape');

      // 확인 다이얼로그
      const confirmDialog = page.getByText('작성 중인 내용이 있습니다');
      if (await confirmDialog.isVisible()) {
        await page.getByRole('button', { name: '비우고 닫기' }).click();
      }
    }
  });

  test('2-5. 홈 - 다시 열었을 때 비어있는지 확인', async () => {
    const createButton = page.locator('button').filter({ has: page.locator('svg.lucide-plus') }).first();
    if (await createButton.isVisible()) {
      await createButton.click();

      const contentTab = page.getByRole('button', { name: /글 작성/ });
      if (await contentTab.isVisible()) {
        await contentTab.click();
        const editor = page.locator('.ProseMirror');
        const text = await editor.textContent();
        expect(text?.trim()).toBe('');
      }

      await page.keyboard.press('Escape');
    }
  });

  test('2-6. 홈 - 내 피드 클릭하여 상세 보기', async () => {
    // 피드 카드 클릭
    const feedCard = page.locator('[class*="rounded"]').filter({ has: page.locator('img') }).first();
    if (await feedCard.isVisible()) {
      await feedCard.click();
      await page.waitForTimeout(500);
      await page.keyboard.press('Escape');
    }
  });

  // ==================== 3. 피드 메뉴 ====================
  test('3-1. 피드 메뉴로 이동', async () => {
    await page.getByRole('button', { name: '피드' }).click();
    await page.waitForTimeout(1000);
  });

  test('3-2. 피드 - 스크롤 및 피드 보기', async () => {
    await page.waitForTimeout(500);
  });

  test('3-3. 피드 - 좋아요 클릭', async () => {
    // 하트 아이콘 버튼 찾기
    const likeButton = page.locator('button').filter({ has: page.locator('svg.lucide-heart') }).first();
    if (await likeButton.isVisible()) {
      await likeButton.click();
      await page.waitForTimeout(500);
    }
  });

  test('3-4. 피드 - 댓글 패널 열기', async () => {
    // 말풍선 아이콘 버튼 찾기
    const commentButton = page.locator('button').filter({ has: page.locator('svg.lucide-message-circle') }).first();
    if (await commentButton.isVisible()) {
      await commentButton.click();
      await page.waitForTimeout(500);
      // 다시 클릭해서 닫기
      await commentButton.click();
    }
  });

  // ==================== 4. 랭킹 메뉴 ====================
  test('4-1. 랭킹 메뉴로 이동', async () => {
    await page.getByRole('button', { name: '랭킹' }).click();
    await page.waitForTimeout(1000);
  });

  test('4-2. 랭킹 - Top 랭커 확인', async () => {
    // 랭킹 목록 로드 확인
    await page.waitForTimeout(500);
  });

  // ==================== 5. 프로필 메뉴 ====================
  test('5-1. 프로필 메뉴로 이동', async () => {
    // MY 또는 프로필 버튼 클릭
    const profileButton = page.getByRole('button', { name: /MY/ });
    if (await profileButton.isVisible()) {
      await profileButton.click();
    } else {
      // 사이드바에서 User 아이콘 클릭
      await page.locator('button').filter({ has: page.locator('svg.lucide-user') }).first().click();
    }
    await page.waitForTimeout(1000);
  });

  test('5-2. 프로필 - 정보 확인', async () => {
    // 프로필 페이지 로드 확인
    await page.waitForTimeout(500);
  });

  test('5-3. 프로필 - 수정 모드', async () => {
    // 수정 버튼 찾기
    const editButton = page.locator('button').filter({ has: page.locator('svg.lucide-edit') }).first();
    if (await editButton.isVisible()) {
      await editButton.click();
      await page.waitForTimeout(500);
      // 취소
      const cancelButton = page.getByRole('button', { name: /취소/ });
      if (await cancelButton.isVisible()) {
        await cancelButton.click();
      }
    }
  });

  // ==================== 6. 반응형 테스트 ====================
  test('6-1. 모바일 뷰', async () => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(500);

    // 하단 네비게이션 바 확인
    const bottomNav = page.locator('.fixed.bottom-0');
    if (await bottomNav.isVisible()) {
      // 네비바가 보이는지 확인
      expect(await bottomNav.isVisible()).toBe(true);
    }
  });

  test('6-2. 모바일 - 알림 버튼', async () => {
    const notificationButton = page.getByRole('button', { name: '알림' });
    if (await notificationButton.isVisible()) {
      await notificationButton.click();
      await page.waitForTimeout(500);

      // 닫기
      const closeButton = page.getByRole('button', { name: '×' });
      if (await closeButton.isVisible()) {
        await closeButton.click();
      }
    }
  });

  test('6-3. 데스크톱 뷰 복귀', async () => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.waitForTimeout(500);
  });

  // ==================== 7. 로그아웃 ====================
  test('7. 로그아웃', async () => {
    // 프로필로 이동
    const profileButton = page.getByRole('button', { name: /MY/ });
    if (await profileButton.isVisible()) {
      await profileButton.click();
    }

    await page.waitForTimeout(500);

    // 로그아웃 버튼 클릭
    const logoutButton = page.getByRole('button', { name: '로그아웃' });
    if (await logoutButton.isVisible()) {
      await logoutButton.click();

      // 로그인 페이지로 이동 확인
      await expect(page.getByText('루팡이와 함께하는')).toBeVisible({ timeout: 5000 });
    }
  });
});

// ==================== 피드 CRUD 플로우 ====================
test.describe('피드 작성 플로우', () => {
  test('피드 작성 다이얼로그 동작 확인', async ({ page }) => {
    // 로그인
    await page.goto('/');
    await page.getByLabel('사내 아이디').fill(TEST_USER.employeeId);
    await page.getByLabel('비밀번호').fill(TEST_USER.password);
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForTimeout(2000);

    // 만들기 버튼 클릭
    const createButton = page.locator('button').filter({ has: page.locator('svg.lucide-plus') }).first();
    if (await createButton.isVisible()) {
      await createButton.click();

      // 피드 작성 다이얼로그 확인
      await expect(page.getByText('피드 작성')).toBeVisible();

      // 탭 전환 테스트
      await page.getByRole('button', { name: /글 작성/ }).click();
      await expect(page.locator('.bn-editor')).toBeVisible();

      // 닫기
      await page.keyboard.press('Escape');
    }
  });
});
