import React from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import Sidebar from '../dashboard/shared/Sidebar';
import AnimatedBackground from '../dashboard/shared/AnimatedBackground';
import Home from '../dashboard/home/Home';
import Ranking from '../dashboard/ranking/Ranking';
import Auction from '../dashboard/auction/Auction';
import Medical from '../dashboard/medical/Medical';
import MemberProfilePage from '../dashboard/profile/MemberProfilePage';
import NotificationPopup from '../dashboard/shared/NotificationPopup';
import FeedDetailDialogHome from '../dashboard/dialogs/FeedDetailDialogHome';
import CreateFeedDialog from '../dashboard/dialogs/CreateFeedDialog';
import EditFeedDialog from '../dashboard/dialogs/EditFeedDialog';
import { SearchInput } from '../molecules';
import { Home as HomeIcon, Video, Trophy, Gavel, Calendar, Bell, Heart, MessageCircle, User, Sparkles } from 'lucide-react';
import { Feed, Notification } from '@/types/dashboard.types';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';

const meta = {
  title: 'Pages/Dashboard',
  parameters: {
    layout: 'fullscreen',
    backgrounds: { default: 'light' },
  },
} satisfies Meta;

export default meta;
type Story = StoryObj;

const memberNavItems = [
  { id: 'home', icon: HomeIcon, label: '홈' },
  { id: 'feed', icon: Video, label: '피드' },
  { id: 'ranking', icon: Trophy, label: '랭킹' },
  { id: 'auction', icon: Gavel, label: '경매' },
  { id: 'medical', icon: Calendar, label: '진료' },
];

// Mock 프로필 이미지 - 유효한 Unsplash 이미지 URL
const mockAvatars: Record<string, string> = {
  '김운동': 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=100&h=100&fit=crop',
  '이헬스': 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop',
  '박피트': 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop',
  '최건강': 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop',
  '정활력': 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop',
  '한체력': 'https://images.unsplash.com/photo-1552058544-f2b08422138a?w=100&h=100&fit=crop',
  '오근육': 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop',
  '강스포츠': 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=100&h=100&fit=crop',
  '신헬시': 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop',
  '박선일': 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=100&h=100&fit=crop',
};

// Mock 피드 데이터
const mockFeeds: Feed[] = [
  {
    id: 1,
    writerId: 1,
    writerName: '김운동',
    author: '김운동',
    activity: '러닝',
    points: 30,
    content: '오늘 아침 공원에서 러닝했어요! 날씨가 좋아서 기분이 상쾌합니다.',
    images: ['https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400&q=75'],
    likes: 15,
    comments: 3,
    time: '2시간 전',
    createdAt: new Date().toISOString(),
  },
  {
    id: 2,
    writerId: 2,
    writerName: '이헬스',
    author: '이헬스',
    activity: '웨이트',
    points: 30,
    content: '오늘 상체 운동 완료!',
    images: ['https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&q=75'],
    likes: 22,
    comments: 5,
    time: '4시간 전',
    createdAt: new Date().toISOString(),
  },
  {
    id: 3,
    writerId: 3,
    writerName: '박피트',
    author: '박피트',
    activity: '수영',
    points: 30,
    content: '수영장에서 1km 완주!',
    images: ['https://images.unsplash.com/photo-1530549387789-4c1017266635?w=400&q=75'],
    likes: 18,
    comments: 2,
    time: '어제',
    createdAt: new Date().toISOString(),
  },
];

// Mock 알림 데이터
const mockNotifications: Notification[] = [
  {
    id: 1,
    type: 'FEED_LIKE',
    title: '좋아요',
    content: '김운동님이 회원님의 피드를 좋아합니다.',
    isRead: false,
    createdAt: '10분 전',
  },
  {
    id: 2,
    type: 'COMMENT',
    title: '댓글',
    content: '이헬스님이 댓글을 남겼습니다: "멋져요!"',
    isRead: false,
    createdAt: '30분 전',
  },
];

// 공통 레이아웃 래퍼
const DashboardWrapper = ({ children, selectedNav }: { children: React.ReactNode; selectedNav: string }) => {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="h-screen w-screen overflow-hidden relative">
      <AnimatedBackground variant="member" />
      <Sidebar
        expanded={expanded}
        onExpandChange={setExpanded}
        navItems={memberNavItems}
        selectedNav={selectedNav}
        onNavSelect={() => {}}
        userType="member"
      />
      <div className={`h-full transition-all duration-300 ${expanded ? 'ml-64' : 'ml-20'}`}>
        {children}
      </div>
    </div>
  );
};

