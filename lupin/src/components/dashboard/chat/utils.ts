// 시간 포맷 함수 (카톡 스타일)
export const formatChatTime = (timeString?: string): string => {
  if (!timeString) return "";

  const messageTime = new Date(timeString);
  const today = new Date();

  // 오늘인지 확인
  const isToday = messageTime.toDateString() === today.toDateString();

  if (isToday) {
    // 오늘이면 시간만 표시 (오후 3:45)
    return messageTime.toLocaleTimeString("ko-KR", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  } else {
    // 오늘이 아니면 날짜 표시 (12월 11일)
    return messageTime.toLocaleDateString("ko-KR", {
      month: "long",
      day: "numeric",
    });
  }
};

// 예약 시간 5분 전부터 입장 가능한지 확인하는 함수
export const canEnterChatRoom = (appointmentTime?: string, status?: string): boolean => {
  // 이미 진료 중이면 무조건 입장 가능
  if (status === "IN_PROGRESS") return true;

  // 예약 예정 상태인 경우, 예약 시간 5분 전부터 입장 가능
  if (status === "SCHEDULED" && appointmentTime) {
    const appointmentDate = new Date(appointmentTime);
    const now = new Date();
    const fiveMinutesBefore = new Date(
      appointmentDate.getTime() - 5 * 60 * 1000
    );

    return now >= fiveMinutesBefore;
  }

  return false;
};
