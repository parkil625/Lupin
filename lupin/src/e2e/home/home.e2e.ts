/**
 * Home Page E2E Tests
 *
 * 홈 페이지 E2E 테스트
 */

import { test, expect, type Page } from "@playwright/test";

const BASE_URL = "http://localhost:3000";

// [수정] page 객체를 인자로 받도록 변경
async function login(page: Page, username = "user01", password = "1") {
  await page.goto(`${BASE_URL}/login`);
  await page.getByPlaceholder("아이디").fill(username);
  await page.getByPlaceholder("비밀번호").fill(password);
  await page.getByRole("button", { name: "로그인" }).click();
  // [수정] Playwright 전용 assertion 사용
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 });
}

test.describe("홈 페이지 E2E 테스트", () => {
  // [수정] ({ page }) 픽스처 주입
  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/dashboard/home`);
  });

  test("홈 페이지가 로드되어야 함", async ({ page }) => {
    // [수정] expect.element -> expect(locator)
    await expect(
      page.getByText("홈").first().or(page.locator('[data-testid="home-page"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test("사용자 인사말이 표시되어야 함", async ({ page }) => {
    await expect(
      page
        .getByText(/안녕하세요|좋은 아침|좋은 저녁/)
        .or(page.locator('[data-testid="greeting"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test("현재 포인트가 표시되어야 함", async ({ page }) => {
    await expect(
      page
        .getByText(/포인트|P$/)
        .or(page.locator('[data-testid="current-points"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test("오늘의 운동 기록 버튼이 표시되어야 함", async ({ page }) => {
    await expect(
      page
        .getByText("오늘의 운동 기록하기")
        .or(page.getByRole("button", { name: /운동 기록|피드 작성/ }))
    ).toBeVisible({ timeout: 5000 });
  });

  test("내 피드 섹션이 표시되어야 함", async ({ page }) => {
    await expect(
      page
        .getByText("내 피드")
        .or(page.locator('[data-testid="my-feeds-section"]'))
    ).toBeVisible({ timeout: 5000 });
  });

  test("오늘 피드 작성 가능 여부가 표시되어야 함", async ({ page }) => {
    await expect(
      page
        .getByText(/오늘 운동 완료|오늘의 운동 기록하기|이미 작성/)
        .or(page.locator('[data-testid="can-post-today"]'))
    ).toBeVisible({ timeout: 5000 });
  });
});

test.describe("홈 페이지 네비게이션 E2E 테스트", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/dashboard/home`);
  });

  test("사이드바가 표시되어야 함 (데스크톱)", async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });

    await expect(
      page.locator('[data-testid="sidebar"]').or(page.locator("nav").first())
    ).toBeVisible({ timeout: 5000 });
  });

  test("모바일에서 하단 네비게이션 바가 표시되어야 함", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });

    await expect(
      page.locator('[data-testid="bottom-nav"]').or(
        page
          .locator(".md\\:hidden")
          .filter({ has: page.locator("button") })
          .first()
      )
    ).toBeVisible({ timeout: 5000 });

    // 데스크톱으로 복원
    await page.setViewportSize({ width: 1280, height: 720 });
  });

  test("피드 탭으로 이동할 수 있어야 함", async ({ page }) => {
    const feedNav = page
      .getByRole("button", { name: /피드/ })
      .or(page.getByText("피드"));

    if (await feedNav.isVisible({ timeout: 3000 })) {
      await feedNav.first().click();
      await expect(page).toHaveURL(/\/feed/, { timeout: 5000 });
    }
  });

  test("랭킹 탭으로 이동할 수 있어야 함", async ({ page }) => {
    const rankingNav = page
      .getByRole("button", { name: /랭킹/ })
      .or(page.getByText("랭킹"));

    if (await rankingNav.isVisible({ timeout: 3000 })) {
      await rankingNav.first().click();
      await expect(page).toHaveURL(/\/ranking/, { timeout: 5000 });
    }
  });

  test("프로필 탭으로 이동할 수 있어야 함", async ({ page }) => {
    const profileNav = page
      .getByRole("button", { name: /MY|프로필/ })
      .or(page.getByText("MY"));

    if (await profileNav.isVisible({ timeout: 3000 })) {
      await profileNav.first().click();
      await expect(page).toHaveURL(/\/profile/, { timeout: 5000 });
    }
  });
});

test.describe("홈 페이지 반응형 E2E 테스트", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/dashboard/home`);
  });

  test("모바일 뷰에서 레이아웃이 올바르게 표시되어야 함", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });

    await expect(
      page.getByText("내 피드").or(page.locator('[data-testid="home-page"]'))
    ).toBeVisible({ timeout: 5000 });

    await page.setViewportSize({ width: 1280, height: 720 });
  });

  test("태블릿 뷰에서 레이아웃이 올바르게 표시되어야 함", async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });

    await expect(
      page.getByText("내 피드").or(page.locator('[data-testid="home-page"]'))
    ).toBeVisible({ timeout: 5000 });

    await page.setViewportSize({ width: 1280, height: 720 });
  });
});
