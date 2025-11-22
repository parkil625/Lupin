/**
 * Login.tsx
 *
 * 로그인 페이지 컴포넌트
 * - 사용자 인증을 위한 로그인 폼 제공
 * - 사내 아이디와 비밀번호 입력
 * - Glassmorphism 디자인으로 구현
 */

import React, {useState, useEffect} from "react";
import {Button} from "../ui/button";
import {Input} from "../ui/input";
import {Label} from "../ui/label";
import {Card} from "../ui/card";
import {ArrowLeft, Sparkles, Lock, User, AlertCircle} from "lucide-react";
import {authApi} from "../../api";
import { useAuthStore } from "../../store/useAuthStore";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || "YOUR_GOOGLE_CLIENT_ID";

declare global {
    interface Window {
        google?: {
            accounts: {
                id: {
                    initialize: (config: any) => void;
                    renderButton: (element: HTMLElement, config: any) => void;
                }
            }
        }
    }
}

interface LoginProps {
    onBack: () => void;
}

export default function Login({onBack}: LoginProps) {
    const login = useAuthStore((state) => state.login);

    const [employeeId, setEmployeeId] = useState("");
    const [password, setPassword] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.onload = () => {
            if (window.google) {
                window.google.accounts.id.initialize({
                    client_id: GOOGLE_CLIENT_ID,
                    callback: handleGoogleLogin,
                });

                const googleButton = document.getElementById('google-signin-button');
                if (googleButton) {
                    window.google.accounts.id
                        .renderButton(googleButton, {
                            theme: 'outline',
                            size: 'large',
                            width: '100%',
                            text: 'signin_with',
                            shape: 'rectangular',
                        });
                }
            }
        };
        document.body.appendChild(script);

        return () => {
            document.body.removeChild(script);
        };
    }, []);

    const handleGoogleLogin = async (response: any) => {
        setError("");
        setIsLoading(true);

        try {
            const result = await authApi.googleLogin(response.credential);

            // 필요한 추가 정보만 저장
            localStorage.setItem('userId', result.userId.toString());
            localStorage.setItem('userEmail', result.email);
            localStorage.setItem('userName', result.name);

            login(result.accessToken, result.role);
        } catch (err: any) {
            console.error('Google login failed:', err);

            if (err.response?.status === 404) {
                setError("등록된 직원이 아닙니다. 인사팀에 문의해주세요.");
            } else {
                setError("구글 로그인 중 오류가 발생했습니다.");
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);

        try {
            const response = await authApi.login(employeeId, password);

            login(response.accessToken, response.role);

        } catch (err: any) {
            console.error('Login failed:', err);

            if (err.response?.status === 401) {
                setError("아이디 또는 비밀번호가 일치하지 않습니다.");
            } else if (err.response?.status === 404) {
                setError("존재하지 않는 사용자입니다.");
            } else {
                setError("로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen w-screen overflow-hidden relative flex items-center justify-center">
            {/* 배경 효과 */}
            <div className="absolute inset-0 -z-10 bg-white">
                <div className="absolute top-20 left-10 w-96 h-96 bg-red-50 rounded-full blur-3xl opacity-40"></div>
                <div
                    className="absolute bottom-20 right-10 w-96 h-96 bg-pink-50 rounded-full blur-3xl opacity-40"></div>
                <div
                    className="absolute top-1/2 left-1/3 w-80 h-80 bg-purple-50 rounded-full blur-3xl opacity-30"></div>
                <div
                    className="absolute bottom-1/3 right-1/4 w-72 h-72 bg-orange-50 rounded-full blur-3xl opacity-30"></div>
            </div>

            {/* 메인으로 돌아가기 버튼 */}
            <button
                onClick={onBack}
                className="absolute top-8 left-8 flex items-center gap-2 px-6 py-3 rounded-full backdrop-blur-3xl bg-white/40 border border-white/60 shadow-lg hover:shadow-xl transition-all hover:bg-white/50 group"
            >
                <ArrowLeft className="w-5 h-5 text-gray-700 group-hover:text-[#C93831] transition-colors"/>
                <span className="text-gray-700 group-hover:text-[#C93831] transition-colors font-medium">메인으로</span>
            </button>

            <Card
                className="w-full max-w-md relative overflow-hidden shadow-2xl backdrop-blur-3xl bg-white/40 border border-white/60">
                <div className="relative p-10 space-y-8">
                    {/* 로고 & 타이틀 */}
                    <div className="text-center space-y-4">
                        <div className="flex justify-center mb-6">
                            <img src="/Lupin.png" alt="Lupin Logo" className="h-20 w-auto object-contain"/>
                        </div>

                        <p className="text-gray-600 font-medium">건강한 습관, 함께 만들어가요</p>

                        <div className="flex items-center gap-2 justify-center">
                            <div className="h-1 w-8 bg-gradient-to-r from-transparent to-[#C93831] rounded-full"></div>
                            <Sparkles className="w-4 h-4 text-[#C93831]"/>
                            <div className="h-1 w-8 bg-gradient-to-l from-transparent to-[#C93831] rounded-full"></div>
                        </div>
                    </div>

                    {/* 로그인 폼 */}
                    <form onSubmit={handleLogin} className="space-y-6">
                        <div className="space-y-2">
                            <Label htmlFor="employeeId" className="text-sm font-bold text-gray-700">
                                사내 아이디
                            </Label>
                            <div className="relative">
                                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"/>
                                <Input
                                    id="employeeId"
                                    type="text"
                                    placeholder="사번 또는 아이디"
                                    value={employeeId}
                                    onChange={(e) => setEmployeeId(e.target.value)}
                                    className="pl-12 h-14 rounded-2xl border-2 border-gray-200 bg-white focus:border-[#C93831] transition-all shadow-sm"
                                    required
                                />
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="password" className="text-sm font-bold text-gray-700">
                                비밀번호
                            </Label>
                            <div className="relative">
                                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"/>
                                <Input
                                    id="password"
                                    type="password"
                                    placeholder="••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    className="pl-12 h-14 rounded-2xl border-2 border-gray-200 bg-white focus:border-[#C93831] transition-all shadow-sm"
                                    required
                                />
                            </div>
                        </div>

                        {error && (
                            <div className="flex items-center gap-2 p-4 rounded-xl bg-red-50 border border-red-200">
                                <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0"/>
                                <p className="text-sm text-red-700 font-medium">{error}</p>
                            </div>
                        )}

                        <Button
                            type="submit"
                            disabled={isLoading}
                            className="w-full h-14 rounded-2xl bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-black text-lg shadow-xl hover:shadow-2xl transition-all border-0 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isLoading ? (
                                <>
                                    <div
                                        className="w-5 h-5 mr-2 border-2 border-white border-t-transparent rounded-full animate-spin"/>
                                    로그인 중...
                                </>
                            ) : (
                                <>
                                    <Sparkles className="w-5 h-5 mr-2"/>
                                    로그인
                                </>
                            )}
                        </Button>
                    </form>

                    <div className="relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-300"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-2 bg-white/40 text-gray-500">또는</span>
                        </div>
                    </div>

                    <div id="google-signin-button" className="w-full flex justify-center"></div>

                    <div className="text-center space-y-2 pt-4">
                        <div className="h-px bg-gradient-to-r from-transparent via-gray-300 to-transparent"></div>
                        <p className="text-sm text-gray-600 font-medium pt-4">
                            직원 전용 서비스입니다
                        </p>
                        <p className="text-xs text-gray-500">
                            계정 문의는 인사팀으로 연락해주세요
                        </p>
                    </div>
                </div>

                <div className="h-2 bg-gradient-to-r from-[#C93831] via-pink-500 to-purple-500"></div>
            </Card>
        </div>
    );
}
