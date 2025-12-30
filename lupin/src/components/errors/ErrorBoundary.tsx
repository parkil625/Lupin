/**
 * React Error Boundary
 * 컴포넌트 렌더링 중 발생하는 에러를 잡아서 처리
 */

import { Component, ErrorInfo, ReactNode } from "react";
import ErrorPage from "./ErrorPage";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("ErrorBoundary caught an error:", error, errorInfo);

    // [수정] 배포 후 구버전 청크 파일(JS)을 찾을 수 없을 때 발생하는 에러 감지 및 자동 새로고침
    const isChunkLoadError =
      error.message.includes("Failed to fetch dynamically imported module") ||
      error.message.includes("Importing a module script failed") ||
      error.message.includes("text/html");

    if (isChunkLoadError) {
      console.warn(
        "⚠️ Chunk Load Error detected! Reloading page to fetch the latest version..."
      );

      // 무한 루프 방지를 위한 안전장치 (최대 2번까지만 재시도)
      const storageKey = `reload_count_${window.location.pathname}`;
      const reloadCount = sessionStorage.getItem(storageKey);

      if (!reloadCount || parseInt(reloadCount) < 2) {
        sessionStorage.setItem(
          storageKey,
          reloadCount ? String(parseInt(reloadCount) + 1) : "1"
        );
        // 0.1초 뒤 새로고침 (즉시 실행 시 브라우저가 막을 수도 있음)
        setTimeout(() => window.location.reload(), 100);
      } else {
        console.error(
          "🚫 Maximum reload attempts reached. Stopping auto-reload."
        );
        // 에러가 지속되면 카운트 초기화 후 에러 페이지 유지
        sessionStorage.removeItem(storageKey);
      }
    }
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return <ErrorPage />;
    }

    return this.props.children;
  }
}
