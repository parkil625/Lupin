import React from "react";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";

// 페이지 컴포넌트
import LandingPage from "../LandingPage";
import Login from "../auth/Login";
import Sidebar from "../dashboard/shared/Sidebar";
import AnimatedBackground from "../dashboard/shared/AnimatedBackground";
import Home from "../dashboard/home/Home";
import FeedView from "../dashboard/feed/Feed";
import Ranking from "../dashboard/ranking/Ranking";
import Auction from "../dashboard/auction/Auction";
import Medical from "../dashboard/medical/Medical";
import ProfilePageComponent from "../dashboard/profile/ProfilePage";
import NotificationPopup from "../dashboard/shared/NotificationPopup";
import FeedDetailDialogHome from "../dashboard/dialogs/FeedDetailDialogHome";
import CreateFeedDialog from "../dashboard/dialogs/CreateFeedDialog";
import EditFeedDialog from "../dashboard/dialogs/EditFeedDialog";
import NotFoundPage from "../errors/NotFoundPage";
import ErrorPage from "../errors/ErrorPage";

// UI 컴포넌트
import { Card } from "@/components/ui/card";

// 아이콘
import {
  Home as HomeIcon,
  Video,
  Trophy,
  Gavel,
  Calendar,
  Bell,
} from "lucide-react";

// 타입
import { Feed, Notification } from "@/types/dashboard.types";

/**
 * 페이지 스토리북
 *
 * 실제 서비스에서 보이는 전체 화면들입니다.
 */
const meta = {
  title: "Pages",
  parameters: {
    layout: "fullscreen",
    backgrounds: { default: "light" },
  },
} satisfies Meta;

export default meta;
type Story = StoryObj;

// ============================================
// Mock 데이터
// ============================================

const memberNavItems = [
  { id: "home", icon: HomeIcon, label: "홈" },
  { id: "feed", icon: Video, label: "피드" },
  { id: "ranking", icon: Trophy, label: "랭킹" },
  { id: "auction", icon: Gavel, label: "경매" },
  { id: "medical", icon: Calendar, label: "진료" },
];

const mockAvatars: Record<string, string> = {
  김운동:
    "https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=100&h=100&fit=crop",
  이헬스:
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop",
  박피트:
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop",
};

const mockFeeds: Feed[] = [
  {
    id: 1,
    writerId: 1,
    writerName: "김운동",
    author: "김운동",
    activity: "러닝",
    points: 30,
    content: "오늘 아침 공원에서 러닝했어요! 날씨가 좋아서 기분이 상쾌합니다.",
    images: [
      "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400&q=75",
    ],
    likes: 15,
    comments: 3,
    time: "2시간 전",
    createdAt: new Date().toISOString(),
  },
  {
    id: 2,
    writerId: 2,
    writerName: "이헬스",
    author: "이헬스",
    activity: "웨이트",
    points: 30,
    content: "오늘 상체 운동 완료!",
    images: [
      "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&q=75",
    ],
    likes: 22,
    comments: 5,
    time: "4시간 전",
    createdAt: new Date().toISOString(),
  },
  {
    id: 3,
    writerId: 3,
    writerName: "박피트",
    author: "박피트",
    activity: "수영",
    points: 30,
    content: "수영장에서 1km 완주!",
    images: [
      "https://images.unsplash.com/photo-1530549387789-4c1017266635?w=400&q=75",
    ],
    likes: 18,
    comments: 2,
    time: "어제",
    createdAt: new Date().toISOString(),
  },
];

const mockNotifications: Notification[] = [
  {
    id: 1,
    type: "FEED_LIKE",
    title: "좋아요",
    content: "김운동님이 회원님의 피드를 좋아합니다.",
    isRead: false,
    createdAt: "10분 전",
  },
  {
    id: 2,
    type: "COMMENT",
    title: "댓글",
    content: '이헬스님이 댓글을 남겼습니다: "멋져요!"',
    isRead: false,
    createdAt: "30분 전",
  },
];

// ============================================
// 공통 레이아웃 래퍼
// ============================================

const DashboardWrapper = ({
  children,
  selectedNav,
}: {
  children: React.ReactNode;
  selectedNav: string;
}) => {
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
      <div
        className={`h-full transition-all duration-300 ${
          expanded ? "ml-64" : "ml-20"
        }`}
      >
        {children}
      </div>
    </div>
  );
};

