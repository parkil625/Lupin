import type { Meta, StoryObj } from '@storybook/react-vite';
import { Avatar, AvatarImage, AvatarFallback } from './avatar';
import { User } from 'lucide-react';

/**
 * Avatar 컴포넌트는 프로필 이미지를 표시합니다.
 * 이미지가 없을 경우 Fallback으로 이니셜이나 아이콘을 표시할 수 있습니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
 *
 * <Avatar>
 *   <AvatarImage src="/profile.jpg" alt="User" />
 *   <AvatarFallback>JD</AvatarFallback>
 * </Avatar>
 * ```
 */
const meta = {
  title: 'UI/Avatar',
  component: Avatar,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Avatar>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 이미지가 있는 Avatar
 */
export const WithImage: Story = {
  render: () => (
    <Avatar>
      <AvatarImage src="https://github.com/shadcn.png" alt="@shadcn" />
      <AvatarFallback>CN</AvatarFallback>
    </Avatar>
  ),
};

/**
 * Fallback (이미지 없을 때)
 */
export const WithFallback: Story = {
  render: () => (
    <Avatar>
      <AvatarImage src="/invalid-url.jpg" alt="User" />
      <AvatarFallback>김직원</AvatarFallback>
    </Avatar>
  ),
};

/**
 * 이니셜 Fallback
 */
export const WithInitials: Story = {
  render: () => (
    <div className="flex gap-4">
      <Avatar>
        <AvatarFallback>JD</AvatarFallback>
      </Avatar>
      <Avatar>
        <AvatarFallback>김직</AvatarFallback>
      </Avatar>
      <Avatar>
        <AvatarFallback>박의</AvatarFallback>
      </Avatar>
    </div>
  ),
};

/**
 * 아이콘 Fallback
 */
export const WithIconFallback: Story = {
  render: () => (
    <Avatar>
      <AvatarFallback>
        <User className="h-4 w-4" />
      </AvatarFallback>
    </Avatar>
  ),
};

/**
 * 다양한 크기
 */
export const DifferentSizes: Story = {
  render: () => (
    <div className="flex gap-4 items-end">
      <Avatar className="h-8 w-8">
        <AvatarImage src="https://github.com/shadcn.png" alt="Small" />
        <AvatarFallback className="text-xs">SM</AvatarFallback>
      </Avatar>
      <Avatar className="h-10 w-10">
        <AvatarImage src="https://github.com/shadcn.png" alt="Default" />
        <AvatarFallback>MD</AvatarFallback>
      </Avatar>
      <Avatar className="h-16 w-16">
        <AvatarImage src="https://github.com/shadcn.png" alt="Large" />
        <AvatarFallback className="text-lg">LG</AvatarFallback>
      </Avatar>
      <Avatar className="h-24 w-24">
        <AvatarImage src="https://github.com/shadcn.png" alt="Extra Large" />
        <AvatarFallback className="text-2xl">XL</AvatarFallback>
      </Avatar>
    </div>
  ),
};

/**
 * 사용자 목록 예시
 */
export const UserList: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      {['김직원', '박의사', '이간호', '최관리'].map((name, idx) => (
        <div key={idx} className="flex items-center gap-3">
          <Avatar>
            <AvatarImage
              src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${name}`}
              alt={name}
            />
            <AvatarFallback>{name.slice(0, 2)}</AvatarFallback>
          </Avatar>
          <div>
            <p className="text-sm font-medium">{name}</p>
            <p className="text-xs text-muted-foreground">{name}@company.com</p>
          </div>
        </div>
      ))}
    </div>
  ),
};

/**
 * 겹쳐진 Avatar 그룹
 */
export const AvatarGroup: Story = {
  render: () => (
    <div className="flex -space-x-2">
      <Avatar className="border-2 border-white">
        <AvatarImage src="https://github.com/shadcn.png" alt="User 1" />
        <AvatarFallback>U1</AvatarFallback>
      </Avatar>
      <Avatar className="border-2 border-white">
        <AvatarFallback>U2</AvatarFallback>
      </Avatar>
      <Avatar className="border-2 border-white">
        <AvatarFallback>U3</AvatarFallback>
      </Avatar>
      <Avatar className="border-2 border-white">
        <AvatarFallback className="text-xs">+5</AvatarFallback>
      </Avatar>
    </div>
  ),
};
