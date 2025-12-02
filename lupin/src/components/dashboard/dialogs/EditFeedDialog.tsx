/**
 * EditFeedDialog.tsx
 *
 * 피드 수정 다이얼로그 컴포넌트
 * - 기존 피드 내용 수정
 * - BlockNote 에디터 사용
 * - 운동 시작/끝 사진 업로드
 */

import { useState, useEffect, useRef } from "react";
import { Dialog, DialogContent, DialogTitle, DialogDescription } from "@/components/ui/dialog";
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
import { Image, FileText } from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { toast } from "sonner";
import { ImageUploadBox, WorkoutTypeSelect } from "@/components/molecules";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
// EXIF 검증은 백엔드에서만 수행됨

interface EditFeedDialogProps {
  feed: Feed | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (feedId: number, images: string[], content: string, workoutType: string, startImage: string | null, endImage: string | null) => void;
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
  const [hasChanges, setHasChanges] = useState(false);
  const [activeTab, setActiveTab] = useState<"photo" | "content">("photo");
  const initialDataRef = useRef<{
    startImage: string | null;
    endImage: string | null;
    otherImages: string[];
    workoutType: string;
    content: string;
  } | null>(null);

  const editor = useCreateBlockNote();

  // 다이얼로그가 닫히면 상태 초기화
  useEffect(() => {
    if (!open) {
      setShowCloseConfirm(false);
      setHasChanges(false);
      initialDataRef.current = null;
    }
  }, [open]);

  // Feed가 변경되면 기존 데이터로 초기화
  useEffect(() => {
    if (feed && editor && open) {
      const initialStartImage = feed.images[0] || null;
      const initialEndImage = feed.images[1] || null;
      const initialOtherImages = feed.images.slice(2) || [];
      const initialWorkoutType = feed.activity || "running";

      setStartImage(initialStartImage);
      setEndImage(initialEndImage);
      setOtherImages(initialOtherImages);
      setWorkoutType(initialWorkoutType);
      setHasChanges(false);

      // 초기 데이터 저장
      initialDataRef.current = {
        startImage: initialStartImage,
        endImage: initialEndImage,
        otherImages: initialOtherImages,
        workoutType: initialWorkoutType,
        content: JSON.stringify(feed.content),
      };

      // 기존 내용을 에디터에 로드
      try {
        let blocks;
        if (typeof feed.content === 'string' && feed.content.startsWith('[')) {
          blocks = JSON.parse(feed.content);
        } else if (typeof feed.content === 'string') {
          blocks = [{ type: "paragraph", content: feed.content }];
        } else {
          blocks = feed.content;
        }

        editor.replaceBlocks(editor.document, blocks);
      } catch (error) {
        console.error('Failed to load feed content:', error);
        editor.replaceBlocks(editor.document, [
          { type: "paragraph", content: feed.content || "" }
        ]);
      }
    }
  }, [feed, editor, open]);

  // 변경사항 감지
  useEffect(() => {
    if (!initialDataRef.current) return;

    const currentContent = JSON.stringify(editor.document);
    const changed =
      startImage !== initialDataRef.current.startImage ||
      endImage !== initialDataRef.current.endImage ||
      JSON.stringify(otherImages) !== JSON.stringify(initialDataRef.current.otherImages) ||
      workoutType !== initialDataRef.current.workoutType ||
      currentContent !== initialDataRef.current.content;

    setHasChanges(changed);
  }, [startImage, endImage, otherImages, workoutType, editor]);

  // 이미지 업로드 핸들러 (FileReader 사용)
  const uploadImage = async (file: File, setter: (url: string) => void) => {
    return new Promise<void>((resolve) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setter(event.target.result as string);
        }
        resolve();
      };
      reader.readAsDataURL(file);
    });
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

  // EXIF 검증은 백엔드에서 수행, 프론트엔드에서는 운동 인증 상태 표시하지 않음
  // 피드 생성 후 응답에서 점수/칼로리 확인

  // 제출 가능: 시작/끝 사진만 있으면 됨
  const canSubmit = startImage && endImage;

  // 저장 처리
  const handleSave = () => {
    if (!feed) return;

    if (!startImage || !endImage) {
      toast.error("시작 사진과 끝 사진을 모두 업로드해주세요!");
      return;
    }

    const images = [startImage, endImage, ...otherImages].filter(Boolean) as string[];
    const blocks = editor.document;
    const contentJson = JSON.stringify(blocks);

    onSave(feed.id, images, contentJson, workoutType, startImage, endImage);
    onOpenChange(false);
  };

  // 다이얼로그 닫기 시 검증
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
    setHasChanges(false);
    onOpenChange(false);
  };

  if (!feed) return null;

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="w-full h-[calc(100%-70px)] max-h-[calc(100vh-70px)] md:h-[95vh] md:max-h-[95vh] md:!max-w-[500px] md:!w-[500px] p-0 overflow-hidden backdrop-blur-3xl bg-white/60 border border-gray-200 shadow-2xl flex flex-col">
          <DialogTitle className="sr-only">피드 수정</DialogTitle>
          <DialogDescription className="sr-only">
            기존 피드 내용을 수정합니다. 운동 종류, 시작/끝 사진, 그리고 내용을 수정할 수 있습니다.
          </DialogDescription>

          {/* 헤더 + 탭 */}
          <div className="p-3 border-b border-gray-200 flex-shrink-0">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-lg font-black text-gray-900">피드 수정</h2>
              <WorkoutTypeSelect
                value={workoutType}
                onChange={setWorkoutType}
                className="w-auto"
              />
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
              onClick={handleSave}
              disabled={!canSubmit}
              className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {canSubmit ? '수정 완료' : '시작/끝 사진 필요'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* 닫기 확인 다이얼로그 */}
      <AlertDialog open={showCloseConfirm} onOpenChange={setShowCloseConfirm}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>변경사항이 저장되지 않았습니다</AlertDialogTitle>
            <AlertDialogDescription>
              수정한 내용이 저장되지 않았습니다. 정말로 닫으시겠습니까?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setShowCloseConfirm(false)}>
              계속 수정하기
            </AlertDialogCancel>
            <AlertDialogAction onClick={handleCloseWithoutSaving}>
              저장하지 않고 닫기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
