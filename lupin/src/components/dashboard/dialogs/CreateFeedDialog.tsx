/**
 * CreateFeedDialog.tsx
 *
 * 피드 작성 다이얼로그 컴포넌트
 * - 새 피드 작성
 * - 인스타그램 스타일 textarea 사용
 * - 운동 시작/끝 사진 업로드
 */

import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { toast } from "sonner";
import { ImageUploadBox, WorkoutTypeSelect } from "@/components/molecules";
import { Image, FileText, X } from "lucide-react";
import { FeedContentInput } from "@/components/shared/FeedContent";
import { imageApi } from "@/api/imageApi";
import exifr from "exifr";
import imageCompression from "browser-image-compression";
import { CheckCircle, AlertCircle, Loader2 } from "lucide-react";

interface CreateFeedDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (
    images: string[],
    content: string,
    workoutType: string,
    startImage: string | null,
    endImage: string | null,
    // [수정] 시간 정보 파라미터 추가
    startAt?: string | null,
    endAt?: string | null
  ) => Promise<void> | void;
}

const DRAFT_STORAGE_KEY = "createFeedDraft";

export default function CreateFeedDialog({
  open,
  onOpenChange,
  onCreate,
}: CreateFeedDialogProps) {
  const [startImage, setStartImage] = useState<string | null>(null);
  const [endImage, setEndImage] = useState<string | null>(null);
  const [otherImages, setOtherImages] = useState<string[]>([]);
  const [workoutType, setWorkoutType] = useState<string>("헬스");
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const [isUploading, setIsUploading] = useState(false); // [주의] 전체 제출(작성버튼) 로딩
  const [activeTab, setActiveTab] = useState<"photo" | "content">("photo");

  // [추가] 각 이미지 박스별 로딩 상태
  const [imageLoading, setImageLoading] = useState({
    start: false,
    end: false,
    other: false,
  });
  const [isDesktop, setIsDesktop] = useState(false);
  const firstButtonRef = useRef<HTMLButtonElement>(null);
  const prevOpenRef = useRef(open);

  // [수정 1] 저장 중인지 확인하는 Ref 추가
  const isSavingRef = useRef(false);

  // 데스크톱 여부 감지
  useEffect(() => {
    const checkDesktop = () => setIsDesktop(window.innerWidth >= 768);
    checkDesktop();
    window.addEventListener("resize", checkDesktop);
    return () => window.removeEventListener("resize", checkDesktop);
  }, []);

  // EXIF 시간 및 검증 상태
  const [startExifTime, setStartExifTime] = useState<Date | null>(null);
  const [endExifTime, setEndExifTime] = useState<Date | null>(null);

  // [수정] verificationStatus를 useState가 아닌 useMemo로 즉시 계산 (싱크 문제 해결)
  const verificationStatus = useMemo(() => {
    if (!startExifTime || !endExifTime) return "none";

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const toleranceHours = 6;

    // 허용 범위: 오늘 0시 - 6시간 ~ 오늘 23:59:59 + 6시간
    const allowedStart = new Date(
      today.getTime() - toleranceHours * 60 * 60 * 1000
    );
    const allowedEnd = new Date(
      today.getTime() +
        24 * 60 * 60 * 1000 -
        1 +
        toleranceHours * 60 * 60 * 1000
    );

    // 조건 검증
    const isStartBeforeEnd = startExifTime < endExifTime;
    const durationHours =
      (endExifTime.getTime() - startExifTime.getTime()) / (1000 * 60 * 60);
    const isDurationValid = durationHours <= 24;
    const isStartInRange =
      startExifTime >= allowedStart && startExifTime <= allowedEnd;
    const isEndInRange =
      endExifTime >= allowedStart && endExifTime <= allowedEnd;

    if (isStartBeforeEnd && isDurationValid && isStartInRange && isEndInRange) {
      return "verified";
    } else {
      return "invalid";
    }
  }, [startExifTime, endExifTime]);

  // [추가] 이미지가 삭제되면(null) EXIF 시간 정보도 초기화하여 문구를 되돌림
  useEffect(() => {
    if (!startImage) setStartExifTime(null);
  }, [startImage]);

  useEffect(() => {
    if (!endImage) setEndExifTime(null);
  }, [endImage]);

  // 피드 내용 (plain text)
  const [content, setContent] = useState("");

  // 콘텐츠가 있는지 확인하는 함수
  const checkHasEditorContent = useCallback(() => {
    return content.trim().length > 0;
  }, [content]);

  // 실제 저장할 가치가 있는 변경사항이 있는지 확인 (이미지 또는 글)
  const checkHasMeaningfulChanges = useCallback(() => {
    return (
      startImage !== null ||
      endImage !== null ||
      otherImages.length > 0 ||
      checkHasEditorContent()
    );
  }, [startImage, endImage, otherImages, checkHasEditorContent]);

  // 외부에서 open이 false로 변경되면 확인 다이얼로그 표시
  useEffect(() => {
    // open이 true에서 false로 바뀔 때
    if (prevOpenRef.current && !open) {
      // [수정 2] 저장 중(isSavingRef.current)이 아닐 때만 확인 창 표시
      if (!isSavingRef.current && checkHasMeaningfulChanges()) {
        // 실제 저장할 내용이 있으면 확인 다이얼로그 표시
        setShowCloseConfirm(true);
      }
    }
    prevOpenRef.current = open;

    // [수정 3] 다이얼로그가 열릴 때 저장 플래그 초기화
    if (open) {
      isSavingRef.current = false;
    }
  }, [open, checkHasMeaningfulChanges]);

  // 다이얼로그 열릴 때 localStorage에서 불러오기
  useEffect(() => {
    if (open) {
      const savedDraft = localStorage.getItem(DRAFT_STORAGE_KEY);
      if (savedDraft) {
        try {
          const draft = JSON.parse(savedDraft);
          setStartImage(draft.startImage || null);
          setEndImage(draft.endImage || null);
          setOtherImages(draft.otherImages || []);
          setWorkoutType(draft.workoutType || "헬스");
          setContent(draft.content || "");
        } catch {
          // 임시 저장 로드 실패 무시
        }
      }
    }
  }, [open]);

  // 상태 변경 시 localStorage에 자동 저장
  useEffect(() => {
    if (open) {
      const draft = {
        startImage,
        endImage,
        otherImages,
        workoutType,
        content,
      };
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
    }
  }, [open, startImage, endImage, otherImages, workoutType, content]);

  // 작성 버튼 클릭
  const handleSubmit = async () => {
    // [수정] 업로드 중이면 중복 실행 방지
    if (isUploading) return;

    if (!startImage || !endImage) {
      toast.error("시작 사진과 끝 사진을 모두 업로드해주세요!");
      return;
    }

    const images = [startImage, endImage, ...otherImages].filter(
      Boolean
    ) as string[];

    try {
      setIsUploading(true); // 로딩 시작 (버튼 비활성화)

      // [헬퍼] 로컬 시간 그대로 ISO 문자열로 변환 (타임존 왜곡 방지)
      const getLocalISOString = (date: Date) => {
        const offset = date.getTimezoneOffset() * 60000;
        return new Date(date.getTime() - offset).toISOString().slice(0, 19);
      };

      const startAtIso = startExifTime
        ? getLocalISOString(startExifTime)
        : null;
      const endAtIso = endExifTime ? getLocalISOString(endExifTime) : null;

      // [수정] 시간 정보 전달
      await onCreate(
        images,
        content,
        workoutType,
        startImage,
        endImage,
        startAtIso,
        endAtIso
      );

      localStorage.removeItem(DRAFT_STORAGE_KEY);
      setStartImage(null);
      setEndImage(null);
      setOtherImages([]);
      setWorkoutType("헬스");
      setContent("");
      setStartExifTime(null);
      setEndExifTime(null);
      setVerificationStatus("none");

      onOpenChange(false);
    } catch {
      toast.error("피드 작성 중 오류가 발생했습니다.");
    } finally {
      setIsUploading(false); // 로딩 끝
    }
  };

  // [수정] 이미지 업로드 핸들러 (압축 + 개별 로딩)
  const uploadImage = async (
    file: File,
    setter: (url: string) => void,
    key: "start" | "end" | "other" // 어느 박스인지 구분
  ) => {
    // 해당 박스 로딩 켜기
    setImageLoading((prev) => ({ ...prev, [key]: true }));

    try {
      // 1. 브라우저 이미지 압축 (500 에러 해결 핵심)
      const options = {
        maxSizeMB: 1, // 1MB 이하로 압축
        maxWidthOrHeight: 1920, // FHD 해상도
        useWebWorker: true, // UI 멈춤 방지
        fileType: "image/webp",
      };

      const compressedFile = await imageCompression(file, options);
      // console.log(`압축: ${file.size} -> ${compressedFile.size}`);

      // 2. 압축된 파일 업로드
      const s3Url = await imageApi.uploadFeedImage(compressedFile);
      setter(s3Url);
    } catch {
      toast.error("이미지 업로드 실패 (용량을 확인해주세요)");
    } finally {
      // 해당 박스 로딩 끄기
      setImageLoading((prev) => ({ ...prev, [key]: false }));
    }
  };

  // EXIF 시간 추출 함수
  const extractExifTime = async (file: File): Promise<Date | null> => {
    try {
      const exif = await exifr.parse(file, {
        pick: ["DateTimeOriginal", "CreateDate", "ModifyDate"],
      });
      if (exif) {
        const dateTime =
          exif.DateTimeOriginal || exif.CreateDate || exif.ModifyDate;
        if (dateTime) {
          return new Date(dateTime);
        }
      }
    } catch {
      // EXIF 추출 실패 무시
    }
    return null;
  };

  // [수정] 시작 이미지 (키: start)
  const handleStartImageUpload = async (file: File) => {
    const exifTime = await extractExifTime(file);
    setStartExifTime(exifTime);
    await uploadImage(file, setStartImage, "start");
  };

  // [수정] 끝 이미지 (키: end)
  const handleEndImageUpload = async (file: File) => {
    const exifTime = await extractExifTime(file);
    setEndExifTime(exifTime);
    await uploadImage(file, setEndImage, "end");
  };

  // [수정] 기타 이미지 (키: other)
  const handleOtherImageUpload = (file: File) =>
    uploadImage(
      file,
      (url) => setOtherImages((prev) => [...prev, url]),
      "other"
    );

  // 제출 가능: 시작/끝 사진만 있으면 됨 (EXIF 검증은 백엔드에서)
  const canSubmit = startImage && endImage;

  // 콘텐츠가 있는지 확인
  const hasEditorContent = content.trim().length > 0;

  // 실제 저장할 가치가 있는 변경사항이 있는지 확인 (이미지 또는 글)
  const hasMeaningfulChanges =
    startImage !== null ||
    endImage !== null ||
    otherImages.length > 0 ||
    hasEditorContent;

  // 다이얼로그 닫기 핸들러
  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen && hasMeaningfulChanges) {
      setShowCloseConfirm(true);
      return;
    }
    onOpenChange(newOpen);
  };

  // 확인 없이 닫기
  const handleCloseWithoutSaving = () => {
    setShowCloseConfirm(false);

    // 상태 초기화
    setStartImage(null);
    setEndImage(null);
    setOtherImages([]);
    setWorkoutType("헬스");
    setContent("");
    setStartExifTime(null);
    setEndExifTime(null);
    setVerificationStatus("none");

    // 다이얼로그 닫은 후 localStorage 삭제 (useEffect 방지)
    onOpenChange(false);
    setTimeout(() => {
      localStorage.removeItem(DRAFT_STORAGE_KEY);
    }, 0);
  };

  // open이 false이고 확인 다이얼로그도 안 보이면 렌더링 안 함
  if (!open && !showCloseConfirm) return null;

  return (
    <>
      {/* 모바일용 전체 화면 (하단 네비 제외) - open일 때만 표시 */}
      {open && (
        <div className="md:hidden fixed inset-x-0 top-0 bottom-[60px] z-50 bg-white flex flex-col">
          {/* 헤더 */}
          <div className="p-4 border-b border-gray-200 flex-shrink-0">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-lg font-black text-gray-900">피드 작성</h2>
              <div className="flex items-center gap-2">
                <WorkoutTypeSelect
                  value={workoutType}
                  onChange={setWorkoutType}
                  className="min-w-[120px] cursor-pointer"
                />
                <button
                  onClick={() => handleOpenChange(false)}
                  className="p-2 hover:bg-gray-100 rounded-full cursor-pointer"
                >
                  <X className="w-5 h-5 text-gray-600" />
                </button>
              </div>
            </div>

            {/* 탭 버튼 */}
            <div className="flex gap-1.5">
              <button
                onClick={() => setActiveTab("photo")}
                className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-md text-sm font-medium transition-all ${
                  activeTab === "photo"
                    ? "bg-[#C93831] text-white cursor-default" // 선택됨: 기본 커서
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200 cursor-pointer" // 선택안됨: 손가락 커서
                }`}
              >
                <Image className="w-3.5 h-3.5" />
                사진
                {(startImage || endImage) && (
                  <span className="w-1.5 h-1.5 bg-green-400 rounded-full"></span>
                )}
              </button>
              <button
                onClick={() => setActiveTab("content")}
                className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-md text-sm font-medium transition-all ${
                  activeTab === "content"
                    ? "bg-[#C93831] text-white cursor-default"
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200 cursor-pointer"
                }`}
              >
                <FileText className="w-3.5 h-3.5" />글 작성
              </button>
            </div>
          </div>

          {/* 탭 콘텐츠 */}
          <div className="flex-1 overflow-hidden">
            {activeTab === "photo" && (
              <ScrollArea className="h-full">
                <div className="p-4">
                  <TooltipProvider>
                    <div className="grid grid-cols-2 gap-3 mb-4">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div>
                            <ImageUploadBox
                              label="시작 사진"
                              image={startImage}
                              onImageChange={setStartImage}
                              onFileSelect={handleStartImageUpload}
                              isLoading={imageLoading.start}
                            />
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="bottom">
                          <p>운동 시작 시 찍은 사진을 업로드하세요.</p>
                        </TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div>
                            <ImageUploadBox
                              label="끝 사진"
                              image={endImage}
                              onImageChange={setEndImage}
                              onFileSelect={handleEndImageUpload}
                              isLoading={imageLoading.end}
                            />
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="bottom">
                          <p>운동 종료 시 찍은 사진을 업로드하세요.</p>
                        </TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div>
                            <ImageUploadBox
                              label="기타 사진"
                              image={otherImages[0] || null}
                              onImageChange={() =>
                                setOtherImages(otherImages.slice(1))
                              }
                              onFileSelect={handleOtherImageUpload}
                              variant="display"
                              showCount={otherImages.length}
                            />
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="bottom">
                          <p>추가 사진 (선택사항)</p>
                        </TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div>
                            <ImageUploadBox
                              label="업로드"
                              image={null}
                              onImageChange={() => {}}
                              onFileSelect={handleOtherImageUpload}
                              variant="upload"
                              isLoading={imageLoading.other}
                            />
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="bottom">
                          <p>클릭해서 추가 사진 업로드</p>
                        </TooltipContent>
                      </Tooltip>
                    </div>
                  </TooltipProvider>

                  {verificationStatus === "verified" && (
                    <div className="flex items-center justify-center gap-2 p-3 bg-green-50 border border-green-200 rounded-lg mb-4">
                      <CheckCircle className="w-5 h-5 text-green-600" />
                      <span className="text-sm font-semibold text-green-700">
                        운동 인증 완료!
                      </span>
                      <span className="text-xs text-green-600">
                        (
                        {Math.round(
                          (endExifTime!.getTime() - startExifTime!.getTime()) /
                            (1000 * 60)
                        )}
                        분 운동)
                      </span>
                    </div>
                  )}
                  {verificationStatus === "invalid" && (
                    <div className="flex items-center justify-center gap-2 p-3 bg-orange-50 border border-orange-200 rounded-lg mb-4">
                      <AlertCircle className="w-5 h-5 text-orange-600" />
                      <div className="text-center">
                        <span className="text-sm font-semibold text-orange-700">
                          시간 조건 미충족
                        </span>
                        <p className="text-xs text-orange-600">
                          피드는 작성되지만 포인트가 0점입니다
                        </p>
                      </div>
                    </div>
                  )}
                  {verificationStatus === "none" && startImage && endImage && (
                    <div className="flex items-center justify-center gap-2 p-3 bg-gray-50 border border-gray-200 rounded-lg mb-4">
                      <AlertCircle className="w-5 h-5 text-gray-500" />
                      <span className="text-sm text-gray-600">
                        EXIF 시간 정보를 읽을 수 없습니다
                      </span>
                    </div>
                  )}

                  <p className="text-xs text-gray-500 text-center">
                    사진의 EXIF 정보로 운동 시간과 점수가 자동 계산됩니다
                  </p>
                </div>
              </ScrollArea>
            )}

            {activeTab === "content" && (
              <FeedContentInput
                value={content}
                onChange={setContent}
                placeholder="무슨 운동을 하셨나요? 오늘의 운동 기록을 남겨보세요 💪"
                className="h-full"
              />
            )}
          </div>

          {/* 하단 버튼 */}
          <div className="p-4 border-t border-gray-200 flex-shrink-0">
            <Button
              onClick={handleSubmit}
              disabled={
                !canSubmit ||
                isUploading ||
                imageLoading.start ||
                imageLoading.end ||
                imageLoading.other
              }
              // [수정] 스피너와 텍스트 정렬을 위해 flex 관련 클래스 추가
              className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>업로드 중...</span>
                </>
              ) : canSubmit ? (
                "작성"
              ) : (
                "시작/끝 사진 필요"
              )}
            </Button>
          </div>
        </div>
      )}

      {/* 데스크톱용 다이얼로그 - 모바일에서는 렌더링하지 않음 */}
      {open && isDesktop && (
        <Dialog open={open} onOpenChange={handleOpenChange}>
          <DialogContent
            className="hidden md:flex w-[500px] max-w-[500px] h-[80vh] max-h-[80vh] p-0 overflow-hidden backdrop-blur-3xl bg-white/60 border border-gray-200 shadow-2xl flex-col fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-2xl"
            onOpenAutoFocus={(e) => {
              e.preventDefault();
              firstButtonRef.current?.focus();
            }}
          >
            <DialogHeader className="sr-only">
              <DialogTitle>피드 작성</DialogTitle>
              <DialogDescription>
                새로운 피드를 작성할 수 있습니다.
              </DialogDescription>
            </DialogHeader>

            {/* 헤더 + 탭 */}
            <div className="p-3 border-b border-gray-200 flex-shrink-0">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-lg font-black text-gray-900">피드 작성</h2>
                <WorkoutTypeSelect
                  value={workoutType}
                  onChange={setWorkoutType}
                  className="min-w-[130px] cursor-pointer"
                />
              </div>

              {/* 탭 버튼 */}
              <div className="flex gap-1.5">
                <button
                  ref={firstButtonRef}
                  onClick={() => setActiveTab("photo")}
                  className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-md text-sm font-medium transition-all ${
                    activeTab === "photo"
                      ? "bg-[#C93831] text-white cursor-default" // 선택됨: 기본 커서
                      : "bg-gray-100 text-gray-600 hover:bg-gray-200 cursor-pointer" // 선택안됨: 손가락 커서
                  }`}
                >
                  <Image className="w-3.5 h-3.5" />
                  사진
                  {(startImage || endImage) && (
                    <span className="w-1.5 h-1.5 bg-green-400 rounded-full"></span>
                  )}
                </button>
                <button
                  onClick={() => setActiveTab("content")}
                  className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-md text-sm font-medium transition-all ${
                    activeTab === "content"
                      ? "bg-[#C93831] text-white cursor-default" // 활성 시 기본 커서
                      : "bg-gray-100 text-gray-600 hover:bg-gray-200 cursor-pointer" // 비활성 시 포인터 커서
                  }`}
                >
                  <FileText className="w-3.5 h-3.5" />글 작성
                </button>
              </div>
            </div>

            {/* 탭 콘텐츠 */}
            <div className="flex-1 overflow-hidden">
              {/* 사진 탭 */}
              {activeTab === "photo" && (
                <ScrollArea className="h-full">
                  <div className="p-4">
                    <TooltipProvider>
                      <div className="grid grid-cols-2 gap-3 mb-4">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <div>
                              <ImageUploadBox
                                label="시작 사진"
                                image={startImage}
                                onImageChange={setStartImage}
                                onFileSelect={handleStartImageUpload}
                                isLoading={imageLoading.start}
                              />
                            </div>
                          </TooltipTrigger>
                          <TooltipContent side="bottom">
                            <p>
                              운동 시작 시 찍은 사진을 업로드하세요.
                              <br />
                              사진의 촬영 시간이 자동으로 인식됩니다.
                            </p>
                          </TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <div>
                              <ImageUploadBox
                                label="끝 사진"
                                image={endImage}
                                onImageChange={setEndImage}
                                onFileSelect={handleEndImageUpload}
                                isLoading={imageLoading.end}
                              />
                            </div>
                          </TooltipTrigger>
                          <TooltipContent side="bottom">
                            <p>
                              운동 종료 시 찍은 사진을 업로드하세요.
                              <br />
                              시작 사진보다 나중에 찍어야 인증됩니다.
                            </p>
                          </TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <div>
                              <ImageUploadBox
                                label="기타 사진"
                                image={otherImages[0] || null}
                                onImageChange={() =>
                                  setOtherImages(otherImages.slice(1))
                                }
                                onFileSelect={handleOtherImageUpload}
                                variant="display"
                                showCount={otherImages.length}
                              />
                            </div>
                          </TooltipTrigger>
                          <TooltipContent side="bottom">
                            <p>
                              추가로 올리고 싶은 사진이 있다면
                              <br />
                              여기에 업로드하세요. (선택사항)
                            </p>
                          </TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <div>
                              <ImageUploadBox
                                label="업로드"
                                image={null}
                                onImageChange={() => {}}
                                onFileSelect={handleOtherImageUpload}
                                variant="upload"
                                isLoading={imageLoading.other}
                              />
                            </div>
                          </TooltipTrigger>
                          <TooltipContent side="bottom">
                            <p>클릭해서 추가 사진을 업로드하세요.</p>
                          </TooltipContent>
                        </Tooltip>
                      </div>
                    </TooltipProvider>

                    {/* 인증 상태 뱃지 */}
                    {verificationStatus === "verified" && (
                      <div className="flex items-center justify-center gap-2 p-3 bg-green-50 border border-green-200 rounded-lg mb-4">
                        <CheckCircle className="w-5 h-5 text-green-600" />
                        <span className="text-sm font-semibold text-green-700">
                          운동 인증 완료!
                        </span>
                        <span className="text-xs text-green-600">
                          (
                          {Math.round(
                            (endExifTime!.getTime() -
                              startExifTime!.getTime()) /
                              (1000 * 60)
                          )}
                          분 운동)
                        </span>
                      </div>
                    )}
                    {verificationStatus === "invalid" && (
                      <div className="flex items-center justify-center gap-2 p-3 bg-orange-50 border border-orange-200 rounded-lg mb-4">
                        <AlertCircle className="w-5 h-5 text-orange-600" />
                        <div className="text-center">
                          <span className="text-sm font-semibold text-orange-700">
                            시간 조건 미충족
                          </span>
                          <p className="text-xs text-orange-600">
                            피드는 작성되지만 포인트가 0점입니다
                          </p>
                        </div>
                      </div>
                    )}
                    {verificationStatus === "none" &&
                      startImage &&
                      endImage && (
                        <div className="flex items-center justify-center gap-2 p-3 bg-gray-50 border border-gray-200 rounded-lg mb-4">
                          <AlertCircle className="w-5 h-5 text-gray-500" />
                          <span className="text-sm text-gray-600">
                            EXIF 시간 정보를 읽을 수 없습니다
                          </span>
                        </div>
                      )}

                    <p className="text-xs text-gray-500 text-center">
                      사진의 EXIF 정보로 운동 시간과 점수가 자동 계산됩니다
                    </p>
                  </div>
                </ScrollArea>
              )}

              {/* 글 작성 탭 */}
              {activeTab === "content" && (
                <FeedContentInput
                  value={content}
                  onChange={setContent}
                  placeholder="무슨 운동을 하셨나요? 오늘의 운동 기록을 남겨보세요 💪"
                  className="h-full"
                />
              )}
            </div>

            {/* 하단 버튼 */}
            <div className="p-4 border-t border-gray-200 flex-shrink-0">
              <Button
                onClick={handleSubmit}
                disabled={
                  !canSubmit ||
                  isUploading ||
                  imageLoading.start ||
                  imageLoading.end ||
                  imageLoading.other
                }
                className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              >
                {isUploading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin mr-2" />
                    <span>사진 올리는 중...</span>
                  </>
                ) : canSubmit ? (
                  "작성"
                ) : (
                  "시작/끝 사진 필요"
                )}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      )}

      {/* 닫기 확인 다이얼로그 */}
      <AlertDialog open={showCloseConfirm} onOpenChange={setShowCloseConfirm}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>작성 중인 내용이 있습니다</AlertDialogTitle>
            <AlertDialogDescription>
              임시 저장하면 다음에 다시 열 때 복원됩니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={handleCloseWithoutSaving}
              className="cursor-pointer"
            >
              비우고 닫기
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                // [수정 4] 저장 중 플래그 설정하여 useEffect가 다시 트리거되지 않도록 함
                isSavingRef.current = true;

                // 명시적으로 localStorage에 저장
                const draft = {
                  startImage,
                  endImage,
                  otherImages,
                  workoutType,
                  content,
                };
                localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
                setShowCloseConfirm(false);
                onOpenChange(false); // 메인 다이얼로그도 닫기
              }}
              className="cursor-pointer"
            >
              저장 후 닫기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