// ============================================
// 1. 소개 페이지 (Landing)
// ============================================

/**
 * 소개 페이지
 *
 * 서비스 첫 화면입니다. "로그인" 버튼을 클릭하면 로그인 페이지로 이동합니다.
 */
export const LandingPageStory: Story = {
  name: "소개페이지",
  render: () => <LandingPage />,
};

// ============================================
// 2. 로그인 페이지
// ============================================

/**
 * 로그인 페이지
 *
 * 사용자 인증 화면입니다. 사내 아이디/비밀번호 또는 SNS 간편 로그인을 지원합니다.
 */
export const LoginPage: Story = {
  name: "로그인페이지",
  render: () => <Login />,
};

// ============================================
// 3. 홈 페이지
// ============================================

/**
 * 홈 페이지
 *
 * 프로필 및 내 피드를 확인하고, 새 피드를 작성할 수 있습니다.
 */
export const HomePage: Story = {
  name: "홈페이지",
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
      if (
        selectedFeed &&
        selectedFeed.images &&
        feedImageIndex < selectedFeed.images.length - 1
      ) {
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
          profileImage={mockAvatars["김운동"]}
          myFeeds={mockFeeds.slice(0, 2)}
          setSelectedFeed={(feed) => {
            setSelectedFeed(feed);
            setFeedImageIndex(0);
            setShowFeedDetail(true);
          }}
          setFeedImageIndex={(_feedId, updater) =>
            setFeedImageIndex(
              typeof updater === "function" ? updater(feedImageIndex) : updater
            )
          }
          setShowFeedDetailInHome={setShowFeedDetail}
          onCreateClick={() => setShowCreateDialog(true)}
          refreshTrigger={0}
          onLoadMore={() =>
            console.log("[Storybook] Home - onLoadMore triggered")
          }
          hasMore={true}
          isLoading={false}
        />

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

        <CreateFeedDialog
          open={showCreateDialog}
          onOpenChange={setShowCreateDialog}
          onCreate={(images, _content, workoutType) => {
            alert(
              `피드 생성!\n운동 종류: ${workoutType}\n이미지 수: ${images.length}`
            );
            setShowCreateDialog(false);
          }}
        />

        <EditFeedDialog
          feed={editingFeed}
          open={showEditDialog}
          onOpenChange={setShowEditDialog}
          onSave={(feedId, _images, _content, workoutType) => {
            alert(`피드 수정 완료!\nID: ${feedId}\n운동 종류: ${workoutType}`);
            setShowEditDialog(false);
          }}
        />
      </DashboardWrapper>
    );
  },
};

// ============================================
// 4. 피드 페이지
// ============================================

/**
 * 피드 페이지
 *
 * 실제 FeedView 컴포넌트를 사용합니다.
 * 피드를 스냅 스크롤로 탐색하고, 댓글 버튼을 누르면 댓글 패널이 열립니다.
 */
export const FeedPage: Story = {
  name: "피드페이지",
  render: () => {
    const [searchQuery, setSearchQuery] = useState("");
    const [showSearch, setShowSearch] = useState(false);
    const [feedImageIndices, setFeedImageIndices] = useState<
      Record<number, number>
    >({});
    const [likedFeeds, setLikedFeeds] = useState<Set<number>>(new Set());
    const feedContainerRef = React.useRef<HTMLDivElement>(null);
    const [scrollToFeedId, setScrollToFeedId] = useState<number | null>(null);

    const getFeedImageIndex = (feedId: number) => feedImageIndices[feedId] || 0;
    const setFeedImageIndex = (
      feedId: number,
      updater: number | ((prev: number) => number)
    ) => {
      setFeedImageIndices((prev) => ({
        ...prev,
        [feedId]:
          typeof updater === "function" ? updater(prev[feedId] || 0) : updater,
      }));
    };
    const hasLiked = (feedId: number) => likedFeeds.has(feedId);
    const handleLike = (feedId: number) => {
      setLikedFeeds((prev) => {
        const newSet = new Set(prev);
        if (newSet.has(feedId)) {
          newSet.delete(feedId);
        } else {
          newSet.add(feedId);
        }
        return newSet;
      });
    };

    return (
      <DashboardWrapper selectedNav="feed">
        <FeedView
          allFeeds={mockFeeds}
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          showSearch={showSearch}
          setShowSearch={setShowSearch}
          getFeedImageIndex={getFeedImageIndex}
          setFeedImageIndex={setFeedImageIndex}
          hasLiked={hasLiked}
          handleLike={handleLike}
          feedContainerRef={feedContainerRef}
          scrollToFeedId={scrollToFeedId}
          setScrollToFeedId={setScrollToFeedId}
          loadMoreFeeds={() => {}}
          hasMoreFeeds={false}
          isLoadingFeeds={false}
        />
      </DashboardWrapper>
    );
  },
};

