/**
 * Dashboard.tsx
 *
 * 메인 대시보드 라우터 컴포넌트
 * - 회원/의사 모드에 따른 페이지 라우팅
 * - 전역 상태 관리 (피드, 댓글, 알림, 처방전 등)
 * - 공통 레이아웃 (사이드바, 배경, 다이얼로그) 제공
 * - 각 페이지 컴포넌트의 Props 전달 및 이벤트 핸들링
 */

import { useState, useRef, useEffect } from "react";
import { useParams, useNavigate, Navigate } from "react-router-dom";
import { Bell } from "lucide-react";
import { toast } from "sonner";
import {
  Home,
  Video,
  Trophy,
  Calendar as CalendarIcon,
  PlusSquare,
  MessageCircle,
  Gavel,
} from "lucide-react";
import Sidebar from "./dashboard/shared/Sidebar";
import NotificationPopup from "./dashboard/shared/NotificationPopup";
import AnimatedBackground from "./dashboard/shared/AnimatedBackground";
import HomeView from "./dashboard/home/Home";
import FeedView from "./dashboard/feed/Feed";
import RankingView from "./dashboard/ranking/Ranking";
import MedicalView from "./dashboard/medical/Medical";
import AuctionView from "./dashboard/auction/Auction";
import PrescriptionModal from "./dashboard/dialogs/PrescriptionModal";
import FeedDetailDialogHome from "./dashboard/dialogs/FeedDetailDialogHome";
import AppointmentDialog from "./dashboard/dialogs/AppointmentDialog";
import ChatDialog from "./dashboard/dialogs/ChatDialog";
import PrescriptionFormDialog from "./dashboard/dialogs/PrescriptionFormDialog";
import EditFeedDialog from "./dashboard/dialogs/EditFeedDialog";
import CreateFeedDialog from "./dashboard/dialogs/CreateFeedDialog";
import DoctorChatPage from "./dashboard/chat/DoctorChatPage";
import DoctorProfilePage from "./dashboard/profile/DoctorProfilePage";
import CreatePage from "./dashboard/create/CreatePage";
import MemberProfilePage from "./dashboard/profile/MemberProfilePage";
import {
  Feed,
  Prescription,
  Notification,
  Member,
  ChatMessage,
} from "@/types/dashboard.types";
import { feedApi, notificationApi, commentApi } from "@/api";
import { getRelativeTime } from "@/lib/utils";

interface DashboardProps {
  onLogout: () => void;
  userType: "member" | "doctor";
}

const memberNavItems = [
  { id: "home", icon: Home, label: "홈" },
  { id: "feed", icon: Video, label: "피드" },
  { id: "ranking", icon: Trophy, label: "랭킹" },
  { id: "auction", icon: Gavel, label: "경매" },
  { id: "medical", icon: CalendarIcon, label: "진료" },
];

const doctorNavItems = [
  { id: "home", icon: Home, label: "대시보드" },
  { id: "feed", icon: Video, label: "피드" },
  { id: "ranking", icon: Trophy, label: "랭킹" },
  { id: "auction", icon: Gavel, label: "경매" },
  { id: "chat", icon: MessageCircle, label: "진료" },
];

const availableDates = [
  new Date(2024, 10, 15),
  new Date(2024, 10, 16),
  new Date(2024, 10, 18),
  new Date(2024, 10, 20),
  new Date(2024, 10, 22),
  new Date(2024, 10, 25),
  new Date(2024, 10, 27),
  new Date(2024, 10, 29),
];
const availableTimes = ["09:00", "10:00", "11:00", "14:00", "15:00", "16:00"];
const bookedTimes = ["10:00", "15:00"];

