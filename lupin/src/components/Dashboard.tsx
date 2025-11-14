import { useState, useEffect, useRef } from "react";
import { Button } from "./ui/button";
import { Card } from "./ui/card";
import { Badge } from "./ui/badge";
import { Avatar, AvatarFallback } from "./ui/avatar";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Textarea } from "./ui/textarea";
import { ScrollArea } from "./ui/scroll-area";
import { Calendar } from "./ui/calendar";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "./ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "./ui/select";
import {
  Dumbbell,
  Trophy,
  Heart,
  MessageCircle,
  Home,
  Video,
  PlusSquare,
  Sparkles,
  Award,
  Search,
  X,
  Calendar as CalendarIcon,
  Flame,
  Target,
  Zap,
  TrendingUp,
  Users,
  Clock,
  FileText,
  Upload,
  Image as ImageIcon,
  CheckCircle,
  XCircle,
  User,
  Edit,
  Send,
  ChevronLeft,
  ChevronRight,
  Bell,
  Download,
  Camera,
  Activity,
  Phone,
  Mail,
  MapPin,
  Stethoscope,
  Reply
} from "lucide-react";
import { ImageWithFallback } from "./figma/ImageWithFallback";
import { toast } from "sonner@2.0.3";
import logoImage from "figma:asset/35ea831620257399a6a4dc008549dcececac4b93.png";

interface DashboardProps {
  onLogout: () => void;
  userType: "patient" | "doctor";
}

interface Feed {
  id: number;
  author: string;
  avatar: string;
  activity: string;
  duration: string;
  points: number;
  content: string;
  images: string[];
  likes: number;
  comments: number;
  time: string;
  stats: { [key: string]: string };
  isMine?: boolean;
  currentImageIndex?: number;
  likedBy?: string[];
}

interface Comment {
  id: number;
  author: string;
  avatar: string;
  content: string;
  time: string;
  parentId?: number;
  replies?: Comment[];
}

interface Prescription {
  id: number;
  name: string;
  date: string;
  doctor: string;
  medicines: string[];
  diagnosis: string;
  instructions: string;
}

interface Notification {
  id: number;
  type: "challenge" | "appointment" | "like" | "comment";
  title: string;
  content: string;
  time: string;
  read: boolean;
}

interface Patient {
  id: number;
  name: string;
  avatar: string;
  age: number;
  gender: string;
  lastVisit: string;
  condition: string;
  status: "waiting" | "in-progress" | "completed";
}

interface Appointment {
  id: number;
  patientName: string;
  patientAvatar: string;
  department: string;
  date: string;
  time: string;
  status: "scheduled" | "completed" | "cancelled";
  reason: string;
}

interface ChatMessage {
  id: number;
  author: string;
  avatar: string;
  content: string;
  time: string;
  isMine: boolean;
}

