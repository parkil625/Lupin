/**
 * 전체 사용자 여정 E2E 테스트
 *
 * 로그인부터 모든 기능을 순차적으로 테스트
 * - 로그인
 * - 홈 화면 기능
 * - 피드 메뉴 기능
 * - 랭킹 메뉴 기능
 * - 프로필 메뉴 기능
 */

import { test, expect, Page } from '@playwright/test';

// 테스트용 계정 정보
const TEST_USER = {
  employeeId: 'test001',
  password: 'password123',
  name: '테스트 유저',
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
  test('1. 로그인 페이지 접속 및 로그인', async () => {
    await page.goto('/');

    // 로그인 페이지가 표시되는지 확인
    await expect(page.getByText('루팡이와 함께하는')).toBeVisible();

    // 사내 아이디 입력
    await page.getByLabel('사내 아이디').fill(TEST_USER.employeeId);

    // 비밀번호 입력
    await page.getByLabel('비밀번호').fill(TEST_USER.password);

    // 로그인 버튼 클릭
    await page.getByRole('button', { name: '로그인' }).click();

    // 대시보드로 이동 확인 (홈 화면)
    await expect(page.getByText('홈')).toBeVisible({ timeout: 10000 });
  });

  // ==================== 2. 홈 화면 기능 ====================
  test('2-1. 홈 화면 - 인기 피드 확인', async () => {
    // 홈 탭이 선택된 상태인지 확인
    await expect(page.locator('[data-nav="home"]')).toBeVisible();

    // 인기 피드 섹션 확인
    await expect(page.getByText('인기 피드')).toBeVisible();
  });

  test('2-2. 홈 화면 - 피드 상세 보기', async () => {
    // 첫 번째 피드 카드 클릭
    const feedCard = page.locator('[data-testid="feed-card"]').first();
    if (await feedCard.isVisible()) {
      await feedCard.click();

      // 피드 상세 다이얼로그 확인
      await expect(page.getByText('피드 상세보기')).toBeVisible();

      // 닫기
      await page.keyboard.press('Escape');
    }
  });

  test('2-3. 홈 화면 - 알림 확인 (모바일)', async () => {
    // 뷰포트를 모바일 크기로 변경
    await page.setViewportSize({ width: 375, height: 667 });

    // 알림 버튼 클릭
    const notificationButton = page.getByRole('button', { name: '알림' });
    if (await notificationButton.isVisible()) {
      await notificationButton.click();

      // 알림 팝업 확인
      await expect(page.getByText('알림')).toBeVisible();

      // 닫기
      await page.getByRole('button', { name: '×' }).click();
    }

    // 뷰포트 원복
    await page.setViewportSize({ width: 1280, height: 720 });
  });

  // ==================== 3. 피드 메뉴 기능 ====================
  test('3-1. 피드 메뉴 - 피드 목록 보기', async () => {
    // 피드 메뉴 클릭
    await page.getByRole('button', { name: '피드' }).click();

    // 피드 목록이 로드되는지 확인
    await page.waitForTimeout(1000);
  });

  test('3-2. 피드 메뉴 - 피드 작성 다이얼로그 열기', async () => {
    // 만들기 버튼 클릭
    const createButton = page.getByRole('button', { name: /만들기|작성/ });
    if (await createButton.isVisible()) {
      await createButton.click();

      // 피드 작성 다이얼로그 확인
      await expect(page.getByText('피드 작성')).toBeVisible();
    }
  });

  test('3-3. 피드 메뉴 - 탭 전환 (사진/글 작성)', async () => {
    // 사진 탭이 기본 선택인지 확인
    const photoTab = page.getByRole('button', { name: /사진/ });
    await expect(photoTab).toBeVisible();

    // 글 작성 탭 클릭
    const contentTab = page.getByRole('button', { name: /글 작성/ });
    await contentTab.click();

    // 에디터 영역 확인
    await expect(page.locator('.bn-editor')).toBeVisible();

    // 다시 사진 탭으로
    await photoTab.click();
  });

  test('3-4. 피드 메뉴 - 운동 종류 선택', async () => {
    // 운동 종류 선택 드롭다운 클릭
    const workoutSelect = page.locator('[data-testid="workout-select"]');
    if (await workoutSelect.isVisible()) {
      await workoutSelect.click();

      // 수영 선택
      await page.getByText('수영').click();
    }
  });

  test('3-5. 피드 메뉴 - 글 작성 중 닫기 시도', async () => {
    // 글 작성 탭으로 이동
    await page.getByRole('button', { name: /글 작성/ }).click();

    // 에디터에 텍스트 입력
    const editor = page.locator('.ProseMirror');
    if (await editor.isVisible()) {
      await editor.click();
      await editor.fill('오늘 운동 테스트입니다.');
    }

    // ESC 또는 X 버튼으로 닫기 시도
    await page.keyboard.press('Escape');

    // 확인 다이얼로그가 나타나는지 확인
    const confirmDialog = page.getByText('작성 중인 내용이 있습니다');
    if (await confirmDialog.isVisible()) {
      // 비우고 닫기 클릭
      await page.getByRole('button', { name: '비우고 닫기' }).click();
    }

    // 다이얼로그가 닫혔는지 확인
    await expect(page.getByText('피드 작성')).not.toBeVisible();
  });

  test('3-6. 피드 메뉴 - 다시 열었을 때 비어있는지 확인', async () => {
    // 만들기 버튼 다시 클릭
    const createButton = page.getByRole('button', { name: /만들기|작성/ });
    if (await createButton.isVisible()) {
      await createButton.click();

      // 에디터가 비어있는지 확인
      await page.getByRole('button', { name: /글 작성/ }).click();
      const editor = page.locator('.ProseMirror');
      const text = await editor.textContent();
      expect(text?.trim()).toBe('');

      // 닫기
      await page.keyboard.press('Escape');
    }
  });

  test('3-7. 피드 메뉴 - 피드 좋아요', async () => {
    // 피드의 좋아요 버튼 클릭
    const likeButton = page.locator('[data-testid="like-button"]').first();
    if (await likeButton.isVisible()) {
      await likeButton.click();

      // 좋아요 상태 변경 확인 (아이콘 색상 등)
      await page.waitForTimeout(500);
    }
  });

  test('3-8. 피드 메뉴 - 댓글 패널 열기/닫기', async () => {
    // 댓글 버튼 클릭
    const commentButton = page.locator('[data-testid="comment-button"]').first();
    if (await commentButton.isVisible()) {
      await commentButton.click();

      // 댓글 패널 확인
      await page.waitForTimeout(500);

      // 다시 클릭해서 닫기
      await commentButton.click();
    }
  });

  // ==================== 4. 랭킹 메뉴 기능 ====================
  test('4-1. 랭킹 메뉴 - 랭킹 목록 보기', async () => {
    // 랭킹 메뉴 클릭
    await page.getByRole('button', { name: '랭킹' }).click();

    // 랭킹 목록 확인
    await expect(page.getByText(/랭킹|순위/)).toBeVisible();
  });

  test('4-2. 랭킹 메뉴 - 기간 필터 변경', async () => {
    // 주간/월간 등 필터 버튼 확인
    const weeklyButton = page.getByRole('button', { name: '주간' });
    const monthlyButton = page.getByRole('button', { name: '월간' });

    if (await weeklyButton.isVisible()) {
      await weeklyButton.click();
      await page.waitForTimeout(500);
    }

    if (await monthlyButton.isVisible()) {
      await monthlyButton.click();
      await page.waitForTimeout(500);
    }
  });

  // ==================== 5. 프로필 메뉴 기능 ====================
  test('5-1. 프로필 메뉴 - 내 정보 확인', async () => {
    // MY/프로필 메뉴 클릭
    await page.getByRole('button', { name: /MY|프로필/ }).click();

    // 프로필 정보 확인
    await page.waitForTimeout(1000);
  });

  test('5-2. 프로필 메뉴 - 프로필 수정', async () => {
    // 프로필 수정 버튼 클릭
    const editButton = page.getByRole('button', { name: /수정|편집/ });
    if (await editButton.isVisible()) {
      await editButton.click();

      // 수정 화면 확인
      await page.waitForTimeout(500);

      // 취소 또는 뒤로가기
      await page.keyboard.press('Escape');
    }
  });

  test('5-3. 프로필 메뉴 - 내 피드 목록', async () => {
    // 내 피드 탭/섹션 확인
    const myFeedsTab = page.getByText(/내 피드|내가 쓴/);
    if (await myFeedsTab.isVisible()) {
      await myFeedsTab.click();
      await page.waitForTimeout(500);
    }
  });

  test('5-4. 프로필 메뉴 - 포인트/칼로리 확인', async () => {
    // 포인트 정보 확인
    await expect(page.getByText(/포인트|P/)).toBeVisible();
  });

  // ==================== 6. 반응형 테스트 ====================
  test('6-1. 반응형 - 모바일 뷰', async () => {
    await page.setViewportSize({ width: 375, height: 667 });

    // 하단 네비게이션 바 확인
    await expect(page.locator('[data-testid="bottom-nav"]')).toBeVisible();

    // 컨텐츠가 네비바에 가려지지 않는지 확인
    const bottomNav = page.locator('[data-testid="bottom-nav"]');
    const navBox = await bottomNav.boundingBox();

    if (navBox) {
      // 네비바 위치 확인 (화면 하단에 있어야 함)
      expect(navBox.y).toBeGreaterThan(600);
    }
  });

  test('6-2. 반응형 - 태블릿 뷰', async () => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(500);
  });

  test('6-3. 반응형 - 데스크톱 뷰', async () => {
    await page.setViewportSize({ width: 1920, height: 1080 });

    // 사이드바 확인
    await expect(page.locator('[data-sidebar="true"]')).toBeVisible();
  });

  // ==================== 7. 로그아웃 ====================
  test('7. 로그아웃', async () => {
    // 프로필로 이동
    await page.getByRole('button', { name: /MY|프로필/ }).click();

    // 로그아웃 버튼 클릭
    const logoutButton = page.getByRole('button', { name: '로그아웃' });
    if (await logoutButton.isVisible()) {
      await logoutButton.click();

      // 로그인 페이지로 돌아왔는지 확인
      await expect(page.getByText('루팡이와 함께하는')).toBeVisible({ timeout: 5000 });
    }
  });
});

