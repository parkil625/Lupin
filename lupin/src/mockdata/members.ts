import { Member, Appointment, ChatMessage } from "@/types/dashboard.types";

export const members: Member[] = [
  { id: 1, name: "김루핀", avatar: "김", age: 32, gender: "남", lastVisit: "2024-11-10", condition: "정기 검진", status: "waiting" },
  { id: 2, name: "이철수", avatar: "이", age: 45, gender: "남", lastVisit: "2024-11-09", condition: "고혈압", status: "in-progress" },
  { id: 3, name: "박영희", avatar: "박", age: 28, gender: "여", lastVisit: "2024-11-08", condition: "감기", status: "completed" },
  { id: 4, name: "최민수", avatar: "최", age: 38, gender: "남", lastVisit: "2024-11-07", condition: "당뇨 관리", status: "completed" },
  { id: 5, name: "정수진", avatar: "정", age: 35, gender: "여", lastVisit: "2024-11-06", condition: "알레르기", status: "completed" }
];

export const appointments: Appointment[] = [
  { id: 1, memberName: "김루핀", memberAvatar: "김", department: "내과", date: "11월 15일", time: "오후 3시", status: "scheduled", reason: "정기 검진" },
  { id: 2, memberName: "이철수", memberAvatar: "이", department: "내과", date: "11월 14일", time: "오전 10시", status: "scheduled", reason: "고혈압 상담" },
  { id: 3, memberName: "박영희", memberAvatar: "박", department: "내과", date: "11월 13일", time: "오후 2시", status: "completed", reason: "감기 치료" },
  { id: 4, memberName: "최민수", memberAvatar: "최", department: "내과", date: "11월 12일", time: "오전 11시", status: "completed", reason: "당뇨 관리" }
];

export const initialDoctorChats: { [key: number]: ChatMessage[] } = {
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

export const initialMedicalChat: ChatMessage[] = [
  { id: 1, author: "김의사", avatar: "의", content: "안녕하세요. 어떤 증상으로 방문하셨나요?", time: "오후 3:00", isMine: false },
  { id: 2, author: "김루핀", avatar: "김", content: "감기 증상이 있어서요", time: "오후 3:02", isMine: true }
];
