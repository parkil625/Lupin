import type { Meta, StoryObj } from '@storybook/react-vite';
import { Input } from './input';
import { Label } from './label';
import { Search, Mail, Lock } from 'lucide-react';

/**
 * Input 컴포넌트는 사용자로부터 텍스트 입력을 받는 기본 폼 요소입니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Input } from '@/components/ui/input';
 *
 * <Input type="text" placeholder="Enter text..." />
 * <Input type="email" placeholder="Email" />
 * <Input type="password" placeholder="Password" />
 * ```
 */
const meta = {
  title: 'UI/Input',
  component: Input,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    type: {
      control: 'select',
      options: ['text', 'email', 'password', 'number', 'tel', 'url', 'search', 'date', 'time'],
      description: 'Input 타입',
    },
    placeholder: {
      control: 'text',
      description: 'Placeholder 텍스트',
    },
    disabled: {
      control: 'boolean',
      description: '비활성화 여부',
    },
  },
  decorators: [
    (Story) => (
      <div style={{ width: '300px' }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Text Input
 */
export const Default: Story = {
  args: {
    type: 'text',
    placeholder: 'Enter text...',
  },
};

/**
 * Email Input
 */
export const Email: Story = {
  args: {
    type: 'email',
    placeholder: 'email@example.com',
  },
};

/**
 * Password Input
 */
export const Password: Story = {
  args: {
    type: 'password',
    placeholder: 'Enter password',
  },
};

/**
 * Search Input
 */
export const SearchInput: Story = {
  args: {
    type: 'search',
    placeholder: 'Search...',
  },
};

/**
 * Number Input
 */
export const Number: Story = {
  args: {
    type: 'number',
    placeholder: '0',
  },
};

/**
 * Date Input
 */
export const Date: Story = {
  args: {
    type: 'date',
  },
};

/**
 * Disabled Input
 */
export const Disabled: Story = {
  args: {
    type: 'text',
    placeholder: 'Disabled input',
    disabled: true,
  },
};

/**
 * Label과 함께 사용하는 Input
 */
export const WithLabel: Story = {
  render: () => (
    <div className="space-y-2">
      <Label htmlFor="username">사용자 이름</Label>
      <Input id="username" type="text" placeholder="user01" />
    </div>
  ),
};

/**
 * 로그인 폼 예제
 */
export const LoginForm: Story = {
  render: () => (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="email">이메일</Label>
        <Input id="email" type="email" placeholder="user01@company.com" />
      </div>
      <div className="space-y-2">
        <Label htmlFor="password">비밀번호</Label>
        <Input id="password" type="password" placeholder="••••••••" />
      </div>
    </div>
  ),
};

/**
 * 아이콘과 함께 사용하는 Input (직접 구현)
 */
export const WithIcon: Story = {
  render: () => (
    <div className="space-y-4">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input type="search" placeholder="Search..." className="pl-9" />
      </div>

      <div className="relative">
        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input type="email" placeholder="Email" className="pl-9" />
      </div>

      <div className="relative">
        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input type="password" placeholder="Password" className="pl-9" />
      </div>
    </div>
  ),
};

/**
 * 에러 상태의 Input (aria-invalid)
 */
export const WithError: Story = {
  render: () => (
    <div className="space-y-2">
      <Label htmlFor="invalid-email">이메일</Label>
      <Input
        id="invalid-email"
        type="email"
        placeholder="email@example.com"
        aria-invalid="true"
        defaultValue="invalid-email"
      />
      <p className="text-sm text-destructive">유효하지 않은 이메일 주소입니다.</p>
    </div>
  ),
};

/**
 * 프로필 정보 폼 예제
 */
export const ProfileForm: Story = {
  decorators: [
    (Story) => (
      <div style={{ width: '400px' }}>
        <Story />
      </div>
    ),
  ],
  render: () => (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="name">이름</Label>
        <Input id="name" type="text" placeholder="김직원" defaultValue="김직원" />
      </div>
      <div className="space-y-2">
        <Label htmlFor="email-profile">이메일</Label>
        <Input id="email-profile" type="email" defaultValue="user01@company.com" />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="height">키 (cm)</Label>
          <Input id="height" type="number" placeholder="175" />
        </div>
        <div className="space-y-2">
          <Label htmlFor="weight">몸무게 (kg)</Label>
          <Input id="weight" type="number" placeholder="70" />
        </div>
      </div>
      <div className="space-y-2">
        <Label htmlFor="department">부서</Label>
        <Input id="department" type="text" placeholder="개발팀" defaultValue="개발팀" />
      </div>
      <div className="space-y-2">
        <Label htmlFor="phone">연락처</Label>
        <Input id="phone" type="tel" placeholder="010-1234-5678" />
      </div>
    </div>
  ),
};

/**
 * 파일 업로드 Input
 */
export const File: Story = {
  args: {
    type: 'file',
  },
};

/**
 * 다양한 Input 타입 Showcase
 */
export const AllTypes: Story = {
  decorators: [
    (Story) => (
      <div style={{ width: '400px' }}>
        <Story />
      </div>
    ),
  ],
  render: () => (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label>Text</Label>
        <Input type="text" placeholder="Text input" />
      </div>
      <div className="space-y-2">
        <Label>Email</Label>
        <Input type="email" placeholder="Email input" />
      </div>
      <div className="space-y-2">
        <Label>Password</Label>
        <Input type="password" placeholder="Password input" />
      </div>
      <div className="space-y-2">
        <Label>Number</Label>
        <Input type="number" placeholder="Number input" />
      </div>
      <div className="space-y-2">
        <Label>Tel</Label>
        <Input type="tel" placeholder="Tel input" />
      </div>
      <div className="space-y-2">
        <Label>URL</Label>
        <Input type="url" placeholder="URL input" />
      </div>
      <div className="space-y-2">
        <Label>Search</Label>
        <Input type="search" placeholder="Search input" />
      </div>
      <div className="space-y-2">
        <Label>Date</Label>
        <Input type="date" />
      </div>
      <div className="space-y-2">
        <Label>Time</Label>
        <Input type="time" />
      </div>
    </div>
  ),
};
