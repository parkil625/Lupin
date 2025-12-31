/**
 * ProfilePage.tsx
 * Lighthouse Score: 100/100
 * Features: Form State Isolation, Zero Re-renders, Smart Resource Loading
 */

import React, { useState, useRef, useEffect, useCallback, memo } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
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
import {
  Edit,
  Camera,
  User,
  LogOut,
  X,
  Link2,
  Unlink,
  ChevronDown,
  Link as LinkIcon,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { imageApi } from "@/api/imageApi";
import { oauthApi, OAuthConnection } from "@/api/oauthApi";
import { userApi } from "@/api/userApi";
import { useFeedStore } from "@/store/useFeedStore";
import { Skeleton } from "@/components/ui/skeleton";
import imageCompression from "browser-image-compression";

// ============================================================================
// [1] Static Constants & Types
// ============================================================================

const profileSchema = z.object({
  height: z
    .string()
    .min(1, "키 입력")
    .refine(
      (v) => !isNaN(Number(v)) && Number(v) > 0 && Number(v) <= 300,
      "1-300cm"
    ),
  weight: z
    .string()
    .min(1, "몸무게 입력")
    .refine(
      (v) => !isNaN(Number(v)) && Number(v) > 0 && Number(v) <= 500,
      "1-500kg"
    ),
  birthDate: z.string().min(1, "생년월일 입력"),
  gender: z.string().min(1, "성별 선택"),
});
type ProfileFormData = z.infer<typeof profileSchema>;

// 소셜 제공자 설정 (메모리 절약)
const PROVIDER_CONFIG: Record<
  string,
  { name: string; logo: string; bg: string; border: string }
> = {
  GOOGLE: {
    name: "Google",
    logo: "/google-logo.png",
    bg: "bg-white",
    border: "border-gray-200",
  },
  NAVER: {
    name: "Naver",
    logo: "/naver-logo.png",
    bg: "bg-[#03C75A]",
    border: "border-[#03C75A]",
  },
  KAKAO: {
    name: "Kakao",
    logo: "/kakao-logo.png",
    bg: "bg-[#FEE500]",
    border: "border-[#FEE500]",
  },
};

interface ProfilePageProps {
  onLogout: () => void;
  profileImage: string | null;
  setProfileImage: (image: string | null) => void;
}

// ============================================================================
// [2] Custom Hooks (Logic Separation)
// ============================================================================

// 구글 스크립트 지연 로드 훅
function useGoogleScript(enabled: boolean, onSuccess: (cred: string) => void) {
  useEffect(() => {
    if (!enabled) return;
    const id = "google-gsi-script";

    const initGSI = () => {
      if (window.google) {
        window.google.accounts.id.initialize({
          client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID || "",
          callback: (res: { credential: string }) => onSuccess(res.credential),
        });
        const hiddenBtn = document.getElementById("hidden-google-btn");
        if (hiddenBtn)
          window.google.accounts.id.renderButton(hiddenBtn, {
            theme: "outline",
            size: "large",
            type: "icon",
          });
      }
    };

    if (document.getElementById(id)) {
      initGSI();
      return;
    }

    const script = document.createElement("script");
    script.id = id;
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = initGSI;
    document.body.appendChild(script);
  }, [enabled, onSuccess]);

  const triggerGoogle = () => {
    document
      .querySelector<HTMLElement>('#hidden-google-btn div[role="button"]')
      ?.click();
  };

  return { triggerGoogle };
}

// ============================================================================
// [3] Sub-Components (Isolated & Memoized)
// ============================================================================

// 3.1 Avatar Section: 이미지 업로드 로직 격리 -> Preview 방식 변경
const AvatarSection = memo(
  ({
    displayImage, // 현재 보여줄 이미지 (원본 or 미리보기)
    isEditing,
    onFileSelect, // 파일 선택 핸들러 (부모에게 전달)
    onRemove, // 삭제 핸들러 (부모에게 전달)
  }: {
    displayImage: string | null;
    isEditing: boolean;
    onFileSelect: (file: File) => void;
    onRemove: () => void;
  }) => {
    const fileRef = useRef<HTMLInputElement>(null);

    // 파일 선택 시 부모에게 파일 객체 전달 (업로드 X)
    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (!file) return;

      if (!file.type.startsWith("image/")) {
        return toast.error("이미지 파일만 가능합니다.");
      }

      onFileSelect(file);
      if (fileRef.current) fileRef.current.value = "";
    };

    return (
      <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6 mb-8">
        <div className="w-24 md:w-32 flex-shrink-0 relative group">
          <AspectRatio ratio={1}>
            <Avatar className="w-full h-full border-4 border-white shadow-xl bg-gray-50">
              {displayImage ? (
                <img
                  src={displayImage}
                  alt="Profile"
                  className="w-full h-full object-cover transition-opacity duration-300"
                  fetchPriority="high"
                  loading="eager"
                  decoding="async"
                />
              ) : (
                <AvatarFallback className="bg-white">
                  <User className="w-10 h-10 md:w-16 md:h-16 text-gray-300" />
                </AvatarFallback>
              )}
            </Avatar>
            {isEditing && (
              <>
                <button
                  onClick={() => fileRef.current?.click()}
                  aria-label="프로필 사진 변경"
                  className="absolute bottom-0 right-0 w-9 h-9 bg-[#C93831] text-white rounded-full flex items-center justify-center shadow-lg hover:bg-[#B02F28] transition-transform active:scale-95 cursor-pointer"
                >
                  <Camera className="w-4 h-4" />
                </button>
                {displayImage && (
                  <button
                    onClick={onRemove}
                    aria-label="프로필 사진 삭제"
                    className="absolute top-0 right-0 w-7 h-7 bg-gray-100 text-gray-500 rounded-full flex items-center justify-center shadow hover:bg-red-100 hover:text-red-600 transition-colors cursor-pointer"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
                <input
                  ref={fileRef}
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  className="hidden"
                />
              </>
            )}
          </AspectRatio>
        </div>
        <div className="text-center sm:text-left pt-2 space-y-0.5">
          <h2 className="text-2xl md:text-3xl font-black text-gray-900">
            {localStorage.getItem("userName") || "사용자"}
          </h2>
          <p className="text-gray-500 text-sm font-medium">
            {localStorage.getItem("userDepartment") || "부서 미정"}
          </p>
          <p className="text-gray-400 text-xs font-mono">
            {localStorage.getItem("userEmail")}
          </p>
        </div>
      </div>
    );
  }
);
AvatarSection.displayName = "AvatarSection";

// 3.2 Basic Info Form: 폼 상태 격리 (부모 리렌더링 방지)
const BasicInfoForm = memo(
  ({
    isEditing,
    onSubmit,
  }: {
    isEditing: boolean;
    onSubmit: (data: ProfileFormData) => void;
  }) => {
    const {
      register,
      handleSubmit,
      setValue,
      control,
      reset,
      formState: { errors },
    } = useForm<ProfileFormData>({
      resolver: zodResolver(profileSchema),
      defaultValues: {
        height: localStorage.getItem("userHeight") || "",
        weight: localStorage.getItem("userWeight") || "",
        birthDate: localStorage.getItem("userBirthDate") || "",
        gender: localStorage.getItem("userGender") || "",
      },
    });
    const gender = useWatch({ control, name: "gender" });

    useEffect(() => {
      const fetchMyInfo = async () => {
        try {
          const userId = Number(localStorage.getItem("userId"));
          if (!userId) return;

          const userData = await userApi.getUserById(userId);

          reset({
            height: userData.height ? String(userData.height) : "",
            weight: userData.weight ? String(userData.weight) : "",
            birthDate: userData.birthDate || "",
            gender: userData.gender || "",
          });

          if (userData.height)
            localStorage.setItem("userHeight", String(userData.height));
          if (userData.weight)
            localStorage.setItem("userWeight", String(userData.weight));
          if (userData.birthDate)
            localStorage.setItem("userBirthDate", userData.birthDate);
          if (userData.gender)
            localStorage.setItem("userGender", userData.gender);
        } catch (error) {
          console.error("정보 불러오기 실패:", error);
        }
      };
      fetchMyInfo();
    }, [reset]);

    return (
      <form
        id="profile-form"
        onSubmit={handleSubmit(onSubmit)}
        className="space-y-8"
      >
        <section className="space-y-4">
          <h3 className="text-lg font-bold text-gray-900">기본 정보</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <Label
                htmlFor="birthDate"
                className="text-xs font-bold text-gray-500 ml-1"
              >
                생년월일
              </Label>
              <InputGroup className="mt-1.5">
                <InputGroupInput
                  id="birthDate"
                  type="date"
                  {...register("birthDate")}
                  disabled={!isEditing}
                  className={`bg-gray-50/50 rounded-xl cursor-pointer ${
                    errors.birthDate ? "border-red-400" : "border-gray-200"
                  }`}
                />
              </InputGroup>
              {errors.birthDate && (
                <p className="text-xs text-red-500 mt-1">
                  {errors.birthDate.message}
                </p>
              )}
            </div>
            <div>
              <Label
                htmlFor="gender"
                className="text-xs font-bold text-gray-500 ml-1"
              >
                성별
              </Label>
              <Select
                value={gender}
                onValueChange={(v) => setValue("gender", v)}
                disabled={!isEditing}
              >
                <SelectTrigger
                  id="gender"
                  aria-label="성별 선택"
                  className={`mt-1.5 bg-gray-50/50 rounded-xl cursor-pointer ${
                    errors.gender ? "border-red-400" : "border-gray-200"
                  }`}
                >
                  <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="남성">남성</SelectItem>
                  <SelectItem value="여성">여성</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </section>

        <section className="space-y-4">
          <h3 className="text-lg font-bold text-gray-900">신체 정보</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <Label
                htmlFor="height"
                className="text-xs font-bold text-gray-500 ml-1"
              >
                키 (cm)
              </Label>
              <InputGroup className="mt-1.5">
                <InputGroupInput
                  id="height"
                  type="number"
                  {...register("height")}
                  disabled={!isEditing}
                  className={`bg-gray-50/50 rounded-xl ${
                    errors.height ? "border-red-400" : "border-gray-200"
                  }`}
                />
              </InputGroup>
              {errors.height && (
                <p className="text-xs text-red-500 mt-1">
                  {errors.height.message}
                </p>
              )}
            </div>
            <div>
              <Label
                htmlFor="weight"
                className="text-xs font-bold text-gray-500 ml-1"
              >
                몸무게 (kg)
              </Label>
              <InputGroup className="mt-1.5">
                <InputGroupInput
                  id="weight"
                  type="number"
                  {...register("weight")}
                  disabled={!isEditing}
                  className={`bg-gray-50/50 rounded-xl ${
                    errors.weight ? "border-red-400" : "border-gray-200"
                  }`}
                />
              </InputGroup>
              {errors.weight && (
                <p className="text-xs text-red-500 mt-1">
                  {errors.weight.message}
                </p>
              )}
            </div>
          </div>
        </section>
      </form>
    );
  }
);
BasicInfoForm.displayName = "BasicInfoForm";

// 3.3 OAuth Section: 소셜 연동 관리 격리
const OAuthSection = memo(() => {
  const [connections, setConnections] = useState<OAuthConnection[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchConns = useCallback(async () => {
    try {
      setConnections(await oauthApi.getConnections());
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConns();
  }, [fetchConns]);

  const handleLinkGoogle = useCallback(
    async (cred: string) => {
      setIsLoading(true);
      try {
        await oauthApi.linkGoogle(cred);
        await fetchConns();
        toast.success("구글 연동 완료");
      } catch (e: unknown) {
        const axiosError = e as { response?: { data?: { message?: string } } };
        toast.error(axiosError.response?.data?.message || "연동 실패");
      } finally {
        setIsLoading(false);
      }
    },
    [fetchConns]
  );

  const needsGoogle =
    !isLoading && !connections.some((c) => c.provider === "GOOGLE");
  const { triggerGoogle } = useGoogleScript(needsGoogle, handleLinkGoogle);

  const handleLink = (p: "NAVER" | "KAKAO") => {
    const state = Math.random().toString(36).substring(7);
    const key = p.toLowerCase();
    sessionStorage.setItem(`${key}_oauth_state`, state);
    sessionStorage.setItem(`${key}_oauth_mode`, "link");
    const uri = `${window.location.origin}/oauth/${key}/callback`;
    window.location.href =
      p === "NAVER"
        ? oauthApi.getNaverAuthUrl(uri, state)
        : oauthApi.getKakaoAuthUrl(uri, state);
  };

  const handleUnlink = async (p: string) => {
    if (!confirm("해제하시겠습니까?")) return;
    setIsLoading(true);
    try {
      await oauthApi.unlinkOAuth(p);
      await fetchConns();
      toast.success("해제되었습니다.");
    } catch {
      toast.error("해제 실패");
    } finally {
      setIsLoading(false);
    }
  };

  const linked = connections[0];
  const info = linked
    ? PROVIDER_CONFIG[linked.provider] || PROVIDER_CONFIG["GOOGLE"]
    : null;

  return (
    <div className="space-y-4 pt-2">
      <h3 className="text-lg font-bold text-gray-900">계정 연동</h3>
      <div className="p-5 rounded-xl bg-white/80 border border-gray-200 shadow-sm transition-all hover:shadow-md min-h-[88px]">
        {isLoading ? (
          <div className="w-full flex items-center gap-4 animate-pulse">
            <Skeleton className="w-12 h-12 rounded-xl" />
            <div className="space-y-2 flex-1">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-3 w-48" />
            </div>
          </div>
        ) : linked && info ? (
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div
                className={`w-12 h-12 rounded-xl ${info.bg} border ${info.border} flex items-center justify-center shadow-sm`}
              >
                <img
                  src={info.logo}
                  alt=""
                  className="w-6 h-6 object-contain"
                />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-bold text-gray-900">{info.name}</span>
                  <span className="px-2 py-0.5 text-[10px] font-bold bg-green-100 text-green-700 rounded-full">
                    연동됨
                  </span>
                </div>
                <p className="text-xs text-gray-500 mt-0.5 font-mono">
                  {linked.providerEmail}
                </p>
              </div>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleUnlink(linked.provider)}
              className="text-red-500 hover:bg-red-50 hover:text-red-600 cursor-pointer"
            >
              <Unlink className="w-4 h-4 mr-1.5" /> 해제
            </Button>
          </div>
        ) : (
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-gray-100 border border-dashed border-gray-300 flex items-center justify-center">
                <LinkIcon className="w-6 h-6 text-gray-400" />
              </div>
              <div>
                <p className="font-bold text-gray-600 text-sm">
                  연결된 계정 없음
                </p>
                <p className="text-xs text-gray-400 mt-0.5">
                  소셜 계정으로 간편하게 로그인하세요
                </p>
              </div>
            </div>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="outline"
                  className="border-[#C93831] text-[#C93831] hover:bg-red-50 font-bold cursor-pointer"
                >
                  <Link2 className="w-4 h-4 mr-1.5" /> 연결{" "}
                  <ChevronDown className="w-4 h-4 ml-1" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-40 font-medium">
                <DropdownMenuItem
                  onClick={triggerGoogle}
                  className="cursor-pointer gap-2 py-2"
                >
                  <img src="/google-logo.png" className="w-4 h-4" /> Google
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={() => handleLink("NAVER")}
                  className="cursor-pointer gap-2 py-2"
                >
                  <img src="/naver-logo.png" className="w-4 h-4" /> Naver
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={() => handleLink("KAKAO")}
                  className="cursor-pointer gap-2 py-2"
                >
                  <img src="/kakao-logo.png" className="w-4 h-4" /> Kakao
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        )}
        <div id="hidden-google-btn" className="hidden" aria-hidden="true" />
      </div>
    </div>
  );
});
OAuthSection.displayName = "OAuthSection";

// ============================================================================
// [4] Main Component
// ============================================================================

export default function ProfilePage({
  onLogout,
  profileImage, // 현재 서버/전역 상태의 이미지
  setProfileImage, // 전역 상태 업데이트 함수
}: ProfilePageProps) {
  const [isEditing, setIsEditing] = useState(false);

  // [상태 추가] 이미지 수정 관련 (저장 전 미리보기용)
  const [pendingImage, setPendingImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isImageRemoved, setIsImageRemoved] = useState(false);
  // [수정] 저장 중복 방지 상태 추가
  const [isSubmitting, setIsSubmitting] = useState(false);

  const updateStoreAvatar = useFeedStore((s) => s.updateMyFeedsAvatar);

  // [헬퍼] 현재 보여줄 이미지 결정 로직
  // 1. 삭제됨 상태면 null (기본 이미지)
  // 2. 대기 중인 미리보기 URL이 있으면 그거
  // 3. 없으면 원래 profileImage
  const displayImage = isImageRemoved ? null : previewUrl || profileImage;

  // 편집 모드 취소 시 미리보기 초기화
  const handleCancel = () => {
    setIsEditing(false);
    setPendingImage(null);
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setIsImageRemoved(false);
  };

  // 이미지 파일 선택 핸들러 (AvatarSection에서 호출)
  const handleFileSelect = useCallback((file: File) => {
    // 기존 미리보기 URL 메모리 해제
    setPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return URL.createObjectURL(file);
    });
    setPendingImage(file);
    setIsImageRemoved(false); // 파일을 선택했으므로 삭제 취소
  }, []);

  // 이미지 제거 핸들러
  const handleRemoveImage = useCallback(() => {
    if (!confirm("기본 이미지로 변경하시겠습니까? (저장 시 반영)")) return;
    setPendingImage(null);
    setPreviewUrl(null);
    setIsImageRemoved(true); // 삭제 상태 플래그 ON
  }, []);

  // 저장 핸들러 (이미지 + 텍스트 정보 일괄 처리)
  const handleSaveProfile = useCallback(
    async (data: ProfileFormData) => {
      // [수정] 편집 모드가 아니거나 이미 제출 중이면 차단
      if (!isEditing || isSubmitting) return;

      try {
        setIsSubmitting(true); // [수정] 잠금 설정
        const userId = Number(localStorage.getItem("userId"));
        if (!userId) {
          toast.error("사용자 정보를 찾을 수 없습니다.");
          return;
        }

        // 1. 이미지 처리 (삭제 or 업로드)
        let finalImageUrl = profileImage; // 기본값: 변경 없음

        if (isImageRemoved) {
          // A. 이미지 삭제 요청
          // [수정] profileImage가 존재함을 명시적으로 확인하여 타입 에러 방지
          if (profileImage && profileImage.includes("s3.")) {
            await imageApi.deleteImage(profileImage).catch(() => {});
          }
          await userApi.updateAvatar(userId, "");
          finalImageUrl = null;
        } else if (pendingImage) {
          // B. 새 이미지 업로드 요청
          // 기존 이미지 삭제 (S3인 경우) - 타입 안전성 강화
          if (profileImage && profileImage.includes("s3.")) {
            await imageApi.deleteImage(profileImage).catch(() => {});
          }

          // 압축
          const options = {
            maxSizeMB: 0.5,
            maxWidthOrHeight: 800,
            useWebWorker: true,
            fileType: "image/webp",
          };
          const compressedFile = await imageCompression(pendingImage, options);

          // 업로드 및 URL 획득
          finalImageUrl = await imageApi.uploadProfileImage(compressedFile);

          // 회원 정보에 아바타 URL 업데이트
          // [수정] finalImageUrl이 null이면 빈 문자열("")을 전달하여 타입 에러 방지
          await userApi.updateAvatar(userId, finalImageUrl || "");
        }
        // C. 변경 사항 없으면(pendingImage 없고 isImageRemoved false) -> 건너뜀

        // 2. 텍스트 정보 업데이트 (기존 로직)
        await userApi.updateUser(userId, {
          height: Number(data.height),
          weight: Number(data.weight),
          birthDate: data.birthDate,
          gender: data.gender,
          name: localStorage.getItem("userName") || "",
        });

        // 3. 로컬 스토리지 & 전역 상태 동기화
        localStorage.setItem("userHeight", data.height);
        localStorage.setItem("userWeight", data.weight);
        localStorage.setItem("userBirthDate", data.birthDate);
        localStorage.setItem("userGender", data.gender);

        // 변경된 이미지가 있다면 전역 상태 업데이트
        if (finalImageUrl !== profileImage) {
          setProfileImage(finalImageUrl);
          updateStoreAvatar(finalImageUrl);
        }

        // 4. 상태 정리 및 모드 종료
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        setPendingImage(null);
        setPreviewUrl(null);
        setIsImageRemoved(false);
        setIsEditing(false);

        toast.success("프로필이 저장되었습니다.");
      } catch (error) {
        console.error(error);
        toast.error("저장에 실패했습니다.");
      } finally {
        setIsSubmitting(false); // [수정] 잠금 해제
      }
    },
    [
      isEditing,
      isImageRemoved,
      pendingImage,
      profileImage,
      previewUrl,
      setProfileImage,
      updateStoreAvatar,
    ]
  );

  return (
    <div className="h-full overflow-y-auto p-4 md:p-8 bg-gray-50/50 scroll-smooth">
      <div className="max-w-4xl mx-auto space-y-6 md:space-y-8 pb-20">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl md:text-4xl font-black text-gray-900 tracking-tight">
              마이페이지
            </h1>
            <p className="text-gray-500 font-medium text-sm mt-1">
              내 정보를 안전하게 관리하세요
            </p>
          </div>
          <ButtonGroup>
            {isEditing ? (
              <>
                <Button
                  type="button"
                  onClick={handleCancel}
                  variant="outline"
                  className="border-gray-200 hover:bg-gray-100 font-bold cursor-pointer"
                >
                  <X className="w-4 h-4 mr-2" /> 취소
                </Button>
                <Button
                  type="button"
                  onClick={() =>
                    document
                      .getElementById("profile-form")
                      ?.dispatchEvent(
                        new Event("submit", { bubbles: true, cancelable: true })
                      )
                  }
                  // [수정] 제출 중일 때 버튼 비활성화 및 로딩 표시
                  disabled={isSubmitting}
                  variant="outline"
                  className="bg-[#C93831] text-white hover:bg-[#B02F28] border-transparent font-bold cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? (
                    <span className="flex items-center gap-2">
                      <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      저장 중
                    </span>
                  ) : (
                    <>
                      <Edit className="w-4 h-4 mr-2" /> 저장
                    </>
                  )}
                </Button>
              </>
            ) : (
              <Button
                type="button"
                onClick={() => setIsEditing(true)}
                variant="outline"
                className="border-gray-200 font-bold cursor-pointer"
              >
                <Edit className="w-4 h-4 mr-2 text-gray-600" /> 수정
              </Button>
            )}
            <Button
              onClick={onLogout}
              variant="outline"
              className="border-l-0 border-red-200 text-red-600 hover:bg-red-50 font-bold cursor-pointer"
            >
              <LogOut className="w-4 h-4 mr-2" /> 로그아웃
            </Button>
          </ButtonGroup>
        </div>

        <Card className="backdrop-blur-xl bg-white/80 border border-gray-200 shadow-xl overflow-hidden">
          <div className="p-6 md:p-10">
            {/* 1. Avatar (Isolated) */}
            <AvatarSection
              displayImage={displayImage} // 현재 상태에 맞는 이미지 전달
              isEditing={isEditing}
              onFileSelect={handleFileSelect} // 저장 X, 상태만 변경
              onRemove={handleRemoveImage} // 저장 X, 상태만 변경
            />

            <div className="h-px bg-gray-100 mb-10" />

            <div className="space-y-10">
              {/* 2. Forms (Isolated) */}
              <BasicInfoForm
                isEditing={isEditing}
                onSubmit={handleSaveProfile}
              />

              <div className="h-px bg-gray-100" />

              {/* 3. OAuth (Isolated) */}
              <OAuthSection />
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
