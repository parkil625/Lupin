import { useEffect, useRef, useCallback } from 'react';
import { Notification } from '@/types/dashboard.types';

interface UseNotificationSseProps {
  onNotificationReceived: (notification: Notification) => void;
  enabled?: boolean;
}

export const useNotificationSse = ({
  onNotificationReceived,
  enabled = true,
}: UseNotificationSseProps) => {
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const connect = useCallback(() => {
    if (!enabled) return;

    // 기존 연결 정리
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const token = localStorage.getItem('accessToken');
    if (!token) {
      console.log('[SSE] 토큰 없음, 연결 스킵');
      return;
    }

    // SSE URL 구성
    const isLocal = window.location.hostname === 'localhost';
    const baseUrl = isLocal ? 'http://localhost:8081' : window.location.origin;
    const sseUrl = `${baseUrl}/api/notifications/subscribe?token=${encodeURIComponent(token)}`;

    console.log('[SSE] 연결 시도');

    const eventSource = new EventSource(sseUrl);
    eventSourceRef.current = eventSource;

    eventSource.addEventListener('connect', (event) => {
      console.log('[SSE] 연결 성공:', event.data);
    });

    eventSource.addEventListener('notification', (event) => {
      try {
        const notification: Notification = JSON.parse(event.data);
        console.log('[SSE] 알림 수신:', notification);
        onNotificationReceived(notification);
      } catch (error) {
        console.error('[SSE] 알림 파싱 에러:', error);
      }
    });

    eventSource.onerror = (error) => {
      console.error('[SSE] 연결 에러:', error);
      eventSource.close();
      eventSourceRef.current = null;

      // 5초 후 재연결 시도
      reconnectTimeoutRef.current = setTimeout(() => {
        console.log('[SSE] 재연결 시도...');
        connect();
      }, 5000);
    };
  }, [enabled, onNotificationReceived]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      console.log('[SSE] 연결 해제');
    }
  }, []);

  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    reconnect: connect,
    disconnect,
  };
};
