import type { Meta, StoryObj } from '@storybook/react';
import LandingPage from '../LandingPage';
import Login from '../auth/Login';

/**
 * 페이지 플로우 - 전체 화면 흐름을 보여줍니다
 *
 * 웹 디자인 보고서용 화면 캡처에 사용할 수 있습니다.
 */
const meta = {
  title: 'Pages/PageFlow',
  parameters: {
    layout: 'fullscreen',
    backgrounds: { default: 'light' },
  },
} satisfies Meta;

export default meta;
type Story = StoryObj;

/**
 * 1. 랜딩 페이지 (첫 화면)
 *
 * 서비스 소개 페이지입니다.
 * "로그인" 버튼을 클릭하면 로그인 페이지로 이동합니다.
 */
export const Step1_LandingPage: Story = {
  render: () => <LandingPage />,
};

/**
 * 2. 로그인 페이지
 *
 * 사용자 인증 화면입니다.
 * - 사내 아이디/비밀번호
 * - SNS 간편 로그인
 */
export const Step2_LoginPage: Story = {
  render: () => <Login />,
};
