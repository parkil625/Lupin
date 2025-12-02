import type { Meta, StoryObj } from '@storybook/react-vite';
import { Switch } from './switch';
import { Label } from './label';

/**
 * Switch 컴포넌트는 On/Off 상태를 토글하는 스위치입니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Switch } from '@/components/ui/switch';
 *
 * <Switch id="notifications" />
 * <Label htmlFor="notifications">알림 수신</Label>
 * ```
 */
const meta = {
  title: 'UI/Switch',
  component: Switch,
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
} satisfies Meta<typeof Switch>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Switch
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
      <Switch id="airplane-mode" />
      <Label htmlFor="airplane-mode" className="cursor-pointer">
        비행기 모드
      </Label>
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
          <Switch id="email-notif" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="push-notif" className="cursor-pointer">
            푸시 알림
          </Label>
          <Switch id="push-notif" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="sms-notif" className="cursor-pointer">
            SMS 알림
          </Label>
          <Switch id="sms-notif" />
        </div>
      </div>

      <div className="border-t pt-4 space-y-3">
        <h3 className="font-semibold">프라이버시</h3>
        <div className="flex items-center justify-between">
          <Label htmlFor="public-profile" className="cursor-pointer">
            공개 프로필
          </Label>
          <Switch id="public-profile" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="show-activity" className="cursor-pointer">
            활동 공개
          </Label>
          <Switch id="show-activity" />
        </div>
      </div>
    </div>
  ),
};

/**
 * 의료 설정 예시
 */
export const MedicalSettings: Story = {
  render: () => (
    <div className="w-96 p-4 border rounded-lg space-y-4">
      <h3 className="font-semibold">복약 알림</h3>
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <Label htmlFor="morning-med" className="cursor-pointer font-medium">
              아침 약 알림
            </Label>
            <p className="text-xs text-muted-foreground">매일 오전 8시</p>
          </div>
          <Switch id="morning-med" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <Label htmlFor="evening-med" className="cursor-pointer font-medium">
              저녁 약 알림
            </Label>
            <p className="text-xs text-muted-foreground">매일 오후 8시</p>
          </div>
          <Switch id="evening-med" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <Label htmlFor="appointment-reminder" className="cursor-pointer font-medium">
              예약 알림
            </Label>
            <p className="text-xs text-muted-foreground">예약 1시간 전</p>
          </div>
          <Switch id="appointment-reminder" defaultChecked />
        </div>
      </div>
    </div>
  ),
};

/**
 * 여러 Switch 상태
 */
export const MultipleStates: Story = {
  render: () => (
    <div className="space-y-4">
      <div className="flex items-center space-x-2">
        <Switch id="state1" defaultChecked />
        <Label htmlFor="state1">활성화됨</Label>
      </div>
      <div className="flex items-center space-x-2">
        <Switch id="state2" />
        <Label htmlFor="state2">비활성화됨</Label>
      </div>
      <div className="flex items-center space-x-2">
        <Switch id="state3" disabled />
        <Label htmlFor="state3">비활성화 (꺼짐)</Label>
      </div>
      <div className="flex items-center space-x-2">
        <Switch id="state4" disabled defaultChecked />
        <Label htmlFor="state4">비활성화 (켜짐)</Label>
      </div>
    </div>
  ),
};

/**
 * 피드 설정 예시
 */
export const FeedSettings: Story = {
  render: () => (
    <div className="w-80 p-4 border rounded-lg space-y-4">
      <h3 className="font-semibold">피드 설정</h3>
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Label htmlFor="show-likes" className="cursor-pointer">
            좋아요 수 표시
          </Label>
          <Switch id="show-likes" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="show-comments" className="cursor-pointer">
            댓글 허용
          </Label>
          <Switch id="show-comments" defaultChecked />
        </div>
        <div className="flex items-center justify-between">
          <Label htmlFor="auto-play" className="cursor-pointer">
            동영상 자동재생
          </Label>
          <Switch id="auto-play" />
        </div>
      </div>
    </div>
  ),
};