export default function Dashboard({ onLogout, userType }: DashboardProps) {
  const { page } = useParams<{ page: string }>();
  const navigate = useNavigate();

  // URL에서 현재 페이지 결정 (기본값: home)
  const defaultPage = "home";
  const validPages = userType === "doctor"
    ? ["home", "feed", "ranking", "auction", "chat", "profile"]
    : ["home", "feed", "ranking", "auction", "medical", "create", "profile"];
  const selectedNav = validPages.includes(page || "") ? page! : defaultPage;

  // 네비게이션 함수 (URL 변경)
  const setSelectedNav = (navId: string) => {
    navigate(`/dashboard/${navId}`);
  };

  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [selectedFeed, setSelectedFeed] = useState<Feed | null>(null);
  const [showFeedDetailInHome, setShowFeedDetailInHome] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const feedContainerRef = useRef<HTMLDivElement>(null);
  const [showNotifications, setShowNotifications] = useState(false);
  const [feedImageIndexes, setFeedImageIndexes] = useState<{
    [key: number]: number;
  }>({});
  const [selectedPrescription, setSelectedPrescription] =
    useState<Prescription | null>(null);
  const [profileImage, setProfileImage] = useState<string | null>(null);
  const [userId] = useState<number>(
    parseInt(localStorage.getItem("userId") || "1")
  );
  const userName = localStorage.getItem("userName") || "사용자";
  const [showAppointment, setShowAppointment] = useState(false);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>();
  const [selectedTime, setSelectedTime] = useState("");
  const [showChat, setShowChat] = useState(false);
  const [chatMessage, setChatMessage] = useState("");
  const [showPrescriptionForm, setShowPrescriptionForm] = useState(false);
  const [prescriptionMember, setPrescriptionMember] = useState<Member | null>(
    null
  );
  const [feedLikes, setFeedLikes] = useState<{ [key: number]: string[] }>({});
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [myFeeds, setMyFeeds] = useState<Feed[]>([]);
  const [allFeeds, setAllFeeds] = useState<Feed[]>([]);
  const [medicalChatMessages, setMedicalChatMessages] = useState<ChatMessage[]>(
    []
  );
  const [editingFeed, setEditingFeed] = useState<Feed | null>(null);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [targetCommentId, setTargetCommentId] = useState<number | null>(null);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [scrollToFeedId, setScrollToFeedId] = useState<number | null>(null);
  const [feedPage, setFeedPage] = useState(0);
  const [hasMoreFeeds, setHasMoreFeeds] = useState(true);
  const [isLoadingFeeds, setIsLoadingFeeds] = useState(false);
  const [pivotFeedId, setPivotFeedId] = useState<number | null>(null);
  const [pivotFeed, setPivotFeed] = useState<Feed | null>(null);

  // 백엔드 응답을 프론트엔드 Feed 타입으로 변환
  const mapBackendFeed = (backendFeed: any): Feed => ({
    id: backendFeed.id,
    writerId: backendFeed.writerId,
    writerName: backendFeed.writerName,
    writerAvatar: backendFeed.writerAvatar,
    activity: backendFeed.activity,
    points: backendFeed.points || 0,
    content: backendFeed.content,
    images: backendFeed.images || [],
    likes: backendFeed.likes || 0,
    comments: backendFeed.comments || 0,
    calories: backendFeed.calories,
    createdAt: backendFeed.createdAt,
    updatedAt: backendFeed.updatedAt,
    time: getRelativeTime(backendFeed.createdAt),
    author: backendFeed.writerName,
  });

  // 내 피드 로드 (전체)
  const loadMyFeeds = async () => {
    try {
      const currentUserId = parseInt(localStorage.getItem("userId") || "0");
      const response = await feedApi.getFeedsByUserId(currentUserId, 0, 100);
      const feeds = response.content || response;
      const mappedFeeds = feeds.map(mapBackendFeed);
      setMyFeeds(mappedFeeds);
    } catch (error) {
      console.error("내 피드 로드 실패:", error);
    }
  };

  // 다른 사람 피드 로드 (페이지네이션)
  const loadFeeds = async (page: number, reset: boolean = false, excludeFeedId?: number) => {
    if (isLoadingFeeds) return;

    setIsLoadingFeeds(true);
    try {
      const pageSize = 10; // 한 번에 10개씩 로드
      const currentUserId = parseInt(localStorage.getItem("userId") || "0");
      // 백엔드에서 내 피드 제외 + excludeFeedId 제외
      const response = await feedApi.getAllFeeds(page, pageSize, currentUserId, excludeFeedId);
      const feeds = response.content || response;

      const mappedFeeds = feeds.map(mapBackendFeed);
      if (reset) {
        setAllFeeds(mappedFeeds.sort((a: Feed, b: Feed) => b.id - a.id));
      } else {
        setAllFeeds((prev) => [...prev, ...mappedFeeds].sort((a, b) => b.id - a.id));
      }

      // 더 이상 로드할 피드가 있는지 확인
      const totalPages = response.totalPages || 1;
      setHasMoreFeeds(page < totalPages - 1);
      setFeedPage(page);
    } catch (error) {
      console.error("피드 데이터 로드 실패:", error);
      toast.error("피드 데이터를 불러오는데 실패했습니다.");
    } finally {
      setIsLoadingFeeds(false);
    }
  };

  // 추가 피드 로드
  const loadMoreFeeds = () => {
    if (hasMoreFeeds && !isLoadingFeeds) {
      loadFeeds(feedPage + 1);
    }
  };

  // 초기 피드 로드 및 refreshTrigger 감지
  useEffect(() => {
    if (userType === "member") {
      loadMyFeeds(); // 내 피드는 전체 로드
      loadFeeds(0, true); // 다른 피드는 페이지네이션
    }
  }, [userType, refreshTrigger]); // refreshTrigger 변경 시 데이터 재로드

  // 알림 데이터 로드
  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        // 임시 사용자 ID (추후 실제 사용자 ID로 교체 필요)
        const userId = 1;
        const response = await notificationApi.getAllNotifications(userId);
        setNotifications(response);
      } catch (error) {
        console.error("알림 데이터 로드 실패:", error);
        // 에러가 발생해도 사용자 경험을 해치지 않도록 토스트 메시지는 표시하지 않음
      }
    };

    fetchNotifications();
  }, []);

  // 알림 클릭 핸들러
  const handleNotificationClick = async (notification: Notification) => {
    try {
      // 읽음 처리
      if (!notification.isRead) {
        await notificationApi.markAsRead(notification.id);

        // 로컬 상태 업데이트
        setNotifications(
          notifications.map((n) =>
            n.id === notification.id ? { ...n, isRead: true } : n
          )
        );
      }

      // 알림 팝업 닫기
      setShowNotifications(false);
      setSidebarExpanded(false);

      // 피드 관련 알림 처리
      if (notification.type === "FEED_LIKE" || notification.type === "COMMENT") {
        // refId가 피드 ID
        const feedId = notification.refId ? parseInt(notification.refId) : null;
        if (feedId) {
          await navigateToFeed(feedId, null);
        }
      } else if (notification.type === "COMMENT_LIKE" || notification.type === "REPLY") {
        // refId가 댓글 ID - 댓글에서 피드 ID 조회 필요
        const commentId = notification.refId ? parseInt(notification.refId) : null;
        if (commentId) {
          try {
            // 댓글 조회해서 feedId 가져오기
            const comment = await commentApi.getCommentById(commentId);
            if (comment.feedId) {
              await navigateToFeed(comment.feedId, commentId);
            }
          } catch (error) {
            console.error("댓글 조회 실패:", error);
          }
        }
      }
    } catch (error) {
      console.error("알림 처리 실패:", error);
      toast.error("알림을 처리하는데 실패했습니다.");
    }
  };

  // 모두 읽음 핸들러
  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications(
        notifications.map((n) => ({ ...n, isRead: true }))
      );
    } catch (error) {
      console.error("모두 읽음 처리 실패:", error);
    }
  };

  // 피드로 이동하는 공통 함수
  const navigateToFeed = async (feedId: number, commentId: number | null) => {
    // 내 피드와 다른 피드 모두에서 검색
    const myFeed = myFeeds.find((f) => f.id === feedId);
    let otherFeed = allFeeds.find((f) => f.id === feedId);
    let feed = myFeed || otherFeed;

    // 댓글 ID 설정
    setTargetCommentId(commentId);

    // 피드가 아직 로드되지 않은 경우 API에서 가져오기
    if (!feed) {
      try {
        const response = await feedApi.getFeedById(feedId);
        feed = mapBackendFeed(response);
        setAllFeeds((prev) => [feed!, ...prev]);
      } catch (error) {
        console.error("피드 로드 실패:", error);
        return;
      }
    }

    if (feed) {
      const currentUserId = parseInt(localStorage.getItem("userId") || "0");
      const isMyFeed = feed.writerId === currentUserId || myFeed !== undefined;

      if (isMyFeed) {
        // 내 피드면 홈으로 이동 + 다이얼로그
        setSelectedNav("home");
        setSelectedFeed(feed);
        setShowFeedDetailInHome(true);
      } else {
        // 다른 사람 피드면 pivot 패턴으로 피드 페이지 이동
        setPivotFeedId(feed.id);
        setPivotFeed(feed);
        loadFeeds(0, true, feed.id);
        setSelectedNav("feed");
        setSelectedFeed(feed);
        setShowFeedDetailInHome(true);
      }
    }
  };

  const navItems = userType === "member" ? memberNavItems : doctorNavItems;

  // 네비게이션 선택 핸들러 - 피드 직접 클릭 시 pivot 초기화
  const handleNavSelect = (navId: string) => {
    if (navId === "feed" && pivotFeedId) {
      // 피드 메뉴 직접 클릭 시 pivot 초기화하고 일반 피드 로드
      setPivotFeedId(null);
      setPivotFeed(null);
      loadFeeds(0, true);
    }
    setSelectedNav(navId);
  };

  // /dashboard 접근 시 기본 페이지로 리다이렉트 (모든 hooks 이후에 위치)
  if (!page) {
    return <Navigate to={`/dashboard/${defaultPage}`} replace />;
  }

  const getFeedImageIndex = (feedId: number) => feedImageIndexes[feedId] || 0;
  const setFeedImageIndex = (feedId: number, index: number) =>
    setFeedImageIndexes((prev) => ({ ...prev, [feedId]: index }));
  const handleLike = (feedId: number) => {
    const currentLikes = feedLikes[feedId] || [];
    const hasLiked = currentLikes.includes(userName);
    setFeedLikes({
      ...feedLikes,
      [feedId]: hasLiked
        ? currentLikes.filter((name) => name !== userName)
        : [...currentLikes, userName],
    });
    setAllFeeds(
      allFeeds.map((feed) =>
        feed.id === feedId
          ? { ...feed, likes: hasLiked ? feed.likes - 1 : feed.likes + 1 }
          : feed
      )
    );
  };
  const hasLiked = (feedId: number) =>
    (feedLikes[feedId] || []).includes(userName);

  const handleEditFeed = (feed: Feed) => {
    setEditingFeed(feed);
    setShowEditDialog(true);
    setShowFeedDetailInHome(false);
  };

  const handleUpdateFeed = async (
    feedId: number,
    images: string[],
    content: string,
    workoutType: string,
    _startImage: string | null,
    _endImage: string | null
  ) => {
    try {
      // API 호출하여 DB에 저장
      await feedApi.updateFeed(feedId, {
        activity: workoutType,
        content,
        images,
      });

      // 로컬 상태 업데이트
      setMyFeeds(
        myFeeds.map((feed) =>
          feed.id === feedId
            ? {
                ...feed,
                images,
                content,
                activity: workoutType,
                time: "방금 전",
                edited: true,
              }
            : feed
        )
      );
      setAllFeeds(
        allFeeds.map((feed) =>
          feed.id === feedId
            ? {
                ...feed,
                images,
                content,
                activity: workoutType,
                time: "방금 전",
                edited: true,
              }
            : feed
        )
      );
      setRefreshTrigger((prev) => prev + 1);
      toast.success("피드가 수정되었습니다!");
    } catch (error: any) {
      const message = error.response?.data?.message || "피드 수정에 실패했습니다.";
      toast.error(message);
    }
  };

  const handleDeleteFeed = async (feedId: number) => {
    try {
      await feedApi.deleteFeed(feedId);
      setMyFeeds(myFeeds.filter((feed) => feed.id !== feedId));
      setAllFeeds(allFeeds.filter((feed) => feed.id !== feedId));
      setRefreshTrigger((prev) => prev + 1); // canPostToday 재확인 + 데이터 재로드
      toast.success("피드가 삭제되고 포인트가 회수되었습니다!");
    } catch {
      toast.error("피드 삭제에 실패했습니다.");
    }
  };

  const handleCreateFeed = async (
    images: string[],
    content: string,
    workoutType: string,
    _startImage: string | null,
    _endImage: string | null
  ) => {
    try {
      const currentUserId = parseInt(localStorage.getItem("userId") || "0");

      // API 호출로 피드 생성
      await feedApi.createFeed({
        activity: workoutType,
        content: content,
        images,
      });

      // 데이터 재로드 및 canPostToday 재확인
      setRefreshTrigger((prev) => prev + 1);
      toast.success("피드가 작성되었습니다!");
    } catch (error: any) {
      const message = error.response?.data?.message || "피드 작성에 실패했습니다.";
      toast.error(message);
    }
  };

  if (userType === "doctor") {
    return (
      <div className="h-screen w-screen overflow-hidden relative">
        <AnimatedBackground variant="doctor" />
        <Sidebar
          expanded={sidebarExpanded}
          onExpandChange={setSidebarExpanded}
          navItems={navItems}
          selectedNav={selectedNav}
          onNavSelect={setSelectedNav}
          userType="doctor"
        />
        <div
          className={`h-full transition-all duration-300 ${
            sidebarExpanded ? "ml-64" : "ml-20"
          }`}
        >
          {selectedNav === "chat" && <DoctorChatPage />}
          {selectedNav === "profile" && (
            <DoctorProfilePage onLogout={onLogout} />
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen w-screen overflow-hidden relative">
      <AnimatedBackground variant="member" />
      <Sidebar
        expanded={sidebarExpanded || showNotifications}
        onExpandChange={(expanded) =>
          !showNotifications && setSidebarExpanded(expanded)
        }
        navItems={navItems}
        selectedNav={selectedNav}
        onNavSelect={handleNavSelect}
        userType="member"
        profileImage={profileImage}
      >
        <div
          className="relative mb-2"
          onMouseEnter={(e) => e.stopPropagation()}
          onMouseLeave={(e) => e.stopPropagation()}
        >
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative w-full flex items-center py-3 rounded-2xl hover:bg-white/30 transition-colors"
            style={{ paddingLeft: "10px" }}
          >
            <div className="relative flex-shrink-0">
              <Bell className="w-7 h-7 text-gray-700" />
              {notifications.filter((n) => !n.isRead).length > 0 && (
                <div className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full"></div>
              )}
            </div>
            <span
              className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ml-6 ${
                sidebarExpanded || showNotifications
                  ? "opacity-100"
                  : "opacity-0 w-0"
              }`}
            >
              알림
            </span>
          </button>
          {showNotifications && (
            <NotificationPopup
              notifications={notifications}
              onClose={(closeSidebar = true) => {
                setShowNotifications(false);
                if (closeSidebar) setSidebarExpanded(false);
              }}
              onNotificationClick={handleNotificationClick}
              onMarkAllAsRead={handleMarkAllAsRead}
            />
          )}
        </div>
      </Sidebar>

      <div
        className={`h-full transition-all duration-300 ${
          sidebarExpanded || showNotifications ? "ml-64" : "ml-20"
        }`}
      >
        {selectedNav === "home" && (
          <HomeView
            profileImage={profileImage}
            myFeeds={myFeeds}
            setSelectedFeed={setSelectedFeed}
            setFeedImageIndex={setFeedImageIndex}
            setShowFeedDetailInHome={setShowFeedDetailInHome}
            refreshTrigger={refreshTrigger}
            onCreateClick={() => setShowCreateDialog(true)}
          />
        )}
        {selectedNav === "feed" && (
          <FeedView
            allFeeds={pivotFeed ? [pivotFeed, ...allFeeds] : allFeeds}
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
            loadMoreFeeds={loadMoreFeeds}
            hasMoreFeeds={hasMoreFeeds}
            isLoadingFeeds={isLoadingFeeds}
          />
        )}
        {selectedNav === "ranking" && (
          <RankingView userId={userId} profileImage={profileImage} />
        )}
        {selectedNav === "auction" && <AuctionView />}
        {selectedNav === "medical" && (
          <MedicalView
            setShowAppointment={setShowAppointment}
            setShowChat={setShowChat}
            setSelectedPrescription={setSelectedPrescription}
          />
        )}
        {selectedNav === "create" && (
          <CreatePage
            onCreatePost={(newFeed) => {
              setMyFeeds([newFeed, ...myFeeds]);
            }}
          />
        )}
        {selectedNav === "profile" && (
          <MemberProfilePage
            onLogout={onLogout}
            profileImage={profileImage}
            setProfileImage={setProfileImage}
          />
        )}
      </div>

      <FeedDetailDialogHome
        feed={selectedFeed}
        open={
          showFeedDetailInHome &&
          (selectedNav === "home" || selectedNav === "feed")
        }
        onOpenChange={() => {
          setShowFeedDetailInHome(false);
          setSelectedFeed(null);
          setTargetCommentId(null);
        }}
        currentImageIndex={
          selectedFeed ? getFeedImageIndex(selectedFeed.id) : 0
        }
        onPrevImage={() =>
          selectedFeed &&
          setFeedImageIndex(
            selectedFeed.id,
            Math.max(0, getFeedImageIndex(selectedFeed.id) - 1)
          )
        }
        onNextImage={() =>
          selectedFeed &&
          setFeedImageIndex(
            selectedFeed.id,
            Math.min(
              selectedFeed.images.length - 1,
              getFeedImageIndex(selectedFeed.id) + 1
            )
          )
        }
        onEdit={handleEditFeed}
        onDelete={handleDeleteFeed}
        targetCommentId={targetCommentId}
      />

      <EditFeedDialog
        feed={editingFeed}
        open={showEditDialog}
        onOpenChange={setShowEditDialog}
        onSave={handleUpdateFeed}
      />

      <CreateFeedDialog
        open={showCreateDialog}
        onOpenChange={setShowCreateDialog}
        onCreate={handleCreateFeed}
      />

      <PrescriptionModal
        prescription={selectedPrescription}
        open={!!selectedPrescription}
        onOpenChange={() => setSelectedPrescription(null)}
        onDownload={() => toast.success("처방전 PDF 다운로드를 시작합니다.")}
      />

      <ChatDialog
        open={showChat}
        onOpenChange={setShowChat}
        messages={medicalChatMessages}
        chatMessage={chatMessage}
        setChatMessage={setChatMessage}
        onSend={() => {
          if (chatMessage.trim()) {
            setMedicalChatMessages([
              ...medicalChatMessages,
              {
                id: Date.now(),
                author: userName,
                avatar: userName.charAt(0),
                content: chatMessage,
                time: new Date().toLocaleTimeString("ko-KR", {
                  hour: "2-digit",
                  minute: "2-digit",
                }),
                isMine: true,
              },
            ]);
            setChatMessage("");
          }
        }}
      />

      <PrescriptionFormDialog
        open={showPrescriptionForm}
        onOpenChange={(open) => {
          setShowPrescriptionForm(open);
          if (!open) setPrescriptionMember(null);
        }}
        member={prescriptionMember}
        onSubmit={() => {
          toast.success("처방전이 저장되었습니다.");
          setShowPrescriptionForm(false);
          setPrescriptionMember(null);
        }}
      />

      <AppointmentDialog
        open={showAppointment}
        onOpenChange={setShowAppointment}
        selectedDepartment={selectedDepartment}
        setSelectedDepartment={setSelectedDepartment}
        selectedDate={selectedDate}
        setSelectedDate={setSelectedDate}
        selectedTime={selectedTime}
        setSelectedTime={setSelectedTime}
        availableDates={availableDates}
        availableTimes={availableTimes}
        bookedTimes={bookedTimes}
        onConfirm={() => {
          toast.success("예약이 완료되었습니다!");
          setShowAppointment(false);
          setSelectedDepartment("");
          setSelectedDate(undefined);
          setSelectedTime("");
        }}
      />
    </div>
  );
}
