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
import { Bell } from "lucide-react";
import { toast } from "sonner";
import { Home, Video, Trophy, Calendar as CalendarIcon, PlusSquare, MessageCircle } from "lucide-react";
import Sidebar from "./dashboard/shared/Sidebar";
import NotificationPopup from "./dashboard/shared/NotificationPopup";
import AnimatedBackground from "./dashboard/shared/AnimatedBackground";
import HomeView from "./dashboard/home/Home";
import FeedView from "./dashboard/feed/Feed";
import RankingView from "./dashboard/ranking/Ranking";
import MedicalView from "./dashboard/medical/Medical";
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
import { Feed, Prescription, Notification, Member, ChatMessage } from "@/types/dashboard.types";
import { feedApi, notificationApi } from "@/api";

// 상대적 시간 표시 함수
function getRelativeTime(date: Date | string): string {
  const now = new Date();
  const targetDate = typeof date === 'string' ? new Date(date) : date;
  const diffMs = now.getTime() - targetDate.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);
  const diffWeeks = Math.floor(diffDays / 7);
  const diffMonths = Math.floor(diffDays / 30);
  const diffYears = Math.floor(diffDays / 365);

  if (diffSeconds < 60) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  if (diffHours < 24) return `${diffHours}시간 전`;
  if (diffDays < 7) return `${diffDays}일 전`;
  if (diffWeeks < 4) return `${diffWeeks}주 전`;
  if (diffMonths < 12) return `${diffMonths}개월 전`;
  return `${diffYears}년 전`;
}

interface DashboardProps {
  onLogout: () => void;
  userType: "member" | "doctor";
}

const memberNavItems = [
  { id: "home", icon: Home, label: "홈" },
  { id: "feed", icon: Video, label: "피드" },
  { id: "ranking", icon: Trophy, label: "랭킹" },
  { id: "medical", icon: CalendarIcon, label: "진료" }
];

const doctorNavItems = [
  { id: "chat", icon: MessageCircle, label: "채팅" }
];

const availableDates = [new Date(2024, 10, 15), new Date(2024, 10, 16), new Date(2024, 10, 18), new Date(2024, 10, 20), new Date(2024, 10, 22), new Date(2024, 10, 25), new Date(2024, 10, 27), new Date(2024, 10, 29)];
const availableTimes = ["09:00", "10:00", "11:00", "14:00", "15:00", "16:00"];
const bookedTimes = ["10:00", "15:00"];

