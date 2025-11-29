import type { Preview } from '@storybook/react';
import { MemoryRouter } from 'react-router-dom';
import '../src/fonts.css'; // NanumSquare Font
import '../src/index.css'; // Tailwind CSS import
import '../src/styles/globals.css'; // Custom utilities (scrollbar-hide, etc.)

// API Mock for Storybook
import { reportApi } from '../src/api';
import { commentApi } from '../src/api';

// Mock reportApi
reportApi.reportFeed = async () => Promise.resolve({ success: true });
reportApi.reportComment = async () => Promise.resolve({ success: true });

// Mock commentApi
commentApi.getCommentsByFeedId = async () => Promise.resolve({ content: [] });
commentApi.getRepliesByCommentId = async () => Promise.resolve([]);
commentApi.createComment = async (data: any) => Promise.resolve({ id: Date.now(), content: data.content, writerName: '테스트 유저' });
commentApi.deleteComment = async () => Promise.resolve({ success: true });

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },

    backgrounds: {
      default: 'light',
      values: [
        { name: 'light', value: '#ffffff' },
        { name: 'dark', value: '#1a1a1a' },
        { name: 'gray', value: '#f5f5f5' },
      ],
    },

    a11y: {
      // 'todo' - show a11y violations in the test UI only
      // 'error' - fail CI on a11y violations
      // 'off' - skip a11y checks entirely
      test: 'todo'
    }
  },
  // 전역 데코레이터: 모든 스토리에 적용
  decorators: [
    (Story) => (
      <MemoryRouter>
        <Story />
      </MemoryRouter>
    ),
  ],
};

export default preview;
