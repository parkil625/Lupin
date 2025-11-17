/**
 * Login.tsx
 *
 * 로그인 페이지 컴포넌트
 * - 사용자 인증을 위한 로그인 폼 제공
 * - 사내 아이디와 비밀번호 입력
 * - Glassmorphism 디자인으로 구현
 */

import React, { useState } from "react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Card } from "../ui/card";
import { ArrowLeft, Sparkles, Lock, User } from "lucide-react";

interface LoginProps {
  onBack: () => void;
  onLogin: (username: string) => void;
}

export default function Login({ onBack, onLogin }: LoginProps) {
  const [employeeId, setEmployeeId] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    onLogin(employeeId);
  };

  return (
    <div className="min-h-screen w-screen overflow-hidden relative flex items-center justify-center">
      {/* Stained Background */}
      <div className="absolute inset-0 -z-10 bg-white">
        <div className="absolute top-20 left-10 w-96 h-96 bg-red-50 rounded-full blur-3xl opacity-40"></div>
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-pink-50 rounded-full blur-3xl opacity-40"></div>
        <div className="absolute top-1/2 left-1/3 w-80 h-80 bg-purple-50 rounded-full blur-3xl opacity-30"></div>
        <div className="absolute bottom-1/3 right-1/4 w-72 h-72 bg-orange-50 rounded-full blur-3xl opacity-30"></div>
      </div>

      {/* Back Button */}
      <button
        onClick={onBack}
        className="absolute top-8 left-8 flex items-center gap-2 px-6 py-3 rounded-full backdrop-blur-3xl bg-white/40 border border-white/60 shadow-lg hover:shadow-xl transition-all hover:bg-white/50 group"
      >
        <ArrowLeft className="w-5 h-5 text-gray-700 group-hover:text-[#C93831] transition-colors" />
        <span className="text-gray-700 group-hover:text-[#C93831] transition-colors font-medium">메인으로</span>
      </button>

      {/* Login Card - Glassmorphic */}
      <Card className="w-full max-w-md relative overflow-hidden shadow-2xl backdrop-blur-3xl bg-white/40 border border-white/60">
        <div className="relative p-10 space-y-8">
          {/* Logo & Title */}
          <div className="text-center space-y-4">
            <div className="flex justify-center mb-6">
              <img src="/Lupin.png" alt="Lupin Logo" className="h-20 w-auto object-contain" />
            </div>

            <p className="text-gray-600 font-medium">건강한 습관, 함께 만들어가요</p>
            
            <div className="flex items-center gap-2 justify-center">
              <div className="h-1 w-8 bg-gradient-to-r from-transparent to-[#C93831] rounded-full"></div>
              <Sparkles className="w-4 h-4 text-[#C93831]" />
              <div className="h-1 w-8 bg-gradient-to-l from-transparent to-[#C93831] rounded-full"></div>
            </div>
          </div>

          {/* Login Form */}
          <form onSubmit={handleLogin} className="space-y-6">
            {/* Employee ID */}
            <div className="space-y-2">
              <Label htmlFor="employeeId" className="text-sm font-bold text-gray-700">
                사내 아이디
              </Label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
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

            {/* Password */}
            <div className="space-y-2">
              <Label htmlFor="password" className="text-sm font-bold text-gray-700">
                비밀번호
              </Label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
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

            {/* Login Button */}
            <Button 
              type="submit" 
              className="w-full h-14 rounded-2xl bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-black text-lg shadow-xl hover:shadow-2xl transition-all border-0"
            >
              <Sparkles className="w-5 h-5 mr-2" />
              로그인
            </Button>
          </form>

          {/* Info Text */}
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

        {/* Bottom Accent */}
        <div className="h-2 bg-gradient-to-r from-[#C93831] via-pink-500 to-purple-500"></div>
      </Card>
    </div>
  );
}
