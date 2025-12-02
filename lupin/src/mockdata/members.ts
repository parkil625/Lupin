import { Member, Appointment } from "@/types/dashboard.types";

export const members: Member[] = [
  {
    id: 1,
    name: "김건강",
    avatar: "김",
    age: 45,
    gender: "남",
    lastVisit: "2024-01-15",
    condition: "고혈압 관리",
    status: "waiting",
  },
  {
    id: 2,
    name: "이헬스",
    avatar: "이",
    age: 38,
    gender: "여",
    lastVisit: "2024-01-12",
    condition: "당뇨병 관리",
    status: "in-progress",
  },
  {
    id: 3,
    name: "박피트",
    avatar: "박",
    age: 52,
    gender: "남",
    lastVisit: "2024-01-10",
    condition: "심장 건강",
    status: "completed",
  },
];

export const appointments: Appointment[] = [
  {
    id: 1,
    memberName: "김건강",
    memberAvatar: "김",
    department: "내과",
    date: "2024-01-20",
    time: "10:00",
    status: "scheduled",
    reason: "정기 검진",
  },
  {
    id: 2,
    memberName: "이헬스",
    memberAvatar: "이",
    department: "내과",
    date: "2024-01-20",
    time: "11:00",
    status: "scheduled",
    reason: "혈당 체크",
  },
  {
    id: 3,
    memberName: "박피트",
    memberAvatar: "박",
    department: "심장내과",
    date: "2024-01-19",
    time: "14:00",
    status: "completed",
    reason: "심전도 검사",
  },
];
