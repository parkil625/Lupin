import { Comment } from "@/types/dashboard.types";

export const initialComments: { [key: number]: Comment[] } = {
  5: [
    { id: 1, author: "박영희", avatar: "박", content: "대단해요! 👍", time: "1시간 전", replies: [], department: "기획팀", activeDays: 26, avgScore: 48, points: 480 },
    { id: 2, author: "이철수", avatar: "이", content: "저도 열심히 해야겠어요", time: "30분 전", replies: [], department: "개발팀", activeDays: 28, avgScore: 52, points: 520 },
    { id: 3, author: "최민수", avatar: "최", content: "응원합니다! 💪", time: "10분 전", replies: [], department: "영업팀", activeDays: 25, avgScore: 45, points: 450 }
  ],
  6: [
    { id: 4, author: "김루핀", avatar: "김", content: "멋져요!", time: "2시간 전", replies: [], department: "개발팀", activeDays: 18, avgScore: 48, points: 138 }
  ],
  7: [
    { id: 5, author: "정수진", avatar: "정", content: "저도 요가 시작해볼까요?", time: "1일 전", replies: [], department: "디자인팀", activeDays: 24, avgScore: 44, points: 420 }
  ]
};
