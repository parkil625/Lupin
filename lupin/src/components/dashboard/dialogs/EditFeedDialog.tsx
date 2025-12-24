/**
 * EditFeedDialog.tsx
 *
 * 피드 수정 다이얼로그 컴포넌트
 * - 기존 피드 내용 수정
 * - 인스타그램 스타일 textarea 사용
 * - 운동 시작/끝 사진 업로드
 */

import { useState, useEffect, useRef } from "react";
import {
  Dialog,
  DialogContent,
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
import { Image, FileText, CheckCircle, AlertCircle, X } from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { toast } from "sonner";
import { ImageUploadBox, WorkoutTypeSelect } from "@/components/molecules";
import {
  FeedContentInput,
  convertBlockNoteToPlainText,
} from "@/components/shared/FeedContent";
import exifr from "exifr";
import { imageApi } from "@/api/imageApi";
import imageCompression from "browser-image-compression";

interface EditFeedDialogProps {
  feed: Feed | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (
    feedId: number,
    images: string[],
    content: string,
    workoutType: string,
    startImage: string | null,
    endImage: string | null
  ) => void;
}

export default function EditFeedDialog({
  feed,
  open,
  onOpenChange,
  onSave,
}: EditFeedDialogProps) {
  const [startImage, setStartImage] = useState<string | null>(null);
  const [endImage, setEndImage] = useState<string | null>(null);
  const [otherImages, setOtherImages] = useState<string[]>([]);
  const [workoutType, setWorkoutType] = useState<string>("");
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const [activeTab, setActiveTab] = useState<"photo" | "content">("photo");
  const [isDesktop, setIsDesktop] = useState(false);
  const prevOpenRef = useRef(open);

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
  const [verificationStatus, setVerificationStatus] = useState<
    "none" | "verified" | "invalid"
  >("none");
  const [imagesChanged, setImagesChanged] = useState(false);
  const firstButtonRef = useRef<HTMLButtonElement>(null);

  const initialDataRef = useRef<{
    startImage: string | null;
    endImage: string | null;
    otherImages: string[];
    workoutType: string;
    content: string;
  } | null>(null);

  // 피드 내용 (plain text)
  const [content, setContent] = useState("");

  // 외부에서 open이 false로 변경되면 확인 다이얼로그 표시
  useEffect(() => {
    // open이 true에서 false로 바뀔 때
    if (prevOpenRef.current && !open) {
      // 실제 변경사항이 있는지 확인
      const hasActualChanges =
        initialDataRef.current &&
        (startImage !== initialDataRef.current.startImage ||
          endImage !== initialDataRef.current.endImage ||
          JSON.stringify(otherImages) !==
            JSON.stringify(initialDataRef.current.otherImages) ||
          workoutType !== initialDataRef.current.workoutType ||
          content !== initialDataRef.current.content);

      if (hasActualChanges) {
        // 변경사항이 있으면 확인 다이얼로그 표시
        // eslint-disable-next-line react-hooks/set-state-in-effect -- 외부 닫기 감지
        setShowCloseConfirm(true);
      } else {
        // 변경사항 없으면 상태 초기화
        initialDataRef.current = null;
      }
    }
    prevOpenRef.current = open;
  }, [open, startImage, endImage, otherImages, workoutType, content]);

  // Feed가 변경되면 기존 데이터로 초기화
  useEffect(() => {
    if (feed && open) {
      const initialStartImage = feed.images[0] || null;
      const initialEndImage = feed.images[1] || null;
      const initialOtherImages = feed.images.slice(2) || [];
      const initialWorkoutType = feed.activity || "running";

      // eslint-disable-next-line react-hooks/set-state-in-effect -- feed 데이터로 폼 초기화 필요
      setStartImage(initialStartImage);
      setEndImage(initialEndImage);
      setOtherImages(initialOtherImages);
      setWorkoutType(initialWorkoutType);

      // 기존 내용을 plain text로 변환
      const plainTextContent = convertBlockNoteToPlainText(feed.content || "");
      setContent(plainTextContent);

      // 초기 데이터 저장 (변경 감지용)
      initialDataRef.current = {
        startImage: initialStartImage,
        endImage: initialEndImage,
        otherImages: initialOtherImages,
        workoutType: initialWorkoutType,
        content: plainTextContent,
      };
    }
  }, [feed, open]);

  // EXIF 시간 검증 (이미지가 변경된 경우에만)
  useEffect(() => {
    if (!imagesChanged) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- 검증 상태 초기화
      setVerificationStatus("none");
      return;
    }

    if (!startExifTime || !endExifTime) {
      setVerificationStatus("none");
      return;
    }

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const toleranceHours = 6;

    const allowedStart = new Date(
      today.getTime() - toleranceHours * 60 * 60 * 1000
    );
    const allowedEnd = new Date(
      today.getTime() +
        24 * 60 * 60 * 1000 -
        1 +
        toleranceHours * 60 * 60 * 1000
    );

    const isStartBeforeEnd = startExifTime < endExifTime;
    const durationHours =
      (endExifTime.getTime() - startExifTime.getTime()) / (1000 * 60 * 60);
    const isDurationValid = durationHours <= 24;
    const isStartInRange =
      startExifTime >= allowedStart && startExifTime <= allowedEnd;
    const isEndInRange =
      endExifTime >= allowedStart && endExifTime <= allowedEnd;

    if (isStartBeforeEnd && isDurationValid && isStartInRange && isEndInRange) {
      setVerificationStatus("verified");
    } else {
      setVerificationStatus("invalid");
    }
  }, [startExifTime, endExifTime, imagesChanged]);

  // 이미지 업로드 핸들러 (S3에 실제 업로드)
  const uploadImage = async (file: File, setter: (url: string) => void) => {
    try {
      // [추가] 브라우저단 이미지 압축 옵션
      const options = {
        maxSizeMB: 1, // 최대 1MB로 제한
        maxWidthOrHeight: 1920, // 최대 해상도 FHD급으로 제한
        useWebWorker: true, // 별도 스레드 사용으로 화면 멈춤 방지
        fileType: "image/webp", // 웹에 최적화된 포맷 사용
      };

      // 압축 수행 (여기서 용량이 확 줄어듭니다)
      const compressedFile = await imageCompression(file, options);

      // 원본 대신 압축된 파일(compressedFile)을 전송
      // 기존 file -> compressedFile 로 변경
      const s3Url = await imageApi.uploadFeedImage(compressedFile);

      setter(s3Url);
    } catch (error) {
      console.error("이미지 업로드 실패:", error);
      toast.error("이미지 업로드 실패");
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
    } catch (error) {
      console.log("EXIF 추출 실패:", error);
    }
    return null;
  };

  // 시작 이미지 업로드 + EXIF 추출
  const handleStartImageUpload = async (file: File) => {
    const exifTime = await extractExifTime(file);
    setStartExifTime(exifTime);
    setImagesChanged(true);
    await uploadImage(file, setStartImage);
  };

  // 끝 이미지 업로드 + EXIF 추출
  const handleEndImageUpload = async (file: File) => {
    const exifTime = await extractExifTime(file);
    setEndExifTime(exifTime);
    setImagesChanged(true);
    await uploadImage(file, setEndImage);
  };

  const handleOtherImageUpload = (file: File) =>
    uploadImage(file, (url) => setOtherImages((prev) => [...prev, url]));

  // 제출 가능: 시작/끝 사진만 있으면 됨
  const canSubmit = startImage && endImage;

  // 저장 처리
  const handleSave = () => {
    if (!feed) return;

    if (!startImage || !endImage) {
      toast.error("시작 사진과 끝 사진을 모두 업로드해주세요!");
      return;
    }

    const images = [startImage, endImage, ...otherImages].filter(
      Boolean
    ) as string[];

    // plain text로 저장
    onSave(feed.id, images, content, workoutType, startImage, endImage);
    // 저장 완료 후 상태 초기화
    initialDataRef.current = null;
    onOpenChange(false);
  };

  // 실제 변경사항이 있는지 확인하는 함수
  const checkHasActualChanges = () => {
    if (!initialDataRef.current) return false;

    return (
      startImage !== initialDataRef.current.startImage ||
      endImage !== initialDataRef.current.endImage ||
      JSON.stringify(otherImages) !==
        JSON.stringify(initialDataRef.current.otherImages) ||
      workoutType !== initialDataRef.current.workoutType ||
      content !== initialDataRef.current.content
    );
  };

  // 다이얼로그 닫기 시 검증
  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      // 실제 변경사항이 있는지 직접 확인
      if (checkHasActualChanges()) {
        setShowCloseConfirm(true);
        return;
      }
      // 변경사항 없으면 바로 닫기
      initialDataRef.current = null;
    }
    onOpenChange(newOpen);
  };

  // 확인 없이 닫기
  const handleCloseWithoutSaving = () => {
    setShowCloseConfirm(false);
    initialDataRef.current = null;
    onOpenChange(false);
  };

  // open이 false이고 확인 다이얼로그도 안 보이면 렌더링 안 함
  if ((!feed || !open) && !showCloseConfirm) return null;

  return (
    <>
      {/* 모바일용 전체 화면 (하단 네비 제외) - open일 때만 표시 */}
      {open && feed && (
        <div className="md:hidden fixed inset-x-0 top-0 bottom-[60px] z-50 bg-white flex flex-col">
          {/* 헤더 */}
          <div className="p-4 border-b border-gray-200 flex-shrink-0">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-lg font-black text-gray-900">피드 수정</h2>
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
                    ? "bg-[#C93831] text-white cursor-default"
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200 cursor-pointer"
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
                            />
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="bottom">
                          <p>클릭해서 추가 사진 업로드</p>
                        </TooltipContent>
                      </Tooltip>
                    </div>
                  </TooltipProvider>

                  {imagesChanged && verificationStatus === "verified" && (
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
                  {imagesChanged && verificationStatus === "invalid" && (
                    <div className="flex items-center justify-center gap-2 p-3 bg-orange-50 border border-orange-200 rounded-lg mb-4">
                      <AlertCircle className="w-5 h-5 text-orange-600" />
                      <div className="text-center">
                        <span className="text-sm font-semibold text-orange-700">
                          시간 조건 미충족
                        </span>
                        <p className="text-xs text-orange-600">
                          피드는 저장되지만 포인트가 0점입니다
                        </p>
                      </div>
                    </div>
                  )}
                  {imagesChanged &&
                    verificationStatus === "none" &&
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
              onClick={handleSave}
              disabled={!canSubmit}
              className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {canSubmit ? "수정 완료" : "시작/끝 사진 필요"}
            </Button>
          </div>
        </div>
      )}

      {/* 데스크톱용 다이얼로그 - 모바일에서는 렌더링하지 않음 */}
      {open && feed && isDesktop && (
        <Dialog open={open} onOpenChange={handleOpenChange}>
          <DialogContent
            className="hidden md:flex w-[500px] max-w-[500px] h-[80vh] max-h-[80vh] p-0 overflow-hidden backdrop-blur-3xl bg-white/60 border border-gray-200 shadow-2xl flex-col fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-2xl"
            onOpenAutoFocus={(e) => {
              e.preventDefault();
              firstButtonRef.current?.focus();
            }}
          >
            <DialogTitle className="sr-only">피드 수정</DialogTitle>
            <DialogDescription className="sr-only">
              기존 피드 내용을 수정합니다. 운동 종류, 시작/끝 사진, 그리고
              내용을 수정할 수 있습니다.
            </DialogDescription>

            {/* 헤더 + 탭 */}
            <div className="p-3 border-b border-gray-200 flex-shrink-0">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-lg font-black text-gray-900">피드 수정</h2>
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
                      ? "bg-[#C93831] text-white cursor-default"
                      : "bg-gray-100 text-gray-600 hover:bg-gray-200 cursor-pointer"
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
                              />
                            </div>
                          </TooltipTrigger>
                          <TooltipContent side="bottom">
                            <p>클릭해서 추가 사진을 업로드하세요.</p>
                          </TooltipContent>
                        </Tooltip>
                      </div>
                    </TooltipProvider>

                    {imagesChanged && verificationStatus === "verified" && (
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
                    {imagesChanged && verificationStatus === "invalid" && (
                      <div className="flex items-center justify-center gap-2 p-3 bg-orange-50 border border-orange-200 rounded-lg mb-4">
                        <AlertCircle className="w-5 h-5 text-orange-600" />
                        <div className="text-center">
                          <span className="text-sm font-semibold text-orange-700">
                            시간 조건 미충족
                          </span>
                          <p className="text-xs text-orange-600">
                            피드는 저장되지만 포인트가 0점입니다
                          </p>
                        </div>
                      </div>
                    )}
                    {imagesChanged &&
                      verificationStatus === "none" &&
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
                onClick={handleSave}
                disabled={!canSubmit}
                className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              >
                {canSubmit ? "수정 완료" : "시작/끝 사진 필요"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      )}

      {/* 닫기 확인 다이얼로그 */}
      <AlertDialog open={showCloseConfirm} onOpenChange={setShowCloseConfirm}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>수정 중인 내용이 있습니다</AlertDialogTitle>
            <AlertDialogDescription>
              저장하지 않고 닫으면 변경사항이 사라집니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setShowCloseConfirm(false)}
              className="cursor-pointer"
            >
              계속 수정하기
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCloseWithoutSaving}
              className="cursor-pointer"
            >
              닫기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
