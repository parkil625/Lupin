/**
 * Login.tsx
 * 원형 아이콘 소셜 로그인 스타일 적용
 * React Hook Form + Zod 적용
 */

import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Card } from "../ui/card";
import { Button } from "../ui/button";
import { ArrowLeft, Lock, User, AlertCircle, X, Eye, EyeOff } from "lucide-react";
import { authApi, oauthApi } from "../../api";
import { useAuthStore } from "../../store/useAuthStore";

// Zod 스키마 정의
const loginSchema = z.object({
  employeeId: z.string().min(1, "아이디를 입력해주세요"),
  password: z.string().min(1, "비밀번호를 입력해주세요"),
});

type LoginFormData = z.infer<typeof loginSchema>;

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID || "YOUR_GOOGLE_CLIENT_ID";

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: any) => void;
          renderButton: (element: HTMLElement, config: any) => void;
        };
      };
    };
  }
}

export default function Login() {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      employeeId: "",
      password: "",
    },
  });

  const employeeId = watch("employeeId");
  const password = watch("password");

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => {
      if (window.google) {
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: handleGoogleLogin,
          use_fedcm_for_prompt: false,
        });

        const googleButton = document.getElementById("google-signin-button");
        if (googleButton) {
          window.google.accounts.id.renderButton(googleButton, {
            theme: "outline",
            size: "large",
            type: "icon",
            shape: "circle",
          });
        }
      }
    };
    document.body.appendChild(script);

    return () => {
      document.body.removeChild(script);
    };
  }, []);

  const handleNaverLogin = () => {
    const state = Math.random().toString(36).substring(7);
    sessionStorage.setItem("naver_oauth_state", state);
    const redirectUri = `${window.location.origin}/oauth/naver/callback`;
    const naverAuthUrl = oauthApi.getNaverAuthUrl(redirectUri, state);
    window.location.href = naverAuthUrl;
  };

  const handleKakaoLogin = () => {
    const state = Math.random().toString(36).substring(7);
    sessionStorage.setItem("kakao_oauth_state", state);
    const redirectUri = `${window.location.origin}/oauth/kakao/callback`;
    const kakaoAuthUrl = oauthApi.getKakaoAuthUrl(redirectUri, state);
    window.location.href = kakaoAuthUrl;
  };

  const handleGoogleLoginClick = () => {
    // Google GSI 버튼 클릭 트리거
    const googleButton = document.querySelector(
      '#google-signin-button div[role="button"]'
    ) as HTMLElement;
    if (googleButton) {
      googleButton.click();
    }
  };

  const handleGoogleLogin = async (response: any) => {
    setError("");
    setIsLoading(true);
    try {
        const result = await authApi.googleLogin(response.credential);

        const safeId = result.id || result.userId;
        if (safeId) {
            localStorage.setItem("userId", safeId.toString());
        }

        if (result.email) localStorage.setItem("userEmail", result.email);
        if (result.name) localStorage.setItem("userName", result.name);

        login(result.accessToken, result.role);
    } catch (err: any) {
        console.error("Google login failed:", err);
        if (err.response?.status === 404) {
            setError("등록된 직원이 아닙니다. 인사팀에 문의해주세요.");
        } else {
            setError("구글 로그인 중 오류가 발생했습니다.");
        }
    } finally {
      setIsLoading(false);
    }
  };

  const onSubmit = async (data: LoginFormData) => {
    console.log("=== 로그인 시도 ===");
    console.log("employeeId:", data.employeeId);
    console.log("password:", data.password);
    console.log("password length:", data.password.length);
    setError("");
    setIsLoading(true);
    try {
      const response = await authApi.login(data.employeeId, data.password);
      console.log("로그인 성공:", response);
        const safeId = response.id || response.userId;
        if (safeId) {
            localStorage.setItem("userId", safeId.toString());
        }

        localStorage.setItem("userEmail", response.email);
        localStorage.setItem("userName", response.name);

        login(response.accessToken, response.role);
    } catch (err: any) {
      console.error("=== 로그인 실패 ===");
      console.error("Error:", err);
      console.error("Response:", err.response);
      console.error("Status:", err.response?.status);
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
    <div className="min-h-screen w-screen overflow-hidden relative flex items-center justify-center px-4 py-16 md:py-8">
      {/* 배경 효과 */}
      <div className="absolute inset-0 -z-10 bg-white">
        <div className="absolute top-20 left-10 w-64 md:w-96 h-64 md:h-96 bg-red-50 rounded-full blur-3xl opacity-40"></div>
        <div className="absolute bottom-20 right-10 w-64 md:w-96 h-64 md:h-96 bg-pink-50 rounded-full blur-3xl opacity-40"></div>
        <div className="absolute top-1/2 left-1/3 w-48 md:w-80 h-48 md:h-80 bg-purple-50 rounded-full blur-3xl opacity-30"></div>
      </div>

      {/* 메인으로 돌아가기 버튼 */}
      <button
        onClick={() => navigate("/")}
        className="absolute top-4 left-4 md:top-8 md:left-8 flex items-center gap-1 md:gap-2 px-4 md:px-6 py-2 md:py-3 rounded-full backdrop-blur-3xl bg-white/40 border border-white/60 shadow-lg hover:shadow-xl transition-all hover:bg-white/50 group"
      >
        <ArrowLeft className="w-4 h-4 md:w-5 md:h-5 text-gray-700 group-hover:text-[#C93831] transition-colors" />
        <span className="text-sm md:text-base text-gray-700 group-hover:text-[#C93831] transition-colors font-medium">
          메인으로
        </span>
      </button>

      <Card className="w-full max-w-md relative overflow-hidden shadow-2xl backdrop-blur-3xl bg-white/40 border border-white/60 mx-4 md:mx-0">
        <div className="relative p-6 md:p-8 space-y-4 md:space-y-5">
          {/* 로고 & 타이틀 */}
          <div className="text-center space-y-4">
            <div className="flex justify-center mb-6">
              <img
                src="/Lupin.png"
                alt="Lupin Logo"
                className="h-20 w-auto object-contain"
              />
            </div>
            <p className="text-gray-600 font-medium">
              건강한 습관, 함께 만들어가요
            </p>
          </div>

          {/* 로그인 폼 */}
          <form onSubmit={handleSubmit(onSubmit, (errors) => {
            console.error("=== 폼 유효성 검사 실패 ===", errors);
          })} className="space-y-4">
            <div className="space-y-2">
              <Label
                htmlFor="employeeId"
                className="text-sm font-bold text-gray-700"
              >
                사내 아이디
              </Label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  id="employeeId"
                  type="text"
                  placeholder="아이디"
                  {...register("employeeId")}
                  className={`pl-12 pr-10 h-14 rounded-2xl border-2 bg-white transition-all shadow-sm ${
                    errors.employeeId ? "border-red-400 focus:border-red-500" : "border-gray-200 focus:border-[#C93831]"
                  }`}
                />
                {employeeId && (
                  <button
                    type="button"
                    onClick={() => setValue("employeeId", "")}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
              </div>
              {errors.employeeId && (
                <p className="text-xs text-red-500 mt-1">{errors.employeeId.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label
                htmlFor="password"
                className="text-sm font-bold text-gray-700"
              >
                비밀번호
              </Label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="비밀번호"
                  {...register("password")}
                  className={`pl-12 pr-20 h-14 rounded-2xl border-2 bg-white transition-all shadow-sm ${
                    errors.password ? "border-red-400 focus:border-red-500" : "border-gray-200 focus:border-[#C93831]"
                  }`}
                />
                {password && (
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center gap-1">
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                    <button
                      type="button"
                      onClick={() => setValue("password", "")}
                      className="text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>
                )}
              </div>
              {errors.password && (
                <p className="text-xs text-red-500 mt-1">{errors.password.message}</p>
              )}
            </div>

            {error && (
              <div className="flex items-center gap-2 p-4 rounded-xl bg-red-50 border border-red-200">
                <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0" />
                <p className="text-sm text-red-700 font-medium">{error}</p>
              </div>
            )}

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full h-14 rounded-2xl bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-black text-lg shadow-xl hover:shadow-2xl transition-all border-0 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? "로그인 중..." : "로그인"}
            </Button>
          </form>

          {/* 구분선 */}
          <div className="relative py-2">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-4 bg-white/40 text-gray-500 rounded-full backdrop-blur-sm">
                SNS 간편 로그인
              </span>
            </div>
          </div>

          {/* 소셜 로그인 버튼 영역 */}
          <div className="flex justify-center items-center gap-6 pb-2">
            {/* 1. 네이버 버튼 */}
            <button
              type="button"
              onClick={isLoading ? undefined : handleNaverLogin}
              className="w-[44px] h-[44px] min-w-[44px] min-h-[44px] max-w-[44px] max-h-[44px] rounded-full flex items-center justify-center transition-all hover:scale-110 shadow-md border border-transparent overflow-hidden relative flex-none"
              title="네이버로 로그인"
            >
              <img
                src="/naver-logo.png"
                alt="Naver"
                className="w-10 h-10 object-contain cursor-pointer"
              />
            </button>

            {/* 2. 카카오 버튼 */}
            <button
              type="button"
              onClick={isLoading ? undefined : handleKakaoLogin}
              className="w-[44px] h-[44px] min-w-[44px] min-h-[44px] max-w-[44px] max-h-[44px] rounded-full flex items-center justify-center transition-all hover:scale-110 shadow-md border border-transparent overflow-hidden relative flex-none"
              title="카카오로 로그인"
            >
              <img
                src="/kakao-logo.png"
                alt="Kakao"
                className="w-10 h-10 object-contain cursor-pointer "
              />
            </button>

            {/* 3. 구글 버튼 */}
            <button
              type="button"
              onClick={isLoading ? undefined : handleGoogleLoginClick}
              className="w-[44px] h-[44px] min-w-[44px] min-h-[44px] max-w-[44px] max-h-[44px] rounded-full flex items-center justify-center transition-all hover:scale-110 shadow-md border border-[#dadce0] overflow-hidden relative bg-white hover:bg-gray-50 flex-none"
              title="구글 계정으로 로그인"
            >
              <img
                src="/google-logo.png"
                alt="Google"
                className="w-10 h-10 object-contain cursor-pointer "
              />
            </button>
            {/* 숨겨진 구글 GSI 버튼 */}
            <div id="google-signin-button" className="hidden"></div>
          </div>

          <div className="text-center">
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
