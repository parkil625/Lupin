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

  // [핵심 해결] 재귀 호출을 위해 connect 함수를 담을 Ref 생성
  const connectRef = useRef<() => void>(() => {});

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

    // [수정] 항상 최신 토큰을 가져옴 (재연결 시에도 갱신된 토큰 사용)
    const token = localStorage.getItem("accessToken");
    if (!token) {
      // 토큰 없을 때는 로그 출력하지 않음 (로그인 전 정상 상태)
      return;
    }

    // SSE URL 구성 (매번 최신 토큰으로 URL 생성)
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
        // [수정] Ref를 통해 안전하게 자기 자신(재연결) 호출
        connectRef.current();
      }, delay);
    };

    eventSource.addEventListener("connect", () => {
      console.log("[SSE] 연결 성공 (Event: connect)");
      isConnectedRef.current = true;
      reconnectAttemptsRef.current = 0;
      lastConnectTimeRef.current = Date.now();
    });

    // Heartbeat 이벤트
    eventSource.addEventListener("heartbeat", () => {
      console.log("[SSE] Heartbeat 수신");
    });

    // [핵심] 기본 메시지(message) 이벤트 수신 추가
    eventSource.onmessage = (event) => {
      try {
        // Heartbeat나 연결 메시지인 경우 무시
        if (event.data === "connected" || event.data.includes("heartbeat")) {
          return;
        }

        const notification: Notification = JSON.parse(event.data);
        console.log("[SSE] 기본 메시지 수신 (onmessage):", notification);
        onNotificationReceivedRef.current(notification);
      } catch (error) {
        console.error("[SSE] 기본 메시지 파싱 에러:", error, event.data);
      }
    };

    // [핵심] notification 이벤트 수신 (백엔드에서 event: notification으로 보낼 경우)
    eventSource.addEventListener("notification", (event) => {
      try {
        const notification: Notification = JSON.parse(event.data);
        console.log("[SSE] Named Event(notification) 수신:", notification);
        onNotificationReceivedRef.current(notification);
      } catch (error) {
        console.error("[SSE] Notification 이벤트 파싱 에러:", error);
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

  // [수정] Ref에 최신 함수 할당 (이게 있어야 connectRef.current()가 작동함)
  useEffect(() => {
    connectRef.current = connectInternal;
  }, [connectInternal]);

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

    // [추가] 25분마다 재연결 (토큰 만료 30분 전에 갱신된 토큰으로 재연결)
    const tokenRefreshInterval = setInterval(() => {
      if (enabled && localStorage.getItem("accessToken")) {
        console.log("[SSE] 토큰 갱신 주기 - SSE 재연결");
        connectInternal();
      }
    }, 25 * 60 * 1000); // 25분

    return () => {
      disconnect();
      clearInterval(tokenRefreshInterval);
    };
  }, [connectInternal, disconnect, enabled]);

  return {
    reconnect: connectInternal,
    disconnect,
  };
};
