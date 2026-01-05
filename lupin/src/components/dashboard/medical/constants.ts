// 예약 가능 시간
export const AVAILABLE_TIMES = [
  "09:00",
  "10:00",
  "11:00",
  "14:00",
  "15:00",
  "16:00",
  "17:00",
];

// 진료과 한글 이름 매핑
export const DEPARTMENT_NAMES: Record<string, string> = {
  internal: "내과",
  surgery: "외과",
  psychiatry: "신경정신과",
  dermatology: "피부과",
  thoracic_surgery: "흉부외과",
  obstetrics_gynecology: "산부인과",
};

// 진료과 한글 이름 → 영문 키 역매핑 (예약 변경 시 사용)
export const DEPARTMENT_KEYS: Record<string, string> = {
  "내과": "internal",
  "외과": "surgery",
  "신경정신과": "psychiatry",
  "피부과": "dermatology",
  "흉부외과": "thoracic_surgery",
  "산부인과": "obstetrics_gynecology",
};

// 예약 상태 설정
export const STATUS_CONFIG = {
  SCHEDULED: { label: "진료 예정", color: "#7950F2" },
  IN_PROGRESS: { label: "진료 중", color: "#20C997" },
  COMPLETED: { label: "진료 완료", color: "#868E96" },
  CANCELLED: { label: "취소됨", color: "#E03131" },
} as const;
