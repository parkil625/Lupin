/**
 * CreateFeedDialog.tsx
 *
 * 피드 작성 다이얼로그 컴포넌트
 * - 새 피드 작성
 * - BlockNote 에디터 사용
 * - 운동 시작/끝 사진 업로드
 */

import { useState, useEffect, useRef } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle
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
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import { imageApi } from "@/api/imageApi";
import exifr from "exifr";
import { CheckCircle, AlertCircle } from "lucide-react";

interface CreateFeedDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (images: string[], content: string, workoutType: string, startImage: string | null, endImage: string | null) => void;
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
  const [isUploading, setIsUploading] = useState(false);
  const [activeTab, setActiveTab] = useState<"photo" | "content">("photo");
  const [isDesktop, setIsDesktop] = useState(false);
  const firstButtonRef = useRef<HTMLButtonElement>(null);
  const prevOpenRef = useRef(open);

  // 데스크톱 여부 감지
  useEffect(() => {
    const checkDesktop = () => setIsDesktop(window.innerWidth >= 768);
    checkDesktop();
    window.addEventListener('resize', checkDesktop);
    return () => window.removeEventListener('resize', checkDesktop);
  }, []);

  // EXIF 시간 및 검증 상태
  const [startExifTime, setStartExifTime] = useState<Date | null>(null);
  const [endExifTime, setEndExifTime] = useState<Date | null>(null);
  const [verificationStatus, setVerificationStatus] = useState<"none" | "verified" | "invalid">("none");

  const editor = useCreateBlockNote({
    initialContent: [
      {
        type: "paragraph",
        content: "",
      },
    ],
  });

  // 에디터에 실제 콘텐츠가 있는지 확인하는 함수
  const checkHasEditorContent = () => {
    return editor.document.some(block => {
      if (block.type === 'paragraph' && Array.isArray(block.content)) {
        return block.content.some((item: unknown) => {
          if (typeof item === 'string') return item.trim().length > 0;
          if (item && typeof item === 'object' && 'text' in item) {
            return String((item as { text: string }).text || '').trim().length > 0;
          }
          return false;
        });
      }
      return block.type !== 'paragraph';
    });
  };

  // 실제 저장할 가치가 있는 변경사항이 있는지 확인 (이미지 또는 글)
  const checkHasMeaningfulChanges = () => {
    return startImage !== null ||
      endImage !== null ||
      otherImages.length > 0 ||
      checkHasEditorContent();
  };

  // 외부에서 open이 false로 변경되면 확인 다이얼로그 표시
  useEffect(() => {
    // open이 true에서 false로 바뀔 때
    if (prevOpenRef.current && !open) {
      if (checkHasMeaningfulChanges()) {
        // 실제 저장할 내용이 있으면 확인 다이얼로그 표시
        setShowCloseConfirm(true);
      }
    }
    prevOpenRef.current = open;
  }, [open, startImage, endImage, otherImages]);

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

          // 에디터 콘텐츠 복원
          if (draft.content && Array.isArray(draft.content)) {
            try {
              editor.replaceBlocks(editor.document, draft.content);
            } catch {
              console.log("Editor content restore failed");
            }
          }
        } catch {
          console.log("Failed to load draft");
        }
      }
    }
  }, [open, editor]);

  // 상태 변경 시 localStorage에 자동 저장
  useEffect(() => {
    if (open) {
      const draft = {
        startImage,
        endImage,
        otherImages,
        workoutType,
        content: editor.document,
      };
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
    }
  }, [open, startImage, endImage, otherImages, workoutType, editor]);

  // EXIF 시간 검증
  useEffect(() => {
    if (!startExifTime || !endExifTime) {
      setVerificationStatus("none");
      return;
    }

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const toleranceHours = 6;

    // 허용 범위: 오늘 0시 - 6시간 ~ 오늘 23:59:59 + 6시간
    const allowedStart = new Date(today.getTime() - toleranceHours * 60 * 60 * 1000);
    const allowedEnd = new Date(today.getTime() + 24 * 60 * 60 * 1000 - 1 + toleranceHours * 60 * 60 * 1000);

    // 조건 검증
    const isStartBeforeEnd = startExifTime < endExifTime;
    const durationHours = (endExifTime.getTime() - startExifTime.getTime()) / (1000 * 60 * 60);
    const isDurationValid = durationHours <= 24;
    const isStartInRange = startExifTime >= allowedStart && startExifTime <= allowedEnd;
    const isEndInRange = endExifTime >= allowedStart && endExifTime <= allowedEnd;

    if (isStartBeforeEnd && isDurationValid && isStartInRange && isEndInRange) {
      setVerificationStatus("verified");
    } else {
      setVerificationStatus("invalid");
    }
  }, [startExifTime, endExifTime]);

  // 작성 버튼 클릭
  const handleSubmit = () => {
    // 시작 사진과 끝 사진이 모두 있어야 함
    if (!startImage || !endImage) {
      toast.error("시작 사진과 끝 사진을 모두 업로드해주세요!");
      return;
    }

    const images = [startImage, endImage, ...otherImages].filter(Boolean) as string[];
    const blocks = editor.document;
    const contentJson = JSON.stringify(blocks);

    // localStorage 초기화
    localStorage.removeItem(DRAFT_STORAGE_KEY);

    // 피드 생성 (시간 검증 여부와 관계없이 제출 가능)
    onCreate(images, contentJson, workoutType, startImage, endImage);

    // 상태 초기화
    setStartImage(null);
    setEndImage(null);
    setOtherImages([]);
    setWorkoutType("헬스");
    setStartExifTime(null);
    setEndExifTime(null);
    setVerificationStatus("none");
    try {
      editor.replaceBlocks(editor.document, [
        {
          type: "paragraph",
          content: "",
        },
      ]);
    } catch {
      console.log("Editor reset skipped");
    }

    // 다이얼로그 닫기
    onOpenChange(false);
  };

  // 이미지 업로드 핸들러
  const uploadImage = async (file: File, setter: (url: string) => void) => {
    setIsUploading(true);

    try {
      const s3Url = await imageApi.uploadFeedImage(file);
      setter(s3Url);
    } catch (error) {
      console.error(error);
      toast.error("이미지 업로드 실패");
    } finally {
      setIsUploading(false);
    }
  };

  // EXIF 시간 추출 함수
  const extractExifTime = async (file: File): Promise<Date | null> => {
    try {
      const exif = await exifr.parse(file, { pick: ["DateTimeOriginal", "CreateDate", "ModifyDate"] });
      if (exif) {
        const dateTime = exif.DateTimeOriginal || exif.CreateDate || exif.ModifyDate;
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
    await uploadImage(file, setStartImage);
  };

  // 끝 이미지 업로드 + EXIF 추출
  const handleEndImageUpload = async (file: File) => {
    const exifTime = await extractExifTime(file);
    setEndExifTime(exifTime);
    await uploadImage(file, setEndImage);
  };

  const handleOtherImageUpload = (file: File) => uploadImage(file, (url) => setOtherImages(prev => [...prev, url]));

  // 제출 가능: 시작/끝 사진만 있으면 됨 (EXIF 검증은 백엔드에서)
  const canSubmit = startImage && endImage;

  // 에디터에 실제 콘텐츠가 있는지 확인
  const hasEditorContent = editor.document.some(block => {
    if (block.type === 'paragraph' && Array.isArray(block.content)) {
      return block.content.some((item: unknown) => {
        if (typeof item === 'string') return item.trim().length > 0;
        if (item && typeof item === 'object' && 'text' in item) {
          return String((item as { text: string }).text || '').trim().length > 0;
        }
        return false;
      });
    }
    // 다른 블록 타입(이미지, 헤더 등)이 있으면 콘텐츠가 있는 것으로 간주
    return block.type !== 'paragraph';
  });

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
    setStartExifTime(null);
    setEndExifTime(null);
    setVerificationStatus("none");
    try {
      editor.replaceBlocks(editor.document, [
        { type: "paragraph", content: "" },
      ]);
    } catch {
      // 에디터 초기화 실패 무시
    }

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
      {open && <div className="md:hidden fixed inset-x-0 top-0 bottom-[60px] z-50 bg-white flex flex-col">
        {/* 헤더 */}
        <div className="p-4 border-b border-gray-200 flex-shrink-0">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-lg font-black text-gray-900">피드 작성</h2>
            <div className="flex items-center gap-2">
              <WorkoutTypeSelect
                value={workoutType}
                onChange={setWorkoutType}
                className="min-w-[120px]"
              />
              <button
                onClick={() => handleOpenChange(false)}
                className="p-2 hover:bg-gray-100 rounded-full"
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
                  ? "bg-[#C93831] text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
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
                  ? "bg-[#C93831] text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              <FileText className="w-3.5 h-3.5" />
              글 작성
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
                            onImageChange={() => setOtherImages(otherImages.slice(1))}
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

                {verificationStatus === "verified" && (
                  <div className="flex items-center justify-center gap-2 p-3 bg-green-50 border border-green-200 rounded-lg mb-4">
                    <CheckCircle className="w-5 h-5 text-green-600" />
                    <span className="text-sm font-semibold text-green-700">운동 인증 완료!</span>
                    <span className="text-xs text-green-600">
                      ({Math.round((endExifTime!.getTime() - startExifTime!.getTime()) / (1000 * 60))}분 운동)
                    </span>
                  </div>
                )}
                {verificationStatus === "invalid" && (
                  <div className="flex items-center justify-center gap-2 p-3 bg-orange-50 border border-orange-200 rounded-lg mb-4">
                    <AlertCircle className="w-5 h-5 text-orange-600" />
                    <div className="text-center">
                      <span className="text-sm font-semibold text-orange-700">시간 조건 미충족</span>
                      <p className="text-xs text-orange-600">피드는 작성되지만 포인트가 0점입니다</p>
                    </div>
                  </div>
                )}
                {verificationStatus === "none" && startImage && endImage && (
                  <div className="flex items-center justify-center gap-2 p-3 bg-gray-50 border border-gray-200 rounded-lg mb-4">
                    <AlertCircle className="w-5 h-5 text-gray-500" />
                    <span className="text-sm text-gray-600">EXIF 시간 정보를 읽을 수 없습니다</span>
                  </div>
                )}

                <p className="text-xs text-gray-500 text-center">
                  사진의 EXIF 정보로 운동 시간과 점수가 자동 계산됩니다
                </p>
              </div>
            </ScrollArea>
          )}

          {activeTab === "content" && (
            <ScrollArea className="h-full">
              <style>{`
                .bn-editor { max-width: 100% !important; width: 100% !important; background: transparent !important; min-height: 300px !important; }
                .bn-container { max-width: 100% !important; width: 100% !important; background: transparent !important; }
                .bn-block-content { max-width: 100% !important; background: transparent !important; }
                .bn-inline-content { word-wrap: break-word !important; overflow-wrap: break-word !important; }
                .bn-block { background: transparent !important; }
                .ProseMirror { background: transparent !important; min-height: 300px !important; }
              `}</style>
              <div className="p-4">
                <BlockNoteView editor={editor} theme="light" />
              </div>
            </ScrollArea>
          )}
        </div>

        {/* 하단 버튼 */}
        <div className="p-4 border-t border-gray-200 flex-shrink-0">
          <Button
            onClick={handleSubmit}
            disabled={!canSubmit || isUploading}
            className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isUploading ? "사진 올리는 중..." : canSubmit ? "작성" : "시작/끝 사진 필요"}
          </Button>
        </div>
      </div>}

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
              className="min-w-[130px]"
            />
          </div>

          {/* 탭 버튼 */}
          <div className="flex gap-1.5">
            <button
              ref={firstButtonRef}
              onClick={() => setActiveTab("photo")}
              className={`flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-md text-sm font-medium transition-all ${
                activeTab === "photo"
                  ? "bg-[#C93831] text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
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
                  ? "bg-[#C93831] text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              <FileText className="w-3.5 h-3.5" />
              글 작성
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
                          />
                        </div>
                      </TooltipTrigger>
                      <TooltipContent side="bottom">
                        <p>운동 시작 시 찍은 사진을 업로드하세요.<br/>사진의 촬영 시간이 자동으로 인식됩니다.</p>
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
                        <p>운동 종료 시 찍은 사진을 업로드하세요.<br/>시작 사진보다 나중에 찍어야 인증됩니다.</p>
                      </TooltipContent>
                    </Tooltip>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div>
                          <ImageUploadBox
                            label="기타 사진"
                            image={otherImages[0] || null}
                            onImageChange={() => setOtherImages(otherImages.slice(1))}
                            onFileSelect={handleOtherImageUpload}
                            variant="display"
                            showCount={otherImages.length}
                          />
                        </div>
                      </TooltipTrigger>
                      <TooltipContent side="bottom">
                        <p>추가로 올리고 싶은 사진이 있다면<br/>여기에 업로드하세요. (선택사항)</p>
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

                {/* 인증 상태 뱃지 */}
                {verificationStatus === "verified" && (
                  <div className="flex items-center justify-center gap-2 p-3 bg-green-50 border border-green-200 rounded-lg mb-4">
                    <CheckCircle className="w-5 h-5 text-green-600" />
                    <span className="text-sm font-semibold text-green-700">운동 인증 완료!</span>
                    <span className="text-xs text-green-600">
                      ({Math.round((endExifTime!.getTime() - startExifTime!.getTime()) / (1000 * 60))}분 운동)
                    </span>
                  </div>
                )}
                {verificationStatus === "invalid" && (
                  <div className="flex items-center justify-center gap-2 p-3 bg-orange-50 border border-orange-200 rounded-lg mb-4">
                    <AlertCircle className="w-5 h-5 text-orange-600" />
                    <div className="text-center">
                      <span className="text-sm font-semibold text-orange-700">시간 조건 미충족</span>
                      <p className="text-xs text-orange-600">피드는 작성되지만 포인트가 0점입니다</p>
                    </div>
                  </div>
                )}
                {verificationStatus === "none" && startImage && endImage && (
                  <div className="flex items-center justify-center gap-2 p-3 bg-gray-50 border border-gray-200 rounded-lg mb-4">
                    <AlertCircle className="w-5 h-5 text-gray-500" />
                    <span className="text-sm text-gray-600">EXIF 시간 정보를 읽을 수 없습니다</span>
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
            <ScrollArea className="h-full">
              <style>{`
                .bn-editor {
                  max-width: 100% !important;
                  width: 100% !important;
                  background: transparent !important;
                  min-height: 300px !important;
                }
                .bn-container {
                  max-width: 100% !important;
                  width: 100% !important;
                  background: transparent !important;
                }
                .bn-block-content {
                  max-width: 100% !important;
                  background: transparent !important;
                }
                .bn-inline-content {
                  word-wrap: break-word !important;
                  overflow-wrap: break-word !important;
                }
                .bn-block {
                  background: transparent !important;
                }
                .ProseMirror {
                  background: transparent !important;
                  min-height: 300px !important;
                }
              `}</style>
              <div className="p-4">
                <BlockNoteView editor={editor} theme="light" />
              </div>
            </ScrollArea>
          )}
        </div>

        {/* 하단 버튼 */}
        <div className="p-4 border-t border-gray-200 flex-shrink-0">
          <Button
            onClick={handleSubmit}
            disabled={!canSubmit || isUploading}
            className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isUploading ? "사진 올리는 중..." : canSubmit ? "작성" : "시작/끝 사진 필요"}
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
            <AlertDialogCancel onClick={handleCloseWithoutSaving}>
              비우고 닫기
            </AlertDialogCancel>
            <AlertDialogAction onClick={() => {
              // 명시적으로 localStorage에 저장
              const draft = {
                startImage,
                endImage,
                otherImages,
                workoutType,
                content: editor.document,
              };
              localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
              setShowCloseConfirm(false);
            }}>
              저장 후 닫기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
