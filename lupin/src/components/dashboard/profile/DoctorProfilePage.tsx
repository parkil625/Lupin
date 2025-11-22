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
import { AspectRatio } from "@/components/ui/aspect-ratio";
import { Button } from "@/components/ui/button";
import { ButtonGroup } from "@/components/ui/button-group";
import { InputGroup, InputGroupInput } from "@/components/ui/input-group";
import { Label } from "@/components/ui/label";
import { User, LogOut } from "lucide-react";

interface DoctorProfilePageProps {
  onLogout: () => void;
}

export default function DoctorProfilePage({ onLogout }: DoctorProfilePageProps) {
  return (
    <div className="h-full overflow-auto p-8 bg-gray-50/50">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Header with Title and Button Group */}
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-5xl font-black text-gray-900 mb-2">내 정보</h1>
            <p className="text-gray-700 font-medium text-lg">의료진 프로필</p>
          </div>
          {/* Button Group */}
          <ButtonGroup>
            <Button
              onClick={onLogout}
              variant="outline"
              className="border-red-300 text-red-600 hover:bg-red-50"
            >
              <LogOut className="w-4 h-4 mr-2" />
              로그아웃
            </Button>
          </ButtonGroup>
        </div>

        <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
          <div className="p-8">
            {/* Profile Section */}
            <div className="flex gap-6 mb-8">
              {/* Profile Image with AspectRatio */}
              <div className="w-32 flex-shrink-0">
                <AspectRatio ratio={1}>
                  <Avatar className="w-full h-full border-4 border-white shadow-xl">
                    <AvatarFallback className="bg-white">
                      <User className="w-16 h-16 text-gray-400" />
                    </AvatarFallback>
                  </Avatar>
                </AspectRatio>
              </div>
              <div className="flex-1">
                <h2 className="text-3xl font-black text-gray-900 mb-2">{localStorage.getItem('userName') || '의료진'}</h2>
                <p className="text-gray-600 font-medium">내과 전문의</p>
              </div>
            </div>

            {/* Information Group */}
            <div className="space-y-6">
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-4">기본 정보</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">이메일</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="email"
                        value={localStorage.getItem('userEmail') || ''}
                        disabled
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">전문 분야</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="text"
                        value="내과, 가정의학과"
                        disabled
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">연락처</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="tel"
                        value={localStorage.getItem('userPhone') || ''}
                        disabled
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">생년월일</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="date"
                        value="1985-05-15"
                        disabled
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">성별</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="text"
                        value="남성"
                        disabled
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">주소</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="text"
                        value="서울특별시 서초구 서초대로 396"
                        disabled
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
