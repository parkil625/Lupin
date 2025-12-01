/**
 * MemberProfilePage.tsx
 *
 * 회원 프로필 페이지 컴포넌트
 * - 개인 정보 수정
 * - 프로필 사진 변경
 * - 신체 정보 관리
 * - OAuth 계정 연동 (구글, 네이버, 카카오)
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
import { Edit, Camera, User, LogOut, X, Link2, Unlink, ChevronDown, LinkIcon } from "lucide-react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { imageApi } from "@/api/imageApi";
import { oauthApi, OAuthConnection } from "@/api/oauthApi";

// 구글 타입을 위한 선언
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

    // 연동 여부 확인 헬퍼 함수
    const isLinked = (provider: string) => {
        return oauthConnections.some(c => c.provider === provider);
    };

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

    // [New] 구글 연동 핸들러
    const handleLinkGoogle = async (response: any) => {
        setIsLoadingOAuth(true);
        try {
            await oauthApi.linkGoogle(response.credential);
            // 목록 갱신
            const connections = await oauthApi.getConnections();
            setOauthConnections(connections);
            toast.success("구글 계정이 연동되었습니다.");
        } catch (error: any) {
            console.error("Google link failed:", error);
            if (error.response?.data?.message) {
                toast.error(error.response.data.message);
            } else {
                toast.error("구글 연동에 실패했습니다.");
            }
        } finally {
            setIsLoadingOAuth(false);
        }
    };

    // [New] 구글 스크립트 로드 및 버튼 초기화
    useEffect(() => {
        // 이미 연동되어 있으면 스크립트 로드 불필요
        if (isLinked('GOOGLE')) return;

        const script = document.createElement("script");
        script.src = "https://accounts.google.com/gsi/client";
        script.async = true;
        script.defer = true;
        script.onload = () => {
            if (window.google) {
                window.google.accounts.id.initialize({
                    client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID || "",
                    callback: handleLinkGoogle,
                });

                // 숨겨진 버튼 렌더링
                const hiddenBtn = document.getElementById("google-link-hidden-btn");
                if (hiddenBtn) {
                    window.google.accounts.id.renderButton(hiddenBtn, { theme: "outline", size: "large", type: "icon" });
                }
            }
        };
        document.body.appendChild(script);

        return () => {
            if (document.body.contains(script)) {
                document.body.removeChild(script);
            }
        };
    }, [oauthConnections]); // 연동 상태가 바뀌면(해제 후 다시 연동 등) 재실행

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
            // 연동 목록 다시 로드하여 실제 상태 반영
            const connections = await oauthApi.getConnections();
            setOauthConnections(connections);
            toast.success(`${provider} 계정 연동이 해제되었습니다.`);
        } catch (error: any) {
            console.error("OAuth 연동 해제 실패:", error);
            const message = error.response?.data?.message || "연동 해제에 실패했습니다.";
            toast.error(message);
        } finally {
            setIsLoadingOAuth(false);
        }
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
                await imageApi.deleteImage(profileImage).catch(() => { });
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
                await imageApi.deleteImage(profileImage).catch(() => { });
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
                                                    className={`absolute bottom-0 right-0 w-10 h-10 rounded-full flex items-center justify-center shadow-lg transition-colors ${isUploading
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
                                <h2 className="text-3xl font-black text-gray-900 mb-1">{localStorage.getItem('userName') || '사용자'}</h2>
                                <p className="text-gray-500 font-medium text-sm mb-1">개발팀</p>
                                <p className="text-gray-400 text-sm">
                                    {(() => {
                                        const email = localStorage.getItem('userEmail');
                                        return email && email !== 'null' ? email : '';
                                    })()}
                                </p>
                            </div>
                        </div>

                        {/* Basic Information Group */}
                        <div className="space-y-6">
                            <div>
                                <h3 className="text-lg font-bold text-gray-900 mb-4">기본 정보</h3>
                                <div className="grid grid-cols-2 gap-4">
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
                                <h3 className="text-lg font-bold text-gray-900 mb-4">소셜 계정 연동</h3>
                                <div className="p-5 rounded-xl bg-white/80 border border-gray-200">
                                    {(() => {
                                        // 연동된 계정 찾기 (하나만 연동 가능)
                                        const linkedAccount = oauthConnections[0];

                                        const providerInfo: Record<string, { name: string; logo: string; bgColor: string; borderColor: string }> = {
                                            'GOOGLE': { name: 'Google', logo: '/google-logo.png', bgColor: 'bg-white', borderColor: 'border-gray-300' },
                                            'NAVER': { name: 'Naver', logo: '/naver-logo.png', bgColor: 'bg-[#03C75A]', borderColor: 'border-[#03C75A]' },
                                            'KAKAO': { name: 'Kakao', logo: '/kakao-logo.png', bgColor: 'bg-[#FEE500]', borderColor: 'border-[#FEE500]' },
                                        };

                                        if (linkedAccount) {
                                            // 연동된 계정이 있으면 해당 계정 정보 표시
                                            const info = providerInfo[linkedAccount.provider] || providerInfo['GOOGLE'];

                                            return (
                                                <div className="flex items-center justify-between">
                                                    <div className="flex items-center gap-4">
                                                        <div className={`w-12 h-12 rounded-xl ${info.bgColor} border ${info.borderColor} flex items-center justify-center shadow-sm`}>
                                                            <img src={info.logo} alt={info.name} className="w-7 h-7 object-contain" />
                                                        </div>
                                                        <div>
                                                            <div className="flex items-center gap-2">
                                                                <span className="font-semibold text-gray-900">{info.name}</span>
                                                                <span className="px-2 py-0.5 text-xs font-medium bg-green-100 text-green-700 rounded-full">연결됨</span>
                                                            </div>
                                                            <p className="text-sm text-gray-500 mt-0.5">{linkedAccount.providerEmail}</p>
                                                        </div>
                                                    </div>
                                                    <Button
                                                        variant="outline"
                                                        size="sm"
                                                        onClick={() => handleUnlinkOAuth(linkedAccount.provider)}
                                                        disabled={isLoadingOAuth}
                                                        className="text-red-600 border-red-200 hover:bg-red-50 hover:border-red-300"
                                                    >
                                                        <Unlink className="w-4 h-4 mr-1.5" />
                                                        연결 끊기
                                                    </Button>
                                                </div>
                                            );
                                        } else {
                                            // 연동된 계정이 없으면 연결 안 된 상태 표시
                                            return (
                                                <div className="flex items-center justify-between">
                                                    <div className="flex items-center gap-4">
                                                        <div className="w-12 h-12 rounded-xl bg-gray-100 border border-dashed border-gray-300 flex items-center justify-center">
                                                            <LinkIcon className="w-6 h-6 text-gray-400" />
                                                        </div>
                                                        <div>
                                                            <p className="font-medium text-gray-500">연결된 계정 없음</p>
                                                            <p className="text-sm text-gray-400 mt-0.5">소셜 계정을 연동하여 간편하게 로그인하세요</p>
                                                        </div>
                                                    </div>
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <Button
                                                                variant="outline"
                                                                disabled={isLoadingOAuth}
                                                                className="border-[#C93831] text-[#C93831] hover:bg-red-50"
                                                            >
                                                                <Link2 className="w-4 h-4 mr-1.5" />
                                                                연결
                                                                <ChevronDown className="w-4 h-4 ml-1" />
                                                            </Button>
                                                        </DropdownMenuTrigger>
                                                        <DropdownMenuContent align="end" className="w-48">
                                                            <DropdownMenuItem
                                                                onClick={() => {
                                                                    const btn = document.querySelector('#google-link-hidden-btn div[role="button"]') as HTMLElement;
                                                                    if (btn) btn.click();
                                                                }}
                                                                className="cursor-pointer"
                                                            >
                                                                <img src="/google-logo.png" alt="Google" className="w-5 h-5 mr-3" />
                                                                Google로 연결
                                                            </DropdownMenuItem>
                                                            <DropdownMenuItem
                                                                onClick={handleLinkNaver}
                                                                className="cursor-pointer"
                                                            >
                                                                <img src="/naver-logo.png" alt="Naver" className="w-5 h-5 mr-3" />
                                                                Naver로 연결
                                                            </DropdownMenuItem>
                                                            <DropdownMenuItem
                                                                onClick={handleLinkKakao}
                                                                className="cursor-pointer"
                                                            >
                                                                <img src="/kakao-logo.png" alt="Kakao" className="w-5 h-5 mr-3" />
                                                                Kakao로 연결
                                                            </DropdownMenuItem>
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                    {/* 숨겨진 구글 GSI 버튼 */}
                                                    <div id="google-link-hidden-btn" className="hidden" style={{ display: 'none' }}></div>
                                                </div>
                                            );
                                        }
                                    })()}
                                </div>
                            </div>
                        </div>
                    </div>
                </Card>
            </div>
        </div>
    );
}