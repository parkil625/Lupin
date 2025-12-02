import type { Meta, StoryObj } from '@storybook/react-vite';
import { Alert, AlertTitle, AlertDescription } from './alert';
import { AlertCircle, CheckCircle, Info, AlertTriangle } from 'lucide-react';

/**
 * Alert 컴포넌트는 사용자에게 중요한 정보를 전달하는 알림 메시지입니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
 *
 * <Alert>
 *   <AlertCircle />
 *   <AlertTitle>알림</AlertTitle>
 *   <AlertDescription>메시지 내용</AlertDescription>
 * </Alert>
 * ```
 */
const meta = {
  title: 'UI/Alert',
  component: Alert,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'destructive'],
      description: 'Alert 스타일 variant',
    },
  },
  decorators: [
    (Story) => (
      <div style={{ width: '500px' }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Alert>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Alert
 */
export const Default: Story = {
  render: () => (
    <Alert>
      <Info />
      <AlertTitle>안내</AlertTitle>
      <AlertDescription>
        일반 정보 메시지입니다.
      </AlertDescription>
    </Alert>
  ),
};

/**
 * Destructive Alert (에러/경고)
 */
export const Destructive: Story = {
  render: () => (
    <Alert variant="destructive">
      <AlertCircle />
      <AlertTitle>오류</AlertTitle>
      <AlertDescription>
        문제가 발생했습니다. 다시 시도해주세요.
      </AlertDescription>
    </Alert>
  ),
};

/**
 * 성공 메시지
 */
export const Success: Story = {
  render: () => (
    <Alert>
      <CheckCircle />
      <AlertTitle>성공</AlertTitle>
      <AlertDescription>
        작업이 성공적으로 완료되었습니다!
      </AlertDescription>
    </Alert>
  ),
};

/**
 * 경고 메시지
 */
export const Warning: Story = {
  render: () => (
    <Alert>
      <AlertTriangle />
      <AlertTitle>주의</AlertTitle>
      <AlertDescription>
        이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?
      </AlertDescription>
    </Alert>
  ),
};

/**
 * 아이콘 없는 Alert
 */
export const WithoutIcon: Story = {
  render: () => (
    <Alert>
      <AlertTitle>알림</AlertTitle>
      <AlertDescription>
        아이콘이 없는 간단한 메시지입니다.
      </AlertDescription>
    </Alert>
  ),
};

/**
 * 긴 텍스트 Alert
 */
export const LongText: Story = {
  render: () => (
    <Alert>
      <Info />
      <AlertTitle>업데이트 안내</AlertTitle>
      <AlertDescription>
        시스템 업데이트가 예정되어 있습니다. 2025년 1월 30일 오전 2시부터 4시까지
        서비스 점검이 진행될 예정입니다. 점검 시간 동안 일부 기능이 제한될 수 있으니
        양해 부탁드립니다. 불편을 드려 죄송합니다.
      </AlertDescription>
    </Alert>
  ),
};

/**
 * 여러 Alert 예시
 */
export const MultipleAlerts: Story = {
  render: () => (
    <div className="space-y-4">
      <Alert>
        <Info />
        <AlertTitle>정보</AlertTitle>
        <AlertDescription>
          새로운 기능이 추가되었습니다.
        </AlertDescription>
      </Alert>

      <Alert variant="destructive">
        <AlertCircle />
        <AlertTitle>에러</AlertTitle>
        <AlertDescription>
          로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.
        </AlertDescription>
      </Alert>

      <Alert>
        <CheckCircle />
        <AlertTitle>완료</AlertTitle>
        <AlertDescription>
          프로필이 성공적으로 업데이트되었습니다.
        </AlertDescription>
      </Alert>
    </div>
  ),
};

/**
 * 의료 시스템 예시
 */
export const MedicalExample: Story = {
  render: () => (
    <div className="space-y-4">
      <Alert>
        <CheckCircle />
        <AlertTitle>예약 완료</AlertTitle>
        <AlertDescription>
          박의사 선생님과의 진료 예약이 완료되었습니다. (2025-01-30 14:00)
        </AlertDescription>
      </Alert>

      <Alert variant="destructive">
        <AlertTriangle />
        <AlertTitle>복용 알림</AlertTitle>
        <AlertDescription>
          혈압약 복용 시간입니다. 지금 복용해주세요.
        </AlertDescription>
      </Alert>
    </div>
  ),
};
