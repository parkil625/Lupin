export interface DashboardProps {
  onLogout: () => void;
  userType: "member" | "doctor";
}

export interface Feed {
  id: number;
  authorId?: number;
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
  edited?: boolean;
  streak?: number;
  department?: string;
  activeDays?: number;
  avgScore?: number;
}

export interface Comment {
  id: number;
  author: string;
  avatar: string;
  content: string;
  time: string;
  parentId?: number;
  replies?: Comment[];
  department?: string;
  activeDays?: number;
  avgScore?: number;
  points?: number;
  isDeleted?: boolean;
}

export interface Prescription {
  id: number;
  name: string;
  date: string;
  doctor: string;
  medicines: string[];
  diagnosis: string;
  instructions: string;
}

export interface Notification {
  id: number;
  type: "challenge" | "appointment" | "like" | "comment" | "reply" | "chat" | "comment_like";
  title: string;
  content: string;
  time: string;
  read: boolean;
  feedId?: number;  // 피드 관련 알림일 경우
  commentId?: number;  // 댓글 관련 알림일 경우
  chatRoomId?: number;  // 채팅 관련 알림일 경우
}

export interface Member {
  id: number;
  name: string;
  avatar: string;
  age: number;
  gender: string;
  lastVisit: string;
  condition: string;
  status: "waiting" | "in-progress" | "completed";
}

export interface Appointment {
  id: number;
  memberName: string;
  memberAvatar: string;
  department: string;
  date: string;
  time: string;
  status: "scheduled" | "completed" | "cancelled";
  reason: string;
}

export interface ChatMessage {
  id: number;
  author: string;
  avatar: string;
  content: string;
  time: string;
  isMine: boolean;
}
