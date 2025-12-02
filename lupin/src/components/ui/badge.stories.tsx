import type { Meta, StoryObj } from '@storybook/react-vite';
import { Badge } from './badge';

/**
 * Badge 컴포넌트는 상태, 태그, 라벨을 표시하는 작은 레이블 요소입니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Badge } from '@/components/ui/badge';
 *
 * <Badge>Default</Badge>
 * <Badge variant="secondary">Secondary</Badge>
 * <Badge variant="destructive">Error</Badge>
 * ```
 */
const meta = {
  title: 'UI/Badge',
  component: Badge,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'secondary', 'destructive', 'outline'],
      description: 'Badge 스타일 variant',
    },
    children: {
      control: 'text',
      description: 'Badge 내용',
    },
  },
} satisfies Meta<typeof Badge>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Badge (Primary 색상)
 */
export const Default: Story = {
  args: {
    children: 'Badge',
    variant: 'default',
  },
};

/**
 * Secondary Badge
 */
export const Secondary: Story = {
  args: {
    children: 'Secondary',
    variant: 'secondary',
  },
};

/**
 * Destructive Badge (에러, 경고용)
 */
export const Destructive: Story = {
  args: {
    children: 'Error',
    variant: 'destructive',
  },
};

/**
 * Outline Badge (테두리만)
 */
export const Outline: Story = {
  args: {
    children: 'Outline',
    variant: 'outline',
  },
};

/**
 * 상태 표시 예시
 */
export const StatusIndicators: Story = {
  render: () => (
    <div className="flex gap-2 flex-wrap">
      <Badge variant="default">Active</Badge>
      <Badge variant="secondary">Pending</Badge>
      <Badge variant="destructive">Rejected</Badge>
      <Badge variant="outline">Draft</Badge>
    </div>
  ),
};

/**
 * 숫자 카운트 Badge
 */
export const WithNumbers: Story = {
  render: () => (
    <div className="flex gap-2 items-center">
      <span className="text-sm">알림</span>
      <Badge variant="destructive">5</Badge>
    </div>
  ),
};

/**
 * 다양한 크기의 텍스트
 */
export const VariousContent: Story = {
  render: () => (
    <div className="flex gap-2 flex-wrap">
      <Badge>신규</Badge>
      <Badge variant="secondary">Beta</Badge>
      <Badge variant="destructive">99+</Badge>
      <Badge variant="outline">인기</Badge>
      <Badge>Long Badge Text Example</Badge>
    </div>
  ),
};

/**
 * 태그 목록 예시
 */
export const TagList: Story = {
  render: () => (
    <div className="flex gap-2 flex-wrap max-w-md">
      <Badge variant="outline">React</Badge>
      <Badge variant="outline">TypeScript</Badge>
      <Badge variant="outline">Vite</Badge>
      <Badge variant="outline">Tailwind CSS</Badge>
      <Badge variant="outline">Storybook</Badge>
      <Badge variant="outline">Zustand</Badge>
    </div>
  ),
};
