// 지난 날짜인지 확인
export const isPastDate = (date: Date): boolean => {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const dateOnly = new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate()
  );
  return dateOnly < today;
};

// 오늘인지 확인
export const isToday = (date: Date | undefined): boolean => {
  if (!date) return false;
  const now = new Date();
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  );
};

// 시간이 지났는지 확인 (오늘인 경우만)
export const isPastTime = (time: string, selectedDate?: Date): boolean => {
  if (!selectedDate || !isToday(selectedDate)) return false;
  const now = new Date();
  const [hours, minutes] = time.split(":").map(Number);
  const currentHours = now.getHours();
  const currentMinutes = now.getMinutes();
  return (
    hours < currentHours ||
    (hours === currentHours && minutes <= currentMinutes)
  );
};

// 날짜를 YYYY-MM-DD 형식으로 변환
export const formatDateToString = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

// 날짜 + 시간을 ISO 8601 형식으로 변환
export const formatDateTime = (date: Date, time: string): string => {
  const [hours, minutes] = time.split(":").map(Number);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hoursStr = String(hours).padStart(2, "0");
  const minutesStr = String(minutes).padStart(2, "0");
  return `${year}-${month}-${day}T${hoursStr}:${minutesStr}:00`;
};
