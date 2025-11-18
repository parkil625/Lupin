/**
 * MemberProfilePage.tsx
 *
 * 회원 프로필 페이지 컴포넌트
 * - 개인 정보 수정
 * - 프로필 사진 변경
 * - 신체 정보 관리
 */

import React, { useState, useRef } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { AspectRatio } from "@/components/ui/aspect-ratio";
import { InputGroup, InputGroupInput } from "@/components/ui/input-group";
import { Button } from "@/components/ui/button";
import { ButtonGroup } from "@/components/ui/button-group";
import { Label } from "@/components/ui/label";
import { Edit, Camera, User, LogOut } from "lucide-react";

interface MemberProfilePageProps {
  onLogout: () => void;
  profileImage: string | null;
  setProfileImage: (image: string | null) => void;
}

export default function MemberProfilePage({ onLogout, profileImage, setProfileImage }: MemberProfilePageProps) {
  const [height, setHeight] = useState("175");
  const [weight, setWeight] = useState("70");
  const [phone, setPhone] = useState("010-1234-5678");
  const [address, setAddress] = useState("서울특별시 강남구 테헤란로 123");
  const [birthDate, setBirthDate] = useState("1990-01-01");
  const [gender, setGender] = useState("남성");
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
        {/* Header with Title and Button Group */}
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-5xl font-black text-gray-900 mb-2">마이페이지</h1>
            <p className="text-gray-700 font-medium text-lg">내 정보를 관리하세요</p>
          </div>
          {/* Button Group */}
          <ButtonGroup>
            <Button
              onClick={() => setIsEditingProfile(!isEditingProfile)}
              variant="outline"
            >
              <Edit className="w-4 h-4 mr-2" />
              {isEditingProfile ? "저장" : "수정"}
            </Button>
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
                  <div className="relative w-full h-full">
                    <Avatar className="w-full h-full border-4 border-white shadow-xl">
                      {profileImage ? (
                        <img src={profileImage} alt="Profile" className="w-full h-full object-cover" />
                      ) : (
                        <AvatarFallback className="bg-white">
                          <User className="w-16 h-16 text-gray-400" />
                        </AvatarFallback>
                      )}
                    </Avatar>
                    {isEditingProfile && (
                      <button
                        onClick={() => profileImageInputRef.current?.click()}
                        className="absolute bottom-0 right-0 w-10 h-10 rounded-full bg-[#C93831] text-white flex items-center justify-center shadow-lg hover:bg-[#B02F28]"
                      >
                        <Camera className="w-5 h-5" />
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
                </AspectRatio>
              </div>
              <div className="flex-1">
                <h2 className="text-3xl font-black text-gray-900 mb-2">김루핀</h2>
                <p className="text-gray-600 font-medium">EMP001</p>
              </div>
            </div>

            {/* Basic Information Group */}
            <div className="space-y-6">
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-4">기본 정보</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">이메일</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="email"
                        value="lupin@company.com"
                        disabled
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">부서</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="text"
                        value="개발팀"
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
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        disabled={!isEditingProfile}
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">생년월일</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="date"
                        value={birthDate}
                        onChange={(e) => setBirthDate(e.target.value)}
                        disabled={!isEditingProfile}
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">성별</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="text"
                        value={gender}
                        onChange={(e) => setGender(e.target.value)}
                        disabled={!isEditingProfile}
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">주소</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="text"
                        value={address}
                        onChange={(e) => setAddress(e.target.value)}
                        disabled={!isEditingProfile}
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                </div>
              </div>

              {/* Body Information Group */}
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-4">신체 정보</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">키 (cm)</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="number"
                        value={height}
                        onChange={(e) => setHeight(e.target.value)}
                        disabled={!isEditingProfile}
                        className="rounded-xl bg-white/80 border-gray-200"
                      />
                    </InputGroup>
                  </div>
                  <div>
                    <Label className="text-sm text-gray-600 font-medium">몸무게 (kg)</Label>
                    <InputGroup className="mt-1.5">
                      <InputGroupInput
                        type="number"
                        value={weight}
                        onChange={(e) => setWeight(e.target.value)}
                        disabled={!isEditingProfile}
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
