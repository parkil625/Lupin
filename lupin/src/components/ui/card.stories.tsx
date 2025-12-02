import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
  CardAction,
} from './card';
import { Button } from './button';
import { Bell, Settings } from 'lucide-react';

/**
 * Card 컴포넌트는 관련된 정보를 그룹화하여 표시하는 컨테이너입니다.
 *
 * ## 사용 방법
 * ```tsx
 * import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
 *
 * <Card>
 *   <CardHeader>
 *     <CardTitle>Title</CardTitle>
 *     <CardDescription>Description</CardDescription>
 *   </CardHeader>
 *   <CardContent>Content</CardContent>
 *   <CardFooter>Footer</CardFooter>
 * </Card>
 * ```
 */
const meta = {
  title: 'UI/Card',
  component: Card,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <div style={{ width: '400px' }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Card>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 Card
 */
export const Default: Story = {
  render: () => (
    <Card>
      <CardHeader>
        <CardTitle>Card Title</CardTitle>
        <CardDescription>Card Description</CardDescription>
      </CardHeader>
      <CardContent>
        <p>Card content goes here.</p>
      </CardContent>
      <CardFooter>
        <Button>Action</Button>
      </CardFooter>
    </Card>
  ),
};

/**
 * Header만 있는 Simple Card
 */
export const SimpleCard: Story = {
  render: () => (
    <Card>
      <CardHeader>
        <CardTitle>Notifications</CardTitle>
        <CardDescription>You have 3 unread messages.</CardDescription>
      </CardHeader>
    </Card>
  ),
};

/**
 * Action 버튼이 있는 Card
 */
export const WithAction: Story = {
  render: () => (
    <Card>
      <CardHeader>
        <CardTitle>알림 설정</CardTitle>
        <CardDescription>알림을 관리하세요</CardDescription>
        <CardAction>
          <Button size="icon" variant="ghost">
            <Settings />
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span>이메일 알림</span>
            <Button size="sm" variant="outline">
              켜기
            </Button>
          </div>
          <div className="flex items-center justify-between">
            <span>푸시 알림</span>
            <Button size="sm" variant="outline">
              끄기
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  ),
};

/**
 * 프로필 카드 예제
 */
export const ProfileCard: Story = {
  render: () => (
    <Card>
      <CardHeader>
        <CardTitle>김직원</CardTitle>
        <CardDescription>개발팀 • MEMBER</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          <div className="flex justify-between">
            <span className="text-muted-foreground">이메일</span>
            <span>user01@company.com</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">포인트</span>
            <span className="font-bold">100P</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">월간 좋아요</span>
            <span>10</span>
          </div>
        </div>
      </CardContent>
      <CardFooter className="gap-2">
        <Button variant="outline" className="flex-1">
          프로필 수정
        </Button>
        <Button className="flex-1">메시지</Button>
      </CardFooter>
    </Card>
  ),
};

/**
 * 통계 카드 예제
 */
export const StatsCard: Story = {
  render: () => (
    <Card>
      <CardHeader>
        <CardTitle>이번 달 활동</CardTitle>
        <CardDescription>2025년 11월</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div>
            <div className="flex justify-between mb-1">
              <span className="text-sm">운동 횟수</span>
              <span className="font-bold">12회</span>
            </div>
            <div className="w-full bg-secondary rounded-full h-2">
              <div className="bg-primary h-2 rounded-full" style={{ width: '60%' }} />
            </div>
          </div>
          <div>
            <div className="flex justify-between mb-1">
              <span className="text-sm">소모 칼로리</span>
              <span className="font-bold">3,240 kcal</span>
            </div>
            <div className="w-full bg-secondary rounded-full h-2">
              <div className="bg-green-500 h-2 rounded-full" style={{ width: '80%' }} />
            </div>
          </div>
          <div>
            <div className="flex justify-between mb-1">
              <span className="text-sm">획득 포인트</span>
              <span className="font-bold">100P</span>
            </div>
            <div className="w-full bg-secondary rounded-full h-2">
              <div className="bg-blue-500 h-2 rounded-full" style={{ width: '45%' }} />
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  ),
};

/**
 * 알림 카드 예제
 */
export const NotificationCard: Story = {
  render: () => (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Bell className="text-primary" />
          <CardTitle>새로운 알림</CardTitle>
        </div>
        <CardDescription>5분 전</CardDescription>
      </CardHeader>
      <CardContent>
        <p>
          <strong>박의사</strong>님이 회원님의 피드에 댓글을 남겼습니다: &quot;좋은 운동이네요!&quot;
        </p>
      </CardContent>
      <CardFooter>
        <Button variant="ghost" size="sm">
          무시
        </Button>
        <Button size="sm">확인</Button>
      </CardFooter>
    </Card>
  ),
};

/**
 * 여러 Card를 함께 보여주는 Grid Layout
 */
export const CardGrid: Story = {
  decorators: [
    (Story) => (
      <div style={{ width: '800px' }}>
        <Story />
      </div>
    ),
  ],
  render: () => (
    <div className="grid grid-cols-2 gap-4">
      <Card>
        <CardHeader>
          <CardTitle>피드</CardTitle>
          <CardDescription>25개</CardDescription>
        </CardHeader>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>좋아요</CardTitle>
          <CardDescription>120개</CardDescription>
        </CardHeader>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>댓글</CardTitle>
          <CardDescription>48개</CardDescription>
        </CardHeader>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>포인트</CardTitle>
          <CardDescription>100P</CardDescription>
        </CardHeader>
      </Card>
    </div>
  ),
};
