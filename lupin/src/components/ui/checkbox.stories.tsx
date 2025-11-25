import type { Meta, StoryObj } from '@storybook/react';
import { Checkbox } from './checkbox';
import { Label } from './label';

/**
 * Checkbox 컴포넌트는 사용자가 옵션을 선택하거나 해제할 수 있는 폼 요소입니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Checkbox } from '@/components/ui/checkbox';
 *
 * <Checkbox id="terms" />
 * <Label htmlFor="terms">약관에 동의합니다</Label>
 * ```
 */
const meta = {
  title: 'UI/Checkbox',
  component: Checkbox,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    disabled: {
      control: 'boolean',
      description: '비활성화 여부',
    },
  },
} satisfies Meta<typeof Checkbox>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Checkbox
 */
export const Default: Story = {
  args: {
    id: 'default',
  },
};

/**
 * Checked 상태
 */
export const Checked: Story = {
  args: {
    id: 'checked',
    defaultChecked: true,
  },
};

/**
 * Disabled 상태
 */
export const Disabled: Story = {
  args: {
    id: 'disabled',
    disabled: true,
  },
};

/**
 * Disabled + Checked
 */
export const DisabledChecked: Story = {
  args: {
    id: 'disabled-checked',
    disabled: true,
    defaultChecked: true,
  },
};

/**
 * Label과 함께 사용
 */
export const WithLabel: Story = {
  render: () => (
    <div className="flex items-center space-x-2">
      <Checkbox id="terms" />
      <Label htmlFor="terms" className="cursor-pointer">
        이용약관에 동의합니다
      </Label>
    </div>
  ),
};

/**
 * 폼 예시 (여러 Checkbox)
 */
export const FormExample: Story = {
  render: () => (
    <div className="space-y-3">
      <div className="flex items-center space-x-2">
        <Checkbox id="notifications" defaultChecked />
        <Label htmlFor="notifications" className="cursor-pointer">
          알림 수신 동의
        </Label>
      </div>
      <div className="flex items-center space-x-2">
        <Checkbox id="marketing" />
        <Label htmlFor="marketing" className="cursor-pointer">
          마케팅 정보 수신 동의
        </Label>
      </div>
      <div className="flex items-center space-x-2">
        <Checkbox id="privacy" defaultChecked />
        <Label htmlFor="privacy" className="cursor-pointer">
          개인정보 처리방침 동의 (필수)
        </Label>
      </div>
    </div>
  ),
};

/**
 * 운동 타입 선택 예시
 */
export const WorkoutSelection: Story = {
  render: () => (
    <div className="space-y-3">
      <p className="font-medium text-sm mb-4">관심 있는 운동을 선택하세요</p>
      <div className="flex items-center space-x-2">
        <Checkbox id="running" defaultChecked />
        <Label htmlFor="running" className="cursor-pointer">
          러닝
        </Label>
      </div>
      <div className="flex items-center space-x-2">
        <Checkbox id="swimming" defaultChecked />
        <Label htmlFor="swimming" className="cursor-pointer">
          수영
        </Label>
      </div>
      <div className="flex items-center space-x-2">
        <Checkbox id="cycling" />
        <Label htmlFor="cycling" className="cursor-pointer">
          사이클
        </Label>
      </div>
      <div className="flex items-center space-x-2">
        <Checkbox id="yoga" />
        <Label htmlFor="yoga" className="cursor-pointer">
          요가
        </Label>
      </div>
      <div className="flex items-center space-x-2">
        <Checkbox id="gym" defaultChecked />
        <Label htmlFor="gym" className="cursor-pointer">
          헬스
        </Label>
      </div>
    </div>
  ),
};

/**
 * 설정 패널 예시
 */
export const SettingsPanel: Story = {
  render: () => (
    <div className="w-80 p-4 border rounded-lg space-y-4">
      <h3 className="font-semibold">알림 설정</h3>
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Label htmlFor="email-notif" className="cursor-pointer">
            이메일 알림
          </Label>
          <Checkbox id="email-notif" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="push-notif" className="cursor-pointer">
            푸시 알림
          </Label>
          <Checkbox id="push-notif" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="sms-notif" className="cursor-pointer">
            SMS 알림
          </Label>
          <Checkbox id="sms-notif" />
        </div>
      </div>
    </div>
  ),
};
