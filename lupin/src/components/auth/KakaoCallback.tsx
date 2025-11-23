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
import {
    AlertDialog,
    AlertDialogContent,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogAction,
} from "@/components/ui/alert-dialog";

export default function KakaoCallback() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const login = useAuthStore((state) => state.login);

    // 에러 메시지 표시용 State
    const [error, setError] = useState("");

    // 연동 에러 모달 표시용 State
    const [showLinkError, setShowLinkError] = useState(false);

    useEffect(() => {
        const handleCallback = async () => {
            const code = searchParams.get("code");
            const state = searchParams.get("state");
            const savedState = sessionStorage.getItem("kakao_oauth_state");
            const mode = sessionStorage.getItem("kakao_oauth_mode") || "login";

            // 카카오 API 호출 시 redirectUri가 필요함
            const redirectUri = `${window.location.origin}/oauth/kakao/callback`;

            // [수정] state 검증 로직을 활성화하여 'unused variable' 에러 해결
            if (state && savedState && state !== savedState) {
                setError("보안 검증에 실패했습니다. (State 불일치)");
                return;
            }

            if (!code) {
                setError("인증 코드가 없습니다.");
                return;
            }

            try {
                if (mode === "link") {
                    // [계정 연동 모드]
                    await oauthApi.linkKakao(code, redirectUri);

                    // 세션 스토리지 정리
                    sessionStorage.removeItem("kakao_oauth_state");
                    sessionStorage.removeItem("kakao_oauth_mode");

                    // 프로필 페이지로 이동
                    navigate("/dashboard/profile", { replace: true });
                } else {
                    // [로그인 모드]
                    const result = await oauthApi.kakaoLogin(code, redirectUri);

                    // 사용자 정보 저장 (authApi.ts 수정 반영: id 사용)
                    localStorage.setItem('userId', result.id.toString());
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
                // [수정] 연동되지 않은 계정일 경우 모달 띄우기
                if (err.response?.status === 404 && err.response?.data?.errorCode === 'OAUTH_NOT_LINKED') {
                    sessionStorage.removeItem("kakao_oauth_state");
                    sessionStorage.removeItem("kakao_oauth_mode");

                    setShowLinkError(true);
                    return;
                }

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

    // 모달 확인 버튼 핸들러
    const handleConfirmLinkError = () => {
        setShowLinkError(false);
        navigate("/login", { replace: true });
    };

    // 일반 에러 화면
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

    // 로딩 화면 + 연동 안내 모달
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <Card className="p-8 max-w-md w-full">
                <div className="flex flex-col items-center gap-4">
                    <Loader2 className="w-12 h-12 text-[#FEE500] animate-spin" />
                    <p className="text-center text-gray-700">카카오 로그인 처리 중...</p>
                </div>
            </Card>

            {/* 연동 필요 안내 모달 */}
            <AlertDialog open={showLinkError} onOpenChange={setShowLinkError}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>계정 연동 필요</AlertDialogTitle>
                        <AlertDialogDescription>
                            연동된 계정이 없습니다.<br />
                            먼저 사내 아이디로 로그인 후 <strong>[마이페이지 &gt; 계정 연동]</strong>을 진행해주세요.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogAction onClick={handleConfirmLinkError} className="bg-[#C93831] hover:bg-[#B02F28]">
                            로그인하러 가기
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
}