export default function Dashboard({ onLogout, userType }: DashboardProps) {
  const [selectedNav, setSelectedNav] = useState(userType === "doctor" ? "patients" : "home");
  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [selectedFeed, setSelectedFeed] = useState<Feed | null>(null);
  const [showCommentsInReels, setShowCommentsInReels] = useState(false);
  const [showFeedDetailInHome, setShowFeedDetailInHome] = useState(false);
  const feedContainerRef = useRef<HTMLDivElement>(null);
  const [challengeJoined, setChallengeJoined] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  // Feed image indexes - each feed has its own index
  const [feedImageIndexes, setFeedImageIndexes] = useState<{[key: number]: number}>({});

  // Prescription detail
  const [selectedPrescription, setSelectedPrescription] = useState<Prescription | null>(null);

  // Create Post States
  const [postImages, setPostImages] = useState<string[]>([]);
  const [postContent, setPostContent] = useState("");
  const [isWorkoutVerified, setIsWorkoutVerified] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Profile edit
  const [height, setHeight] = useState("175");
  const [weight, setWeight] = useState("70");
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [profileImage, setProfileImage] = useState<string | null>(null);
  const profileImageInputRef = useRef<HTMLInputElement>(null);

  // Medical appointment
  const [showAppointment, setShowAppointment] = useState(false);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>();
  const [selectedTime, setSelectedTime] = useState("");
  const [showChat, setShowChat] = useState(false);
  const [chatMessage, setChatMessage] = useState("");

  // Doctor - selected patient
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [selectedChatPatient, setSelectedChatPatient] = useState<Patient | null>(null);
  const [showPrescriptionForm, setShowPrescriptionForm] = useState(false);
  const [prescriptionPatient, setPrescriptionPatient] = useState<Patient | null>(null);
  
  // Chat messages
  const [chatMessages, setChatMessages] = useState<{[key: number]: ChatMessage[]}>({});
  const [medicalChatMessages, setMedicalChatMessages] = useState<ChatMessage[]>([
    { id: 1, author: "김의사", avatar: "의", content: "안녕하세요. 어떤 증상으로 방문하셨나요?", time: "오후 3:00", isMine: false },
    { id: 2, author: "김루핀", avatar: "김", content: "감기 증상이 있어서요", time: "오후 3:02", isMine: true }
  ]);

  // Comments and likes
  const [feedComments, setFeedComments] = useState<{[key: number]: Comment[]}>({});
  const [feedLikes, setFeedLikes] = useState<{[key: number]: string[]}>({});
  const [newComment, setNewComment] = useState("");
  const [replyingTo, setReplyingTo] = useState<number | null>(null);

  // Notifications
  const [notifications, setNotifications] = useState<Notification[]>([
    { id: 1, type: "challenge", title: "웰빙 챌린지 시작!", content: "오늘 오후 6시에 새로운 챌린지가 시작됩니다.", time: "1시간 전", read: false },
    { id: 2, type: "like", title: "이철수님이 좋아요를 눌렀습니다", content: "스쿼트 100kg 달성! 💪 게시물", time: "3시간 전", read: false },
    { id: 3, type: "comment", title: "박영희님이 댓글을 남겼습니다", content: "대단해요! 👍", time: "5시간 전", read: true },
    { id: 4, type: "appointment", title: "진료 예약 확인", content: "11월 15일 오후 3시 내과 상담이 예정되어 있습니다.", time: "1일 전", read: true }
  ]);

  const [myFeeds, setMyFeeds] = useState<Feed[]>([
    {
      id: 1,
      author: "김루핀",
      avatar: "김",
      activity: "헬스 운동",
      duration: "60분",
      points: 30,
      content: "오늘 스쿼트 100kg 달성! 💪 꾸준히 해온 결과가 드디어 나타나네요. 작년에는 80kg도 힘들었는데 정말 뿌듯합니다!",
      images: [
        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800"
      ],
      likes: 45,
      comments: 8,
      time: "3시간 전",
      stats: { strength: "+15", endurance: "+10" },
      isMine: true,
      likedBy: []
    },
    {
      id: 2,
      author: "김루핀",
      avatar: "김",
      activity: "러닝",
      duration: "30분",
      points: 20,
      content: "아침 러닝 5km 완주! ☀️ 날씨가 좋아서 기분도 최고입니다.",
      images: [
        "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800",
        "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800"
      ],
      likes: 32,
      comments: 5,
      time: "1일 전",
      stats: { cardio: "+20", calories: "320kcal" },
      isMine: true,
      likedBy: []
    },
    {
      id: 3,
      author: "김루핀",
      avatar: "김",
      activity: "요가",
      duration: "45분",
      points: 20,
      content: "요가로 하루 시작 🧘‍♀️ 몸과 마음이 한결 가벼워진 느낌!",
      images: ["https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800"],
      likes: 28,
      comments: 4,
      time: "2일 전",
      stats: { flexibility: "+25", mindfulness: "+30" },
      isMine: true,
      likedBy: []
    },
    {
      id: 4,
      author: "김루핀",
      avatar: "김",
      activity: "수영",
      duration: "40분",
      points: 25,
      content: "자유형 1km 달성! 🏊‍♂️",
      images: ["https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800"],
      likes: 20,
      comments: 3,
      time: "3일 전",
      stats: { cardio: "+20" },
      isMine: true,
      likedBy: []
    },
    {
      id: 5,
      author: "김루핀",
      avatar: "김",
      activity: "필라테스",
      duration: "50분",
      points: 25,
      content: "코어 운동 집중! 💪",
      images: ["https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800"],
      likes: 18,
      comments: 2,
      time: "4일 전",
      stats: { core: "+30" },
      isMine: true,
      likedBy: []
    }
  ]);

  const [allFeeds, setAllFeeds] = useState<Feed[]>([
    {
      id: 5,
      author: "이철수",
      avatar: "이",
      activity: "헬스 운동",
      duration: "60분",
      points: 30,
      content: "오늘도 데드리프트 120kg 성공! 💪 작년 이맘때는 80kg도 힘들었는데... 꾸준함이 정말 중요하다는 걸 느낍니다. 모두 파이팅!",
      images: [
        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
        "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=800"
      ],
      likes: 124,
      comments: 23,
      time: "2시간 전",
      stats: { strength: "+15", endurance: "+10" },
      likedBy: []
    },
    {
      id: 6,
      author: "박영희",
      avatar: "박",
      activity: "아침 러닝",
      duration: "45분",
      points: 25,
      content: "한강 러닝 10km 완주 ☀️ 아침 공기가 정말 상쾌했어요. 오늘 하루도 화이팅!",
      images: [
        "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800",
        "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800"
      ],
      likes: 89,
      comments: 15,
      time: "5시간 전",
      stats: { cardio: "+20", calories: "520kcal" },
      likedBy: []
    },
    {
      id: 7,
      author: "최민수",
      avatar: "최",
      activity: "요가 클래스",
      duration: "50분",
      points: 20,
      content: "빈야사 플로우 클래스 완료! 🧘‍♂️ 몸과 마음이 한결 가벼워진 느낌. 스트레스 해소에 최고예요.",
      images: ["https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800"],
      likes: 67,
      comments: 12,
      time: "1일 전",
      stats: { flexibility: "+25", mindfulness: "+30" },
      likedBy: []
    }
  ]);

  // Initialize comments
  useEffect(() => {
    const initialComments = {
      5: [
        { id: 1, author: "박영희", avatar: "박", content: "대단해요! 👍", time: "1시간 전", replies: [] },
        { id: 2, author: "이철수", avatar: "이", content: "저도 열심히 해야겠어요", time: "30분 전", replies: [] },
        { id: 3, author: "최민수", avatar: "최", content: "응원합니다! 💪", time: "10분 전", replies: [] }
      ],
      6: [
        { id: 4, author: "김루핀", avatar: "김", content: "멋져요!", time: "2시간 전", replies: [] }
      ],
      7: [
        { id: 5, author: "정수진", avatar: "정", content: "저도 요가 시작해볼까요?", time: "1일 전", replies: [] }
      ]
    };
    setFeedComments(initialComments);
  }, []);

  // Initialize chat messages for doctor
  useEffect(() => {
    if (userType === "doctor") {
      const initialChats: {[key: number]: ChatMessage[]} = {
        1: [
          { id: 1, author: "김루핀", avatar: "김", content: "안녕하세요 선생님", time: "오후 3:00", isMine: false },
          { id: 2, author: "김의사", avatar: "의", content: "네, 안녕하세요. 무엇을 도와드릴까요?", time: "오후 3:02", isMine: true }
        ],
        2: [
          { id: 1, author: "이철수", avatar: "이", content: "혈압약 처방 부탁드립니다", time: "오전 10:00", isMine: false }
        ],
        3: [
          { id: 1, author: "박영희", avatar: "박", content: "감기약 받았습니다. 감사합니다", time: "오후 2:00", isMine: false }
        ],
        4: [
          { id: 1, author: "최민수", avatar: "최", content: "당뇨 관리 잘하고 있습니다", time: "오전 11:00", isMine: false }
        ]
      };
      setChatMessages(initialChats);
    }
  }, [userType]);

  const prescriptions: Prescription[] = [
    { 
      id: 1, 
      name: "감기약 처방", 
      date: "11월 10일", 
      doctor: "이의사",
      medicines: ["타이레놀 500mg", "콧물약", "기침약"],
      diagnosis: "급성 상기도 감염",
      instructions: "하루 3회, 식후 30분에 복용하세요. 충분한 휴식과 수분 섭취가 필요합니다."
    },
    { 
      id: 2, 
      name: "소화제 처방", 
      date: "10월 28일", 
      doctor: "최의사",
      medicines: ["소화제", "제산제"],
      diagnosis: "소화불량",
      instructions: "하루 2회, 식후에 복용하세요."
    },
    { 
      id: 3, 
      name: "진통제 처방", 
      date: "10월 15일", 
      doctor: "김의사",
      medicines: ["이부프로펜 200mg"],
      diagnosis: "근육통",
      instructions: "통증이 있을 때 4-6시간 간격으로 복용하세요."
    },
    { 
      id: 4, 
      name: "알레르기약", 
      date: "10월 1일", 
      doctor: "박의사",
      medicines: ["항히스타민제"],
      diagnosis: "알레르기성 비염",
      instructions: "하루 1회, 취침 전 복용하세요."
    }
  ];

  // Doctor Data
  const [patients] = useState<Patient[]>([
    { id: 1, name: "김루핀", avatar: "김", age: 32, gender: "남", lastVisit: "2024-11-10", condition: "정기 검진", status: "waiting" },
    { id: 2, name: "이철수", avatar: "이", age: 45, gender: "남", lastVisit: "2024-11-09", condition: "고혈압", status: "in-progress" },
    { id: 3, name: "박영희", avatar: "박", age: 28, gender: "여", lastVisit: "2024-11-08", condition: "감기", status: "completed" },
    { id: 4, name: "최민수", avatar: "최", age: 38, gender: "남", lastVisit: "2024-11-07", condition: "당뇨 관리", status: "completed" },
    { id: 5, name: "정수진", avatar: "정", age: 35, gender: "여", lastVisit: "2024-11-06", condition: "알레르기", status: "completed" }
  ]);

  const [appointments] = useState<Appointment[]>([
    { id: 1, patientName: "김루핀", patientAvatar: "김", department: "내과", date: "11월 15일", time: "오후 3시", status: "scheduled", reason: "정기 검진" },
    { id: 2, patientName: "이철수", patientAvatar: "이", department: "내과", date: "11월 14일", time: "오전 10시", status: "scheduled", reason: "고혈압 상담" },
    { id: 3, patientName: "박영희", patientAvatar: "박", department: "내과", date: "11월 13일", time: "오후 2시", status: "completed", reason: "���기 치료" },
    { id: 4, patientName: "최민수", patientAvatar: "최", department: "내과", date: "11월 12일", time: "오전 11시", status: "completed", reason: "당뇨 관리" }
  ]);

  const navItems = userType === "patient" ? [
    { id: "home", icon: Home, label: "홈" },
    { id: "reels", icon: Video, label: "피드" },
    { id: "ranking", icon: Trophy, label: "랭킹" },
    { id: "medical", icon: CalendarIcon, label: "진료" },
    { id: "create", icon: PlusSquare, label: "만들기" }
  ] : [
    { id: "patients", icon: Users, label: "환자 목록" },
    { id: "appointments", icon: CalendarIcon, label: "예약 관리" },
    { id: "chat", icon: MessageCircle, label: "채팅" }
  ];

  const handleCreatePost = () => {
    if (postImages.length === 0 || !postContent) {
      toast.error("이미지와 내용을 모두 입력해주세요!");
      return;
    }

    if (!isWorkoutVerified) {
      toast.error("운동 인증이 필요합니다!");
      return;
    }

    const newFeed: Feed = {
      id: Date.now(),
      author: "김루핀",
      avatar: "김",
      activity: "운동",
      duration: "30분",
      points: 20,
      content: postContent,
      images: postImages,
      likes: 0,
      comments: 0,
      time: "방금 전",
      stats: { workout: "+20" },
      isMine: true,
      likedBy: []
    };

    setMyFeeds([newFeed, ...myFeeds]);
    toast.success("피드가 작성되었습니다!");
    setPostImages([]);
    setPostContent("");
    setIsWorkoutVerified(false);
  };

  const handleJoinChallenge = () => {
    toast.success("응모가 완료되었습니다!");
    setChallengeJoined(true);
  };

  const downloadPrescriptionPDF = (prescription: Prescription) => {
    toast.success("처방전 PDF 다운로드를 시작합니다.");
    console.log("Downloading PDF for:", prescription);
  };

  const handleProfileImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setProfileImage(event.target.result as string);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const getFeedImageIndex = (feedId: number) => {
    return feedImageIndexes[feedId] || 0;
  };

  const setFeedImageIndex = (feedId: number, index: number) => {
    setFeedImageIndexes(prev => ({ ...prev, [feedId]: index }));
  };

  const handleLike = (feedId: number) => {
    const currentLikes = feedLikes[feedId] || [];
    const hasLiked = currentLikes.includes("김루핀");
    
    if (hasLiked) {
      setFeedLikes({ ...feedLikes, [feedId]: currentLikes.filter(name => name !== "김루핀") });
    } else {
      setFeedLikes({ ...feedLikes, [feedId]: [...currentLikes, "김루핀"] });
    }
    
    // Update feed likes count
    setAllFeeds(allFeeds.map(feed => {
      if (feed.id === feedId) {
        return { ...feed, likes: hasLiked ? feed.likes - 1 : feed.likes + 1 };
      }
      return feed;
    }));
  };

  const hasLiked = (feedId: number) => {
    const currentLikes = feedLikes[feedId] || [];
    return currentLikes.includes("김루핀");
  };

  const handleAddComment = (feedId: number) => {
    if (!newComment.trim()) return;
    
    const comment: Comment = {
      id: Date.now(),
      author: "김루핀",
      avatar: "김",
      content: newComment,
      time: "방금 전",
      parentId: replyingTo || undefined,
      replies: []
    };

    const currentComments = feedComments[feedId] || [];
    
    if (replyingTo) {
      // Add as reply
      const updatedComments = currentComments.map(c => {
        if (c.id === replyingTo) {
          return { ...c, replies: [...(c.replies || []), comment] };
        }
        return c;
      });
      setFeedComments({ ...feedComments, [feedId]: updatedComments });
    } else {
      // Add as top-level comment
      setFeedComments({ ...feedComments, [feedId]: [...currentComments, comment] });
    }
    
    // Update comment count
    setAllFeeds(allFeeds.map(feed => {
      if (feed.id === feedId) {
        return { ...feed, comments: feed.comments + 1 };
      }
      return feed;
    }));
    
    setNewComment("");
    setReplyingTo(null);
  };

  const handleSendDoctorChat = () => {
    if (!chatMessage.trim() || !selectedChatPatient) return;
    
    const newMsg: ChatMessage = {
      id: Date.now(),
      author: "김의사",
      avatar: "의",
      content: chatMessage,
      time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
      isMine: true
    };
    
    const patientChats = chatMessages[selectedChatPatient.id] || [];
    setChatMessages({ ...chatMessages, [selectedChatPatient.id]: [...patientChats, newMsg] });
    setChatMessage("");
  };

  const handleSendMedicalChat = () => {
    if (!chatMessage.trim()) return;
    
    const newMsg: ChatMessage = {
      id: Date.now(),
      author: "김루핀",
      avatar: "김",
      content: chatMessage,
      time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
      isMine: true
    };
    
    setMedicalChatMessages([...medicalChatMessages, newMsg]);
    setChatMessage("");
  };

  // Verify workout when image is uploaded
  useEffect(() => {
    if (postImages.length > 0) {
      setTimeout(() => {
        setIsWorkoutVerified(true);
      }, 1000);
    } else {
      setIsWorkoutVerified(false);
    }
  }, [postImages]);

  // Drag and Drop handlers
  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = Array.from(e.dataTransfer.files);
    files.forEach(file => {
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (event) => {
          if (event.target?.result) {
            setPostImages(prev => [...prev, event.target!.result as string]);
          }
        };
        reader.readAsDataURL(file);
      }
    });
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    files.forEach(file => {
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (event) => {
          if (event.target?.result) {
            setPostImages(prev => [...prev, event.target!.result as string]);
          }
        };
        reader.readAsDataURL(file);
      }
    });
  };

  // Snap scroll effect
  useEffect(() => {
    if (selectedNav === "reels" && feedContainerRef.current) {
      const container = feedContainerRef.current;
      let scrollTimeout: NodeJS.Timeout;

      const handleScroll = () => {
        clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(() => {
          const scrollTop = container.scrollTop;
          const itemHeight = container.clientHeight;
          const targetIndex = Math.round(scrollTop / itemHeight);
          container.scrollTo({
            top: targetIndex * itemHeight,
            behavior: 'smooth'
          });
        }, 150);
      };

      container.addEventListener('scroll', handleScroll);
      return () => {
        container.removeEventListener('scroll', handleScroll);
        clearTimeout(scrollTimeout);
      };
    }
  }, [selectedNav]);

  const currentMonth = new Date().getMonth() + 1;

  const availableDates = [
    new Date(2024, 10, 15),
    new Date(2024, 10, 16),
    new Date(2024, 10, 18),
    new Date(2024, 10, 20),
    new Date(2024, 10, 22),
    new Date(2024, 10, 25),
    new Date(2024, 10, 27),
    new Date(2024, 10, 29)
  ];

  const availableTimes = ["09:00", "10:00", "11:00", "14:00", "15:00", "16:00"];
  const bookedTimes = ["10:00", "15:00"];

  // Doctor view
  if (userType === "doctor") {
    return (
      <div className="h-screen w-screen overflow-hidden relative">
        <div className="fixed inset-0 -z-10 bg-gradient-to-br from-purple-100 via-pink-50 to-blue-100">
          <div className="absolute top-20 left-10 w-96 h-96 bg-gradient-to-br from-purple-300 to-pink-300 rounded-full blur-3xl opacity-40 animate-float"></div>
          <div className="absolute bottom-20 right-10 w-96 h-96 bg-gradient-to-br from-blue-300 to-cyan-300 rounded-full blur-3xl opacity-40 animate-float-delayed"></div>
          <div className="absolute top-1/2 left-1/3 w-80 h-80 bg-gradient-to-br from-yellow-200 to-orange-300 rounded-full blur-3xl opacity-30 animate-pulse"></div>
          <div className="absolute bottom-1/3 right-1/4 w-72 h-72 bg-gradient-to-br from-green-200 to-emerald-300 rounded-full blur-3xl opacity-30 animate-float"></div>
        </div>

        {/* Sidebar */}
        <div
          className={`fixed left-0 top-0 h-full z-50 transition-all duration-300 ${
            sidebarExpanded ? 'w-64' : 'w-20'
          }`}
          onMouseEnter={() => setSidebarExpanded(true)}
          onMouseLeave={() => setSidebarExpanded(false)}
        >
          <div className="absolute inset-0 backdrop-blur-3xl bg-white/40 border-r border-white/60 shadow-2xl"></div>
          
          <div className="relative h-full flex flex-col p-4">
            {/* Logo */}
            <div className="mb-8 flex items-center justify-center">
              <img src={logoImage} alt="Lupin" className="h-16 w-16 object-contain" />
            </div>

            {/* Navigation */}
            <nav className="flex-1 space-y-2">
              {navItems.map((item) => (
                <button
                  key={item.id}
                  onClick={() => setSelectedNav(item.id)}
                  className={`w-full flex items-center gap-4 px-3 py-3 rounded-2xl transition-all duration-200 relative ${
                    selectedNav === item.id ? '' : 'hover:bg-white/30'
                  }`}
                >
                  <item.icon className={`w-7 h-7 flex-shrink-0 transition-colors ${
                    selectedNav === item.id ? 'text-[#C93831]' : 'text-gray-700'
                  }`} strokeWidth={selectedNav === item.id ? 2.5 : 2} />
                  <span className={`whitespace-nowrap transition-opacity duration-200 font-medium text-gray-700 ${
                    sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
                  }`}>
                    {item.label}
                  </span>
                </button>
              ))}
            </nav>

            {/* User Profile */}
            <button
              onClick={() => setSelectedNav("profile")}
              className="flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-white/30 transition-all"
            >
              <Avatar className="w-9 h-9 border-2 border-[#C93831]">
                <AvatarFallback className="bg-gradient-to-br from-blue-600 to-cyan-600 text-white font-black">
                  의
                </AvatarFallback>
              </Avatar>
              <span className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ${
                sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
              }`}>
                김의사
              </span>
            </button>
          </div>
        </div>

        {/* Main Content */}
        <div className={`h-full transition-all duration-300 ${sidebarExpanded ? 'ml-64' : 'ml-20'}`}>
          {/* Patients List */}
          {selectedNav === "patients" && (
            <div className="h-full overflow-auto p-8">
              <div className="max-w-7xl mx-auto space-y-6">
                <div>
                  <h1 className="text-5xl font-black text-gray-900 mb-2">환자 목록</h1>
                  <p className="text-gray-700 font-medium text-lg">오늘의 진료 환자</p>
                </div>

                <div className="grid gap-4">
                  {patients.map((patient) => (
                    <Card 
                      key={patient.id}
                      className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl hover:shadow-2xl transition-all cursor-pointer"
                      onClick={() => setSelectedPatient(patient)}
                    >
                      <div className="p-6">
                        <div className="flex items-center gap-6">
                          <Avatar className="w-16 h-16 border-4 border-white shadow-lg">
                            <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xl">
                              {patient.avatar}
                            </AvatarFallback>
                          </Avatar>
                          
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                              <h3 className="text-2xl font-black text-gray-900">{patient.name}</h3>
                              <Badge className={`${
                                patient.status === "waiting" ? "bg-yellow-500" :
                                patient.status === "in-progress" ? "bg-green-500" :
                                "bg-gray-500"
                              } text-white font-bold border-0`}>
                                {patient.status === "waiting" ? "대기중" :
                                 patient.status === "in-progress" ? "진료중" :
                                 "완료"}
                              </Badge>
                            </div>
                            <div className="flex gap-6 text-sm">
                              <div className="flex items-center gap-2 text-gray-700 font-medium">
                                <User className="w-4 h-4" />
                                {patient.age}세 / {patient.gender}
                              </div>
                              <div className="flex items-center gap-2 text-gray-700 font-medium">
                                <CalendarIcon className="w-4 h-4" />
                                최근 방문: {patient.lastVisit}
                              </div>
                              <div className="flex items-center gap-2 text-gray-700 font-medium">
                                <Stethoscope className="w-4 h-4" />
                                {patient.condition}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Appointments Management */}
          {selectedNav === "appointments" && (
            <div className="h-full overflow-auto p-8">
              <div className="max-w-7xl mx-auto space-y-6">
                <div>
                  <h1 className="text-5xl font-black text-gray-900 mb-2">예약 관리</h1>
                  <p className="text-gray-700 font-medium text-lg">환자 예약 현황</p>
                </div>

                <div className="grid lg:grid-cols-2 gap-6">
                  {appointments.map((apt) => (
                    <Card 
                      key={apt.id}
                      className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl"
                    >
                      <div className="p-6">
                        <div className="flex items-start justify-between mb-4">
                          <div className="flex items-center gap-4">
                            <Avatar className="w-12 h-12 border-2 border-white shadow-lg">
                              <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black">
                                {apt.patientAvatar}
                              </AvatarFallback>
                            </Avatar>
                            <div>
                              <h3 className="text-xl font-black text-gray-900">{apt.patientName}</h3>
                              <div className="text-sm text-gray-600 font-medium">{apt.department}</div>
                            </div>
                          </div>
                          <Badge className={`${
                            apt.status === "scheduled" ? "bg-blue-500" :
                            apt.status === "completed" ? "bg-green-500" :
                            "bg-gray-500"
                          } text-white font-bold border-0`}>
                            {apt.status === "scheduled" ? "예정" :
                             apt.status === "completed" ? "완료" :
                             "취소"}
                          </Badge>
                        </div>

                        <div className="space-y-2 mb-4">
                          <div className="flex items-center gap-2 text-gray-700 font-medium">
                            <CalendarIcon className="w-4 h-4" />
                            {apt.date} {apt.time}
                          </div>
                          <div className="flex items-center gap-2 text-gray-700 font-medium">
                            <FileText className="w-4 h-4" />
                            {apt.reason}
                          </div>
                        </div>

                        {apt.status === "scheduled" && (
                          <div className="flex gap-2">
                            <Button 
                              variant="outline" 
                              className="flex-1 rounded-xl border-blue-300 text-blue-600 hover:bg-blue-50"
                              onClick={() => setShowChat(true)}
                            >
                              <MessageCircle className="w-4 h-4 mr-2" />
                              채팅
                            </Button>
                            <Button 
                              variant="outline" 
                              className="flex-1 rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                            >
                              <XCircle className="w-4 h-4 mr-2" />
                              취소
                            </Button>
                          </div>
                        )}
                      </div>
                    </Card>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Chat */}
          {selectedNav === "chat" && (
            <div className="h-full overflow-auto p-8">
              <div className="max-w-5xl mx-auto">
                <div>
                  <h1 className="text-5xl font-black text-gray-900 mb-6">채팅</h1>
                </div>

                <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl h-[calc(100vh-200px)]">
                  <div className="h-full flex">
                    {/* Chat List */}
                    <div className="w-80 border-r border-gray-200 p-4">
                      <h3 className="text-xl font-black text-gray-900 mb-4">대화 목록</h3>
                      <ScrollArea className="h-[calc(100%-60px)]">
                        <div className="space-y-2">
                          {patients.slice(0, 4).map((patient) => (
                            <div 
                              key={patient.id}
                              onClick={() => setSelectedChatPatient(patient)}
                              className={`p-3 rounded-xl border cursor-pointer hover:shadow-lg transition-all ${
                                selectedChatPatient?.id === patient.id 
                                  ? 'bg-blue-50 border-blue-300' 
                                  : 'bg-white/80 border-gray-200'
                              }`}
                            >
                              <div className="flex items-center gap-3">
                                <Avatar className="w-10 h-10">
                                  <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-sm">
                                    {patient.avatar}
                                  </AvatarFallback>
                                </Avatar>
                                <div className="flex-1 min-w-0">
                                  <div className="font-bold text-sm text-gray-900">{patient.name}</div>
                                  <div className="text-xs text-gray-600 truncate">
                                    {chatMessages[patient.id]?.[chatMessages[patient.id].length - 1]?.content || "메시지 없음"}
                                  </div>
                                </div>
                                {chatMessages[patient.id] && chatMessages[patient.id].length > 0 && (
                                  <div className="w-2 h-2 bg-red-500 rounded-full flex-shrink-0"></div>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    </div>

                    {/* Chat Area */}
                    <div className="flex-1 flex flex-col p-6">
                      {selectedChatPatient ? (
                        <>
                          <div className="flex items-center justify-between pb-4 border-b border-gray-200 mb-4">
                            <div className="flex items-center gap-3">
                              <Avatar className="w-10 h-10">
                                <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black">
                                  {selectedChatPatient.avatar}
                                </AvatarFallback>
                              </Avatar>
                              <div>
                                <div className="font-bold text-gray-900">{selectedChatPatient.name}</div>
                                <div className="text-xs text-gray-600">온라인</div>
                              </div>
                            </div>
                            <Button
                              onClick={() => {
                                toast.success("진료가 종료되었습니다.");
                              }}
                              variant="outline"
                              className="rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                            >
                              <CheckCircle className="w-4 h-4 mr-2" />
                              진료 종료
                            </Button>
                          </div>

                          <ScrollArea className="flex-1 mb-4">
                            <div className="space-y-4">
                              {(chatMessages[selectedChatPatient.id] || []).map((msg) => (
                                <div key={msg.id} className={`flex gap-3 ${msg.isMine ? 'justify-end' : ''}`}>
                                  {!msg.isMine && (
                                    <Avatar className="w-8 h-8">
                                      <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xs">
                                        {msg.avatar}
                                      </AvatarFallback>
                                    </Avatar>
                                  )}
                                  <div className={`rounded-2xl p-3 max-w-xs ${msg.isMine ? 'bg-[#C93831] text-white' : 'bg-gray-100'}`}>
                                    {!msg.isMine && <div className="font-bold text-xs text-gray-900 mb-1">{msg.author}</div>}
                                    <div className="text-sm">{msg.content}</div>
                                    <div className={`text-xs mt-1 ${msg.isMine ? 'text-white/80' : 'text-gray-500'}`}>{msg.time}</div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </ScrollArea>
                          
                          <div className="flex gap-2">
                            <Input 
                              placeholder="메시지 입력..." 
                              className="rounded-xl"
                              value={chatMessage}
                              onChange={(e) => setChatMessage(e.target.value)}
                              onKeyPress={(e) => {
                                if (e.key === 'Enter') {
                                  handleSendDoctorChat();
                                }
                              }}
                            />
                            <Button 
                              onClick={handleSendDoctorChat}
                              className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
                            >
                              <Send className="w-4 h-4" />
                            </Button>
                          </div>
                        </>
                      ) : (
                        <div className="flex items-center justify-center h-full text-gray-500">
                          환자를 선택하세요
                        </div>
                      )}
                    </div>
                  </div>
                </Card>
              </div>
            </div>
          )}

          {/* Profile */}
          {selectedNav === "profile" && (
            <div className="h-full overflow-auto p-8">
              <div className="max-w-4xl mx-auto space-y-8">
                <div>
                  <h1 className="text-5xl font-black text-gray-900 mb-2">내 정보</h1>
                  <p className="text-gray-700 font-medium text-lg">의료진 프로필</p>
                </div>

                <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
                  <div className="p-8">
                    <div className="flex items-center gap-6 mb-8">
                      <Avatar className="w-24 h-24 border-4 border-white shadow-xl">
                        <AvatarFallback className="bg-gradient-to-br from-blue-600 to-cyan-600 text-white text-3xl font-black">
                          의
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <h2 className="text-3xl font-black text-gray-900 mb-2">김의사</h2>
                        <p className="text-gray-600 font-medium">내과 전문의</p>
                      </div>
                    </div>

                    <div className="space-y-4">
                      <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                        <div className="text-sm text-gray-600 font-medium mb-1">이메일</div>
                        <div className="font-bold text-gray-900">doctor@company.com</div>
                      </div>
                      
                      <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                        <div className="text-sm text-gray-600 font-medium mb-1">전문 분야</div>
                        <div className="font-bold text-gray-900">내과, 가정의학과</div>
                      </div>
                    </div>

                    <div className="mt-8 pt-8 border-t border-gray-200">
                      <Button 
                        onClick={onLogout}
                        variant="outline" 
                        className="w-full h-14 rounded-2xl border-2 border-red-300 text-red-600 hover:bg-red-50 font-bold text-lg"
                      >
                        로그아웃
                      </Button>
                    </div>
                  </div>
                </Card>
              </div>
            </div>
          )}
        </div>

        {/* Patient Detail Dialog */}
        <Dialog open={!!selectedPatient} onOpenChange={() => setSelectedPatient(null)}>
          <DialogContent className="max-w-3xl">
            <DialogHeader>
              <DialogTitle className="text-2xl font-black">환자 상세 정보</DialogTitle>
              <DialogDescription>환자의 진료 기록 및 정보를 확인할 수 있습니다.</DialogDescription>
            </DialogHeader>
            {selectedPatient && (
              <div className="space-y-6 p-4">
                <div className="flex items-center gap-6">
                  <Avatar className="w-20 h-20 border-4 border-white shadow-xl">
                    <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-2xl">
                      {selectedPatient.avatar}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <h3 className="text-2xl font-black text-gray-900 mb-1">{selectedPatient.name}</h3>
                    <div className="text-gray-700 font-medium">{selectedPatient.age}세 / {selectedPatient.gender}</div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="p-4 rounded-xl bg-gradient-to-br from-blue-50 to-cyan-50 border border-blue-200">
                    <div className="text-sm text-gray-600 mb-1">최근 방문</div>
                    <div className="font-bold text-gray-900">{selectedPatient.lastVisit}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-gradient-to-br from-purple-50 to-pink-50 border border-purple-200">
                    <div className="text-sm text-gray-600 mb-1">진료 사유</div>
                    <div className="font-bold text-gray-900">{selectedPatient.condition}</div>
                  </div>
                </div>

                <div className="p-6 rounded-2xl bg-gradient-to-br from-green-50 to-emerald-50 border border-green-200">
                  <h4 className="text-lg font-black text-gray-900 mb-3">진료 기록</h4>
                  <div className="space-y-2">
                    <div className="p-3 bg-white rounded-lg">
                      <div className="font-bold text-sm text-gray-900">2024-11-10 - 정기 검진</div>
                      <div className="text-xs text-gray-600">혈압: 120/80, 혈당: 정상</div>
                    </div>
                    <div className="p-3 bg-white rounded-lg">
                      <div className="font-bold text-sm text-gray-900">2024-10-15 - 건강 상담</div>
                      <div className="text-xs text-gray-600">운동 처방, 식이요법 권장</div>
                    </div>
                  </div>
                </div>

                <div className="flex gap-3">
                  <Button 
                    onClick={() => {
                      setSelectedChatPatient(selectedPatient);
                      setSelectedNav("chat");
                      setSelectedPatient(null);
                    }}
                    className="flex-1 bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-2xl h-12"
                  >
                    <MessageCircle className="w-5 h-5 mr-2" />
                    채팅 시작
                  </Button>
                  <Button 
                    onClick={() => {
                      if (selectedPatient) {
                        setPrescriptionPatient(selectedPatient);
                        setShowPrescriptionForm(true);
                        setSelectedPatient(null);
                      } else {
                        toast.error("환자를 선택해주세요.");
                      }
                    }}
                    className="flex-1 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-bold rounded-2xl h-12"
                  >
                    <FileText className="w-5 h-5 mr-2" />
                    처방전 작성
                  </Button>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    );
  }

  return (
    <div className="h-screen w-screen overflow-hidden relative">
      {/* Colorful Stained Background */}
      <div className="fixed inset-0 -z-10 bg-gradient-to-br from-purple-100 via-pink-50 to-blue-100">
        <div className="absolute top-20 left-10 w-96 h-96 bg-gradient-to-br from-purple-300 to-pink-300 rounded-full blur-3xl opacity-40 animate-float"></div>
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-gradient-to-br from-blue-300 to-cyan-300 rounded-full blur-3xl opacity-40 animate-float-delayed"></div>
        <div className="absolute top-1/2 left-1/3 w-80 h-80 bg-gradient-to-br from-yellow-200 to-orange-300 rounded-full blur-3xl opacity-30 animate-pulse"></div>
        <div className="absolute bottom-1/3 right-1/4 w-72 h-72 bg-gradient-to-br from-green-200 to-emerald-300 rounded-full blur-3xl opacity-30 animate-float"></div>
        <div className="absolute top-1/3 right-10 w-64 h-64 bg-gradient-to-br from-red-200 to-pink-200 rounded-full blur-3xl opacity-25 animate-float"></div>
        <div className="absolute bottom-10 left-1/2 w-96 h-96 bg-gradient-to-br from-indigo-200 to-purple-200 rounded-full blur-3xl opacity-30 animate-float-delayed"></div>
      </div>

      {/* Glassmorphic Sidebar */}
      <div
        className={`fixed left-0 top-0 h-full z-50 transition-all duration-300 ${
          sidebarExpanded ? 'w-64' : 'w-20'
        }`}
        onMouseEnter={() => setSidebarExpanded(true)}
        onMouseLeave={() => setSidebarExpanded(false)}
      >
        <div className="absolute inset-0 backdrop-blur-3xl bg-white/40 border-r border-white/60 shadow-2xl"></div>
        
        <div className="relative h-full flex flex-col p-4">
          {/* Logo - 1.5x bigger */}
          <div className="mb-8 flex items-center justify-center">
            <img src={logoImage} alt="Lupin" className="h-16 w-16 object-contain" />
          </div>

          {/* Navigation */}
          <nav className="flex-1 space-y-2">
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => setSelectedNav(item.id)}
                className={`w-full flex items-center gap-4 px-3 py-3 rounded-2xl transition-all duration-200 relative ${
                  selectedNav === item.id ? '' : 'hover:bg-white/30'
                }`}
              >
                <item.icon className={`w-7 h-7 flex-shrink-0 transition-colors ${
                  selectedNav === item.id ? 'text-[#C93831]' : 'text-gray-700'
                }`} strokeWidth={selectedNav === item.id ? 2.5 : 2} />
                <span className={`whitespace-nowrap transition-opacity duration-200 font-medium text-gray-700 ${
                  sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
                }`}>
                  {item.label}
                </span>
              </button>
            ))}
          </nav>

          {/* Notification Button - Above Profile */}
          <div className="relative mb-2">
            <button
              onClick={() => setShowNotifications(!showNotifications)}
              className="relative w-full flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-white/30 transition-all"
            >
              <div className="relative w-7 h-7 flex items-center justify-center flex-shrink-0">
                <Bell className="w-7 h-7 text-gray-700" />
                {/* Notification Badge - Relative to icon */}
                {notifications.filter(n => !n.read).length > 0 && (
                  <div className="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full"></div>
                )}
              </div>
              <span className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ${
                sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
              }`}>
                알림
              </span>
            </button>

            {/* Notification Popup */}
            {showNotifications && (
              <div className="absolute bottom-full left-full ml-2 mb-2 w-80 backdrop-blur-3xl bg-white/95 border border-white/60 shadow-2xl rounded-2xl z-50">
                <div className="p-4">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-black text-gray-900">알림</h3>
                    <button 
                      onClick={() => setShowNotifications(false)}
                      className="w-6 h-6 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                  
                  <ScrollArea className="max-h-96">
                    <div className="space-y-2">
                      {notifications.map((notif) => (
                        <div key={notif.id} className={`p-3 rounded-xl cursor-pointer transition-all ${
                          notif.read ? 'bg-white/60' : 'bg-gradient-to-r from-red-50/80 to-pink-50/80'
                        }`}>
                          <div className="flex items-start gap-3">
                            <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                              notif.type === "challenge" ? "bg-gradient-to-br from-purple-400 to-pink-500" :
                              notif.type === "appointment" ? "bg-gradient-to-br from-blue-400 to-cyan-500" :
                              notif.type === "like" ? "bg-gradient-to-br from-red-400 to-pink-500" :
                              "bg-gradient-to-br from-green-400 to-emerald-500"
                            }`}>
                              {notif.type === "challenge" && <Zap className="w-4 h-4 text-white" />}
                              {notif.type === "appointment" && <CalendarIcon className="w-4 h-4 text-white" />}
                              {notif.type === "like" && <Heart className="w-4 h-4 text-white" />}
                              {notif.type === "comment" && <MessageCircle className="w-4 h-4 text-white" />}
                            </div>
                            
                            <div className="flex-1 min-w-0">
                              <div className="font-bold text-sm text-gray-900 mb-1">{notif.title}</div>
                              <div className="text-xs text-gray-700 mb-1 line-clamp-2">{notif.content}</div>
                              <div className="text-xs text-gray-500">{notif.time}</div>
                            </div>
                            
                            {!notif.read && (
                              <div className="w-2 h-2 bg-[#C93831] rounded-full flex-shrink-0 mt-1"></div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                </div>
              </div>
            )}
          </div>

          {/* User Profile */}
          <button
            onClick={() => setSelectedNav("profile")}
            className="flex items-center gap-3 px-3 py-3 rounded-2xl hover:bg-white/30 transition-all"
          >
            <Avatar className="w-9 h-9 border-2 border-[#C93831]">
              {profileImage ? (
                <img src={profileImage} alt="Profile" className="w-full h-full object-cover" />
              ) : (
                <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white font-black">
                  김
                </AvatarFallback>
              )}
            </Avatar>
            <span className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ${
              sidebarExpanded ? 'opacity-100' : 'opacity-0 w-0'
            }`}>
              김루핀
            </span>
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className={`h-full transition-all duration-300 ${sidebarExpanded ? 'ml-64' : 'ml-20'}`}>
        {/* Home */}
        {selectedNav === "home" && (
          <div className="h-full overflow-auto p-8">
            <div className="max-w-6xl mx-auto space-y-8">
              {/* Wellness Challenge Banner - Smaller height */}
              {!challengeJoined && (
                <Card className="backdrop-blur-2xl bg-white/70 border border-gray-200 shadow-xl overflow-hidden relative">
                  <div className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="space-y-2 flex-1">
                        <Badge className="bg-gradient-to-r from-[#C93831] to-pink-500 text-white px-4 py-1.5 font-bold border-0">
                          <Zap className="w-4 h-4 mr-1" />
                          진행중
                        </Badge>
                        <h2 className="text-3xl font-black text-gray-900">웰빙 챌린지</h2>
                        <p className="text-gray-700 font-medium">오늘 오후 6시 시작 | 선착순 100명 특별 보상</p>
                      </div>
                      
                      {/* Product Image */}
                      <div className="relative w-48 h-48 flex-shrink-0">
                        <img 
                          src="https://images.unsplash.com/photo-1762328500413-1a4cb2023059?w=400" 
                          alt="Supplements"
                          className="w-full h-full object-contain"
                        />
                      </div>
                      
                      <Button 
                        onClick={handleJoinChallenge}
                        className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold px-6 py-5 rounded-2xl shadow-xl border-0 ml-6"
                      >
                        참여하기
                      </Button>
                    </div>
                  </div>
                </Card>
              )}

              {/* Profile Header - Left Aligned */}
              <div className="p-8">
                <div className="flex items-start gap-8 mb-8">
                  <Avatar className="w-40 h-40 border-4 border-white shadow-xl">
                    {profileImage ? (
                      <img src={profileImage} alt="Profile" className="w-full h-full object-cover" />
                    ) : (
                      <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white text-5xl font-black">
                        김
                      </AvatarFallback>
                    )}
                  </Avatar>

                  <div className="flex-1">
                    <h1 className="text-3xl font-black text-gray-900 mb-4">김루핀</h1>
                    
                    <div className="flex gap-8 mb-4">
                      <div>
                        <div className="text-2xl font-black text-[#C93831]">{myFeeds.length}</div>
                        <div className="text-xs text-gray-600 font-bold">게시물</div>
                      </div>
                      <div>
                        <div className="text-2xl font-black text-[#C93831]">240</div>
                        <div className="text-xs text-gray-600 font-bold">총 점수</div>
                      </div>
                      <div>
                        <div className="text-2xl font-black text-[#C93831]">8</div>
                        <div className="text-xs text-gray-600 font-bold">추첨권</div>
                      </div>
                      <div>
                        <div className="text-2xl font-black text-[#C93831]">#12</div>
                        <div className="text-xs text-gray-600 font-bold">순위</div>
                      </div>
                    </div>

                    <p className="text-gray-700 font-medium text-sm mb-3">
                      🏃‍♂️ 건강한 습관 만들기<br/>
                      💪 매일 운동 챌린지 진행중
                    </p>

                    <div className="flex gap-2 flex-wrap">
                      <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                        <Flame className="w-3 h-3 mr-1" />
                        7일 연속
                      </Badge>
                      <Badge className="bg-gradient-to-r from-purple-400 to-pink-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                        <Award className="w-3 h-3 mr-1" />
                        TOP 20
                      </Badge>
                      <Badge className="bg-gradient-to-r from-blue-400 to-cyan-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                        <Target className="w-3 h-3 mr-1" />
                        목표 달성
                      </Badge>
                    </div>
                  </div>
                </div>
              </div>

              {/* Posts Grid - 5 items, smaller */}
              <div className="grid grid-cols-5 gap-3">
                {myFeeds.map((feed) => (
                  <div 
                    key={feed.id} 
                    className="cursor-pointer group aspect-[3/4]"
                    onClick={() => {
                      setSelectedFeed(feed);
                      setFeedImageIndex(feed.id, 0);
                      setShowFeedDetailInHome(true);
                    }}
                  >
                    <Card className="h-full overflow-hidden backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg hover:shadow-2xl transition-all relative">
                      <img 
                        src={feed.images[0]} 
                        alt={feed.activity}
                        className="w-full h-full object-cover"
                      />
                      
                      <div className="absolute inset-0 bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-sm">
                        <div className="text-center text-white space-y-2">
                          <div className="flex items-center justify-center gap-4">
                            <span className="flex items-center gap-1 font-bold text-base">
                              <Heart className="w-5 h-5" />
                              {feed.likes}
                            </span>
                            <span className="flex items-center gap-1 font-bold text-base">
                              <MessageCircle className="w-5 h-5" />
                              {feed.comments}
                            </span>
                          </div>
                          <div className="text-sm font-bold">
                            <Sparkles className="w-4 h-4 inline mr-1" />
                            +{feed.points}점
                          </div>
                        </div>
                      </div>
                    </Card>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Feed - Reels Style with Snap Scroll - Full Height */}
        {selectedNav === "reels" && (
          <div className="h-full relative flex items-center justify-center">
            {/* Feed Container with Snap Scroll */}
            <div 
              ref={feedContainerRef}
              className="h-full w-full overflow-y-scroll snap-y snap-mandatory scrollbar-hide"
              style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
            >
              <style>{`
                .scrollbar-hide::-webkit-scrollbar {
                  display: none;
                }
              `}</style>
              
              <div className="flex flex-col items-center">
                {/* Search Overlay */}
                {showSearch && (
                  <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40" onClick={() => setShowSearch(false)}>
                    <div className="absolute top-8 left-1/2 -translate-x-1/2 w-full max-w-2xl px-4" onClick={(e) => e.stopPropagation()}>
                      <div className="relative">
                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-600" />
                        <Input
                          type="text"
                          placeholder="피드 검색..."
                          value={searchQuery}
                          onChange={(e) => setSearchQuery(e.target.value)}
                          autoFocus
                          className="pl-12 pr-12 h-14 rounded-2xl backdrop-blur-2xl bg-white/80 border border-gray-300 font-medium shadow-2xl"
                        />
                        <button
                          onClick={() => {
                            setSearchQuery("");
                            setShowSearch(false);
                          }}
                          className="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors"
                        >
                          <X className="w-4 h-4 text-gray-600" />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* Feed Cards - Full Height */}
                {allFeeds
                  .filter(feed => 
                    feed.content.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    feed.author.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    feed.activity.toLowerCase().includes(searchQuery.toLowerCase())
                  )
                  .map((feed) => {
                    const currentIndex = getFeedImageIndex(feed.id);
                    const liked = hasLiked(feed.id);
                    return (
                      <div key={feed.id} className="snap-start snap-always flex-shrink-0 w-full h-screen flex items-center justify-center py-4">
                        <div className="w-[400px] h-full max-h-[95vh]">
                          <Card className="h-full overflow-hidden backdrop-blur-2xl bg-white/70 border border-gray-200 shadow-2xl relative flex flex-col">
                            {/* Image Carousel */}
                            <div className="relative flex-[2]">
                              <img 
                                src={feed.images[currentIndex] || feed.images[0]} 
                                alt={feed.activity}
                                className="w-full h-full object-cover"
                              />
                              
                              {feed.images.length > 1 && (
                                <>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setFeedImageIndex(feed.id, Math.max(0, currentIndex - 1));
                                    }}
                                    className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                                  >
                                    <ChevronLeft className="w-5 h-5" />
                                  </button>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setFeedImageIndex(feed.id, Math.min(feed.images.length - 1, currentIndex + 1));
                                    }}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                                  >
                                    <ChevronRight className="w-5 h-5" />
                                  </button>
                                  <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                                    {feed.images.map((_, idx) => (
                                      <div key={idx} className={`w-1.5 h-1.5 rounded-full ${idx === currentIndex ? 'bg-white' : 'bg-white/50'}`}></div>
                                    ))}
                                  </div>
                                </>
                              )}

                              {/* Author Info */}
                              <div className="absolute top-4 left-4 flex items-center gap-3 backdrop-blur-xl bg-white/20 rounded-full px-4 py-2 border border-white/30">
                                <Avatar className="w-8 h-8 border-2 border-white">
                                  <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white font-black text-sm">
                                    {feed.avatar}
                                  </AvatarFallback>
                                </Avatar>
                                <div>
                                  <div className="text-white text-xs font-bold">{feed.author}</div>
                                  <div className="text-white/80 text-xs">{feed.time}</div>
                                </div>
                              </div>

                              {/* Right Actions */}
                              <div className="absolute right-4 bottom-4 flex flex-col gap-4">
                                <button 
                                  onClick={() => handleLike(feed.id)}
                                  className="flex flex-col items-center gap-1 group"
                                >
                                  <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                                    <Heart className={`w-5 h-5 ${liked ? 'fill-red-500 text-red-500' : 'text-white'}`} />
                                  </div>
                                  <span className="text-white text-xs font-bold">{feed.likes}</span>
                                </button>

                                <button 
                                  className="flex flex-col items-center gap-1 group"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setSelectedFeed(feed);
                                    setShowCommentsInReels(true);
                                  }}
                                >
                                  <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                                    <MessageCircle className="w-5 h-5 text-white" />
                                  </div>
                                  <span className="text-white text-xs font-bold">{feed.comments}</span>
                                </button>
                              </div>
                            </div>

                            {/* Content */}
                            <div className="p-6 space-y-3 flex-1 overflow-auto">
                              <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
                                <Sparkles className="w-3 h-3 mr-1" />
                                +{feed.points}
                              </Badge>

                              <p className="text-gray-700 font-medium text-sm leading-relaxed">
                                {feed.content}
                              </p>

                              <div className="flex gap-2 flex-wrap">
                                <Badge className="bg-white border border-gray-300 text-gray-700 px-3 py-1 font-bold text-xs">
                                  {feed.duration}
                                </Badge>
                                {Object.entries(feed.stats).map(([key, value]) => (
                                  <Badge key={key} className="bg-red-50 border border-red-200 text-[#C93831] px-3 py-1 font-bold text-xs">
                                    {value}
                                  </Badge>
                                ))}
                              </div>
                            </div>
                          </Card>
                        </div>
                      </div>
                    );
                  })}
              </div>
            </div>

            {/* Comments - Match Feed Height */}
            {showCommentsInReels && selectedFeed && (
              <div className="absolute top-1/2 -translate-y-1/2 left-1/2 ml-[200px] w-96 h-[95vh] max-h-[95vh] backdrop-blur-2xl bg-white/90 border-l border-gray-200 shadow-2xl z-50 flex flex-col rounded-r-3xl">
                <div className="p-6 flex-1 flex flex-col max-h-[95vh] my-auto">
                  <div className="flex items-center justify-between mb-6">
                    <h3 className="text-2xl font-black text-gray-900">댓글</h3>
                    <button 
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowCommentsInReels(false);
                        setReplyingTo(null);
                      }}
                      className="w-8 h-8 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  
                  <ScrollArea className="flex-1 mb-4">
                    <div className="space-y-4 pr-2">
                      {(feedComments[selectedFeed.id] || []).map((comment) => (
                        <div key={comment.id}>
                          <div className="p-3 rounded-xl bg-gray-50">
                            <div className="flex items-start gap-3">
                              <Avatar className="w-8 h-8">
                                <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xs">
                                  {comment.avatar}
                                </AvatarFallback>
                              </Avatar>
                              <div className="flex-1">
                                <div className="font-bold text-sm text-gray-900">{comment.author}</div>
                                <div className="text-sm text-gray-700">{comment.content}</div>
                                <div className="flex items-center gap-3 mt-1">
                                  <div className="text-xs text-gray-500">{comment.time}</div>
                                  <button 
                                    onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)}
                                    className="text-xs text-[#C93831] font-bold hover:underline"
                                  >
                                    답글
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>
                          
                          {/* Reply Input Box - Right under the comment */}
                          {replyingTo === comment.id && (
                            <div className="ml-11 mt-2 flex gap-2">
                              <Input 
                                placeholder="답글 입력..." 
                                className="rounded-xl text-sm"
                                value={newComment}
                                onChange={(e) => setNewComment(e.target.value)}
                                onKeyPress={(e) => {
                                  if (e.key === 'Enter' && selectedFeed) {
                                    handleAddComment(selectedFeed.id);
                                  }
                                }}
                                autoFocus
                              />
                              <Button 
                                onClick={() => selectedFeed && handleAddComment(selectedFeed.id)}
                                className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl px-3"
                              >
                                <Send className="w-3 h-3" />
                              </Button>
                              <button 
                                onClick={() => setReplyingTo(null)}
                                className="text-xs text-gray-500 hover:text-gray-700 px-2"
                              >
                                취소
                              </button>
                            </div>
                          )}
                          
                          {/* Replies - YouTube Style with connecting line */}
                          {comment.replies && comment.replies.length > 0 && (
                            <div className="ml-11 mt-3 relative">
                              {/* Connecting vertical line */}
                              <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-gray-300"></div>
                              
                              <div className="space-y-3 pl-6">
                                {comment.replies.map((reply) => (
                                  <div key={reply.id} className="relative">
                                    {/* Horizontal connecting line */}
                                    <div className="absolute left-[-24px] top-4 w-6 h-0.5 bg-gray-300"></div>
                                    
                                    <div className="p-3 rounded-xl bg-blue-50/50">
                                      <div className="flex items-start gap-3">
                                        <Avatar className="w-7 h-7">
                                          <AvatarFallback className="bg-gradient-to-br from-blue-600 to-cyan-600 text-white font-black text-xs">
                                            {reply.avatar}
                                          </AvatarFallback>
                                        </Avatar>
                                        <div className="flex-1">
                                          <div className="font-bold text-sm text-gray-900">{reply.author}</div>
                                          <div className="text-sm text-gray-700">{reply.content}</div>
                                          <div className="text-xs text-gray-500 mt-1">{reply.time}</div>
                                        </div>
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                  
                  {!replyingTo && (
                    <div className="flex gap-2">
                      <Input 
                        placeholder="댓글 입력..." 
                        className="rounded-xl"
                        value={newComment}
                        onChange={(e) => setNewComment(e.target.value)}
                        onKeyPress={(e) => {
                          if (e.key === 'Enter' && selectedFeed) {
                            handleAddComment(selectedFeed.id);
                          }
                        }}
                      />
                      <Button 
                        onClick={() => selectedFeed && handleAddComment(selectedFeed.id)}
                        className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
                      >
                        <Send className="w-4 h-4" />
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Floating Search Button */}
            <button
              onClick={() => setShowSearch(true)}
              className="fixed right-8 bottom-8 w-14 h-14 rounded-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white shadow-2xl hover:shadow-3xl flex items-center justify-center transition-all hover:scale-110 z-30"
            >
              <Search className="w-6 h-6" />
            </button>
          </div>
        )}

        {/* Ranking - Very Compact */}
        {selectedNav === "ranking" && (
          <div className="h-full overflow-auto p-8">
            <div className="max-w-7xl mx-auto">
              <div className="mb-6">
                <h1 className="text-5xl font-black text-gray-900 mb-2">{currentMonth}월 랭킹</h1>
                <p className="text-gray-700 font-medium text-lg">이번 달 TOP 운동왕은 누구?</p>
              </div>

              <div className="grid lg:grid-cols-3 gap-8">
                {/* Ranking List - Super Compact */}
                <div className="lg:col-span-2 space-y-1.5">
                  {[
                    { rank: 1, name: "이철수", points: 520, avatar: "이", badge: "🥇" },
                    { rank: 2, name: "박영희", points: 480, avatar: "박", badge: "🥈" },
                    { rank: 3, name: "최민수", points: 450, avatar: "최", badge: "🥉" },
                    { rank: 4, name: "정수진", points: 420, avatar: "정" },
                    { rank: 5, name: "강민호", points: 390, avatar: "강" },
                    { rank: 6, name: "윤서연", points: 370, avatar: "윤" },
                    { rank: 7, name: "장동건", points: 350, avatar: "장" },
                    { rank: 8, name: "송혜교", points: 330, avatar: "송" },
                    { rank: 9, name: "전지현", points: 310, avatar: "전" },
                    { rank: 10, name: "현빈", points: 290, avatar: "현" },
                    { rank: 12, name: "김루핀", points: 240, avatar: "김", isMe: true }
                  ].map((ranker) => (
                    <Card key={ranker.rank} className={`backdrop-blur-2xl border shadow-lg overflow-hidden transition-all hover:scale-[1.01] ${
                      ranker.isMe 
                        ? 'bg-gradient-to-r from-red-50/80 to-pink-50/80 border-[#C93831]' 
                        : 'bg-white/60 border-gray-200'
                    }`}>
                      <div className="p-2">
                        <div className="flex items-center gap-2">
                          <div className="text-xl font-black text-gray-900 w-8 text-center">
                            {ranker.badge || ranker.rank}
                          </div>
                          
                          <Avatar className="w-8 h-8 border-2 border-white shadow-lg">
                            <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xs">
                              {ranker.avatar}
                            </AvatarFallback>
                          </Avatar>
                          
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <span className="font-black text-sm text-gray-900">{ranker.name}</span>
                              {ranker.isMe && (
                                <Badge className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold border-0 text-xs">
                                  나
                                </Badge>
                              )}
                            </div>
                            <div className="text-gray-600 font-bold text-xs">{ranker.points}점</div>
                          </div>
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>

                {/* Stats Panel */}
                <div className="space-y-6">
                  <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
                    <div className="p-6 space-y-4">
                      <h3 className="text-xl font-black text-gray-900 flex items-center gap-2">
                        <TrendingUp className="w-6 h-6 text-[#C93831]" />
                        내 통계
                      </h3>
                      
                      <div className="space-y-3">
                        <div className="flex justify-between items-center">
                          <span className="text-gray-700 font-medium">이번 달 활동</span>
                          <span className="font-black text-xl text-[#C93831]">18일</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-700 font-medium">평균 점수</span>
                          <span className="font-black text-xl text-[#C93831]">48</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-700 font-medium">연속 기록</span>
                          <span className="font-black text-xl text-[#C93831]">7일</span>
                        </div>
                      </div>
                    </div>
                  </Card>

                  <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
                    <div className="p-6 space-y-4">
                      <h3 className="text-xl font-black text-gray-900 flex items-center gap-2">
                        <Users className="w-6 h-6 text-[#C93831]" />
                        전체 현황
                      </h3>
                      
                      <div className="space-y-3">
                        <div className="flex justify-between items-center">
                          <span className="text-gray-700 font-medium">총 참여자</span>
                          <span className="font-black text-xl text-gray-900">248명</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-700 font-medium">이번 달 활동</span>
                          <span className="font-black text-xl text-gray-900">220명</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-700 font-medium">평균 점수</span>
                          <span className="font-black text-xl text-gray-900">42점</span>
                        </div>
                      </div>
                    </div>
                  </Card>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Medical - with ScrollArea */}
        {selectedNav === "medical" && (
          <div className="h-full overflow-auto p-8">
            <div className="max-w-7xl mx-auto space-y-6">
              <div>
                <h1 className="text-5xl font-black text-gray-900 mb-2">비대면 진료</h1>
                <p className="text-gray-700 font-medium text-lg">전문 의료진과 상담하세요</p>
              </div>

              <div className="grid grid-cols-4 gap-6">
                {/* New Appointment - Smaller */}
                <Card className="col-span-2 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl hover:shadow-2xl transition-all h-64">
                  <div className="h-full p-6 flex flex-col items-center justify-center text-center space-y-4">
                    <div className="w-16 h-16 bg-gradient-to-br from-[#C93831] to-[#B02F28] rounded-3xl flex items-center justify-center shadow-xl">
                      <CalendarIcon className="w-8 h-8 text-white" />
                    </div>
                    <div>
                      <h3 className="text-2xl font-black text-gray-900 mb-1">새 진료 예약</h3>
                      <p className="text-gray-600 font-medium text-sm">의료진과 비대면 상담</p>
                    </div>
                    <Button 
                      onClick={() => setShowAppointment(true)}
                      className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold px-6 py-4 rounded-2xl border-0"
                    >
                      예약하기
                    </Button>
                  </div>
                </Card>

                {/* Appointments - with ScrollArea */}
                <Card className="col-span-2 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl overflow-hidden h-64">
                  <div className="p-4 h-full flex flex-col">
                    <h3 className="text-lg font-black text-gray-900 mb-3 flex items-center gap-2">
                      <Clock className="w-5 h-5 text-[#C93831]" />
                      예약 내역
                    </h3>
                    
                    <div className="flex-1 overflow-auto pr-2" style={{ scrollbarWidth: 'thin', scrollbarColor: '#C93831 #f0f0f0' }}>
                      <div className="space-y-2">
                        {[
                          { id: 1, type: "내과 상담", doctor: "김의사", date: "11월 15일", time: "오후 3시", status: "예정", hasChat: true },
                          { id: 2, type: "정형외과", doctor: "이의사", date: "11월 10일", time: "오전 10시", status: "완료", hasChat: false },
                          { id: 3, type: "피부과", doctor: "박의사", date: "11월 5일", time: "오후 2시", status: "완료", hasChat: false },
                          { id: 4, type: "내과", doctor: "최의사", date: "10월 28일", time: "오전 11시", status: "완료", hasChat: false }
                        ].map((apt) => (
                          <div key={apt.id} className={`p-3 rounded-xl ${apt.status === "예정" ? "bg-white/80" : "bg-gray-100/50"}`}>
                            <div className="flex items-start justify-between mb-1">
                              <div>
                                <div className="font-bold text-gray-900 text-sm">{apt.type}</div>
                                <div className="text-xs text-gray-600">{apt.doctor} 원장</div>
                              </div>
                              <Badge className={`${apt.status === "예정" ? "bg-green-500" : "bg-gray-500"} text-white font-bold border-0 text-xs`}>
                                {apt.status}
                              </Badge>
                            </div>
                            <div className="text-xs text-gray-600 font-medium mb-2">{apt.date} {apt.time}</div>
                            <div className="flex gap-2">
                              {apt.hasChat && (
                                <Button 
                                  onClick={() => setShowChat(true)}
                                  variant="outline" 
                                  size="sm" 
                                  className="flex-1 rounded-lg text-xs border-blue-300 text-blue-600 hover:bg-blue-50"
                                >
                                  <MessageCircle className="w-3 h-3 mr-1" />
                                  채팅
                                </Button>
                              )}
                              {apt.status === "예정" && (
                                <Button variant="outline" size="sm" className="flex-1 rounded-lg text-xs border-red-300 text-red-600 hover:bg-red-50">
                                  <XCircle className="w-3 h-3 mr-1" />
                                  취소
                                </Button>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </Card>

                {/* Prescriptions - Scrollable */}
                <Card className="col-span-4 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl overflow-hidden h-48">
                  <div className="p-4 h-full flex flex-col">
                    <h3 className="text-lg font-black text-gray-900 mb-3 flex items-center gap-2">
                      <FileText className="w-5 h-5 text-[#C93831]" />
                      처방전
                    </h3>
                    
                    <div className="flex-1 overflow-auto pr-2" style={{ scrollbarWidth: 'thin', scrollbarColor: '#C93831 #f0f0f0' }}>
                      <div className="grid grid-cols-4 gap-3">
                        {prescriptions.map((pres) => (
                          <div key={pres.id} className="p-3 rounded-xl bg-white/80 border border-gray-200">
                            <div className="font-bold text-gray-900 mb-1 text-sm">{pres.name}</div>
                            <div className="text-xs text-gray-600 mb-1">{pres.doctor} 원장</div>
                            <div className="text-xs text-gray-500 mb-2">{pres.date}</div>
                            <Button 
                              size="sm" 
                              variant="outline" 
                              className="w-full rounded-lg text-xs"
                              onClick={() => setSelectedPrescription(pres)}
                            >
                              상세보기
                            </Button>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </Card>
              </div>
            </div>
          </div>
        )}

        {/* Create - Drag & Drop */}
        {selectedNav === "create" && (
          <div className="h-full overflow-auto p-8">
            <div className="max-w-4xl mx-auto space-y-6">
              <div>
                <h1 className="text-5xl font-black text-gray-900 mb-2">새 피드 작성</h1>
                <p className="text-gray-700 font-medium text-lg">운동 기록을 공유하세요</p>
              </div>

              <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
                <div className="p-8 space-y-6">
                  {/* Image Upload Area - Wide with Drag & Drop */}
                  <div className="space-y-3">
                    <div className="flex items-center gap-4 mb-2">
                      <Label className="text-base font-black text-gray-900">이미지</Label>
                      {isWorkoutVerified && (
                        <Badge className="bg-green-500 text-white px-4 py-2 font-bold border-0">
                          <CheckCircle className="w-4 h-4 mr-1" />
                          운동 인증 완료
                        </Badge>
                      )}
                    </div>
                    
                    <div
                      onDragEnter={handleDragEnter}
                      onDragLeave={handleDragLeave}
                      onDragOver={handleDragOver}
                      onDrop={handleDrop}
                      onClick={() => fileInputRef.current?.click()}
                      className={`w-full h-32 rounded-2xl border-2 border-dashed transition-all cursor-pointer ${
                        isDragging 
                          ? 'border-[#C93831] bg-red-50' 
                          : 'border-gray-300 hover:border-[#C93831] bg-white/50'
                      }`}
                    >
                      <div className="h-full flex flex-col items-center justify-center gap-2">
                        <Upload className="w-8 h-8 text-gray-400" />
                        <span className="font-bold text-gray-600 text-sm">
                          클릭하거나 드래그하여 이미지 첨부
                        </span>
                      </div>
                    </div>
                    
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      multiple
                      onChange={handleFileSelect}
                      className="hidden"
                    />
                  </div>

                  {/* Image Preview List - Fixed X button with higher z-index */}
                  {postImages.length > 0 && (
                    <ScrollArea className="max-h-40">
                      <div className="flex gap-3 pb-2">
                        {postImages.map((img, idx) => (
                          <div key={idx} className="relative flex-shrink-0" style={{ width: '136px', height: '136px' }}>
                            <div className="w-32 h-32 rounded-xl overflow-hidden bg-gray-100">
                              <img src={img} alt={`Upload ${idx + 1}`} className="w-full h-full object-contain" />
                            </div>
                            <button
                              onClick={() => setPostImages(postImages.filter((_, i) => i !== idx))}
                              className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-[100] pointer-events-auto"
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </div>
                        ))}
                      </div>
                    </ScrollArea>
                  )}

                  {/* Content - Taller */}
                  <div className="space-y-3">
                    <Label className="text-base font-black text-gray-900">내용</Label>
                    <Textarea
                      placeholder="오늘의 운동은 어땠나요? 자세히 공유해주세요..."
                      value={postContent}
                      onChange={(e) => setPostContent(e.target.value)}
                      className="min-h-[400px] rounded-2xl bg-white border-2 border-gray-200 focus:border-[#C93831] font-medium text-base resize-none transition-all"
                    />
                  </div>

                  {/* Submit */}
                  <Button 
                    onClick={handleCreatePost}
                    disabled={!isWorkoutVerified}
                    className="w-full h-16 rounded-2xl bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-black text-xl border-0 shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Zap className="w-6 h-6 mr-2" />
                    게시하기
                  </Button>
                </div>
              </Card>
            </div>
          </div>
        )}

        {/* Profile */}
        {selectedNav === "profile" && (
          <div className="h-full overflow-auto p-8 bg-gray-50/50">
            <div className="max-w-4xl mx-auto space-y-8">
              <div>
                <h1 className="text-5xl font-black text-gray-900 mb-2">마이페이지</h1>
                <p className="text-gray-700 font-medium text-lg">내 정보를 관리하세요</p>
              </div>

              <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
                <div className="p-8">
                  <div className="flex items-center gap-6 mb-8">
                    <div className="relative">
                      <Avatar className="w-24 h-24 border-4 border-white shadow-xl">
                        {profileImage ? (
                          <img src={profileImage} alt="Profile" className="w-full h-full object-cover" />
                        ) : (
                          <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white text-3xl font-black">
                            김
                          </AvatarFallback>
                        )}
                      </Avatar>
                      {isEditingProfile && (
                        <button
                          onClick={() => profileImageInputRef.current?.click()}
                          className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-[#C93831] text-white flex items-center justify-center shadow-lg hover:bg-[#B02F28]"
                        >
                          <Camera className="w-4 h-4" />
                        </button>
                      )}
                      <input
                        ref={profileImageInputRef}
                        type="file"
                        accept="image/*"
                        onChange={handleProfileImageChange}
                        className="hidden"
                      />
                    </div>
                    <div>
                      <h2 className="text-3xl font-black text-gray-900 mb-2">김루핀</h2>
                      <p className="text-gray-600 font-medium">EMP001</p>
                    </div>
                    <Button
                      onClick={() => setIsEditingProfile(!isEditingProfile)}
                      variant="outline"
                      className="ml-auto rounded-xl"
                    >
                      <Edit className="w-4 h-4 mr-2" />
                      {isEditingProfile ? "저장" : "수정"}
                    </Button>
                  </div>

                  <div className="space-y-4">
                    <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                      <div className="text-sm text-gray-600 font-medium mb-1">이메일</div>
                      <div className="font-bold text-gray-900">lupin@company.com</div>
                    </div>
                    
                    <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                      <div className="text-sm text-gray-600 font-medium mb-1">부서</div>
                      <div className="font-bold text-gray-900">개발팀</div>
                    </div>

                    <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                      <div className="text-sm text-gray-600 font-medium mb-2">키 (cm)</div>
                      {isEditingProfile ? (
                        <Input
                          type="number"
                          value={height}
                          onChange={(e) => setHeight(e.target.value)}
                          className="rounded-xl border-2 border-gray-200"
                        />
                      ) : (
                        <div className="font-bold text-gray-900">{height}cm</div>
                      )}
                    </div>

                    <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                      <div className="text-sm text-gray-600 font-medium mb-2">몸무게 (kg)</div>
                      {isEditingProfile ? (
                        <Input
                          type="number"
                          value={weight}
                          onChange={(e) => setWeight(e.target.value)}
                          className="rounded-xl border-2 border-gray-200"
                        />
                      ) : (
                        <div className="font-bold text-gray-900">{weight}kg</div>
                      )}
                    </div>
                  </div>

                  <div className="mt-8 pt-8 border-t border-gray-200">
                    <Button 
                      onClick={onLogout}
                      variant="outline" 
                      className="w-full h-14 rounded-2xl border-2 border-red-300 text-red-600 hover:bg-red-50 font-bold text-lg"
                    >
                      로그아웃
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        )}
      </div>

      {/* Feed Detail Modal - Home Only */}
      <Dialog open={showFeedDetailInHome && selectedNav === "home"} onOpenChange={() => {
        setShowFeedDetailInHome(false);
        setSelectedFeed(null);
      }}>
        <DialogContent className="max-w-md p-0 max-h-[90vh]">
          <DialogHeader className="sr-only">
            <DialogTitle>피드 상세보기</DialogTitle>
            <DialogDescription>피드의 상세 내용을 확인할 수 있습니다.</DialogDescription>
          </DialogHeader>
          {selectedFeed && (
            <div style={{ height: '85vh' }} className="relative">
              {/* Image Carousel */}
              <div className="relative h-3/4">
                <img 
                  src={selectedFeed.images[getFeedImageIndex(selectedFeed.id)] || selectedFeed.images[0]} 
                  alt={selectedFeed.activity}
                  className="w-full h-full object-cover"
                />
                
                {selectedFeed.images.length > 1 && (
                  <>
                    <button
                      onClick={() => setFeedImageIndex(selectedFeed.id, Math.max(0, getFeedImageIndex(selectedFeed.id) - 1))}
                      className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                    >
                      <ChevronLeft className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => setFeedImageIndex(selectedFeed.id, Math.min(selectedFeed.images.length - 1, getFeedImageIndex(selectedFeed.id) + 1))}
                      className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                    >
                      <ChevronRight className="w-5 h-5" />
                    </button>
                    <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                      {selectedFeed.images.map((_, idx) => (
                        <div key={idx} className={`w-1.5 h-1.5 rounded-full ${idx === getFeedImageIndex(selectedFeed.id) ? 'bg-white' : 'bg-white/50'}`}></div>
                      ))}
                    </div>
                  </>
                )}

                {/* Author Info */}
                <div className="absolute top-4 left-4 flex items-center gap-3 backdrop-blur-xl bg-white/20 rounded-full px-4 py-2 border border-white/30">
                  <Avatar className="w-8 h-8 border-2 border-white">
                    <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white font-black text-sm">
                      {selectedFeed.avatar}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="text-white text-xs font-bold">{selectedFeed.author}</div>
                    <div className="text-white/80 text-xs">{selectedFeed.time}</div>
                  </div>
                </div>

                {/* Right Actions */}
                <div className="absolute right-4 bottom-4 flex flex-col gap-4">
                  <button className="flex flex-col items-center gap-1 group">
                    <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                      <Heart className="w-5 h-5 text-white" />
                    </div>
                    <span className="text-white text-xs font-bold">{selectedFeed.likes}</span>
                  </button>

                  <button 
                    className="flex flex-col items-center gap-1 group"
                    onClick={() => {
                      toast.info("댓글을 보려면 피드 메뉴에서 확인하세요");
                    }}
                  >
                    <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                      <MessageCircle className="w-5 h-5 text-white" />
                    </div>
                    <span className="text-white text-xs font-bold">{selectedFeed.comments}</span>
                  </button>
                </div>
              </div>

              {/* Content */}
              <div className="p-6 space-y-3 h-1/4 overflow-auto bg-white">
                <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
                  <Sparkles className="w-3 h-3 mr-1" />
                  +{selectedFeed.points}
                </Badge>

                <p className="text-gray-700 font-medium text-sm leading-relaxed">
                  {selectedFeed.content}
                </p>

                <div className="flex gap-2 flex-wrap">
                  <Badge className="bg-white border border-gray-300 text-gray-700 px-3 py-1 font-bold text-xs">
                    {selectedFeed.duration}
                  </Badge>
                  {Object.entries(selectedFeed.stats).map(([key, value]) => (
                    <Badge key={key} className="bg-red-50 border border-red-200 text-[#C93831] px-3 py-1 font-bold text-xs">
                      {value}
                    </Badge>
                  ))}
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Prescription Detail Dialog */}
      <Dialog open={!!selectedPrescription} onOpenChange={() => setSelectedPrescription(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">처방전 상세</DialogTitle>
            <DialogDescription>처방전의 상세 내용을 확인하고 PDF로 다운로드할 수 있습니다.</DialogDescription>
          </DialogHeader>
          {selectedPrescription && (
            <div className="space-y-6 p-4">
              <div className="p-6 rounded-2xl bg-gradient-to-br from-blue-50 to-cyan-50 border border-blue-200">
                <div className="text-sm text-gray-600 mb-2">처방명</div>
                <div className="text-2xl font-black text-gray-900 mb-4">{selectedPrescription.name}</div>
                
                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div>
                    <div className="text-sm text-gray-600 mb-1">처방 날짜</div>
                    <div className="font-bold text-gray-900">{selectedPrescription.date}</div>
                  </div>
                  <div>
                    <div className="text-sm text-gray-600 mb-1">담당 의사</div>
                    <div className="font-bold text-gray-900">{selectedPrescription.doctor} 원장</div>
                  </div>
                </div>
                
                <div className="mb-4">
                  <div className="text-sm text-gray-600 mb-2">진단명</div>
                  <div className="font-bold text-gray-900">{selectedPrescription.diagnosis}</div>
                </div>
                
                <div className="mb-4">
                  <div className="text-sm text-gray-600 mb-2">처방 약물</div>
                  <div className="space-y-2">
                    {selectedPrescription.medicines.map((med, idx) => (
                      <div key={idx} className="p-2 bg-white rounded-lg border border-blue-200">
                        <div className="font-bold text-gray-900">{med}</div>
                      </div>
                    ))}
                  </div>
                </div>
                
                <div>
                  <div className="text-sm text-gray-600 mb-2">복용 방법</div>
                  <div className="text-gray-700 font-medium">{selectedPrescription.instructions}</div>
                </div>
              </div>
              
              <Button
                onClick={() => downloadPrescriptionPDF(selectedPrescription)}
                className="w-full bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 text-white font-bold rounded-2xl h-12"
              >
                <Download className="w-5 h-5 mr-2" />
                PDF 다운로드
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Chat Dialog - Working Chat */}
      <Dialog open={showChat} onOpenChange={setShowChat}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">진료 채팅</DialogTitle>
            <DialogDescription>의료진과 실시간으로 채팅할 수 있습니다.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4 p-4">
            <ScrollArea className="h-96">
              <div className="space-y-4 pr-4">
                {medicalChatMessages.map((msg) => (
                  <div key={msg.id} className={`flex gap-3 ${msg.isMine ? 'justify-end' : ''}`}>
                    {!msg.isMine && (
                      <Avatar className="w-8 h-8">
                        <AvatarFallback className="bg-blue-500 text-white font-black text-xs">{msg.avatar}</AvatarFallback>
                      </Avatar>
                    )}
                    <div className={`rounded-2xl p-3 max-w-xs ${msg.isMine ? 'bg-[#C93831] text-white' : 'bg-gray-100'}`}>
                      {!msg.isMine && <div className="font-bold text-xs text-gray-900 mb-1">{msg.author}</div>}
                      <div className="text-sm">{msg.content}</div>
                      <div className={`text-xs mt-1 ${msg.isMine ? 'text-white/80' : 'text-gray-500'}`}>{msg.time}</div>
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
            
            <div className="flex gap-2 pt-4">
              <Input 
                placeholder="메시지 입력..." 
                className="rounded-xl"
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    handleSendMedicalChat();
                  }
                }}
              />
              <Button 
                onClick={handleSendMedicalChat}
                className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
              >
                <Send className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Prescription Form Dialog */}
      <Dialog open={showPrescriptionForm && !!prescriptionPatient} onOpenChange={(open) => {
        setShowPrescriptionForm(open);
        if (!open) setPrescriptionPatient(null);
      }}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">처방전 작성</DialogTitle>
            <DialogDescription>환자의 진단 및 처방 정보를 입력하세요.</DialogDescription>
          </DialogHeader>
          
          {prescriptionPatient && (
            <div className="space-y-6">
              {/* Patient Info */}
              <div className="p-4 rounded-2xl bg-gradient-to-br from-blue-50 to-cyan-50 border border-blue-200">
                <div className="flex items-center gap-4">
                  <Avatar className="w-16 h-16">
                    <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-500 text-white font-black text-xl">
                      {prescriptionPatient.avatar}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="font-black text-xl text-gray-900">{prescriptionPatient.name}</div>
                    <div className="text-sm text-gray-600">
                      {prescriptionPatient.age}세 · {prescriptionPatient.gender}
                    </div>
                    <div className="text-sm text-gray-600">
                      마지막 방문: {prescriptionPatient.lastVisit}
                    </div>
                  </div>
                </div>
              </div>

              {/* Diagnosis */}
              <div>
                <Label className="text-base font-black mb-2 block">진단명</Label>
                <Input 
                  placeholder="진단명을 입력하세요 (예: 급성 상기도 감염)" 
                  className="rounded-xl"
                />
              </div>

              {/* Symptoms */}
              <div>
                <Label className="text-base font-black mb-2 block">증상</Label>
                <Textarea 
                  placeholder="환자의 주요 증상을 입력하세요"
                  className="rounded-xl min-h-[100px]"
                />
              </div>

              {/* Medicines */}
              <div>
                <Label className="text-base font-black mb-2 block">처방 의약품</Label>
                <div className="space-y-3">
                  <div className="flex gap-2">
                    <Input placeholder="약품명" className="rounded-xl flex-1" />
                    <Input placeholder="용량" className="rounded-xl w-32" />
                    <Input placeholder="횟수/일" className="rounded-xl w-32" />
                    <Input placeholder="일수" className="rounded-xl w-24" />
                  </div>
                  <Button 
                    variant="outline" 
                    className="w-full rounded-xl border-2 border-dashed border-gray-300 hover:border-[#C93831] hover:bg-red-50"
                  >
                    <PlusSquare className="w-4 h-4 mr-2" />
                    약품 추가
                  </Button>
                </div>
              </div>

              {/* Instructions */}
              <div>
                <Label className="text-base font-black mb-2 block">복용 방법 및 주의사항</Label>
                <Textarea 
                  placeholder="복용 방법, 주의사항, 부작용 등을 입력하세요"
                  className="rounded-xl min-h-[120px]"
                />
              </div>

              {/* Next Appointment */}
              <div>
                <Label className="text-base font-black mb-2 block">다음 진료 예정일</Label>
                <Input 
                  type="date"
                  className="rounded-xl"
                />
              </div>

              {/* Action Buttons */}
              <div className="flex gap-3 pt-4">
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowPrescriptionForm(false);
                    setPrescriptionPatient(null);
                  }}
                  className="flex-1 rounded-2xl h-12 font-bold"
                >
                  취소
                </Button>
                <Button
                  onClick={() => {
                    toast.success("처방전이 저장되었습니다.");
                    setShowPrescriptionForm(false);
                    setPrescriptionPatient(null);
                  }}
                  className="flex-1 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-bold rounded-2xl h-12"
                >
                  <CheckCircle className="w-5 h-5 mr-2" />
                  처방전 저장
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Appointment Dialog - Fixed date selection */}
      <Dialog open={showAppointment} onOpenChange={setShowAppointment}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">진료 예약</DialogTitle>
            <DialogDescription>진료과와 날짜, 시간을 선택하여 예약할 수 있습니다.</DialogDescription>
          </DialogHeader>
          <div className="space-y-6">
            <div>
              <Label className="text-base font-black mb-2 block">진료과 선택</Label>
              <Select value={selectedDepartment} onValueChange={setSelectedDepartment}>
                <SelectTrigger className="rounded-xl">
                  <SelectValue placeholder="진료과를 선택하세요" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="internal">내과</SelectItem>
                  <SelectItem value="surgery">외과</SelectItem>
                  <SelectItem value="psychiatry">신경정신과</SelectItem>
                  <SelectItem value="dermatology">피부과</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {selectedDepartment && (
              <>
                <div>
                  <Label className="text-base font-black mb-2 block">날짜 선택</Label>
                  <Calendar
                    mode="single"
                    selected={selectedDate}
                    onSelect={setSelectedDate}
                    modifiers={{
                      available: availableDates
                    }}
                    modifiersStyles={{
                      available: {
                        fontWeight: 'bold',
                        color: '#C93831'
                      }
                    }}
                    className="rounded-xl border"
                  />
                  <p className="text-xs text-gray-600 mt-2">* 빨간색 날짜만 선택 가능합니다</p>
                </div>

                {selectedDate && (
                  <div>
                    <Label className="text-base font-black mb-2 block">시간 선택</Label>
                    <div className="grid grid-cols-3 gap-2">
                      {availableTimes.map((time) => {
                        const isBooked = bookedTimes.includes(time);
                        const isSelected = selectedTime === time;
                        return (
                          <Button
                            key={time}
                            variant={isSelected ? "default" : "outline"}
                            disabled={isBooked}
                            onClick={() => setSelectedTime(time)}
                            className={`rounded-xl ${isSelected ? 'bg-[#C93831]' : ''} ${isBooked ? 'opacity-50' : ''}`}
                          >
                            {time}
                            {isBooked && " (예약됨)"}
                          </Button>
                        );
                      })}
                    </div>
                  </div>
                )}
              </>
            )}

            <Button
              disabled={!selectedDepartment || !selectedDate || !selectedTime}
              className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-xl h-12"
              onClick={() => {
                toast.success("예약이 완료되었습니다!");
                setShowAppointment(false);
                setSelectedDepartment("");
                setSelectedDate(undefined);
                setSelectedTime("");
              }}
            >
              예약 확인
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
