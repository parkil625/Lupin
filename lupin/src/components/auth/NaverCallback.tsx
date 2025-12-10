/**
 * NaverCallback.tsx
 *
 * 네이버 OAuth 콜백 처리 컴포넌트
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
} from "@/components/ui/alert-dialog"; // 경로 확인 필요 (혹은 "../ui/alert-dialog")

export default function NaverCallback() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const login = useAuthStore((state) => state.login);

    // 에러 메시지 표시용 State
    const [error, setError] = useState("");

    // [수정 1] 모달 표시 여부를 제어하는 State 추가
    const [showLinkError, setShowLinkError] = useState(false);

    useEffect(() => {
        const handleCallback = async () => {
            const code = searchParams.get("code");
            const state = searchParams.get("state");
            const savedState = sessionStorage.getItem("naver_oauth_state");
            const mode = sessionStorage.getItem("naver_oauth_mode") || "login";

            // state 검증
            if (!state || state !== savedState) {
                setError("잘못된 요청입니다. 다시 시도해주세요.");
                return;
            }

            if (!code) {
                setError("인증 코드가 없습니다.");
                return;
            }

            try {
                if (mode === "link") {
                    // 계정 연동 모드
                    await oauthApi.linkNaver(code, state);

                    // 세션 스토리지 정리
                    sessionStorage.removeItem("naver_oauth_state");
                    sessionStorage.removeItem("naver_oauth_mode");

                    // 프로필 페이지로 이동
                    navigate("/dashboard/profile", { replace: true });
                } else {
                    // 로그인 모드
                    const result = await oauthApi.naverLogin(code, state);

                    // 사용자 정보 저장 (백엔드 DTO 수정 여부에 따라 result.id 또는 result.userId 확인)
                    localStorage.setItem('userId', result.id.toString());
                    localStorage.setItem('userEmail', result.email);
                    localStorage.setItem('userName', result.name);
                    if (result.department) localStorage.setItem('userDepartment', result.department);

                    // 로그인 처리
                    login(result.accessToken, result.role);

                    // 세션 스토리지 정리
                    sessionStorage.removeItem("naver_oauth_state");

                    // 홈으로 이동
                    navigate("/dashboard", { replace: true });
                }
            } catch (err: unknown) {
                const axiosError = err as { response?: { status?: number; data?: { errorCode?: string; message?: string } } };

                // alert() 대신 State를 true로 변경하여 모달 띄우기
                if (axiosError.response?.status === 404 && axiosError.response?.data?.errorCode === 'OAUTH_NOT_LINKED') {
                    // 세션 정리
                    sessionStorage.removeItem("naver_oauth_state");
                    sessionStorage.removeItem("naver_oauth_mode");

                    // 여기서 바로 이동하지 않고 모달을 띄움!
                    setShowLinkError(true);
                    return;
                }

                sessionStorage.removeItem("naver_oauth_state");
                sessionStorage.removeItem("naver_oauth_mode");

                if (axiosError.response?.status === 404) {
                    setError("등록된 직원이 아닙니다. 인사팀에 문의해주세요.");
                } else if (axiosError.response?.data?.message) {
                    setError(axiosError.response.data.message);
                } else {
                    setError(mode === "link" ? "네이버 계정 연동 중 오류가 발생했습니다." : "네이버 로그인 중 오류가 발생했습니다.");
                }
            }
        };

        handleCallback();
    }, [searchParams, login, navigate]);

    // 모달 확인 버튼 클릭 시 실행될 함수 추가
    const handleConfirmLinkError = () => {
        setShowLinkError(false);
        navigate("/login", { replace: true });
    };

    // 일반 에러 화면 (연동 에러 아닐 때)
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
                    <Loader2 className="w-12 h-12 text-[#03C75A] animate-spin" />
                    <p className="text-center text-gray-700">네이버 로그인 처리 중...</p>
                </div>
            </Card>

            {/* 연동 필요 안내 모달 (shadcn) */}
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