// ============================================
// 5. 랭킹 페이지
// ============================================

/**
 * 랭킹 페이지
 *
 * 포인트 순위를 확인합니다. 현재 사용자는 23등으로 가정합니다.
 */
export const RankingPage: Story = {
  name: "랭킹페이지",
  render: () => (
    <DashboardWrapper selectedNav="ranking">
      <Ranking userId={23} profileImage={mockAvatars["김운동"]} />
    </DashboardWrapper>
  ),
};

// ============================================
// 6. 경매 페이지
// ============================================

/**
 * 경매 페이지
 *
 * 포인트로 상품을 경매합니다.
 */
export const AuctionPage: Story = {
  name: "경매페이지",
  render: () => (
    <DashboardWrapper selectedNav="auction">
      <Auction />
    </DashboardWrapper>
  ),
};

// ============================================
// 7. 진료 페이지
// ============================================

/**
 * 진료 페이지
 *
 * 예약 및 비대면 진료 채팅을 관리합니다.
 */
export const MedicalPage: Story = {
  name: "진료페이지",
  render: () => (
    <DashboardWrapper selectedNav="medical">
      <Medical setSelectedPrescription={() => {}} />
    </DashboardWrapper>
  ),
};

// ============================================
// 8. 마이페이지
// ============================================

/**
 * 마이페이지
 *
 * 프로필 설정 및 계정 관리를 합니다.
 */
export const ProfilePage: Story = {
  name: "마이페이지",
  render: () => {
    const [profileImage, setProfileImage] = useState<string | null>(
      mockAvatars["김운동"]
    );

    return (
      <DashboardWrapper selectedNav="profile">
        <ProfilePageComponent
          onLogout={() => {}}
          profileImage={profileImage}
          setProfileImage={setProfileImage}
        />
      </DashboardWrapper>
    );
  },
};

// ============================================
// 9. 알림 팝업
// ============================================

/**
 * 알림 팝업
 *
 * 사이드바에서 알림 버튼 클릭 시 나타나는 팝업입니다.
 */
export const NotificationPopupStory: Story = {
  name: "알림팝업",
  render: () => {
    const [expanded, setExpanded] = useState(true);
    const [showNotifications, setShowNotifications] = useState(true);
    const unreadCount = mockNotifications.filter((n) => !n.isRead).length;

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
              style={{ paddingLeft: "10px" }}
            >
              <div className="relative flex-shrink-0">
                <Bell className="w-7 h-7 text-gray-700" />
                {unreadCount > 0 && (
                  <div className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full"></div>
                )}
              </div>
              <span
                className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ml-6 ${
                  expanded ? "opacity-100" : "opacity-0 w-0"
                }`}
              >
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
        <div
          className={`h-full transition-all duration-300 ${
            expanded ? "ml-64" : "ml-20"
          }`}
        >
          <div className="h-full flex items-center justify-center">
            <Card className="p-8 text-center max-w-md">
              <Bell className="w-16 h-16 mx-auto text-gray-300 mb-4" />
              <h3 className="text-xl font-bold text-gray-700 mb-2">
                알림 팝업
              </h3>
              <p className="text-gray-500">사이드바의 알림 버튼을 클릭하세요</p>
            </Card>
          </div>
        </div>
      </div>
    );
  },
};

// ============================================
// 10. 에러 페이지 (404)
// ============================================

/**
 * 404 에러 페이지
 *
 * 페이지를 찾을 수 없을 때 표시됩니다.
 */
export const Error404: Story = {
  name: "에러404",
  render: () => <NotFoundPage />,
};

// ============================================
// 11. 에러 페이지 (500)
// ============================================

/**
 * 500 에러 페이지
 *
 * 서버 오류 발생 시 표시됩니다.
 */
export const Error500: Story = {
  name: "에러500",
  render: () => <ErrorPage />,
};
