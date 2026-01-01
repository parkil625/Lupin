import { useEffect, useRef, useCallback } from "react";
import { Notification } from "@/types/dashboard.types";

interface UseNotificationSseProps {
  onNotificationReceived: (notification: Notification) => void;
  // [추가] 알림 삭제 이벤트를 처리할 콜백 (삭제된 ID 목록을 받음)
  onNotificationDeleted: (notificationIds: number[]) => void;
  enabled?: boolean;
}

export const useNotificationSse = ({
  onNotificationReceived,
  onNotificationDeleted, // [추가]
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
  // [추가] 삭제 콜백 ref
  const onNotificationDeletedRef = useRef(onNotificationDeleted);

  // 콜백이 변경되면 ref 업데이트 (리렌더링 유발 없음)
  useEffect(() => {
    onNotificationReceivedRef.current = onNotificationReceived;
    onNotificationDeletedRef.current = onNotificationDeleted; // [추가] 최신화
  }, [onNotificationReceived, onNotificationDeleted]);

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
      // [최종 수정] 딜레이 없는 즉시 재연결 (5초 -> 0.1초)
      // 사용자가 연결 끊김을 느끼지 못하게 즉시 다시 붙습니다.
      reconnectTimeoutRef.current = setTimeout(() => {
        connectRef.current();
      }, 100); // 100ms (0.1초)
    };

    eventSource.addEventListener("connect", () => {
      isConnectedRef.current = true;
      reconnectAttemptsRef.current = 0;
      lastConnectTimeRef.current = Date.now();
    });

    // Heartbeat 이벤트
    eventSource.addEventListener("heartbeat", () => {
      // Heartbeat 수신됨
    });

    // [핵심] 기본 메시지(message) 이벤트 수신 추가
    eventSource.onmessage = (event) => {
      try {
        // Heartbeat나 연결 메시지인 경우 무시
        if (event.data === "connected" || event.data.includes("heartbeat")) {
          return;
        }

        const notification: Notification = JSON.parse(event.data);
        onNotificationReceivedRef.current(notification);
      } catch (error) {
        // 파싱 에러 무시
      }
    };

    // [핵심] notification 이벤트 수신 (백엔드에서 event: notification으로 보낼 경우)
    eventSource.addEventListener("notification", (event) => {
      try {
        const notification: Notification = JSON.parse(event.data);
        onNotificationReceivedRef.current(notification);
      } catch (error) {
        // 파싱 에러 무시
      }
    });

    // [신규] 알림 삭제 이벤트 수신 (백엔드에서 notification-delete 이벤트 전송 시)
    eventSource.addEventListener("notification-delete", (event) => {
      try {
        // 백엔드에서 삭제된 알림 ID 배열을 보냄 (예: [10, 11])
        const deletedIds: number[] = JSON.parse(event.data);

        if (deletedIds && deletedIds.length > 0) {
          onNotificationDeletedRef.current(deletedIds);
        }
      } catch (error) {
        // 파싱 에러 무시
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