/**
 * 홈 페이지 - 프로필 및 내 피드 (기존 FeedDetailDialogHome, CreateFeedDialog, EditFeedDialog 사용)
 */
export const HomePage: Story = {
  render: () => {
    const [selectedFeed, setSelectedFeed] = useState<Feed | null>(null);
    const [feedImageIndex, setFeedImageIndex] = useState(0);
    const [showFeedDetail, setShowFeedDetail] = useState(false);
    const [showCreateDialog, setShowCreateDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [editingFeed, setEditingFeed] = useState<Feed | null>(null);

    const handlePrevImage = () => {
      if (selectedFeed && feedImageIndex > 0) {
        setFeedImageIndex(feedImageIndex - 1);
      }
    };

    const handleNextImage = () => {
      if (selectedFeed && selectedFeed.images && feedImageIndex < selectedFeed.images.length - 1) {
        setFeedImageIndex(feedImageIndex + 1);
      }
    };

    const handleEdit = (feed: Feed) => {
      setEditingFeed(feed);
      setShowFeedDetail(false);
      setShowEditDialog(true);
    };

    return (
      <DashboardWrapper selectedNav="home">
        <Home
          profileImage={mockAvatars['김운동']}
          myFeeds={mockFeeds.slice(0, 2)}
          setSelectedFeed={(feed) => {
            setSelectedFeed(feed);
            setFeedImageIndex(0);
            setShowFeedDetail(true);
          }}
          setFeedImageIndex={(_feedId, index) => setFeedImageIndex(index)}
          setShowFeedDetailInHome={setShowFeedDetail}
          onCreateClick={() => setShowCreateDialog(true)}
          refreshTrigger={0}
        />

        {/* 기존 피드 상세 다이얼로그 사용 */}
        <FeedDetailDialogHome
          feed={selectedFeed}
          open={showFeedDetail}
          onOpenChange={setShowFeedDetail}
          currentImageIndex={feedImageIndex}
          onPrevImage={handlePrevImage}
          onNextImage={handleNextImage}
          onEdit={handleEdit}
          onDelete={(feedId) => alert(`피드 삭제: ${feedId}`)}
        />

        {/* 기존 피드 만들기 다이얼로그 사용 */}
        <CreateFeedDialog
          open={showCreateDialog}
          onOpenChange={setShowCreateDialog}
          onCreate={(images, _content, workoutType, _startImage, _endImage) => {
            alert(`피드 생성!\n운동 종류: ${workoutType}\n이미지 수: ${images.length}`);
            setShowCreateDialog(false);
          }}
        />

        {/* 기존 피드 수정 다이얼로그 사용 */}
        <EditFeedDialog
          feed={editingFeed}
          open={showEditDialog}
          onOpenChange={setShowEditDialog}
          onSave={(feedId, _images, _content, workoutType, _startImage, _endImage) => {
            alert(`피드 수정 완료!\nID: ${feedId}\n운동 종류: ${workoutType}`);
            setShowEditDialog(false);
          }}
        />
      </DashboardWrapper>
    );
  },
};

/**
 * 피드 페이지 - FeedDetailDialogHome과 동일한 스타일 (스냅 스크롤)
 */
export const FeedPage: Story = {
  render: () => {
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedFeed, setSelectedFeed] = useState<Feed | null>(null);
    const [feedImageIndex, setFeedImageIndex] = useState(0);
    const [showFeedDetail, setShowFeedDetail] = useState(false);

    const filteredFeeds = mockFeeds.filter(feed =>
      (feed.author || feed.writerName || '').toLowerCase().includes(searchQuery.toLowerCase())
    );

    const handlePrevImage = () => {
      if (selectedFeed && feedImageIndex > 0) {
        setFeedImageIndex(feedImageIndex - 1);
      }
    };

    const handleNextImage = () => {
      if (selectedFeed && selectedFeed.images && feedImageIndex < selectedFeed.images.length - 1) {
        setFeedImageIndex(feedImageIndex + 1);
      }
    };

    return (
      <DashboardWrapper selectedNav="feed">
        <div className="h-full flex flex-col">
          {/* 검색창 */}
          <div className="sticky top-0 z-30 flex justify-center px-4 py-3 bg-gradient-to-b from-gray-50 to-transparent">
            <div className="w-full max-w-md">
              <SearchInput
                value={searchQuery}
                onChange={setSearchQuery}
                placeholder="작성자 이름으로 검색..."
                suggestions={mockFeeds.map(f => f.author || f.writerName).filter((name): name is string => !!name)}
              />
            </div>
          </div>

          {/* 스냅 스크롤 피드 목록 - 클릭하면 FeedDetailDialogHome 열림 */}
          <div className="flex-1 overflow-y-auto snap-y snap-mandatory" style={{ scrollSnapType: 'y mandatory' }}>
            {filteredFeeds.map(feed => {
              const hasImages = feed.images && feed.images.length > 0;

              return (
                <div
                  key={feed.id}
                  className="h-full snap-start snap-always flex items-center justify-center cursor-pointer"
                  style={{ scrollSnapAlign: 'start', minHeight: '100%' }}
                  onClick={() => {
                    setSelectedFeed(feed);
                    setFeedImageIndex(0);
                    setShowFeedDetail(true);
                  }}
                >
                  {/* 피드 미리보기 카드 */}
                  <div className="h-[95vh] max-h-[800px] w-[475px] overflow-hidden rounded-lg backdrop-blur-2xl bg-white/60 border border-gray-200/30 shadow-2xl flex flex-col">
                    {/* 이미지 영역 */}
                    {hasImages ? (
                      <div className="relative h-[545px] w-full overflow-hidden rounded-t-lg">
                        <img src={feed.images[0]} alt={feed.activity} className="w-full h-full object-cover" />
                        {/* 이미지 인디케이터 */}
                        {feed.images.length > 1 && (
                          <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                            {feed.images.map((_, idx) => (
                              <div key={idx} className={`w-1.5 h-1.5 rounded-full ${idx === 0 ? 'bg-white' : 'bg-white/50'}`} />
                            ))}
                          </div>
                        )}
                        {/* 아바타 */}
                        <div className="absolute top-4 left-4">
                          <Avatar className="w-10 h-10 border-2 border-white shadow-lg">
                            {mockAvatars[feed.author || ''] ? (
                              <img src={mockAvatars[feed.author || '']} alt={feed.author} className="w-full h-full object-cover rounded-full" />
                            ) : (
                              <AvatarFallback className="bg-white"><User className="w-5 h-5 text-gray-400" /></AvatarFallback>
                            )}
                          </Avatar>
                        </div>
                        {/* 액션 버튼 */}
                        <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
                          <div className="flex flex-col items-center gap-1">
                            <div className="w-12 h-12 rounded-full flex items-center justify-center">
                              <Heart className="w-6 h-6 fill-red-500 text-red-500" />
                            </div>
                            <span className="text-xs font-bold text-white">{feed.likes}</span>
                          </div>
                          <div className="flex flex-col items-center gap-1">
                            <div className="w-12 h-12 rounded-full flex items-center justify-center">
                              <MessageCircle className="w-6 h-6 text-white" />
                            </div>
                            <span className="text-xs font-bold text-white">{feed.comments}</span>
                          </div>
                        </div>
                      </div>
                    ) : (
                      <div className="relative h-[545px] w-full bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center rounded-t-lg">
                        <Sparkles className="w-16 h-16 text-gray-300" />
                        <div className="absolute top-4 left-4">
                          <Avatar className="w-10 h-10 border-2 border-gray-300 shadow-lg">
                            <AvatarFallback className="bg-white"><User className="w-5 h-5 text-gray-400" /></AvatarFallback>
                          </Avatar>
                        </div>
                        <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
                          <div className="flex flex-col items-center gap-1">
                            <Heart className="w-6 h-6 fill-red-500 text-red-500" />
                            <span className="text-xs font-bold text-gray-700">{feed.likes}</span>
                          </div>
                          <div className="flex flex-col items-center gap-1">
                            <MessageCircle className="w-6 h-6 text-gray-700" />
                            <span className="text-xs font-bold text-gray-700">{feed.comments}</span>
                          </div>
                        </div>
                      </div>
                    )}
                    {/* 피드 내용 */}
                    <ScrollArea className="flex-1 bg-transparent">
                      <div className="p-6 space-y-3">
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex items-center gap-2 flex-wrap">
                            <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
                              <Sparkles className="w-3 h-3 mr-1" />+{feed.points}
                            </Badge>
                            <Badge className="bg-white text-blue-700 px-3 py-1 font-bold text-xs border-0">{feed.activity}</Badge>
                          </div>
                          <Badge className="bg-white text-gray-700 px-3 py-1 font-bold text-xs border-0">{feed.time}</Badge>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-black text-gray-900">{feed.author}</span>
                        </div>
                        <p className="text-sm text-gray-700">{typeof feed.content === 'string' ? feed.content : ''}</p>
                      </div>
                    </ScrollArea>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* FeedDetailDialogHome - 실제 컴포넌트 사용 */}
        <FeedDetailDialogHome
          feed={selectedFeed}
          open={showFeedDetail}
          onOpenChange={setShowFeedDetail}
          currentImageIndex={feedImageIndex}
          onPrevImage={handlePrevImage}
          onNextImage={handleNextImage}
          onEdit={(feed) => alert(`피드 수정: ${feed.id}`)}
          onDelete={(feedId) => alert(`피드 삭제: ${feedId}`)}
        />
      </DashboardWrapper>
    );
  },
};

/**
 * 랭킹 페이지 - 점수 순위 (23등 가정, 22/23/24등 표시)
 */
export const RankingPage: Story = {
  render: () => (
    <DashboardWrapper selectedNav="ranking">
      <Ranking userId={23} profileImage="https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=100&h=100&fit=crop" />
    </DashboardWrapper>
  ),
};

/**
 * 경매 페이지 - 포인트 경매
 */
export const AuctionPage: Story = {
  render: () => (
    <DashboardWrapper selectedNav="auction">
      <Auction />
    </DashboardWrapper>
  ),
};

/**
 * 진료 페이지 - 예약 및 채팅
 */
export const MedicalPage: Story = {
  render: () => (
    <DashboardWrapper selectedNav="medical">
      <Medical
        setShowAppointment={() => {}}
        setShowChat={() => {}}
        setSelectedPrescription={() => {}}
      />
    </DashboardWrapper>
  ),
};

/**
 * 마이페이지 - 프로필 설정 (프로필 이미지 포함)
 */
export const ProfilePage: Story = {
  render: () => {
    const [profileImage, setProfileImage] = useState<string | null>(mockAvatars['김운동']);

    return (
      <DashboardWrapper selectedNav="profile">
        <MemberProfilePage
          onLogout={() => {}}
          profileImage={profileImage}
          setProfileImage={setProfileImage}
        />
      </DashboardWrapper>
    );
  },
};

/**
 * 알림 팝업 - 사이드바에서 알림 확인 (기존 프론트엔드 스타일)
 */
export const NotificationView: Story = {
  render: () => {
    const [expanded, setExpanded] = useState(true);
    const [showNotifications, setShowNotifications] = useState(true);
    const unreadCount = mockNotifications.filter(n => !n.isRead).length;

    return (
      <div className="h-screen w-screen overflow-hidden relative">
        <AnimatedBackground variant="member" />
        <Sidebar
          expanded={expanded}
          onExpandChange={setExpanded}
          navItems={memberNavItems}
          selectedNav="home"
          onNavSelect={() => {}}
          userType="member"
        >
          <div className="relative mb-2">
            <button
              onClick={() => setShowNotifications(!showNotifications)}
              className="relative w-full flex items-center py-3 rounded-2xl hover:bg-white/30 transition-colors"
              style={{ paddingLeft: '10px' }}
            >
              {/* 기존 프론트엔드와 동일한 스타일 */}
              <div className="relative flex-shrink-0">
                <Bell className="w-7 h-7 text-gray-700" />
                {unreadCount > 0 && (
                  <div className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full"></div>
                )}
              </div>
              <span className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ml-6 ${expanded ? "opacity-100" : "opacity-0 w-0"}`}>
                알림
              </span>
            </button>
            {showNotifications && (
              <NotificationPopup
                notifications={mockNotifications}
                onClose={() => setShowNotifications(false)}
                onNotificationClick={() => {}}
                onMarkAllAsRead={() => {}}
              />
            )}
          </div>
        </Sidebar>
        <div className={`h-full transition-all duration-300 ${expanded ? 'ml-64' : 'ml-20'}`}>
          <div className="h-full flex items-center justify-center">
            <Card className="p-8 text-center max-w-md">
              <Bell className="w-16 h-16 mx-auto text-gray-300 mb-4" />
              <h3 className="text-xl font-bold text-gray-700 mb-2">알림 팝업</h3>
              <p className="text-gray-500">← 사이드바의 알림 버튼을 클릭하여 알림을 확인하세요</p>
            </Card>
          </div>
        </div>
      </div>
    );
  },
};
