/**
 * CreateFeedDialog.tsx
 *
 * 피드 작성 다이얼로그 컴포넌트
 * - 새 피드 작성
 * - BlockNote 에디터 사용
 * - 운동 시작/끝 사진 업로드
 */

import { useState, useEffect } from "react";
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
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { CheckCircle, Clock, Flame, Trophy } from "lucide-react";
import { toast } from "sonner";
import { ImageUploadBox, WorkoutTypeSelect } from "@/components/molecules";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import { imageApi } from "@/api/imageApi";
import {
  extractImageMetadata,
  validateWorkoutTimes,
  calculateWorkoutMetrics,
  type WorkoutCalculation
} from "@/lib/imageMetadata";

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
  const [workoutType, setWorkoutType] = useState<string>("running");
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [startTime, setStartTime] = useState<Date | null>(null);
  const [endTime, setEndTime] = useState<Date | null>(null);
  const [workoutMetrics, setWorkoutMetrics] = useState<WorkoutCalculation | null>(null);

  const editor = useCreateBlockNote({
    initialContent: [
      {
        type: "paragraph",
        content: "",
      },
    ],
  });

  // 다이얼로그가 닫히면 상태 초기화
  useEffect(() => {
    if (!open) {
      setShowCloseConfirm(false);
    }
  }, [open]);

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
          setWorkoutType(draft.workoutType || "running");

          // 에디터 콘텐츠 복원
          if (draft.content && Array.isArray(draft.content)) {
            try {
              editor.replaceBlocks(editor.document, draft.content);
            } catch (error) {
              console.log("Editor content restore failed");
            }
          }
        } catch (error) {
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

  // 작성 버튼 클릭
  const handleSubmit = () => {
    // 운동 인증 필수: 시작 사진과 끝 사진이 모두 있어야 함
    if (!startImage || !endImage) {
      toast.error("운동 인증을 위해 시작 사진과 끝 사진을 모두 업로드해주세요!");
      return;
    }

    // 시간 유효성 검증
    const validation = validateWorkoutTimes(startTime, endTime);
    if (!validation.valid) {
      toast.error(validation.error || "시간 정보가 올바르지 않습니다.");
      return;
    }

    const images = [startImage, endImage, ...otherImages].filter(Boolean) as string[];
    const blocks = editor.document;
    const contentJson = JSON.stringify(blocks);

    // localStorage 초기화
    localStorage.removeItem(DRAFT_STORAGE_KEY);

    // 피드 생성
    onCreate(images, contentJson, workoutType, startImage, endImage);

    // 상태 초기화
    setStartImage(null);
    setEndImage(null);
    setOtherImages([]);
    setWorkoutType("running");
    setStartTime(null);
    setEndTime(null);
    setWorkoutMetrics(null);
    try {
      editor.replaceBlocks(editor.document, [
        {
          type: "paragraph",
          content: "",
        },
      ]);
    } catch (error) {
      console.log("Editor reset skipped");
    }

    // 다이얼로그 닫기
    onOpenChange(false);
  };

  // 이미지 업로드 핸들러
  const uploadImage = async (file: File, setter: (url: string) => void) => {
    setIsUploading(true);
    const loadingToast = toast.loading("이미지를 업로드하고 있습니다...");

    try {
      const s3Url = await imageApi.uploadImage(file);
      setter(s3Url);
      toast.success("업로드 완료!");
    } catch (error) {
      console.error(error);
      toast.error("이미지 업로드 실패");
    } finally {
      toast.dismiss(loadingToast);
      setIsUploading(false);
    }
  };

  // 시작 이미지 업로드 (메타데이터 추출 포함)
  const handleStartImageUpload = async (file: File) => {
    const metadata = await extractImageMetadata(file);
    setStartTime(metadata.dateTime);

    if (!metadata.dateTime) {
      toast.warning("사진에서 시간 정보를 찾을 수 없습니다. 다른 사진을 시도해보세요.");
    }

    await uploadImage(file, setStartImage);
  };

  // 끝 이미지 업로드 (메타데이터 추출 및 계산 포함)
  const handleEndImageUpload = async (file: File) => {
    const metadata = await extractImageMetadata(file);
    setEndTime(metadata.dateTime);

    if (!metadata.dateTime) {
      toast.warning("사진에서 시간 정보를 찾을 수 없습니다. 다른 사진을 시도해보세요.");
    }

    await uploadImage(file, setEndImage);
  };

  const handleOtherImageUpload = (file: File) => uploadImage(file, (url) => setOtherImages(prev => [...prev, url]));

  // 시작/끝 시간이 모두 있을 때 운동 지표 계산
  useEffect(() => {
    if (startTime && endTime) {
      const validation = validateWorkoutTimes(startTime, endTime);
      if (validation.valid) {
        const metrics = calculateWorkoutMetrics(startTime, endTime, workoutType);
        setWorkoutMetrics(metrics);
      } else {
        setWorkoutMetrics(null);
      }
    } else {
      setWorkoutMetrics(null);
    }
  }, [startTime, endTime, workoutType]);

  const isVerified = startImage && endImage;

  // 변경사항이 있는지 확인
  const hasChanges =
    startImage !== null ||
    endImage !== null ||
    otherImages.length > 0 ||
    workoutType !== "running" ||
    JSON.stringify(editor.document) !== JSON.stringify([{ type: "paragraph", content: "" }]);

  // 다이얼로그 닫기 핸들러
  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen && hasChanges) {
      setShowCloseConfirm(true);
      return;
    }
    onOpenChange(newOpen);
  };

  // 확인 없이 닫기
  const handleCloseWithoutSaving = () => {
    setShowCloseConfirm(false);
    localStorage.removeItem(DRAFT_STORAGE_KEY);

    // 상태 초기화
    setStartImage(null);
    setEndImage(null);
    setOtherImages([]);
    setWorkoutType("running");
    setStartTime(null);
    setEndTime(null);
    setWorkoutMetrics(null);

    onOpenChange(false);
  };

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="!max-w-[795px] !w-[795px] h-[95vh] p-0 overflow-hidden backdrop-blur-3xl bg-white/60 border border-gray-200 shadow-2xl !flex !gap-0" style={{ width: '795px', maxWidth: '795px' }}>
        <DialogHeader className="sr-only">
          <DialogTitle>피드 작성</DialogTitle>
          <DialogDescription>
            새로운 피드를 작성할 수 있습니다.
          </DialogDescription>
        </DialogHeader>
        <div className="flex h-full overflow-hidden w-full">
          {/* Left Sidebar */}
          <div className="w-80 bg-transparent border-r border-gray-200 p-6 flex-shrink-0">
            <h2 className="text-xl font-black text-gray-900 mb-4">피드 작성</h2>

            {/* Workout Type */}
            <WorkoutTypeSelect
              value={workoutType}
              onChange={setWorkoutType}
              className="mb-4"
            />

            {/* 2x2 Photo Grid */}
            <div className="grid grid-cols-2 gap-3 mb-4">
              <ImageUploadBox
                label="시작 사진"
                image={startImage}
                onImageChange={setStartImage}
                onFileSelect={handleStartImageUpload}
              />
              <ImageUploadBox
                label="끝 사진"
                image={endImage}
                onImageChange={setEndImage}
                onFileSelect={handleEndImageUpload}
              />
              <ImageUploadBox
                label="기타 사진"
                image={otherImages[0] || null}
                onImageChange={() => setOtherImages(otherImages.slice(1))}
                onFileSelect={handleOtherImageUpload}
                variant="display"
                showCount={otherImages.length}
              />
              <ImageUploadBox
                label="업로드"
                image={null}
                onImageChange={() => {}}
                onFileSelect={handleOtherImageUpload}
                variant="upload"
              />
            </div>

            {/* Submit Button */}
            <Button
              onClick={handleSubmit}
              disabled={!isVerified || isUploading}
              className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors mb-4 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isUploading ? "사진 올리는 중..." : "작성"}
            </Button>

            {/* Workout Metrics */}
            {workoutMetrics && (
              <div className="mb-4 p-3 bg-white/50 rounded-lg border border-gray-200">
                <div className="grid grid-cols-3 gap-2 text-center">
                  <div>
                    <Clock className="w-4 h-4 mx-auto mb-1 text-blue-500" />
                    <div className="text-xs text-gray-500">시간</div>
                    <div className="text-sm font-bold">{workoutMetrics.durationMinutes}분</div>
                  </div>
                  <div>
                    <Flame className="w-4 h-4 mx-auto mb-1 text-orange-500" />
                    <div className="text-xs text-gray-500">칼로리</div>
                    <div className="text-sm font-bold">{workoutMetrics.calories}kcal</div>
                  </div>
                  <div>
                    <Trophy className="w-4 h-4 mx-auto mb-1 text-yellow-500" />
                    <div className="text-xs text-gray-500">점수</div>
                    <div className="text-sm font-bold">{workoutMetrics.score}점</div>
                  </div>
                </div>
              </div>
            )}

            {/* Verification Badge */}
            {isVerified && (
              <Badge className="bg-green-500 text-white px-3 py-1.5 font-bold border-0 w-full justify-center text-xs">
                <CheckCircle className="w-3 h-3 mr-1" />
                운동 인증 완료
              </Badge>
            )}
          </div>

          {/* Right Editor */}
          <div className="w-[475px] bg-transparent flex-shrink-0 flex flex-col overflow-hidden">
            <ScrollArea className="flex-1 w-[475px] h-full" style={{ width: '475px', maxWidth: '475px' }}>
              <style>{`
                .bn-editor {
                  max-width: 443px !important;
                  width: 443px !important;
                  background: transparent !important;
                }
                .bn-container {
                  max-width: 475px !important;
                  width: 475px !important;
                  background: transparent !important;
                }
                .bn-block-content {
                  max-width: 443px !important;
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
                }
                [data-radix-scroll-area-viewport] {
                  width: 475px !important;
                  max-width: 475px !important;
                }
              `}</style>
              <div style={{ minWidth: '475px', width: '475px' }}>
                <div style={{ padding: '1rem' }}>
                  <BlockNoteView editor={editor} theme="light" />
                </div>
              </div>
            </ScrollArea>
          </div>
        </div>
      </DialogContent>
    </Dialog>

      {/* 닫기 확인 다이얼로그 */}
      <AlertDialog open={showCloseConfirm} onOpenChange={setShowCloseConfirm}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>작성 중인 내용이 있습니다</AlertDialogTitle>
            <AlertDialogDescription>
              작성 중인 피드가 임시 저장되어 있습니다. 정말로 닫으시겠습니까?
              (임시 저장된 내용은 다음에 다시 열 때 복원됩니다)
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setShowCloseConfirm(false)}>
              계속 작성하기
            </AlertDialogCancel>
            <AlertDialogAction onClick={handleCloseWithoutSaving}>
              임시저장 삭제하고 닫기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
