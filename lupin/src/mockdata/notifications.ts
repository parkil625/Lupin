import { Notification } from "@/types/dashboard.types";

export const initialNotifications: Notification[] = [
  { id: 1, type: "challenge", title: "웰빙 챌린지 시작!", content: "오늘 오후 6시에 새로운 챌린지가 시작됩니다.", time: "1시간 전", read: false },
  { id: 2, type: "like", title: "이철수님이 좋아요를 눌렀습니다", content: "스쿼트 100kg 달성! 💪 게시물", time: "3시간 전", read: false },
  { id: 3, type: "comment", title: "박영희님이 댓글을 남겼습니다", content: "대단해요! 👍", time: "5시간 전", read: true },
  { id: 4, type: "appointment", title: "진료 예약 확인", content: "11월 15일 오후 3시 내과 상담이 예정되어 있습니다.", time: "1일 전", read: true }
];
