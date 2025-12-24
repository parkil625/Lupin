import { useEffect, useRef, useCallback } from "react";
import { Notification } from "@/types/dashboard.types";

interface UseNotificationSseProps {
  onNotificationReceived: (notification: Notification) => void;
  enabled?: boolean;
}

export const useNotificationSse = ({
  onNotificationReceived,
  enabled = true,
}: UseNotificationSseProps) => {
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(
    null
  );
  const reconnectAttemptsRef = useRef(0);
  const isConnectedRef = useRef(false);
  const lastConnectTimeRef = useRef<number>(0);
  // 콜백을 ref로 저장하여 의존성 문제 해결
  const onNotificationReceivedRef = useRef(onNotificationReceived);

  // 콜백이 변경되면 ref 업데이트 (리렌더링 유발 없음)
  useEffect(() => {
    onNotificationReceivedRef.current = onNotificationReceived;
  }, [onNotificationReceived]);

  const connectInternal = useCallback(() => {
    if (!enabled) return;

    // 기존 연결 정리
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const token = localStorage.getItem("accessToken");
    if (!token) {
      // 토큰 없을 때는 로그 출력하지 않음 (로그인 전 정상 상태)
      return;
    }

    // SSE URL 구성
    const isLocal = window.location.hostname === "localhost";
    const baseUrl = isLocal ? "http://localhost:8081" : window.location.origin;
    const sseUrl = `${baseUrl}/api/notifications/subscribe?token=${encodeURIComponent(
      token
    )}`;

    const eventSource = new EventSource(sseUrl);
    eventSourceRef.current = eventSource;

    const scheduleReconnect = () => {
      // 재연결 대기 시간: Exponential backoff
      const baseDelay = 5000; // 5초
      const maxDelay = 30000; // 최대 30초
      const delay = Math.min(
        baseDelay * Math.pow(1.5, reconnectAttemptsRef.current),
        maxDelay
      );

      // 재연결 시도 (로그 없이)
      reconnectTimeoutRef.current = setTimeout(() => {
        connectInternal();
      }, delay);
    };

    eventSource.addEventListener("connect", () => {
      isConnectedRef.current = true;
      reconnectAttemptsRef.current = 0; // 연결 성공 시 재시도 카운터 리셋
      lastConnectTimeRef.current = Date.now();
    });

    // Heartbeat 이벤트 (연결 유지용, 로그 생략)
    eventSource.addEventListener("heartbeat", () => {
      // 연결 유지용 heartbeat - 별도 처리 불필요
    });

    eventSource.addEventListener("notification", (event) => {
      try {
        const notification: Notification = JSON.parse(event.data);
        // 알림 수신 시에만 로그 출력 (중요한 이벤트)
        console.log("[SSE] 알림 수신:", notification);
        onNotificationReceivedRef.current(notification);
      } catch (error) {
        console.error("[SSE] 알림 파싱 에러:", error);
      }
    });

    eventSource.onerror = () => {
      const wasConnected = isConnectedRef.current;
      isConnectedRef.current = false;

      eventSource.close();
      eventSourceRef.current = null;

      // 정상적인 타임아웃 재연결은 로그 출력하지 않음
      // 연결 실패가 3회 이상 반복되는 경우에만 경고 로그 출력
      if (!wasConnected) {
        reconnectAttemptsRef.current += 1;

        if (reconnectAttemptsRef.current >= 3) {
          reconnectAttemptsRef.current = 0;
        }
      }

      scheduleReconnect();
    };
  }, [enabled]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      // 연결 해제 로그 제거 (정상적인 동작)
    }
  }, []);

  useEffect(() => {
    connectInternal();

    return () => {
      disconnect();
    };
  }, [connectInternal, disconnect]);

  return {
    reconnect: connectInternal,
    disconnect,
  };
};