export default function Dashboard({ onLogout, userType }: DashboardProps) {
  const [selectedNav, setSelectedNav] = useState(userType === "doctor" ? "chat" : "home");
  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [selectedFeed, setSelectedFeed] = useState<Feed | null>(null);
  const [showFeedDetailInHome, setShowFeedDetailInHome] = useState(false);
  const feedContainerRef = useRef<HTMLDivElement>(null);
  const [challengeJoined, setChallengeJoined] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [feedImageIndexes, setFeedImageIndexes] = useState<{[key: number]: number}>({});
  const [selectedPrescription, setSelectedPrescription] = useState<Prescription | null>(null);
  const [profileImage, setProfileImage] = useState<string | null>(null);
  const [userId] = useState<number>(parseInt(localStorage.getItem('userId') || '1'));
  const [showAppointment, setShowAppointment] = useState(false);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>();
  const [selectedTime, setSelectedTime] = useState("");
  const [showChat, setShowChat] = useState(false);
  const [chatMessage, setChatMessage] = useState("");
  const [showPrescriptionForm, setShowPrescriptionForm] = useState(false);
  const [prescriptionMember, setPrescriptionMember] = useState<Member | null>(null);
  const [feedLikes, setFeedLikes] = useState<{[key: number]: string[]}>({});
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [myFeeds, setMyFeeds] = useState<Feed[]>([]);
  const [allFeeds, setAllFeeds] = useState<Feed[]>([]);
  const [medicalChatMessages, setMedicalChatMessages] = useState<ChatMessage[]>([]);
  const [editingFeed, setEditingFeed] = useState<Feed | null>(null);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [targetCommentId, setTargetCommentId] = useState<number | null>(null);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [scrollToFeedId, setScrollToFeedId] = useState<number | null>(null);
  const [feedPage, setFeedPage] = useState(0);
  const [hasMoreFeeds, setHasMoreFeeds] = useState(true);
  const [isLoadingFeeds, setIsLoadingFeeds] = useState(false);

  // 내 피드 로드 (전체)
  const loadMyFeeds = async () => {
    try {
      const currentUserId = parseInt(localStorage.getItem('userId') || '0');
      const response = await feedApi.getFeedsByUserId(currentUserId, 0, 100);
      const feeds = response.content || response;

      // 백엔드 응답을 프론트엔드 타입으로 변환
      const mappedFeeds = feeds.map((backendFeed: any) => {
        let stats = {};
        try {
          stats = backendFeed.statsJson ? JSON.parse(backendFeed.statsJson) : {};
        } catch (e) {
          console.error('Failed to parse statsJson:', e);
        }
        const points = Math.min(Math.floor(backendFeed.duration / 5) * 5, 30);

        return {
          id: backendFeed.id,
          authorId: backendFeed.writerId,
          author: backendFeed.authorName,
          avatar: '',
          activity: backendFeed.activityType,
          duration: `${backendFeed.duration}분`,
          points: points,
          content: backendFeed.content,
          images: backendFeed.images || [],
          likes: backendFeed.likesCount || 0,
          comments: backendFeed.commentsCount || 0,
          time: getRelativeTime(backendFeed.createdAt),
          stats: stats,
          likedBy: [],
        };
      });

      setMyFeeds(mappedFeeds);
    } catch (error) {
      console.error("내 피드 로드 실패:", error);
    }
  };

  // 다른 사람 피드 로드 (페이지네이션)
  const loadFeeds = async (page: number, reset: boolean = false) => {
    if (isLoadingFeeds) return;

    setIsLoadingFeeds(true);
    try {
      const pageSize = 1; // 한 번에 1개씩 로드 (성능 최적화)
      const currentUserId = parseInt(localStorage.getItem('userId') || '0');
      // 백엔드에서 내 피드 제외
      const response = await feedApi.getAllFeeds(page, pageSize, currentUserId);
      const feeds = response.content || response;

      if (reset) {
        setAllFeeds(feeds);
      } else {
        setAllFeeds(prev => [...prev, ...feeds]);
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

  // 초기 피드 로드
  useEffect(() => {
    if (userType === "member") {
      loadMyFeeds(); // 내 피드는 전체 로드
      loadFeeds(0, true); // 다른 피드는 페이지네이션
    }
  }, [userType]);

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
      if (!notification.read) {
        const userId = 1; // 임시 사용자 ID
        await notificationApi.markAsRead(notification.id, userId);

        // 로컬 상태 업데이트
        setNotifications(notifications.map(n =>
          n.id === notification.id ? { ...n, read: true } : n
        ));
      }

      // 알림 팝업 닫기
      setShowNotifications(false);
      setSidebarExpanded(false);

      // 알림 타입에 따라 적절한 페이지로 이동
      if (notification.type === "like" || notification.type === "comment" || notification.type === "reply" || notification.type === "comment_like") {
        // 피드 관련 알림 - 내 게시글이면 홈으로, 아니면 피드 페이지로
        if (notification.feedId) {
          // 내 피드와 다른 피드 모두에서 검색
          const myFeed = myFeeds.find(f => f.id === notification.feedId);
          let otherFeed = allFeeds.find(f => f.id === notification.feedId);
          let feed = myFeed || otherFeed;

          // 댓글/답글/댓글좋아요 알림이면 댓글 ID 설정
          if ((notification.type === "comment" || notification.type === "reply" || notification.type === "comment_like") && notification.commentId) {
            setTargetCommentId(notification.commentId);
          } else {
            setTargetCommentId(null);
          }

          // 피드가 아직 로드되지 않은 경우 API에서 가져오기
          if (!feed) {
            try {
              const response = await feedApi.getFeedById(notification.feedId);
              // mapBackendFeedToFrontend와 동일한 변환 적용
              const backendFeed = response;
              let stats = {};
              try {
                stats = backendFeed.statsJson ? JSON.parse(backendFeed.statsJson) : {};
              } catch (e) {
                console.error('Failed to parse statsJson:', e);
              }
              const points = Math.min(Math.floor(backendFeed.duration / 5) * 5, 30);

              feed = {
                id: backendFeed.id,
                authorId: backendFeed.writerId,
                author: backendFeed.authorName,
                avatar: '',
                activity: backendFeed.activityType,
                duration: `${backendFeed.duration}분`,
                points: points,
                content: backendFeed.content,
                images: backendFeed.imageUrls || [],
                likes: backendFeed.likesCount || 0,
                comments: backendFeed.commentsCount || 0,
                time: getRelativeTime(backendFeed.createdAt),
                stats: stats,
                likedBy: [],
              };

              // allFeeds에 추가 (맨 앞에)
              setAllFeeds(prev => [feed!, ...prev]);
            } catch (error) {
              console.error('피드 로드 실패:', error);
              return;
            }
          }

          if (feed) {
            // 현재 사용자 ID로 내 피드인지 확인
            const currentUserId = parseInt(localStorage.getItem('userId') || '0');
            const isMyFeed = feed.authorId === currentUserId || myFeed !== undefined;

            if (isMyFeed) {
              // 내 피드면 홈으로 이동
              setSelectedNav("home");
            } else {
              // 다른 사람 피드면 피드 페이지로
              setSelectedNav("feed");
            }

            // 댓글/답글/댓글좋아요 알림이면 상세 다이얼로그 열기
            setSelectedFeed(feed);
            setShowFeedDetailInHome(true);
          }
        }
      } else if (notification.type === "chat") {
        // 채팅 알림 - 채팅 페이지로 이동
        if (userType === "doctor") {
          setSelectedNav("chat");
        } else {
          setShowChat(true);
        }
      } else if (notification.type === "appointment") {
        // 예약 알림 - 진료 페이지로 이동
        setSelectedNav("medical");
      } else if (notification.type === "challenge") {
        // 챌린지 알림 - 홈으로 이동
        setSelectedNav("home");
      }
    } catch (error) {
      console.error("알림 처리 실패:", error);
      toast.error("알림을 처리하는데 실패했습니다.");
    }
  };

  const navItems = userType === "member" ? memberNavItems : doctorNavItems;
  const getFeedImageIndex = (feedId: number) => feedImageIndexes[feedId] || 0;
  const setFeedImageIndex = (feedId: number, index: number) => setFeedImageIndexes(prev => ({ ...prev, [feedId]: index }));
  const handleLike = (feedId: number) => {
    const currentLikes = feedLikes[feedId] || [];
    const hasLiked = currentLikes.includes("김루핀");
    setFeedLikes({ ...feedLikes, [feedId]: hasLiked ? currentLikes.filter(name => name !== "김루핀") : [...currentLikes, "김루핀"] });
    setAllFeeds(allFeeds.map(feed => feed.id === feedId ? { ...feed, likes: hasLiked ? feed.likes - 1 : feed.likes + 1 } : feed));
  };
  const hasLiked = (feedId: number) => (feedLikes[feedId] || []).includes("김루핀");

  const handleEditFeed = (feed: Feed) => {
    setEditingFeed(feed);
    setShowEditDialog(true);
    setShowFeedDetailInHome(false);
  };

  const handleUpdateFeed = (
    feedId: number,
    images: string[],
    content: string,
    workoutType: string,
    _startImage: string | null,
    _endImage: string | null
  ) => {
    setMyFeeds(myFeeds.map(feed =>
      feed.id === feedId
        ? { ...feed, images, content, activity: workoutType, time: "방금 전", edited: true }
        : feed
    ));
    setAllFeeds(allFeeds.map(feed =>
      feed.id === feedId
        ? { ...feed, images, content, activity: workoutType, time: "방금 전", edited: true }
        : feed
    ));
    toast.success("피드가 수정되었습니다!");
  };

  const handleDeleteFeed = (feedId: number) => {
    setMyFeeds(myFeeds.filter(feed => feed.id !== feedId));
    setAllFeeds(allFeeds.filter(feed => feed.id !== feedId));
    toast.success("피드가 삭제되었습니다!");
  };

  const handleCreateFeed = (
    images: string[],
    content: string,
    workoutType: string,
    _startImage: string | null,
    _endImage: string | null
  ) => {
    const newFeed: Feed = {
      id: Date.now(),
      author: "김루핀",
      avatar: "김",
      time: "방금 전",
      activity: workoutType,
      duration: "30분",
      images,
      content,
      likes: 0,
      comments: 0,
      points: 10,
      stats: {},
      edited: false,
    };
    setMyFeeds([newFeed, ...myFeeds]);
    setAllFeeds([newFeed, ...allFeeds]);
    toast.success("피드가 작성되었습니다!");
  };

  if (userType === "doctor") {
    return (
      <div className="h-screen w-screen overflow-hidden relative">
        <AnimatedBackground variant="doctor" />
        <Sidebar expanded={sidebarExpanded} onExpandChange={setSidebarExpanded} navItems={navItems} selectedNav={selectedNav} onNavSelect={setSelectedNav} userType="doctor" />
        <div className={`h-full transition-all duration-300 ${sidebarExpanded ? 'ml-64' : 'ml-20'}`}>
          {selectedNav === "chat" && <DoctorChatPage />}
          {selectedNav === "profile" && <DoctorProfilePage onLogout={onLogout} />}
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen w-screen overflow-hidden relative">
      <AnimatedBackground variant="member" />
      <Sidebar expanded={sidebarExpanded || showNotifications} onExpandChange={(expanded) => !showNotifications && setSidebarExpanded(expanded)} navItems={navItems} selectedNav={selectedNav} onNavSelect={setSelectedNav} userType="member" profileImage={profileImage}>
        <div className="relative mb-2" onMouseEnter={(e) => e.stopPropagation()} onMouseLeave={(e) => e.stopPropagation()}>
          <button onClick={() => setShowNotifications(!showNotifications)} className="relative w-full flex items-center py-3 rounded-2xl hover:bg-white/30 transition-colors" style={{ paddingLeft: '10px' }}>
            <div className="relative flex-shrink-0">
              <Bell className="w-7 h-7 text-gray-700" />
              {notifications.filter(n => !n.read).length > 0 && <div className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full"></div>}
            </div>
            <span className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ml-6 ${(sidebarExpanded || showNotifications) ? 'opacity-100' : 'opacity-0 w-0'}`}>알림</span>
          </button>
          {showNotifications && <NotificationPopup notifications={notifications} onClose={(closeSidebar = true) => { setShowNotifications(false); if (closeSidebar) setSidebarExpanded(false); }} onNotificationClick={handleNotificationClick} />}
        </div>
      </Sidebar>

      <div className={`h-full transition-all duration-300 ${(sidebarExpanded || showNotifications) ? 'ml-64' : 'ml-20'}`}>
        {selectedNav === "home" && <HomeView challengeJoined={challengeJoined} handleJoinChallenge={() => { toast.success("응모가 완료되었습니다!"); setChallengeJoined(true); }}
          profileImage={profileImage} myFeeds={myFeeds} setSelectedFeed={setSelectedFeed} setFeedImageIndex={setFeedImageIndex} setShowFeedDetailInHome={setShowFeedDetailInHome}
          onCreateClick={() => setShowCreateDialog(true)} />}
        {selectedNav === "feed" && <FeedView allFeeds={allFeeds} searchQuery={searchQuery} setSearchQuery={setSearchQuery} showSearch={showSearch} setShowSearch={setShowSearch}
          getFeedImageIndex={getFeedImageIndex} setFeedImageIndex={setFeedImageIndex} hasLiked={hasLiked} handleLike={handleLike} feedContainerRef={feedContainerRef} scrollToFeedId={scrollToFeedId} setScrollToFeedId={setScrollToFeedId}
          loadMoreFeeds={loadMoreFeeds} hasMoreFeeds={hasMoreFeeds} isLoadingFeeds={isLoadingFeeds} />}
        {selectedNav === "ranking" && <RankingView userId={userId} profileImage={profileImage} />}
        {selectedNav === "medical" && <MedicalView setShowAppointment={setShowAppointment} setShowChat={setShowChat} setSelectedPrescription={setSelectedPrescription} />}
        {selectedNav === "create" && <CreatePage onCreatePost={(newFeed) => { setMyFeeds([newFeed, ...myFeeds]); }} />}
        {selectedNav === "profile" && <MemberProfilePage onLogout={onLogout} profileImage={profileImage} setProfileImage={setProfileImage} />}
      </div>

      <FeedDetailDialogHome feed={selectedFeed} open={showFeedDetailInHome && (selectedNav === "home" || selectedNav === "feed")}
        onOpenChange={() => { setShowFeedDetailInHome(false); setSelectedFeed(null); setTargetCommentId(null); }}
        currentImageIndex={selectedFeed ? getFeedImageIndex(selectedFeed.id) : 0}
        onPrevImage={() => selectedFeed && setFeedImageIndex(selectedFeed.id, Math.max(0, getFeedImageIndex(selectedFeed.id) - 1))}
        onNextImage={() => selectedFeed && setFeedImageIndex(selectedFeed.id, Math.min(selectedFeed.images.length - 1, getFeedImageIndex(selectedFeed.id) + 1))}
        onEdit={handleEditFeed}
        onDelete={handleDeleteFeed}
        targetCommentId={targetCommentId} />

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

      <PrescriptionModal prescription={selectedPrescription} open={!!selectedPrescription} onOpenChange={() => setSelectedPrescription(null)} onDownload={() => toast.success("처방전 PDF 다운로드를 시작합니다.")} />

      <ChatDialog open={showChat} onOpenChange={setShowChat} messages={medicalChatMessages} chatMessage={chatMessage} setChatMessage={setChatMessage}
        onSend={() => { if (chatMessage.trim()) { setMedicalChatMessages([...medicalChatMessages, { id: Date.now(), author: "김루핀", avatar: "김", content: chatMessage, time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }), isMine: true }]); setChatMessage(""); }}} />

      <PrescriptionFormDialog open={showPrescriptionForm} onOpenChange={(open) => { setShowPrescriptionForm(open); if (!open) setPrescriptionMember(null); }} member={prescriptionMember}
        onSubmit={() => { toast.success("처방전이 저장되었습니다."); setShowPrescriptionForm(false); setPrescriptionMember(null); }} />

      <AppointmentDialog open={showAppointment} onOpenChange={setShowAppointment} selectedDepartment={selectedDepartment} setSelectedDepartment={setSelectedDepartment}
        selectedDate={selectedDate} setSelectedDate={setSelectedDate} selectedTime={selectedTime} setSelectedTime={setSelectedTime} availableDates={availableDates}
        availableTimes={availableTimes} bookedTimes={bookedTimes} onConfirm={() => { toast.success("예약이 완료되었습니다!"); setShowAppointment(false); setSelectedDepartment(""); setSelectedDate(undefined); setSelectedTime(""); }} />
    </div>
  );
}
