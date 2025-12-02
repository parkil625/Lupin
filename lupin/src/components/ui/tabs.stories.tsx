import type { Meta, StoryObj } from '@storybook/react-vite';
import { Tabs, TabsList, TabsTrigger, TabsContent } from './tabs';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from './card';

/**
 * Tabs 컴포넌트는 여러 콘텐츠를 탭으로 전환하여 표시합니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
 *
 * <Tabs defaultValue="tab1">
 *   <TabsList>
 *     <TabsTrigger value="tab1">Tab 1</TabsTrigger>
 *     <TabsTrigger value="tab2">Tab 2</TabsTrigger>
 *   </TabsList>
 *   <TabsContent value="tab1">Content 1</TabsContent>
 *   <TabsContent value="tab2">Content 2</TabsContent>
 * </Tabs>
 * ```
 */
const meta = {
  title: 'UI/Tabs',
  component: Tabs,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <div style={{ width: '600px' }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Tabs>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Tabs
 */
export const Default: Story = {
  render: () => (
    <Tabs defaultValue="tab1" className="w-full">
      <TabsList>
        <TabsTrigger value="tab1">Tab 1</TabsTrigger>
        <TabsTrigger value="tab2">Tab 2</TabsTrigger>
        <TabsTrigger value="tab3">Tab 3</TabsTrigger>
      </TabsList>
      <TabsContent value="tab1">
        <p className="text-sm">첫 번째 탭의 내용입니다.</p>
      </TabsContent>
      <TabsContent value="tab2">
        <p className="text-sm">두 번째 탭의 내용입니다.</p>
      </TabsContent>
      <TabsContent value="tab3">
        <p className="text-sm">세 번째 탭의 내용입니다.</p>
      </TabsContent>
    </Tabs>
  ),
};

/**
 * 프로필 정보 Tabs
 */
export const ProfileTabs: Story = {
  render: () => (
    <Tabs defaultValue="profile" className="w-full">
      <TabsList>
        <TabsTrigger value="profile">프로필</TabsTrigger>
        <TabsTrigger value="activity">활동</TabsTrigger>
        <TabsTrigger value="settings">설정</TabsTrigger>
      </TabsList>

      <TabsContent value="profile" className="space-y-4">
        <Card>
          <CardHeader>
            <CardTitle>프로필 정보</CardTitle>
            <CardDescription>내 프로필 정보를 확인하세요</CardDescription>
          </CardHeader>
          <CardContent className="space-y-2">
            <div>
              <p className="text-sm font-medium">이름</p>
              <p className="text-sm text-muted-foreground">김직원</p>
            </div>
            <div>
              <p className="text-sm font-medium">이메일</p>
              <p className="text-sm text-muted-foreground">user01@company.com</p>
            </div>
            <div>
              <p className="text-sm font-medium">부서</p>
              <p className="text-sm text-muted-foreground">개발팀</p>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="activity">
        <Card>
          <CardHeader>
            <CardTitle>최근 활동</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 text-sm">
              <p>러닝 기록 - 5km (30분)</p>
              <p>수영 기록 - 1km (45분)</p>
              <p>피드 작성 - "오늘 운동 완료!"</p>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="settings">
        <Card>
          <CardHeader>
            <CardTitle>설정</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>알림 설정</p>
            <p>프라이버시 설정</p>
            <p>계정 관리</p>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  ),
};

/**
 * 의료 기록 Tabs
 */
export const MedicalRecordTabs: Story = {
  render: () => (
    <Tabs defaultValue="appointments" className="w-full">
      <TabsList>
        <TabsTrigger value="appointments">예약</TabsTrigger>
        <TabsTrigger value="prescriptions">처방전</TabsTrigger>
        <TabsTrigger value="history">진료 기록</TabsTrigger>
      </TabsList>

      <TabsContent value="appointments">
        <Card>
          <CardHeader>
            <CardTitle>예약 목록</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>박의사 - 일반 진료</span>
              <span className="text-muted-foreground">2025-01-30 14:00</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>이의사 - 건강검진</span>
              <span className="text-muted-foreground">2025-02-05 10:00</span>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="prescriptions">
        <Card>
          <CardHeader>
            <CardTitle>처방전</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <div>
              <p className="font-medium">혈압약</p>
              <p className="text-muted-foreground">1일 2회, 식후 복용</p>
            </div>
            <div>
              <p className="font-medium">소화제</p>
              <p className="text-muted-foreground">1일 3회, 식후 복용</p>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="history">
        <Card>
          <CardHeader>
            <CardTitle>진료 기록</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <div>
              <p className="font-medium">2025-01-15</p>
              <p className="text-muted-foreground">감기 - 박의사</p>
            </div>
            <div>
              <p className="font-medium">2024-12-20</p>
              <p className="text-muted-foreground">정기 검진 - 이의사</p>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  ),
};

/**
 * 운동 통계 Tabs
 */
export const WorkoutStatsTabs: Story = {
  render: () => (
    <Tabs defaultValue="weekly" className="w-full">
      <TabsList>
        <TabsTrigger value="weekly">주간</TabsTrigger>
        <TabsTrigger value="monthly">월간</TabsTrigger>
        <TabsTrigger value="yearly">연간</TabsTrigger>
      </TabsList>

      <TabsContent value="weekly">
        <Card>
          <CardHeader>
            <CardTitle>이번 주 운동</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>총 운동 시간</span>
              <span className="font-medium">7시간 30분</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>칼로리 소모</span>
              <span className="font-medium">2,450 kcal</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>운동 일수</span>
              <span className="font-medium">5일</span>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="monthly">
        <Card>
          <CardHeader>
            <CardTitle>이번 달 운동</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>총 운동 시간</span>
              <span className="font-medium">32시간 15분</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>칼로리 소모</span>
              <span className="font-medium">10,800 kcal</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>운동 일수</span>
              <span className="font-medium">22일</span>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="yearly">
        <Card>
          <CardHeader>
            <CardTitle>올해 운동</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>총 운동 시간</span>
              <span className="font-medium">180시간</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>칼로리 소모</span>
              <span className="font-medium">65,000 kcal</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>운동 일수</span>
              <span className="font-medium">245일</span>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  ),
};

/**
 * 많은 탭 예시
 */
export const ManyTabs: Story = {
  render: () => (
    <Tabs defaultValue="tab1" className="w-full">
      <TabsList>
        <TabsTrigger value="tab1">탭 1</TabsTrigger>
        <TabsTrigger value="tab2">탭 2</TabsTrigger>
        <TabsTrigger value="tab3">탭 3</TabsTrigger>
        <TabsTrigger value="tab4">탭 4</TabsTrigger>
        <TabsTrigger value="tab5">탭 5</TabsTrigger>
      </TabsList>
      <TabsContent value="tab1">
        <p className="text-sm">탭 1 내용</p>
      </TabsContent>
      <TabsContent value="tab2">
        <p className="text-sm">탭 2 내용</p>
      </TabsContent>
      <TabsContent value="tab3">
        <p className="text-sm">탭 3 내용</p>
      </TabsContent>
      <TabsContent value="tab4">
        <p className="text-sm">탭 4 내용</p>
      </TabsContent>
      <TabsContent value="tab5">
        <p className="text-sm">탭 5 내용</p>
      </TabsContent>
    </Tabs>
  ),
};
