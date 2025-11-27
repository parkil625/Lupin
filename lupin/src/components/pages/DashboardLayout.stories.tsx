import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import Sidebar from '../dashboard/shared/Sidebar';
import AnimatedBackground from '../dashboard/shared/AnimatedBackground';
import { Home, Video, Trophy, Gavel, Calendar, MessageCircle } from 'lucide-react';

/**
 * 대시보드 레이아웃 - 메인 화면 구조
 */
const meta = {
  title: 'Pages/DashboardLayout',
  parameters: {
    layout: 'fullscreen',
    backgrounds: { default: 'light' },
  },
} satisfies Meta;

export default meta;
type Story = StoryObj;

const memberNavItems = [
  { id: 'home', icon: Home, label: '홈' },
  { id: 'feed', icon: Video, label: '피드' },
  { id: 'ranking', icon: Trophy, label: '랭킹' },
  { id: 'auction', icon: Gavel, label: '경매' },
  { id: 'medical', icon: Calendar, label: '진료' },
];

const doctorNavItems = [
  { id: 'chat', icon: MessageCircle, label: '채팅' },
];

/**
 * 회원 대시보드 레이아웃
 */
export const MemberDashboard: Story = {
  render: () => {
    const [expanded, setExpanded] = useState(false);
    const [selectedNav, setSelectedNav] = useState('home');

    return (
      <div className="h-screen w-screen overflow-hidden relative">
        <AnimatedBackground variant="member" />
        <Sidebar
          expanded={expanded}
          onExpandChange={setExpanded}
          navItems={memberNavItems}
          selectedNav={selectedNav}
          onNavSelect={setSelectedNav}
          userType="member"
        />
        <div className={`h-full transition-all duration-300 ${expanded ? 'ml-64' : 'ml-20'}`}>
          <div className="h-full flex items-center justify-center">
            <div className="text-center space-y-4">
              <h1 className="text-4xl font-bold text-gray-800">
                {memberNavItems.find(n => n.id === selectedNav)?.label} 페이지
              </h1>
              <p className="text-gray-600">
                왼쪽 사이드바에서 메뉴를 선택하세요
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  },
};

/**
 * 의사 대시보드 레이아웃
 */
export const DoctorDashboard: Story = {
  render: () => {
    const [expanded, setExpanded] = useState(false);
    const [selectedNav, setSelectedNav] = useState('chat');

    return (
      <div className="h-screen w-screen overflow-hidden relative">
        <AnimatedBackground variant="doctor" />
        <Sidebar
          expanded={expanded}
          onExpandChange={setExpanded}
          navItems={doctorNavItems}
          selectedNav={selectedNav}
          onNavSelect={setSelectedNav}
          userType="doctor"
        />
        <div className={`h-full transition-all duration-300 ${expanded ? 'ml-64' : 'ml-20'}`}>
          <div className="h-full flex items-center justify-center">
            <div className="text-center space-y-4">
              <h1 className="text-4xl font-bold text-gray-800">
                의사 채팅 페이지
              </h1>
              <p className="text-gray-600">
                환자와의 비대면 진료 채팅
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  },
};

/**
 * 사이드바 확장 상태
 */
export const ExpandedSidebar: Story = {
  render: () => {
    const [selectedNav, setSelectedNav] = useState('home');

    return (
      <div className="h-screen w-screen overflow-hidden relative">
        <AnimatedBackground variant="member" />
        <Sidebar
          expanded={true}
          onExpandChange={() => {}}
          navItems={memberNavItems}
          selectedNav={selectedNav}
          onNavSelect={setSelectedNav}
          userType="member"
        />
        <div className="h-full ml-64">
          <div className="h-full flex items-center justify-center">
            <div className="text-center space-y-4">
              <h1 className="text-4xl font-bold text-gray-800">
                사이드바 확장 상태
              </h1>
            </div>
          </div>
        </div>
      </div>
    );
  },
};