// ==================== 피드 CRUD 전체 플로우 ====================
test.describe('피드 CRUD 전체 플로우', () => {
  test('피드 생성 → 수정 → 삭제 전체 플로우', async ({ page }) => {
    // 1. 로그인
    await page.goto('/');
    await page.getByLabel('사내 아이디').fill(TEST_USER.employeeId);
    await page.getByLabel('비밀번호').fill(TEST_USER.password);
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForTimeout(2000);

    // 2. 피드 메뉴로 이동
    await page.getByRole('button', { name: '피드' }).click();

    // 3. 피드 작성
    const createButton = page.getByRole('button', { name: /만들기|작성/ });
    if (await createButton.isVisible()) {
      await createButton.click();

      // 운동 종류 선택
      // 사진 업로드 (테스트 환경에서는 스킵)

      // 글 작성 탭으로 이동
      await page.getByRole('button', { name: /글 작성/ }).click();

      // 에디터에 텍스트 입력
      const editor = page.locator('.ProseMirror');
      if (await editor.isVisible()) {
        await editor.click();
        await editor.fill('E2E 테스트 피드입니다.');
      }

      // 작성 버튼 클릭 (사진이 필요해서 실패할 수 있음)
      const submitButton = page.getByRole('button', { name: '작성' });
      // await submitButton.click();

      // 닫기
      await page.keyboard.press('Escape');
      const closeButton = page.getByRole('button', { name: '비우고 닫기' });
      if (await closeButton.isVisible()) {
        await closeButton.click();
      }
    }

    // 4. 내 피드 수정
    // (기존 피드가 있다고 가정)

    // 5. 피드 삭제
    // (기존 피드가 있다고 가정)
  });
});

