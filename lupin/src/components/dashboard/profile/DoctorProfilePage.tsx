/**
 * DoctorProfilePage.tsx
 *
 * 의사 프로필 페이지 컴포넌트
 * - 의사 개인 정보 표시
 * - 전문 분야 및 경력 정보
 * - 로그아웃 기능
 */

import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { User } from "lucide-react";

interface DoctorProfilePageProps {
  onLogout: () => void;
}

export default function DoctorProfilePage({ onLogout }: DoctorProfilePageProps) {
  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-4xl mx-auto space-y-8">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">내 정보</h1>
          <p className="text-gray-700 font-medium text-lg">의료진 프로필</p>
        </div>

        <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
          <div className="p-8">
            <div className="flex items-center gap-6 mb-8">
              <Avatar className="w-24 h-24 border-4 border-white shadow-xl">
                <AvatarFallback className="bg-white">
                  <User className="w-12 h-12 text-gray-400" />
                </AvatarFallback>
              </Avatar>
              <div>
                <h2 className="text-3xl font-black text-gray-900 mb-2">김의사</h2>
                <p className="text-gray-600 font-medium">내과 전문의</p>
              </div>
            </div>

            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                <div className="text-sm text-gray-600 font-medium mb-1">이메일</div>
                <div className="font-bold text-gray-900">doctor@company.com</div>
              </div>

              <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                <div className="text-sm text-gray-600 font-medium mb-1">전문 분야</div>
                <div className="font-bold text-gray-900">내과, 가정의학과</div>
              </div>
            </div>

            <div className="mt-8 pt-8 border-t border-gray-200">
              <Button
                onClick={onLogout}
                variant="outline"
                className="w-full h-14 rounded-2xl border-2 border-red-300 text-red-600 hover:bg-red-50 font-bold text-lg"
              >
                로그아웃
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
