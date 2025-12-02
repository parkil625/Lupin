import type { Meta, StoryObj } from '@storybook/react-vite';
import LandingPage from '../LandingPage';

/**
 * 랜딩 페이지 - 서비스 소개 및 로그인 유도
 *
 * Lupin 헬스케어 플랫폼의 첫 화면입니다.
 * - 서비스 소개
 * - 주요 기능 안내
 * - 로그인 버튼
 */
const meta = {
  title: 'Pages/LandingPage',
  component: LandingPage,
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
} satisfies Meta<typeof LandingPage>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 랜딩 페이지
 */
export const Default: Story = {};
