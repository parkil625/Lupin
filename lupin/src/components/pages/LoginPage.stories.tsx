import type { Meta, StoryObj } from '@storybook/react-vite';
import Login from '../auth/Login';

/**
 * 로그인 페이지
 *
 * 사용자 인증을 위한 로그인 화면입니다.
 * - 사내 아이디/비밀번호 로그인
 * - SNS 간편 로그인 (네이버, 카카오, 구글)
 */
const meta = {
  title: 'Pages/LoginPage',
  component: Login,
  parameters: {
    layout: 'fullscreen',
    backgrounds: { default: 'light' },
  },
  decorators: [
    (Story) => (
      <div style={{ minHeight: '100vh', padding: 0 }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Login>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 로그인 페이지
 */
export const Default: Story = {};