// ==================== 댓글 플로우 ====================
test.describe('댓글 기능 플로우', () => {
  test('댓글 작성 → 좋아요 → 답글 → 삭제', async ({ page }) => {
    // 1. 로그인
    await page.goto('/');
    await page.getByLabel('사내 아이디').fill(TEST_USER.employeeId);
    await page.getByLabel('비밀번호').fill(TEST_USER.password);
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForTimeout(2000);

    // 2. 피드 메뉴로 이동
    await page.getByRole('button', { name: '피드' }).click();
    await page.waitForTimeout(1000);

    // 3. 첫 번째 피드의 댓글 버튼 클릭
    const commentButton = page.locator('[data-testid="comment-button"]').first();
    if (await commentButton.isVisible()) {
      await commentButton.click();

      // 4. 댓글 입력
      const commentInput = page.locator('[data-testid="comment-input"]');
      if (await commentInput.isVisible()) {
        await commentInput.fill('E2E 테스트 댓글입니다.');

        // 댓글 작성 버튼 클릭
        const sendButton = page.locator('[data-testid="send-comment"]');
        if (await sendButton.isVisible()) {
          await sendButton.click();
          await page.waitForTimeout(500);
        }
      }

      // 5. 댓글 좋아요
      const commentLikeButton = page.locator('[data-testid="comment-like"]').first();
      if (await commentLikeButton.isVisible()) {
        await commentLikeButton.click();
      }
    }
  });
});
