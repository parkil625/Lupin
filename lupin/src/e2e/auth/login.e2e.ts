/**
 * Login E2E Tests
 *
 * 로그인 기능 E2E 테스트
 * - 일반 로그인 (아이디/비밀번호)
 * - 유효성 검사 실패 케이스
 * - 로그인 실패 케이스
 */

import { test, expect } from 'vitest';
import { page } from '@vitest/browser/context';

const BASE_URL = 'http://localhost:3000';

test.describe('로그인 E2E 테스트', () => {
  test.beforeEach(async () => {
    await page.goto(`${BASE_URL}/login`);
    // 페이지 로드 대기
    await expect.element(page.getByRole('button', { name: '로그인' })).toBeVisible();
  });

  test('로그인 폼이 올바르게 렌더링되어야 함', async () => {
    // 아이디 입력 필드
    await expect.element(page.getByPlaceholder('아이디')).toBeVisible();

    // 비밀번호 입력 필드
    await expect.element(page.getByPlaceholder('비밀번호')).toBeVisible();

    // 로그인 버튼
    await expect.element(page.getByRole('button', { name: '로그인' })).toBeVisible();

    // 소셜 로그인 버튼들
    await expect.element(page.getByTitle('네이버로 로그인')).toBeVisible();
    await expect.element(page.getByTitle('카카오로 로그인')).toBeVisible();
    await expect.element(page.getByTitle('구글 계정으로 로그인')).toBeVisible();
  });

  test('아이디를 입력하지 않으면 유효성 검사 실패해야 함', async () => {
    // 비밀번호만 입력
    await page.getByPlaceholder('비밀번호').fill('1');

    // 로그인 버튼 클릭
    await page.getByRole('button', { name: '로그인' }).click();

    // 에러 메시지 확인
    await expect.element(page.getByText('아이디를 입력해주세요')).toBeVisible();
  });

  test('비밀번호를 입력하지 않으면 유효성 검사 실패해야 함', async () => {
    // 아이디만 입력
    await page.getByPlaceholder('아이디').fill('user01');

    // 로그인 버튼 클릭
    await page.getByRole('button', { name: '로그인' }).click();

    // 에러 메시지 확인
    await expect.element(page.getByText('비밀번호를 입력해주세요')).toBeVisible();
  });

  test('올바른 자격 증명으로 로그인 성공해야 함', async () => {
    // 아이디 입력
    await page.getByPlaceholder('아이디').fill('user01');

    // 비밀번호 입력
    await page.getByPlaceholder('비밀번호').fill('1');

    // 로그인 버튼 클릭
    await page.getByRole('button', { name: '로그인' }).click();

    // 대시보드로 리다이렉트 확인 (로그인 성공 시)
    // Note: 백엔드가 실행 중이어야 함
    await expect.poll(
      () => page.url(),
      { timeout: 10000 }
    ).toContain('/dashboard');
  });

  test('잘못된 자격 증명으로 로그인 실패해야 함', async () => {
    // 아이디 입력
    await page.getByPlaceholder('아이디').fill('wronguser');

    // 비밀번호 입력
    await page.getByPlaceholder('비밀번호').fill('wrongpassword');

    // 로그인 버튼 클릭
    await page.getByRole('button', { name: '로그인' }).click();

    // 에러 메시지 확인 (401 또는 404)
    await expect.element(
      page.getByText(/아이디 또는 비밀번호가 일치하지 않습니다|존재하지 않는 사용자입니다|로그인 중 오류가 발생했습니다/)
    ).toBeVisible({ timeout: 10000 });
  });

  test('입력 필드 클리어 버튼이 동작해야 함', async () => {
    // 아이디 입력
    const idInput = page.getByPlaceholder('아이디');
    await idInput.fill('testuser');

    // 입력값 확인
    await expect.element(idInput).toHaveValue('testuser');

    // X 버튼 클릭하여 클리어
    await page.locator('[id="employeeId"]').locator('..').getByRole('button').first().click();

    // 입력값이 비워졌는지 확인
    await expect.element(idInput).toHaveValue('');
  });

  test('비밀번호 표시/숨김 토글이 동작해야 함', async () => {
    const passwordInput = page.getByPlaceholder('비밀번호');

    // 비밀번호 입력
    await passwordInput.fill('secretpassword');

    // 기본적으로 password 타입
    await expect.element(passwordInput).toHaveAttribute('type', 'password');

    // 눈 아이콘 클릭하여 표시
    await page.locator('[id="password"]').locator('..').getByRole('button').first().click();

    // text 타입으로 변경됨
    await expect.element(passwordInput).toHaveAttribute('type', 'text');
  });

  test('메인으로 돌아가기 버튼이 동작해야 함', async () => {
    // 메인으로 버튼 클릭
    await page.getByText('메인으로').click();

    // 메인 페이지로 이동 확인
    await expect.poll(
      () => page.url(),
      { timeout: 5000 }
    ).toBe(`${BASE_URL}/`);
  });
});
