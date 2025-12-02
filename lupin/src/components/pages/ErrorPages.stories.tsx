import type { Meta, StoryObj } from '@storybook/react';
import { NotFoundPage, ErrorPage } from '../errors';

/**
 * 에러 페이지 컴포넌트
 *
 * 다양한 에러 상황에서 사용자에게 보여지는 페이지입니다.
 * - 404 Not Found: 페이지를 찾을 수 없을 때
 * - 500 Error: 서버 오류 또는 예기치 않은 에러 발생 시
 */

// NotFoundPage 스토리
const notFoundMeta = {
  title: 'Pages/ErrorPages/NotFoundPage',
  component: NotFoundPage,
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
} satisfies Meta<typeof NotFoundPage>;

export default notFoundMeta;
type NotFoundStory = StoryObj<typeof notFoundMeta>;

/**
 * 404 페이지
 *
 * 요청한 페이지를 찾을 수 없을 때 표시됩니다.
 * 당황한 루핀이 캐릭터와 함께 친근한 메시지를 보여줍니다.
 */
export const NotFound: NotFoundStory = {};

// ErrorPage 스토리 (별도 export)
/**
 * 500 에러 페이지 - 기본
 *
 * 서버 에러 발생 시 표시됩니다.
 * 지친 루핀이 캐릭터와 함께 에러 메시지를 보여줍니다.
 */
export const ServerError: StoryObj<typeof ErrorPage> = {
  render: () => (
    <div style={{ minHeight: '100vh', padding: 0 }}>
      <ErrorPage />
    </div>
  ),
  parameters: {
    layout: 'fullscreen',
  },
};

/**
 * 500 에러 페이지 - 커스텀 메시지
 */
export const ServerErrorCustomMessage: StoryObj<typeof ErrorPage> = {
  render: () => (
    <div style={{ minHeight: '100vh', padding: 0 }}>
      <ErrorPage
        title="서버와 연결할 수 없습니다"
        message="잠시 후 다시 시도해주세요. 문제가 계속되면 관리자에게 문의하세요."
      />
    </div>
  ),
  parameters: {
    layout: 'fullscreen',
  },
};

