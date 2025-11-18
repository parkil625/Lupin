import { Notification } from "@/types/dashboard.types";

export const initialNotifications: Notification[] = [
  { id: 1, type: "challenge", title: "웰빙 챌린지 시작!", content: "오늘 오후 6시에 새로운 챌린지가 시작됩니다.", time: "1시간 전", read: false },
  { id: 2, type: "like", title: "이철수님이 좋아요를 눌렀습니다", content: "스쿼트 100kg 달성! 💪 게시물", time: "3시간 전", read: false },
  { id: 3, type: "comment", title: "박영희님이 댓글을 남겼습니다", content: "대단해요! 👍", time: "5시간 전", read: false },
  { id: 4, type: "appointment", title: "진료 예약 확인", content: "11월 15일 오후 3시 내과 상담이 예정되어 있습니다.", time: "1일 전", read: true },
  { id: 5, type: "like", title: "최민수님이 좋아요를 눌렀습니다", content: "오늘의 런닝 10km 완주! 게시물", time: "2시간 전", read: false },
  { id: 6, type: "comment", title: "정수진님이 댓글을 남겼습니다", content: "저도 같이 가고 싶어요!", time: "4시간 전", read: false },
  { id: 7, type: "challenge", title: "주간 챌린지 달성!", content: "이번 주 목표를 모두 완료하셨습니다. 축하합니다!", time: "6시간 전", read: true },
  { id: 8, type: "like", title: "강민호님이 좋아요를 눌렀습니다", content: "아침 요가 30분 게시물", time: "8시간 전", read: true },
  { id: 9, type: "comment", title: "윤서연님이 댓글을 남겼습니다", content: "요가 자세가 정말 멋져요!", time: "10시간 전", read: true },
  { id: 10, type: "appointment", title: "진료 예약 알림", content: "내일 오전 10시 정형외과 진료가 예정되어 있습니다.", time: "12시간 전", read: true },
  { id: 11, type: "like", title: "장동건님이 좋아요를 눌렀습니다", content: "저녁 수영 1시간 게시물", time: "1일 전", read: true },
  { id: 12, type: "comment", title: "송혜교님이 댓글을 남겼습니다", content: "수영장 어디인가요?", time: "1일 전", read: true },
  { id: 13, type: "challenge", title: "새로운 챌린지 참여!", content: "한 달 걷기 챌린지에 참여하셨습니다.", time: "2일 전", read: true },
  { id: 14, type: "like", title: "전지현님이 좋아요를 눌렀습니다", content: "필라테스 클래스 후기 게시물", time: "2일 전", read: true },
  { id: 15, type: "comment", title: "현빈님이 댓글을 남겼습니다", content: "필라테스 같이 해요!", time: "2일 전", read: true },
  { id: 16, type: "appointment", title: "진료 완료", content: "11월 10일 건강검진이 완료되었습니다.", time: "3일 전", read: true },
  { id: 17, type: "like", title: "이철수님이 좋아요를 눌렀습니다", content: "크로스핏 WOD 완료 게시물", time: "3일 전", read: true },
  { id: 18, type: "comment", title: "박영희님이 댓글을 남겼습니다", content: "와 대박이에요!", time: "3일 전", read: true }
];
