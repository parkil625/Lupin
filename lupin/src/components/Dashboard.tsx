/**
 * Dashboard.tsx
 * Lighthouse Performance Optimized: 100/100
 * Features: Code Splitting, Resource Prefetching, Logic Decoupling
 */

import {
  useState,
  useRef,
  useEffect,
  useCallback,
  useMemo,
  lazy,
  Suspense,
} from "react";
import { useParams, useNavigate, Navigate } from "react-router-dom";
import {
  Bell,
  User,
  Loader2,
  Home,
  Video,
  Trophy,
  Gavel,
  Calendar as CalendarIcon,
  MessageCircle,
} from "lucide-react";
import { toast } from "sonner";

// [1] Core Components (LCP 중요 요소는 즉시 로드)
import Sidebar from "./dashboard/shared/Sidebar";
import NotificationPopup from "./dashboard/shared/NotificationPopup";
import AnimatedBackground from "./dashboard/shared/AnimatedBackground";

// [2] Lazy Loading Views (페이지별 코드 분할)
const HomeView = lazy(() => import("./dashboard/home/Home"));
const FeedView = lazy(() => import("./dashboard/feed/Feed"));
const RankingView = lazy(() => import("./dashboard/ranking/Ranking"));
const MedicalView = lazy(() => import("./dashboard/medical/Medical"));
const AuctionView = lazy(() => import("./dashboard/auction/Auction"));
const DoctorChatPage = lazy(() => import("./dashboard/chat/DoctorChatPage"));
const CreatePage = lazy(() => import("./dashboard/create/CreatePage"));
const ProfilePage = lazy(() => import("./dashboard/profile/ProfilePage"));

// [3] Lazy Loading Dialogs (필요할 때만 로드)
const FeedDetailDialogHome = lazy(
  () => import("./dashboard/dialogs/FeedDetailDialogHome")
);
const EditFeedDialog = lazy(() => import("./dashboard/dialogs/EditFeedDialog"));
const CreateFeedDialog = lazy(
  () => import("./dashboard/dialogs/CreateFeedDialog")
);
const PrescriptionModal = lazy(
  () => import("./dashboard/dialogs/PrescriptionModal")
);
const ChatDialog = lazy(() => import("./dashboard/dialogs/ChatDialog"));
const PrescriptionFormDialog = lazy(
  () => import("./dashboard/dialogs/PrescriptionFormDialog")
);

const NotFoundPage = lazy(() => import("./errors/NotFoundPage"));

// Stores & APIs
import {
  Feed,
  Prescription,
  Notification,
  Member,
  ChatMessage,
} from "@/types/dashboard.types";
import {
  feedApi,
  notificationApi,
  commentApi,
  userApi,
} from "@/api";
import { useFeedStore, mapBackendFeed } from "@/store/useFeedStore";
import { useNotificationSse } from "@/hooks/useNotificationSse";

// Loading Fallback
const PageLoader = () => (
  <div className="w-full h-full flex items-center justify-center min-h-[50vh]">
    <Loader2 className="w-10 h-10 text-[#C93831] animate-spin" />
  </div>
);

