import { Prescription } from "@/types/dashboard.types";

export const prescriptions: Prescription[] = [
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
