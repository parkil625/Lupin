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
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import { imageApi } from "@/api/imageApi";
// EXIF 검증은 백엔드에서만 수행됨

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
          setWorkoutType(draft.workoutType || "헬스");

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

  // 시작 이미지 업로드 (EXIF 검증은 백엔드에서 수행)
  const handleStartImageUpload = async (file: File) => {
    await uploadImage(file, setStartImage);
  };

  // 끝 이미지 업로드 (EXIF 검증은 백엔드에서 수행)
  const handleEndImageUpload = async (file: File) => {
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

  // 변경사항이 있는지 확인
  const hasChanges =
    startImage !== null ||
    endImage !== null ||
    otherImages.length > 0 ||
    workoutType !== "헬스" ||
    hasEditorContent;

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
    setWorkoutType("헬스");

    onOpenChange(false);
  };

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="w-full h-full md:h-[95vh] md:!max-w-[795px] md:!w-[795px] p-0 overflow-hidden backdrop-blur-3xl bg-white/60 border border-gray-200 shadow-2xl !flex !gap-0">
        <DialogHeader className="sr-only">
          <DialogTitle>피드 작성</DialogTitle>
          <DialogDescription>
            새로운 피드를 작성할 수 있습니다.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col md:flex-row h-full overflow-hidden w-full">
          {/* Left Sidebar */}
          <div className="w-full md:w-80 bg-transparent border-b md:border-b-0 md:border-r border-gray-200 p-4 md:p-6 flex-shrink-0">
            <h2 className="text-xl font-black text-gray-900 mb-4">피드 작성</h2>

            {/* Workout Type */}
            <WorkoutTypeSelect
              value={workoutType}
              onChange={setWorkoutType}
              className="mb-4"
            />

            {/* 2x2 Photo Grid */}
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

            {/* Submit Button */}
            <Button
              onClick={handleSubmit}
              disabled={!canSubmit || isUploading}
              className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors mb-4 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isUploading ? "사진 올리는 중..." : canSubmit ? "작성" : "시작/끝 사진 필요"}
            </Button>

            {/* 운동 점수는 피드 작성 후 백엔드에서 계산됩니다 */}
            <p className="text-xs text-gray-500 text-center">
              사진의 EXIF 정보로 운동 시간과 점수가 자동 계산됩니다
            </p>
          </div>

          {/* Right Editor */}
          <div className="flex-1 md:w-[475px] bg-transparent md:flex-shrink-0 flex flex-col overflow-hidden">
            <ScrollArea className="flex-1 w-full md:w-[475px] h-full">
              <style>{`
                .bn-editor {
                  max-width: 100% !important;
                  width: 100% !important;
                  background: transparent !important;
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
                }
                @media (min-width: 768px) {
                  .bn-editor, .bn-container {
                    max-width: 443px !important;
                    width: 443px !important;
                  }
                  .bn-block-content {
                    max-width: 443px !important;
                  }
                }
              `}</style>
              <div className="w-full md:min-w-[475px] md:w-[475px]">
                <div className="p-4">
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
              임시 저장하면 다음에 다시 열 때 복원됩니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={handleCloseWithoutSaving}>
              비우고 닫기
            </AlertDialogCancel>
            <AlertDialogAction onClick={() => {
              setShowCloseConfirm(false);
              onOpenChange(false);
            }}>
              저장 후 닫기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
