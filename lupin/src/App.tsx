import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "./components/ui/sonner";
import Login from "./components/auth/Login";
import NaverCallback from "./components/auth/NaverCallback";
import KakaoCallback from "./components/auth/KakaoCallback";
import Dashboard from "./components/Dashboard";
import LandingPage from "./components/LandingPage";
import { useAuthStore } from "./store/useAuthStore.ts";
import { NotFoundPage, ErrorPage, ErrorBoundary } from "./components/errors";

// 인증된 사용자만 접근 가능한 라우트
function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

    if (!isLoggedIn) {
        return <Navigate to="/login" replace />;
    }

    return <>{children}</>;
}

// 비인증 사용자만 접근 가능한 라우트 (로그인 페이지 등)
function PublicRoute({ children }: { children: React.ReactNode }) {
    const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

    if (isLoggedIn) {
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
}

export default function App() {
    const userRole = useAuthStore((state) => state.userRole);
    const logout = useAuthStore((state) => state.logout);

    return (
        <ErrorBoundary>
            <BrowserRouter>
                <Routes>
                    {/* 랜딩 페이지 */}
                    <Route
                        path="/"
                        element={
                            <PublicRoute>
                                <LandingPage />
                            </PublicRoute>
                        }
                    />

                    {/* 로그인 페이지 */}
                    <Route
                        path="/login"
                        element={
                            <PublicRoute>
                                <Login />
                            </PublicRoute>
                        }
                    />

                    {/* OAuth 콜백 */}
                    <Route path="/oauth/naver/callback" element={<NaverCallback />} />
                    <Route path="/oauth/kakao/callback" element={<KakaoCallback />} />

                    {/* 대시보드 (인증 필요) */}
                    <Route
                        path="/dashboard"
                        element={
                            <ProtectedRoute>
                                <Dashboard onLogout={logout} userType={userRole || 'member'} />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/dashboard/:page"
                        element={
                            <ProtectedRoute>
                                <Dashboard onLogout={logout} userType={userRole || 'member'} />
                            </ProtectedRoute>
                        }
                    />

                    {/* 시연용 에러 페이지 */}
                    <Route path="/demo/404" element={<NotFoundPage />} />
                    <Route path="/demo/500" element={<ErrorPage />} />

                    {/* 404 페이지 */}
                    <Route path="*" element={<NotFoundPage />} />
                </Routes>
                <Toaster />
            </BrowserRouter>
        </ErrorBoundary>
    );
}
