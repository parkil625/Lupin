import type { Meta, StoryObj } from '@storybook/react';
import { Button } from './button';
import { Mail, Loader2 } from 'lucide-react';

/**
 * Button 컴포넌트는 사용자 액션을 트리거하는 기본 UI 요소입니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Button } from '@/components/ui/button';
 *
 * <Button>Click me</Button>
 * <Button variant="destructive">Delete</Button>
 * <Button size="lg">Large Button</Button>
 * ```
 */
const meta = {
  title: 'UI/Button',
  component: Button,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'destructive', 'outline', 'secondary', 'ghost', 'link'],
      description: '버튼의 스타일 변형',
    },
    size: {
      control: 'select',
      options: ['default', 'sm', 'lg', 'icon'],
      description: '버튼의 크기',
    },
    disabled: {
      control: 'boolean',
      description: '버튼 비활성화 여부',
    },
    asChild: {
      control: 'boolean',
      description: 'Radix Slot을 사용하여 자식 요소로 렌더링',
    },
  },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Primary 버튼
 */
export const Default: Story = {
  args: {
    children: 'Button',
    variant: 'default',
    size: 'default',
  },
};

/**
 * 위험한 액션(삭제, 취소 등)에 사용되는 Destructive 버튼
 */
export const Destructive: Story = {
  args: {
    children: 'Delete',
    variant: 'destructive',
  },
};

/**
 * 외곽선 스타일 버튼
 */
export const Outline: Story = {
  args: {
    children: 'Outline',
    variant: 'outline',
  },
};

/**
 * Secondary 스타일 버튼
 */
export const Secondary: Story = {
  args: {
    children: 'Secondary',
    variant: 'secondary',
  },
};

/**
 * Ghost 스타일 버튼 (배경 없음)
 */
export const Ghost: Story = {
  args: {
    children: 'Ghost',
    variant: 'ghost',
  },
};

/**
 * 링크 스타일 버튼
 */
export const Link: Story = {
  args: {
    children: 'Link',
    variant: 'link',
  },
};

/**
 * 아이콘이 포함된 버튼
 */
export const WithIcon: Story = {
  args: {
    children: (
      <>
        <Mail />
        Login with Email
      </>
    ),
  },
};

/**
 * 로딩 상태의 버튼
 */
export const Loading: Story = {
  args: {
    disabled: true,
    children: (
      <>
        <Loader2 className="animate-spin" />
        Please wait
      </>
    ),
  },
};

/**
 * 아이콘만 있는 버튼
 */
export const IconOnly: Story = {
  args: {
    size: 'icon',
    variant: 'outline',
    children: <Mail />,
  },
};

/**
 * Small 크기 버튼
 */
export const Small: Story = {
  args: {
    size: 'sm',
    children: 'Small Button',
  },
};

/**
 * Large 크기 버튼
 */
export const Large: Story = {
  args: {
    size: 'lg',
    children: 'Large Button',
  },
};

/**
 * 비활성화된 버튼
 */
export const Disabled: Story = {
  args: {
    disabled: true,
    children: 'Disabled',
  },
};

/**
 * 모든 variant를 보여주는 Showcase
 */
export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <div className="flex gap-2">
        <Button variant="default">Default</Button>
        <Button variant="destructive">Destructive</Button>
        <Button variant="outline">Outline</Button>
      </div>
      <div className="flex gap-2">
        <Button variant="secondary">Secondary</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="link">Link</Button>
      </div>
    </div>
  ),
};

/**
 * 모든 size를 보여주는 Showcase
 */
export const AllSizes: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <Button size="sm">Small</Button>
      <Button size="default">Default</Button>
      <Button size="lg">Large</Button>
      <Button size="icon">
        <Mail />
      </Button>
    </div>
  ),
};
