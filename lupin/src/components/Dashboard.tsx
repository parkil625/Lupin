/**
 * Dashboard.tsx
 *
 * 메인 대시보드 라우터 컴포넌트
 * - 회원/의사 모드에 따른 페이지 라우팅
 * - 전역 상태 관리 (피드, 댓글, 알림, 처방전 등)
 * - 공통 레이아웃 (사이드바, 배경, 다이얼로그) 제공
 * - 각 페이지 컴포넌트의 Props 전달 및 이벤트 핸들링
 */

import { useState, useRef } from "react";
import { Bell } from "lucide-react";
import { toast } from "sonner";
import { Home, Video, Trophy, Calendar as CalendarIcon, PlusSquare, Users, MessageCircle } from "lucide-react";
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
import MemberDetailDialog from "./dashboard/dialogs/MemberDetailDialog";
import EditFeedDialog from "./dashboard/dialogs/EditFeedDialog";
import CreateFeedDialog from "./dashboard/dialogs/CreateFeedDialog";
import MembersPage from "./dashboard/members/MembersPage";
import AppointmentsPage from "./dashboard/appointments/AppointmentsPage";
import DoctorChatPage from "./dashboard/chat/DoctorChatPage";
import DoctorProfilePage from "./dashboard/profile/DoctorProfilePage";
import CreatePage from "./dashboard/create/CreatePage";
import MemberProfilePage from "./dashboard/profile/MemberProfilePage";
import { Feed, Prescription, Notification, Member, ChatMessage } from "@/types/dashboard.types";
import { myFeeds as initialMyFeeds, allFeeds as initialAllFeeds } from "@/mockdata/feeds";
import { initialNotifications } from "@/mockdata/notifications";

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
  { id: "members", icon: Users, label: "회원 목록" },
  { id: "appointments", icon: CalendarIcon, label: "예약 관리" },
  { id: "chat", icon: MessageCircle, label: "채팅" }
];

const availableDates = [new Date(2024, 10, 15), new Date(2024, 10, 16), new Date(2024, 10, 18), new Date(2024, 10, 20), new Date(2024, 10, 22), new Date(2024, 10, 25), new Date(2024, 10, 27), new Date(2024, 10, 29)];
const availableTimes = ["09:00", "10:00", "11:00", "14:00", "15:00", "16:00"];
const bookedTimes = ["10:00", "15:00"];

