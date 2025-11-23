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
    // 여기서 에러 로깅 서비스로 전송할 수 있음
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <ErrorPage
          title="앱에서 오류가 발생했습니다"
          message="예기치 않은 오류가 발생했습니다. 페이지를 새로고침하거나 홈으로 이동해주세요."
        />
      );
    }

    return this.props.children;
  }
}
