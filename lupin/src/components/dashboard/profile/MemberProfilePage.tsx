/**
 * MemberProfilePage.tsx
 *
 * 회원 프로필 페이지 컴포넌트
 * - 개인 정보 수정
 * - 프로필 사진 변경
 * - 신체 정보 관리
 */

import { useState, useRef } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Edit, Camera } from "lucide-react";

interface MemberProfilePageProps {
  onLogout: () => void;
  profileImage: string | null;
  setProfileImage: (image: string | null) => void;
}

export default function MemberProfilePage({ onLogout, profileImage, setProfileImage }: MemberProfilePageProps) {
  const [height, setHeight] = useState("175");
  const [weight, setWeight] = useState("70");
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const profileImageInputRef = useRef<HTMLInputElement>(null);

  const handleProfileImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setProfileImage(event.target.result as string);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="h-full overflow-auto p-8 bg-gray-50/50">
      <div className="max-w-4xl mx-auto space-y-8">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">마이페이지</h1>
          <p className="text-gray-700 font-medium text-lg">내 정보를 관리하세요</p>
        </div>

        <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
          <div className="p-8">
            <div className="flex items-center gap-6 mb-8">
              <div className="relative">
                <Avatar className="w-24 h-24 border-4 border-white shadow-xl">
                  {profileImage ? (
                    <img src={profileImage} alt="Profile" className="w-full h-full object-cover" />
                  ) : (
                    <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white text-3xl font-black">
                      김
                    </AvatarFallback>
                  )}
                </Avatar>
                {isEditingProfile && (
                  <button
                    onClick={() => profileImageInputRef.current?.click()}
                    className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-[#C93831] text-white flex items-center justify-center shadow-lg hover:bg-[#B02F28]"
                  >
                    <Camera className="w-4 h-4" />
                  </button>
                )}
                <input
                  ref={profileImageInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleProfileImageChange}
                  className="hidden"
                />
              </div>
              <div>
                <h2 className="text-3xl font-black text-gray-900 mb-2">김루핀</h2>
                <p className="text-gray-600 font-medium">EMP001</p>
              </div>
              <Button
                onClick={() => setIsEditingProfile(!isEditingProfile)}
                variant="outline"
                className="ml-auto rounded-xl"
              >
                <Edit className="w-4 h-4 mr-2" />
                {isEditingProfile ? "저장" : "수정"}
              </Button>
            </div>

            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                <div className="text-sm text-gray-600 font-medium mb-1">이메일</div>
                <div className="font-bold text-gray-900">lupin@company.com</div>
              </div>

              <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                <div className="text-sm text-gray-600 font-medium mb-1">부서</div>
                <div className="font-bold text-gray-900">개발팀</div>
              </div>

              <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                <div className="text-sm text-gray-600 font-medium mb-2">키 (cm)</div>
                {isEditingProfile ? (
                  <Input
                    type="number"
                    value={height}
                    onChange={(e) => setHeight(e.target.value)}
                    className="rounded-xl border-2 border-gray-200"
                  />
                ) : (
                  <div className="font-bold text-gray-900">{height}cm</div>
                )}
              </div>

              <div className="p-4 rounded-xl bg-white/80 border border-gray-200">
                <div className="text-sm text-gray-600 font-medium mb-2">몸무게 (kg)</div>
                {isEditingProfile ? (
                  <Input
                    type="number"
                    value={weight}
                    onChange={(e) => setWeight(e.target.value)}
                    className="rounded-xl border-2 border-gray-200"
                  />
                ) : (
                  <div className="font-bold text-gray-900">{weight}kg</div>
                )}
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
