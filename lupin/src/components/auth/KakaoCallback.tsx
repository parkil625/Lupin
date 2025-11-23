/**
 * KakaoCallback.tsx
 *
 * 카카오 OAuth 콜백 처리 컴포넌트
 */

import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { oauthApi } from "../../api";
import { useAuthStore } from "../../store/useAuthStore";
import { Card } from "../ui/card";
import { AlertCircle, Loader2 } from "lucide-react";

export default function KakaoCallback() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const login = useAuthStore((state) => state.login);
    const [error, setError] = useState("");

    useEffect(() => {
        const handleCallback = async () => {
            const code = searchParams.get("code");
            const state = searchParams.get("state");
            const savedState = sessionStorage.getItem("kakao_oauth_state");
            const mode = sessionStorage.getItem("kakao_oauth_mode") || "login";

            // state 검증
            if (!state || state !== savedState) {
                setError("잘못된 요청입니다. 다시 시도해주세요.");
                return;
            }

            if (!code) {
                setError("인증 코드가 없습니다.");
                return;
            }

            const redirectUri = `${window.location.origin}/oauth/kakao/callback`;

            try {
                if (mode === "link") {
                    // 계정 연동 모드
                    await oauthApi.linkKakao(code, redirectUri);

                    // 세션 스토리지 정리
                    sessionStorage.removeItem("kakao_oauth_state");
                    sessionStorage.removeItem("kakao_oauth_mode");

                    // 프로필 페이지로 이동
                    navigate("/dashboard/profile", { replace: true });
                } else {
                    // 로그인 모드
                    const result = await oauthApi.kakaoLogin(code, redirectUri);

                    // 사용자 정보 저장
                    localStorage.setItem('userId', result.userId.toString());
                    localStorage.setItem('userEmail', result.email);
                    localStorage.setItem('userName', result.name);

                    // 로그인 처리
                    login(result.accessToken, result.role);

                    // 세션 스토리지 정리
                    sessionStorage.removeItem("kakao_oauth_state");

                    // 홈으로 이동
                    navigate("/dashboard", { replace: true });
                }
            } catch (err: any) {
                console.error("Kakao OAuth failed:", err);

                sessionStorage.removeItem("kakao_oauth_state");
                sessionStorage.removeItem("kakao_oauth_mode");

                if (err.response?.status === 404) {
                    setError("등록된 직원이 아닙니다. 인사팀에 문의해주세요.");
                } else if (err.response?.data?.message) {
                    setError(err.response.data.message);
                } else {
                    setError(mode === "link" ? "카카오 계정 연동 중 오류가 발생했습니다." : "카카오 로그인 중 오류가 발생했습니다.");
                }
            }
        };

        handleCallback();
    }, [searchParams, login, navigate]);

    if (error) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <Card className="p-8 max-w-md w-full">
                    <div className="flex flex-col items-center gap-4">
                        <AlertCircle className="w-12 h-12 text-red-500" />
                        <p className="text-center text-gray-700">{error}</p>
                        <button
                            onClick={() => navigate("/login")}
                            className="mt-4 px-6 py-2 bg-[#C93831] text-white rounded-lg hover:bg-[#B02F28] transition-colors"
                        >
                            로그인으로 돌아가기
                        </button>
                    </div>
                </Card>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <Card className="p-8 max-w-md w-full">
                <div className="flex flex-col items-center gap-4">
                    <Loader2 className="w-12 h-12 text-[#FEE500] animate-spin" />
                    <p className="text-center text-gray-700">카카오 로그인 처리 중...</p>
                </div>
            </Card>
        </div>
    );
}
