import { ReactNode, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "./components/ui/sonner";
import { useAuthStore } from "./store/useAuthStore";
import ErrorBoundary from "./components/errors/ErrorBoundary";
import { lazyWithPreload } from "./utils/lazyWithPreload";

// 🚀 [최적화 1] 랜딩페이지는 가장 먼저 보여야 하므로 일반 import 유지 (즉시 로딩)
import LandingPage from "./components/LandingPage";

// 🚀 [최적화 2] Lazy Loading + Preload 지원
// 마우스 hover 시 미리 다운로드 시작 가능
const Login = lazyWithPreload(() => import("./components/auth/Login"));
const NaverCallback = lazyWithPreload(
  () => import("./components/auth/NaverCallback")
);
const KakaoCallback = lazyWithPreload(
  () => import("./components/auth/KakaoCallback")
);
const Dashboard = lazyWithPreload(() => import("./components/Dashboard"));
const NotFoundPage = lazyWithPreload(
  () => import("./components/errors/NotFoundPage")
);
const ErrorPage = lazyWithPreload(
  () => import("./components/errors/ErrorPage")
);

// 로딩 중에 보여줄 가벼운 스피너 (화면 깜빡임 방지)
const PageLoader = () => (
  <div className="flex h-screen w-screen items-center justify-center bg-white">
    <div className="h-10 w-10 animate-spin rounded-full border-4 border-gray-100 border-t-[#C93831]"></div>
  </div>
);

// 인증된 사용자만 접근 가능한 라우트
function ProtectedRoute({ children }: { children: ReactNode }) {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  if (!isLoggedIn) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

// 비인증 사용자만 접근 가능한 라우트
function PublicRoute({ children }: { children: ReactNode }) {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  if (isLoggedIn) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

export default function App() {
  const userRole = useAuthStore((state) => state.userRole);
  const logout = useAuthStore((state) => state.logout);
  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  // 인증 상태가 localStorage에서 로드될 때까지 대기 (깜빡임 방지)
  if (!hasHydrated) {
    return <PageLoader />;
  }

  return (
    <BrowserRouter>
      <ErrorBoundary>
        {/* 🚀 [최적화 3] Suspense로 Lazy 컴포넌트 로딩 대기 처리 */}
        <Suspense fallback={<PageLoader />}>
          <Routes>
            {/* 1. 랜딩 페이지 (최우선 로딩) */}
            <Route
              path="/"
              element={
                <PublicRoute>
                  <LandingPage />
                </PublicRoute>
              }
            />

            {/* 2. 로그인 페이지 (지연 로딩) */}
            <Route
              path="/login"
              element={
                <PublicRoute>
                  <Login />
                </PublicRoute>
              }
            />

            {/* OAuth 콜백 (지연 로딩) */}
            <Route path="/oauth/naver/callback" element={<NaverCallback />} />
            <Route path="/oauth/kakao/callback" element={<KakaoCallback />} />

            {/* 🚀 [최적화 4] 와일드카드(*)로 리마운트 방지 및 상태 보존 */}
            <Route
              path="/dashboard/*"
              element={
                <ProtectedRoute>
                  <Dashboard
                    onLogout={logout}
                    userType={userRole || "member"}
                  />
                </ProtectedRoute>
              }
            />

            {/* 에러 페이지 */}
            <Route path="/demo/500" element={<ErrorPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
        <Toaster />
      </ErrorBoundary>
    </BrowserRouter>
  );
}