export default function Dashboard({ onLogout, userType }: DashboardProps) {
  const [selectedNav, setSelectedNav] = useState(userType === "doctor" ? "members" : "home");
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
  const [showAppointment, setShowAppointment] = useState(false);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>();
  const [selectedTime, setSelectedTime] = useState("");
  const [showChat, setShowChat] = useState(false);
  const [chatMessage, setChatMessage] = useState("");
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [showPrescriptionForm, setShowPrescriptionForm] = useState(false);
  const [prescriptionMember, setPrescriptionMember] = useState<Member | null>(null);
  const [feedLikes, setFeedLikes] = useState<{[key: number]: string[]}>({});
  const [notifications] = useState<Notification[]>(initialNotifications);
  const [myFeeds, setMyFeeds] = useState<Feed[]>(initialMyFeeds);
  const [allFeeds, setAllFeeds] = useState<Feed[]>(initialAllFeeds);
  const [medicalChatMessages, setMedicalChatMessages] = useState<ChatMessage[]>([]);
  const [editingFeed, setEditingFeed] = useState<Feed | null>(null);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [showCreateDialog, setShowCreateDialog] = useState(false);

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
          {selectedNav === "members" && <MembersPage onMemberSelect={setSelectedMember} />}
          {selectedNav === "appointments" && <AppointmentsPage onChatClick={() => setShowChat(true)} />}
          {selectedNav === "chat" && <DoctorChatPage />}
          {selectedNav === "profile" && <DoctorProfilePage onLogout={onLogout} />}
        </div>
        <MemberDetailDialog open={!!selectedMember} onOpenChange={() => setSelectedMember(null)} member={selectedMember}
          onStartChat={() => { setSelectedNav("chat"); setSelectedMember(null); }}
          onWritePrescription={(p) => { setPrescriptionMember(p); setShowPrescriptionForm(true); setSelectedMember(null); }} />
      </div>
    );
  }

  return (
    <div className="h-screen w-screen overflow-hidden relative">
      <AnimatedBackground variant="member" />
      <Sidebar expanded={sidebarExpanded || showNotifications} onExpandChange={(expanded) => !showNotifications && setSidebarExpanded(expanded)} navItems={navItems} selectedNav={selectedNav} onNavSelect={setSelectedNav} userType="member" profileImage={profileImage}>
        <div className="relative mb-2" onMouseEnter={(e) => e.stopPropagation()} onMouseLeave={(e) => e.stopPropagation()}>
          <button onClick={() => setShowNotifications(!showNotifications)} className="relative w-full flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-white/30 transition-all">
            <div className="relative w-7 h-7 flex items-center justify-center flex-shrink-0">
              <Bell className="w-7 h-7 text-gray-700" />
              {notifications.filter(n => !n.read).length > 0 && <div className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full"></div>}
            </div>
            <span className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ${(sidebarExpanded || showNotifications) ? 'opacity-100' : 'opacity-0 w-0'}`}>알림</span>
          </button>
          {showNotifications && <NotificationPopup notifications={notifications} onClose={(closeSidebar = true) => { setShowNotifications(false); if (closeSidebar) setSidebarExpanded(false); }} />}
        </div>
      </Sidebar>

      <div className={`h-full transition-all duration-300 ${(sidebarExpanded || showNotifications) ? 'ml-64' : 'ml-20'}`}>
        {selectedNav === "home" && <HomeView challengeJoined={challengeJoined} handleJoinChallenge={() => { toast.success("응모가 완료되었습니다!"); setChallengeJoined(true); }}
          profileImage={profileImage} myFeeds={myFeeds} setSelectedFeed={setSelectedFeed} setFeedImageIndex={setFeedImageIndex} setShowFeedDetailInHome={setShowFeedDetailInHome}
          onCreateClick={() => setShowCreateDialog(true)} />}
        {selectedNav === "feed" && <FeedView allFeeds={allFeeds} searchQuery={searchQuery} setSearchQuery={setSearchQuery} showSearch={showSearch} setShowSearch={setShowSearch}
          getFeedImageIndex={getFeedImageIndex} setFeedImageIndex={setFeedImageIndex} hasLiked={hasLiked} handleLike={handleLike} feedContainerRef={feedContainerRef} />}
        {selectedNav === "ranking" && <RankingView />}
        {selectedNav === "medical" && <MedicalView setShowAppointment={setShowAppointment} setShowChat={setShowChat} setSelectedPrescription={setSelectedPrescription} />}
        {selectedNav === "create" && <CreatePage onCreatePost={(newFeed) => { setMyFeeds([newFeed, ...myFeeds]); }} />}
        {selectedNav === "profile" && <MemberProfilePage onLogout={onLogout} profileImage={profileImage} setProfileImage={setProfileImage} />}
      </div>

      <FeedDetailDialogHome feed={selectedFeed} open={showFeedDetailInHome && selectedNav === "home"}
        onOpenChange={() => { setShowFeedDetailInHome(false); setSelectedFeed(null); }}
        currentImageIndex={selectedFeed ? getFeedImageIndex(selectedFeed.id) : 0}
        onPrevImage={() => selectedFeed && setFeedImageIndex(selectedFeed.id, Math.max(0, getFeedImageIndex(selectedFeed.id) - 1))}
        onNextImage={() => selectedFeed && setFeedImageIndex(selectedFeed.id, Math.min(selectedFeed.images.length - 1, getFeedImageIndex(selectedFeed.id) + 1))}
        onEdit={handleEditFeed}
        onDelete={handleDeleteFeed} />

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
