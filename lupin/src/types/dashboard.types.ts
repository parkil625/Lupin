export interface DashboardProps {
  onLogout: () => void;
  userType: "member" | "doctor";
}

export interface Feed {
  id: number;
  writerId: number;
  writerName: string;
  writerAvatar?: string;
  writerDepartment?: string;
  writerActiveDays?: number;
  activity: string;
  points: number;
  content: string;
  images: string[];
  likes: number;
  comments: number;
  calories?: number;
  createdAt: string;
  updatedAt?: string;
  isLiked?: boolean;
  isReported?: boolean; // [추가] 서버에서 오는 신고 여부
  // 프론트엔드 전용 (API 응답에서 계산)
  time?: string;
  author?: string; // writerName alias (하위호환)
  isMine?: boolean;
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
  likeCount?: number;
  isLiked?: boolean;
  updatedAt?: string;
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
  type: string; // "FEED_LIKE", "COMMENT", "COMMENT_LIKE", "REPLY"
  title: string;
  content?: string;
  isRead: boolean;
  refId?: string;
  targetId?: number; // 하이라이트할 댓글/답글 ID
  actorProfileImage?: string; // 알림 발생시킨 사용자 프로필 이미지
  createdAt: string;
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

export interface Department {
  id: string;
  name: string;
}

export interface Doctor {
  id: string;
  name: string;
  departmentId: string;
  avatar?: string;
  isAvailable?: boolean;
}
