/**
 * MemberProfilePage.tsx
 *
 * 회원 프로필 페이지 컴포넌트
 * - 개인 정보 수정
 * - 프로필 사진 변경
 * - 신체 정보 관리
 */

import React, { useState, useRef, useEffect } from "react";
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
import { Edit, Camera, User, LogOut, X, Link2, Unlink } from "lucide-react";
import { toast } from "sonner";
import { imageApi } from "@/api/imageApi";
import { oauthApi, OAuthConnection } from "@/api/oauthApi";

interface MemberProfilePageProps {
  onLogout: () => void;
  profileImage: string | null;
  setProfileImage: (image: string | null) => void;
}

export default function MemberProfilePage({ onLogout, profileImage, setProfileImage }: MemberProfilePageProps) {
  // localStorage에서 초기값 로드
  const [height, setHeight] = useState(() => localStorage.getItem("userHeight") || "175");
  const [weight, setWeight] = useState(() => localStorage.getItem("userWeight") || "70");
  const [phone, setPhone] = useState(() => localStorage.getItem("userPhone") || "");
  const [address, setAddress] = useState(() => localStorage.getItem("userAddress") || "서울특별시 강남구 테헤란로 123");
  const [birthDate, setBirthDate] = useState(() => localStorage.getItem("userBirthDate") || "1990-01-01");
  const [gender, setGender] = useState(() => localStorage.getItem("userGender") || "남성");
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const profileImageInputRef = useRef<HTMLInputElement>(null);

  // OAuth 연동 관련 상태
  const [oauthConnections, setOauthConnections] = useState<OAuthConnection[]>([]);
  const [isLoadingOAuth, setIsLoadingOAuth] = useState(false);

  // OAuth 연동 목록 로드
  useEffect(() => {
    const loadConnections = async () => {
      try {
        const connections = await oauthApi.getConnections();
        setOauthConnections(connections);
      } catch (error) {
        console.error("OAuth 연동 목록 로드 실패:", error);
      }
    };
    loadConnections();
  }, []);

  // 네이버 계정 연동
  const handleLinkNaver = () => {
    const state = Math.random().toString(36).substring(7);
    sessionStorage.setItem('naver_oauth_state', state);
    sessionStorage.setItem('naver_oauth_mode', 'link');

    const redirectUri = `${window.location.origin}/oauth/naver/callback`;
    const naverAuthUrl = oauthApi.getNaverAuthUrl(redirectUri, state);
    window.location.href = naverAuthUrl;
  };

  // 카카오 계정 연동
  const handleLinkKakao = () => {
    const state = Math.random().toString(36).substring(7);
    sessionStorage.setItem('kakao_oauth_state', state);
    sessionStorage.setItem('kakao_oauth_mode', 'link');

    const redirectUri = `${window.location.origin}/oauth/kakao/callback`;
    const kakaoAuthUrl = oauthApi.getKakaoAuthUrl(redirectUri, state);
    window.location.href = kakaoAuthUrl;
  };

  // OAuth 연동 해제
  const handleUnlinkOAuth = async (provider: string) => {
    if (!window.confirm(`${provider} 계정 연동을 해제하시겠습니까?`)) return;

    setIsLoadingOAuth(true);
    try {
      await oauthApi.unlinkOAuth(provider);
      setOauthConnections(prev => prev.filter(c => c.provider !== provider));
      toast.success(`${provider} 계정 연동이 해제되었습니다.`);
    } catch (error) {
      console.error("OAuth 연동 해제 실패:", error);
      toast.error("연동 해제에 실패했습니다.");
    } finally {
      setIsLoadingOAuth(false);
    }
  };

  // 연동 여부 확인
  const isLinked = (provider: string) => {
    return oauthConnections.some(c => c.provider === provider);
  };

  // 프로필 저장 핸들러
  const handleSaveProfile = () => {
    localStorage.setItem("userHeight", height);
    localStorage.setItem("userWeight", weight);
    localStorage.setItem("userPhone", phone);
    localStorage.setItem("userAddress", address);
    localStorage.setItem("userBirthDate", birthDate);
    localStorage.setItem("userGender", gender);
    setIsEditingProfile(false);
    toast.success("프로필이 저장되었습니다!");
  };

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
              onClick={() => isEditingProfile ? handleSaveProfile() : setIsEditingProfile(true)}
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

              {/* OAuth 연동 관리 */}
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-4">계정 연동</h3>
                <div className="space-y-3">
                  {/* 구글 연동 */}
                  <div className="flex items-center justify-between p-4 rounded-xl bg-white/80 border border-gray-200">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-white flex items-center justify-center">
                        <svg width="20" height="20" viewBox="0 0 24 24">
                          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">구글</p>
                        {isLinked('GOOGLE') && (
                          <p className="text-sm text-gray-500">
                            {oauthConnections.find(c => c.provider === 'GOOGLE')?.providerEmail || '연동됨'}
                          </p>
                        )}
                      </div>
                    </div>
                    {isLinked('GOOGLE') ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleUnlinkOAuth('GOOGLE')}
                        disabled={isLoadingOAuth}
                        className="text-red-600 border-red-300 hover:bg-red-50"
                      >
                        <Unlink className="w-4 h-4 mr-1" />
                        연동 해제
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={isLoadingOAuth}
                        className="text-gray-600 border-gray-300 hover:bg-gray-50"
                      >
                        <Link2 className="w-4 h-4 mr-1" />
                        연동하기
                      </Button>
                    )}
                  </div>

                  {/* 네이버 연동 */}
                  <div className="flex items-center justify-between p-4 rounded-xl bg-white/80 border border-gray-200">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-[#03C75A] flex items-center justify-center">
                        <img src="/naver-logo.svg" alt="Naver" className="w-5 h-5 rounded" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">네이버</p>
                        {isLinked('NAVER') && (
                          <p className="text-sm text-gray-500">
                            {oauthConnections.find(c => c.provider === 'NAVER')?.providerEmail || '연동됨'}
                          </p>
                        )}
                      </div>
                    </div>
                    {isLinked('NAVER') ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleUnlinkOAuth('NAVER')}
                        disabled={isLoadingOAuth}
                        className="text-red-600 border-red-300 hover:bg-red-50"
                      >
                        <Unlink className="w-4 h-4 mr-1" />
                        연동 해제
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleLinkNaver}
                        disabled={isLoadingOAuth}
                        className="text-[#03C75A] border-[#03C75A] hover:bg-green-50"
                      >
                        <Link2 className="w-4 h-4 mr-1" />
                        연동하기
                      </Button>
                    )}
                  </div>

                  {/* 카카오 연동 */}
                  <div className="flex items-center justify-between p-4 rounded-xl bg-white/80 border border-gray-200">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-[#FEE500] flex items-center justify-center">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                          <path fillRule="evenodd" clipRule="evenodd" d="M12 4C7.02944 4 3 7.16467 3 11.04C3 13.4227 4.5584 15.5272 6.93 16.7L6.3 19.5C6.26 19.68 6.36 19.8 6.52 19.8C6.6 19.8 6.68 19.76 6.74 19.7L10.08 17.58C10.7 17.66 11.34 17.7 12 17.7C16.9706 17.7 21 14.5353 21 10.66C21 7.16467 16.9706 4 12 4Z" fill="#3C1E1E"/>
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">카카오</p>
                        {isLinked('KAKAO') && (
                          <p className="text-sm text-gray-500">
                            {oauthConnections.find(c => c.provider === 'KAKAO')?.providerEmail || '연동됨'}
                          </p>
                        )}
                      </div>
                    </div>
                    {isLinked('KAKAO') ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleUnlinkOAuth('KAKAO')}
                        disabled={isLoadingOAuth}
                        className="text-red-600 border-red-300 hover:bg-red-50"
                      >
                        <Unlink className="w-4 h-4 mr-1" />
                        연동 해제
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleLinkKakao}
                        disabled={isLoadingOAuth}
                        className="text-[#3C1E1E] border-[#FEE500] hover:bg-yellow-50"
                      >
                        <Link2 className="w-4 h-4 mr-1" />
                        연동하기
                      </Button>
                    )}
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
