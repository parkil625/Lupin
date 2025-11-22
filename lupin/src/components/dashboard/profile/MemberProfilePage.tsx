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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Edit, Camera, User, LogOut, X } from "lucide-react";
import { toast } from "sonner";
import { imageApi } from "@/api/imageApi";

interface MemberProfilePageProps {
  onLogout: () => void;
  profileImage: string | null;
  setProfileImage: (image: string | null) => void;
}

export default function MemberProfilePage({ onLogout, profileImage, setProfileImage }: MemberProfilePageProps) {
  const [height, setHeight] = useState("175");
  const [weight, setWeight] = useState("70");
  const [phone, setPhone] = useState(localStorage.getItem("userPhone") || "");
  const [address, setAddress] = useState("서울특별시 강남구 테헤란로 123");
  const [birthDate, setBirthDate] = useState("1990-01-01");
  const [gender, setGender] = useState("남성");
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const profileImageInputRef = useRef<HTMLInputElement>(null);

  const handleProfileImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      toast.error("이미지 파일만 업로드할 수 있습니다.");
      return;
    }

    setIsUploading(true);
    const loadingToast = toast.loading("프로필 사진을 변경하고 있습니다...");

    try {
      // 이전 프로필 이미지 삭제 (S3 URL인 경우만)
      if (profileImage && profileImage.includes('s3.')) {
        await imageApi.deleteImage(profileImage).catch(() => {});
      }

      const s3Url = await imageApi.uploadImage(file, 'profile');
      setProfileImage(s3Url);
      toast.success("프로필 사진이 변경되었습니다!");
    } catch (error) {
      console.error("프로필 이미지 업로드 실패:", error);
      toast.error("사진 업로드에 실패했습니다.");
    } finally {
      toast.dismiss(loadingToast);
      setIsUploading(false);
      if (e.target) e.target.value = '';
    }
  };

  const handleRemoveProfileImage = async () => {
    if (window.confirm("프로필 사진을 삭제하고 기본 이미지로 변경하시겠습니까?")) {
      // S3에서 이미지 삭제
      if (profileImage && profileImage.includes('s3.')) {
        await imageApi.deleteImage(profileImage).catch(() => {});
      }

      setProfileImage(null);
      if (profileImageInputRef.current) {
        profileImageInputRef.current.value = '';
      }
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
                      <>
                        <button
                          onClick={() => !isUploading && profileImageInputRef.current?.click()}
                          disabled={isUploading}
                          className={`absolute bottom-0 right-0 w-10 h-10 rounded-full flex items-center justify-center shadow-lg transition-colors ${
                            isUploading
                              ? "bg-gray-400 cursor-not-allowed"
                              : "bg-[#C93831] text-white hover:bg-[#B02F28]"
                          }`}
                        >
                          <Camera className="w-5 h-5" />
                        </button>
                        {profileImage && (
                          <button
                            onClick={handleRemoveProfileImage}
                            disabled={isUploading}
                            className="absolute -top-2 -right-2 w-8 h-8 rounded-full bg-gray-200 text-gray-500 flex items-center justify-center shadow-md hover:bg-red-100 hover:text-red-500 transition-all z-10"
                            title="기본 이미지로 변경"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        )}
                      </>
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
                <h2 className="text-3xl font-black text-gray-900 mb-2">{localStorage.getItem('userName') || '사용자'}</h2>
                <p className="text-gray-600 font-medium">{localStorage.getItem('userEmail') || ''}</p>
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
                        value={localStorage.getItem('userEmail') || ''}
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
                    <Select value={gender} onValueChange={setGender} disabled={!isEditingProfile}>
                      <SelectTrigger className="mt-1.5 rounded-xl bg-white/80 border-gray-200">
                        <SelectValue placeholder="성별 선택" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="남성">남성</SelectItem>
                        <SelectItem value="여성">여성</SelectItem>
                      </SelectContent>
                    </Select>
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
