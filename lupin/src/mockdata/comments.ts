import { Comment } from "@/types/dashboard.types";

export const initialComments: { [key: number]: Comment[] } = {
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
