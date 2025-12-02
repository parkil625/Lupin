import type { Meta, StoryObj } from '@storybook/react-vite';
import { Progress } from './progress';
import { Label } from './label';

/**
 * Progress 컴포넌트는 작업의 진행 상태를 시각적으로 표시합니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Progress } from '@/components/ui/progress';
 *
 * <Progress value={50} />
 * ```
 */
const meta = {
  title: 'UI/Progress',
  component: Progress,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    value: {
      control: { type: 'range', min: 0, max: 100, step: 1 },
      description: '진행률 (0-100)',
    },
  },
  decorators: [
    (Story) => (
      <div style={{ width: '400px' }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Progress>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Progress (50%)
 */
export const Default: Story = {
  args: {
    value: 50,
  },
};

/**
 * 0% (시작)
 */
export const Empty: Story = {
  args: {
    value: 0,
  },
};

/**
 * 100% (완료)
 */
export const Full: Story = {
  args: {
    value: 100,
  },
};

/**
 * 25%
 */
export const Quarter: Story = {
  args: {
    value: 25,
  },
};

/**
 * 75%
 */
export const ThreeQuarters: Story = {
  args: {
    value: 75,
  },
};

/**
 * Label과 함께 사용
 */
export const WithLabel: Story = {
  render: () => (
    <div className="space-y-2">
      <div className="flex justify-between text-sm">
        <Label>업로드 진행률</Label>
        <span className="text-muted-foreground">65%</span>
      </div>
      <Progress value={65} />
    </div>
  ),
};

/**
 * 여러 단계의 Progress
 */
export const MultipleSteps: Story = {
  render: () => (
    <div className="space-y-6">
      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>프로필 완성도</Label>
          <span className="text-muted-foreground">30%</span>
        </div>
        <Progress value={30} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>이번 주 운동 목표</Label>
          <span className="text-muted-foreground">70%</span>
        </div>
        <Progress value={70} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>월간 활동 달성</Label>
          <span className="text-muted-foreground">100%</span>
        </div>
        <Progress value={100} />
      </div>
    </div>
  ),
};

/**
 * 파일 업로드 Progress
 */
export const FileUpload: Story = {
  render: () => (
    <div className="space-y-4">
      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <span>profile.jpg</span>
          <span className="text-muted-foreground">2.5 MB / 5.0 MB</span>
        </div>
        <Progress value={50} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <span>workout_video.mp4</span>
          <span className="text-muted-foreground">18.2 MB / 25.0 MB</span>
        </div>
        <Progress value={73} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <span>medical_record.pdf</span>
          <span className="text-green-600 font-medium">완료</span>
        </div>
        <Progress value={100} />
      </div>
    </div>
  ),
};

/**
 * 운동 목표 Progress
 */
export const WorkoutGoals: Story = {
  render: () => (
    <div className="p-6 border rounded-lg space-y-6">
      <h3 className="font-semibold">이번 주 운동 목표</h3>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>러닝 10km</Label>
          <span className="text-muted-foreground">8.5km / 10km</span>
        </div>
        <Progress value={85} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>수영 3회</Label>
          <span className="text-muted-foreground">2회 / 3회</span>
        </div>
        <Progress value={67} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>헬스 5회</Label>
          <span className="text-green-600 font-medium">달성!</span>
        </div>
        <Progress value={100} />
      </div>
    </div>
  ),
};

/**
 * 의료 검진 Progress
 */
export const MedicalCheckup: Story = {
  render: () => (
    <div className="p-6 border rounded-lg space-y-6">
      <h3 className="font-semibold">건강 검진 진행 상황</h3>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>혈액 검사</Label>
          <span className="text-green-600">완료</span>
        </div>
        <Progress value={100} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>X-ray 촬영</Label>
          <span className="text-green-600">완료</span>
        </div>
        <Progress value={100} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>심전도 검사</Label>
          <span className="text-blue-600">진행 중</span>
        </div>
        <Progress value={45} />
      </div>

      <div className="space-y-2">
        <div className="flex justify-between text-sm">
          <Label>초음파 검사</Label>
          <span className="text-muted-foreground">대기 중</span>
        </div>
        <Progress value={0} />
      </div>
    </div>
  ),
};

/**
 * 다양한 진행률
 */
export const VariousProgress: Story = {
  render: () => (
    <div className="space-y-4">
      {[0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100].map((value) => (
        <div key={value} className="space-y-1">
          <div className="text-xs text-muted-foreground">{value}%</div>
          <Progress value={value} />
        </div>
      ))}
    </div>
  ),
};