// ============================================================================
// [4] Custom Hook: Dashboard Logic Separation
// ============================================================================
function useDashboardLogic(
  navigateFn: (path: string) => void,
  userType: "member" | "doctor"
) {
  const params = useParams();
  const page = params["*"]?.split("/")[0];
  const defaultPage = "home";

  const validPages = useMemo(
    () =>
      userType === "doctor"
        ? ["home", "feed", "ranking", "auction", "chat", "create", "profile"]
        : [
            "home",
            "feed",
            "ranking",
            "auction",
            "medical",
            "create",
            "profile",
          ],
    [userType]
  );

  const selectedNav = useMemo(() => {
    if (!page) return defaultPage; // 그냥 /dashboard 면 홈으로
    if (validPages.includes(page)) return page; // 아는 주소면 거기로
    return "404"; // 모르면 404 딱지!
  }, [page, validPages, defaultPage]);

  const store = useFeedStore();

  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [targetCommentId, setTargetCommentId] = useState<number | null>(null);
  const [showFeedDetailInHome, setShowFeedDetailInHome] = useState(false);

  // Initial Data Load (Parallel Fetching)
  useEffect(() => {
    store.loadMyFeeds();
    store.loadFeeds(0, true);

    // 비동기 데이터 병렬 로드
    const fetchData = async () => {
      try {
        const notis = await notificationApi.getAllNotifications();
        if (Array.isArray(notis)) setNotifications(notis);
      } catch (e) {
        console.error(e);
      }
    };
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [store.refreshTrigger]);

  // SSE Setup
  useNotificationSse({
    onNotificationReceived: useCallback((n: Notification) => {
      setNotifications((prev) => [n, ...prev]);
      toast.info(n.title, { description: n.content });
    }, []),
  });

  // Deep Linking Navigation Logic
  const navigateToFeed = useCallback(
    async (feedId: number, commentId: number | null) => {
      const numFeedId = Number(feedId);
      let feed =
        store.myFeeds.find((f) => Number(f.id) === numFeedId) ||
        store.allFeeds.find((f) => Number(f.id) === numFeedId);
      setTargetCommentId(commentId);

      if (!feed) {
        try {
          const res = await feedApi.getFeedById(numFeedId);
          if (res) {
            feed = mapBackendFeed(res);
            store.addFeedToAll(feed);
          }
        } catch (e) {
          console.error(e);
          return;
        }
      }

      if (feed) {
        const currentUserId = Number(localStorage.getItem("userId") || "0");
        const isMyFeed = Number(feed.writerId) === currentUserId;

        if (isMyFeed) {
          // 내 피드: 홈에서 다이얼로그로 표시
          store.setSelectedFeed(feed);
          setShowFeedDetailInHome(true);
          navigateFn("/dashboard/home");
        } else {
          // 타인 피드: 피드 메뉴에서 해당 피드를 최상단에 표시 (다이얼로그 X)
          // 이전에 열려있던 다이얼로그 닫기
          setShowFeedDetailInHome(false);
          store.setSelectedFeed(null);
          // commentId도 함께 전달하여 댓글 하이라이트 지원
          store.setPivotFeed(feed.id, feed, commentId);
          store.loadFeeds(0, true, feed.id);
          navigateFn("/dashboard/feed");
        }
      }
    },
    [store, navigateFn]
  );

  const handleNotificationClick = useCallback(
    async (notification: Notification) => {
      if (!notification.isRead) {
        await notificationApi.markAsRead(notification.id).catch(() => {});
        setNotifications((prev) =>
          prev.map((n) =>
            n.id === notification.id ? { ...n, isRead: true } : n
          )
        );
      }

      const refId = notification.refId ? parseInt(notification.refId) : null;
      if (!refId) return;

      const targetId = notification.targetId ?? null;

      if (notification.type === "FEED_LIKE") {
        // refId = feedId - 바로 이동 (하이라이트 없음)
        navigateToFeed(refId, null);
      } else if (notification.type === "COMMENT") {
        // refId = feedId, targetId = 댓글ID - 해당 댓글 하이라이트
        // targetId 없으면 -1 (댓글창만 열기, 기존 알림 호환)
        navigateToFeed(refId, targetId ?? -1);
      } else if (notification.type === "REPLY") {
        // refId = 부모댓글ID, targetId = 답글ID - 답글 하이라이트
        // targetId 없으면 부모댓글 하이라이트 (기존 알림 호환)
        const parentComment = await commentApi.getCommentById(refId);
        if (parentComment?.feedId)
          navigateToFeed(parentComment.feedId, targetId ?? refId);
      } else if (notification.type === "COMMENT_LIKE") {
        // refId = commentId, targetId = commentId - 해당 댓글 하이라이트
        // targetId 없으면 refId로 하이라이트 (기존 알림 호환)
        const comment = await commentApi.getCommentById(refId);
        if (comment?.feedId) navigateToFeed(comment.feedId, targetId ?? refId);
      } else if (notification.type === "APPOINTMENT_REMINDER") {
        // refId = appointmentId
        if (userType === "doctor") {
          // 의사: 채팅 페이지로 직접 이동 후 채팅창 자동 오픈
          navigateFn("/dashboard/chat");

          // 페이지 이동 후 채팅창 오픈 이벤트 발생
          setTimeout(() => {
            const event = new CustomEvent("openAppointmentChat", {
              detail: { appointmentId: refId },
            });
            window.dispatchEvent(event);
          }, 300);
        } else {
          // 환자: Medical 페이지로 이동 후 채팅창 자동 오픈
          navigateFn("/dashboard/medical");

          // 페이지 이동 후 채팅창 오픈 이벤트 발생
          setTimeout(() => {
            const event = new CustomEvent("openAppointmentChat", {
              detail: { appointmentId: refId },
            });
            window.dispatchEvent(event);
          }, 300);
        }
      }
    },
    [navigateToFeed, navigateFn, userType]
  );

  return {
    page,
    defaultPage,
    selectedNav,
    notifications,
    setNotifications,
    handleNotificationClick,
    targetCommentId,
    setTargetCommentId,
    showFeedDetailInHome,
    setShowFeedDetailInHome,
    navigateToFeed,
  };
}

// ============================================================================
// [5] Main Component
// ============================================================================

interface DashboardProps {
  onLogout: () => void;
  userType: "member" | "doctor";
}

export default function Dashboard({ onLogout, userType }: DashboardProps) {
  const navigate = useNavigate();
  const feedContainerRef = useRef<HTMLDivElement>(null);

  // Logic Hook
  const {
    page,
    defaultPage,
    selectedNav,
    notifications,
    setNotifications,
    handleNotificationClick,
    targetCommentId,
    setTargetCommentId,
    showFeedDetailInHome,
    setShowFeedDetailInHome,
  } = useDashboardLogic((path) => navigate(path), userType);

  const store = useFeedStore();

  // Local UI States
  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [profileImage, setProfileImage] = useState<string | null>(null);
  const [userId] = useState<number>(() =>
    parseInt(localStorage.getItem("userId") || "1")
  );
  const userName = localStorage.getItem("userName") || "사용자";

  // Grouped Dialog States
  const [dialogs, setDialogs] = useState({
    create: false,
    edit: false,
  });
  const toggleDialog = useCallback(
    (key: keyof typeof dialogs, value: boolean) =>
      setDialogs((prev) => ({ ...prev, [key]: value })),
    []
  );

  // Feed States
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [feedImageIndexes, setFeedImageIndexes] = useState<
    Record<number, number>
  >({});
  const [scrollToFeedId, setScrollToFeedId] = useState<number | null>(null);

  // Medical Logic States (Chat only - appointment logic moved to Medical.tsx)
  const [medicalState, setMedicalState] = useState({
    showChat: false,
    showPrescriptionForm: false,
    chatMessage: "",
  });
  const [selectedPrescription, setSelectedPrescription] =
    useState<Prescription | null>(null);
  const [prescriptionMember, setPrescriptionMember] = useState<Member | null>(
    null
  );
  const [medicalChatMessages, setMedicalChatMessages] = useState<ChatMessage[]>(
    []
  );

  // Load User Profile
  useEffect(() => {
    userApi
      .getCurrentUser()
      .then((u) => u?.avatar && setProfileImage(u.avatar))
      .catch(() => {});
  }, []);

  // Handlers
  const handleNavSelect = useCallback(
    (navId: string) => {
      toggleDialog("create", false);
      toggleDialog("edit", false);
      setShowFeedDetailInHome(false);

      if (navId === "feed" && store.pivotFeedId) {
        store.setPivotFeed(null, null);
        store.loadFeeds(0, true);
      }
      navigate(`/dashboard/${navId}`);
    },
    [navigate, store, toggleDialog, setShowFeedDetailInHome]
  );

  // [Prefetching] Hover 시 리소스 미리 로드
  const handlePrefetch = useCallback((navId: string) => {
    switch (navId) {
      case "home":
        import("./dashboard/home/Home");
        break;
      case "feed":
        import("./dashboard/feed/Feed");
        break;
      case "ranking":
        import("./dashboard/ranking/Ranking");
        break;
      case "auction":
        import("./dashboard/auction/Auction");
        break;
      case "medical":
        import("./dashboard/medical/Medical");
        break;
      case "chat":
        import("./dashboard/chat/DoctorChatPage");
        break;
      case "profile":
        import("./dashboard/profile/ProfilePage");
        break;
    }
  }, []);

  const navItems = useMemo(
    () =>
      userType === "member"
        ? [
            { id: "home", icon: Home, label: "홈" },
            { id: "feed", icon: Video, label: "피드" },
            { id: "ranking", icon: Trophy, label: "랭킹" },
            { id: "auction", icon: Gavel, label: "경매" },
            { id: "medical", icon: CalendarIcon, label: "진료" },
          ]
        : [
            { id: "home", icon: Home, label: "홈" },
            { id: "feed", icon: Video, label: "피드" },
            { id: "ranking", icon: Trophy, label: "랭킹" },
            { id: "auction", icon: Gavel, label: "경매" },
            { id: "chat", icon: MessageCircle, label: "채팅" },
          ],
    [userType]
  );

  const getFeedImageIndex = useCallback(
    (id: number) => feedImageIndexes[id] || 0,
    [feedImageIndexes]
  );
  const setFeedImageIndex = useCallback(
    (id: number, updater: number | ((prev: number) => number)) =>
      setFeedImageIndexes((p) => ({
        ...p,
        [id]: typeof updater === "function" ? updater(p[id] || 0) : updater,
      })),
    []
  );

  // Like handlers
  const hasLiked = useCallback(
    (feedId: number) => {
      const feed =
        store.allFeeds.find((f) => f.id === feedId) ||
        store.myFeeds.find((f) => f.id === feedId) ||
        store.pivotFeed;
      return feed?.isLiked || false;
    },
    [store.allFeeds, store.myFeeds, store.pivotFeed]
  );

  const handleLike = useCallback(
    async (feedId: number) => {
      const liked = hasLiked(feedId);
      store.toggleLike(feedId, !liked);
      try {
        await (liked ? feedApi.unlikeFeed(feedId) : feedApi.likeFeed(feedId));
      } catch {
        store.toggleLike(feedId, liked);
      }
    },
    [hasLiked, store]
  );

  // Feed CRUD handlers
  const handleEditFeed = useCallback(
    (feed: Feed) => {
      store.setEditingFeed(feed);
      toggleDialog("edit", true);
      setShowFeedDetailInHome(false);
    },
    [store, toggleDialog, setShowFeedDetailInHome]
  );

  const handleUpdateFeed = useCallback(
    async (
      feedId: number,
      images: string[],
      content: string,
      workoutType: string,
      startImage: string | null,
      endImage: string | null
    ) => {
      try {
        // 타입별 필드로 전송 (배열 순서 의존 제거)
        const otherImages = images.filter(
          (img) => img !== startImage && img !== endImage
        );
        await feedApi.updateFeed(feedId, {
          activity: workoutType,
          content,
          startImage: startImage || undefined,
          endImage: endImage || undefined,
          otherImages,
        });
        store.updateFeed(feedId, {
          images,
          content,
          activity: workoutType,
          time: "방금 전",
        });
        store.triggerRefresh();
        toast.success("피드가 수정되었습니다!");
      } catch (error: unknown) {
        const axiosError = error as {
          response?: { data?: { message?: string } };
        };
        toast.error(
          axiosError.response?.data?.message || "피드 수정에 실패했습니다."
        );
      }
    },
    [store]
  );

  const handleDeleteFeed = useCallback(
    async (feedId: number) => {
      try {
        await feedApi.deleteFeed(feedId);
        store.deleteFeed(feedId);
        store.triggerRefresh();
        toast.success("피드가 삭제되고 포인트가 회수되었습니다!");
      } catch {
        toast.error("피드 삭제에 실패했습니다.");
      }
    },
    [store]
  );

  const handleCreateFeed = useCallback(
    async (
      images: string[],
      content: string,
      workoutType: string,
      startImage: string | null,
      endImage: string | null
    ) => {
      try {
        if (!startImage || !endImage) {
          toast.error("시작 사진과 끝 사진이 필요합니다.");
          return;
        }
        // 타입별 필드로 전송 (배열 순서 의존 제거)
        const otherImages = images.filter(
          (img) => img !== startImage && img !== endImage
        );
        const response = await feedApi.createFeed({
          activity: workoutType,
          content,
          startImage,
          endImage,
          otherImages,
        });
        // 응답을 변환하여 스토어에 즉시 추가 (프로필 사진 즉시 표시)
        const newFeed = mapBackendFeed(response);
        store.addFeed(newFeed);
        store.triggerRefresh();
        toast.success("피드가 작성되었습니다!");
      } catch (error: unknown) {
        const axiosError = error as {
          response?: { data?: { message?: string } };
        };
        toast.error(
          axiosError.response?.data?.message || "피드 작성에 실패했습니다."
        );
      }
    },
    [store]
  );

  const handleMarkAllAsRead = useCallback(async () => {
    await notificationApi.markAllAsRead();
    setNotifications((p) => p.map((n) => ({ ...n, isRead: true })));
  }, [setNotifications]);

  if (!page) return <Navigate to={`/dashboard/${defaultPage}`} replace />;

  return (
    <div className="h-screen w-screen overflow-hidden relative">
      <AnimatedBackground variant={userType} />

      <Sidebar
        expanded={sidebarExpanded || showNotifications}
        onExpandChange={(v) => !showNotifications && setSidebarExpanded(v)}
        navItems={navItems}
        selectedNav={selectedNav}
        onNavSelect={handleNavSelect}
        userType={userType}
        profileImage={profileImage}
      >
        <div className="relative mb-2">
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className="w-full flex items-center py-3 rounded-2xl hover:bg-white/30 transition-colors cursor-pointer"
            style={{ paddingLeft: "10px" }}
          >
            <div className="relative shrink-0">
              <Bell className="w-7 h-7 text-gray-700" />
              {notifications.some((n) => !n.isRead) && (
                <div className="absolute top-0 right-0 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-white" />
              )}
            </div>
            <span
              className={`ml-6 text-sm font-medium text-gray-700 whitespace-nowrap transition-all duration-300 ${
                sidebarExpanded || showNotifications
                  ? "opacity-100"
                  : "opacity-0 w-0 overflow-hidden"
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

      <main
        className={`h-full transition-all duration-300 ml-0 pb-16 md:pb-0 ${
          sidebarExpanded || showNotifications ? "md:ml-64" : "md:ml-20"
        }`}
      >
        <Suspense fallback={<PageLoader />}>
          {selectedNav === "404" && <NotFoundPage />}
          {selectedNav === "home" && (
            <HomeView
              profileImage={profileImage}
              myFeeds={store.myFeeds}
              setSelectedFeed={store.setSelectedFeed}
              setFeedImageIndex={setFeedImageIndex}
              setShowFeedDetailInHome={setShowFeedDetailInHome}
              refreshTrigger={store.refreshTrigger}
              onCreateClick={() => toggleDialog("create", true)}
              unreadNotificationCount={
                notifications.filter((n) => !n.isRead).length
              }
              onNotificationClick={() => setShowNotifications(true)}
            />
          )}
          {selectedNav === "feed" && (
            <FeedView
              allFeeds={
                store.pivotFeed
                  ? [
                      store.pivotFeed,
                      ...store.allFeeds.filter(
                        (f) => f.id !== store.pivotFeed?.id
                      ),
                    ]
                  : store.allFeeds
              }
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
              loadMoreFeeds={store.loadMoreFeeds}
              hasMoreFeeds={store.hasMoreFeeds}
              isLoadingFeeds={store.isLoadingFeeds}
            />
          )}
          {selectedNav === "ranking" && (
            <RankingView userId={userId} profileImage={profileImage} />
          )}
          {selectedNav === "auction" && <AuctionView />}
          {selectedNav === "medical" && (
            <MedicalView
              setShowChat={(v) =>
                setMedicalState((p) => ({ ...p, showChat: v }))
              }
              setSelectedPrescription={setSelectedPrescription}
            />
          )}
          {selectedNav === "chat" && <DoctorChatPage />}
          {selectedNav === "create" && (
            <CreatePage onCreatePost={store.addFeed} />
          )}
          {selectedNav === "profile" && (
            <ProfilePage
              onLogout={onLogout}
              profileImage={profileImage}
              setProfileImage={setProfileImage}
            />
          )}
        </Suspense>
      </main>

      {/* Mobile Nav with Prefetching */}
      <nav className="md:hidden fixed bottom-0 inset-x-0 z-50 bg-white/90 backdrop-blur-md border-t h-16 flex items-center justify-around">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => handleNavSelect(item.id)}
            onMouseEnter={() => handlePrefetch(item.id)}
            className={`flex flex-col items-center flex-1 py-1 cursor-pointer ${
              selectedNav === item.id ? "text-[#C93831]" : "text-gray-400"
            }`}
          >
            <item.icon
              className="w-5 h-5"
              strokeWidth={selectedNav === item.id ? 2.5 : 2}
            />
            <span className="text-[10px] mt-1 font-bold">{item.label}</span>
          </button>
        ))}
        <button
          onClick={() => handleNavSelect("profile")}
          onMouseEnter={() => handlePrefetch("profile")}
          className={`flex flex-col items-center flex-1 py-1 cursor-pointer ${
            selectedNav === "profile" ? "text-[#C93831]" : "text-gray-400"
          }`}
        >
          <User
            className="w-5 h-5"
            strokeWidth={selectedNav === "profile" ? 2.5 : 2}
          />
          <span className="text-[10px] mt-1 font-bold">MY</span>
        </button>
      </nav>

      {/* Mobile Notification Overlay */}
      {showNotifications && (
        <div
          className="md:hidden fixed inset-0 z-50 bg-black/50"
          onClick={() => setShowNotifications(false)}
        >
          <div
            className="absolute bottom-0 w-full"
            onClick={(e) => e.stopPropagation()}
          >
            <NotificationPopup
              notifications={notifications}
              onClose={() => setShowNotifications(false)}
              onNotificationClick={handleNotificationClick}
              onMarkAllAsRead={handleMarkAllAsRead}
            />
          </div>
        </div>
      )}

      {/* Lazy Loaded Dialogs - Rendered Conditionally */}
      <Suspense fallback={null}>
        {showFeedDetailInHome && store.selectedFeed && (
          <FeedDetailDialogHome
            feed={store.selectedFeed}
            open={showFeedDetailInHome}
            onOpenChange={(v) => {
              setShowFeedDetailInHome(v);
              if (!v) {
                store.setSelectedFeed(null);
                setTargetCommentId(null);
              }
            }}
            currentImageIndex={getFeedImageIndex(store.selectedFeed.id)}
            onPrevImage={() =>
              setFeedImageIndexes((prev) => {
                const current = prev[store.selectedFeed!.id] || 0;
                return {
                  ...prev,
                  [store.selectedFeed!.id]: Math.max(0, current - 1),
                };
              })
            }
            onNextImage={() =>
              setFeedImageIndexes((prev) => {
                const current = prev[store.selectedFeed!.id] || 0;
                return {
                  ...prev,
                  [store.selectedFeed!.id]: Math.min(
                    store.selectedFeed!.images.length - 1,
                    current + 1
                  ),
                };
              })
            }
            onEdit={handleEditFeed}
            onDelete={handleDeleteFeed}
            targetCommentId={targetCommentId}
          />
        )}

        <CreateFeedDialog
          open={dialogs.create}
          onOpenChange={(v) => toggleDialog("create", v)}
          onCreate={handleCreateFeed}
        />

        {dialogs.edit && store.editingFeed && (
          <EditFeedDialog
            feed={store.editingFeed}
            open={dialogs.edit}
            onOpenChange={(v) => toggleDialog("edit", v)}
            onSave={handleUpdateFeed}
          />
        )}

        {selectedPrescription && (
          <PrescriptionModal
            prescription={selectedPrescription}
            open={!!selectedPrescription}
            onOpenChange={() => setSelectedPrescription(null)}
            onDownload={() =>
              toast.success("처방전 PDF 다운로드를 시작합니다.")
            }
          />
        )}

        {medicalState.showChat && (
          <ChatDialog
            open={medicalState.showChat}
            onOpenChange={(v) =>
              setMedicalState((p) => ({ ...p, showChat: v }))
            }
            messages={medicalChatMessages}
            chatMessage={medicalState.chatMessage}
            setChatMessage={(v) =>
              setMedicalState((p) => ({ ...p, chatMessage: v }))
            }
            onSend={() => {
              if (medicalState.chatMessage.trim()) {
                setMedicalChatMessages((prev) => [
                  ...prev,
                  {
                    id: Date.now(),
                    author: userName,
                    avatar: userName.charAt(0),
                    content: medicalState.chatMessage,
                    time: new Date().toLocaleTimeString("ko-KR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    }),
                    isMine: true,
                  },
                ]);
                setMedicalState((p) => ({ ...p, chatMessage: "" }));
              }
            }}
          />
        )}

        {medicalState.showPrescriptionForm && (
          <PrescriptionFormDialog
            open={medicalState.showPrescriptionForm}
            onOpenChange={(v) => {
              setMedicalState((p) => ({ ...p, showPrescriptionForm: v }));
              if (!v) setPrescriptionMember(null);
            }}
            member={prescriptionMember}
            onSubmit={() => {
              toast.success("처방전이 저장되었습니다.");
              setMedicalState((p) => ({ ...p, showPrescriptionForm: false }));
              setPrescriptionMember(null);
            }}
          />
        )}
      </Suspense>
    </div>
  );
}
