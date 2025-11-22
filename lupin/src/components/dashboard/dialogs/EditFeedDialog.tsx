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
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { CheckCircle, Clock, Flame, Trophy } from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { toast } from "sonner";
import { ImageUploadBox, WorkoutTypeSelect } from "@/components/molecules";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import {
  extractImageMetadata,
  validateWorkoutTimes,
  calculateWorkoutMetrics,
  type WorkoutCalculation
} from "@/lib/imageMetadata";

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
  const [startTime, setStartTime] = useState<Date | null>(null);
  const [endTime, setEndTime] = useState<Date | null>(null);
  const [workoutMetrics, setWorkoutMetrics] = useState<WorkoutCalculation | null>(null);
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

  // 시작 이미지 업로드 (메타데이터 추출 포함)
  const handleStartImageUpload = async (file: File) => {
    const metadata = await extractImageMetadata(file);
    setStartTime(metadata.dateTime);

    if (!metadata.dateTime) {
      toast.warning("사진에서 시간 정보를 찾을 수 없습니다. 다른 사진을 시도해보세요.");
    }

    await uploadImage(file, setStartImage);
  };

  // 끝 이미지 업로드 (메타데이터 추출 포함)
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

  // 저장 처리
  const handleSave = () => {
    if (!feed) return;

    if (!startImage || !endImage) {
      toast.error("운동 인증을 위해 시작 사진과 끝 사진을 모두 업로드해주세요!");
      return;
    }

    // 시간 유효성 검증 (새로 업로드한 경우에만)
    if (startTime && endTime) {
      const validation = validateWorkoutTimes(startTime, endTime);
      if (!validation.valid) {
        toast.error(validation.error || "시간 정보가 올바르지 않습니다.");
        return;
      }
    }

    const images = [startImage, endImage, ...otherImages].filter(Boolean) as string[];
    const blocks = editor.document;
    const contentJson = JSON.stringify(blocks);

    onSave(feed.id, images, contentJson, workoutType, startImage, endImage);
    toast.success("피드가 수정되었습니다!");
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
        <DialogContent className="!max-w-[795px] !w-[795px] h-[95vh] p-0 overflow-hidden backdrop-blur-3xl bg-white/60 border border-gray-200 shadow-2xl !flex !gap-0" style={{ width: '795px', maxWidth: '795px' }}>
          <DialogTitle className="sr-only">피드 수정</DialogTitle>
          <DialogDescription className="sr-only">
            기존 피드 내용을 수정합니다. 운동 종류, 시작/끝 사진, 그리고 내용을 수정할 수 있습니다.
          </DialogDescription>
          <div className="flex h-full overflow-hidden w-full">
          {/* Left Sidebar */}
          <div className="w-80 bg-transparent border-r border-gray-200 p-6 flex-shrink-0">
            <h2 className="text-xl font-black text-gray-900 mb-4">피드 수정</h2>

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

            {/* 작성 버튼 */}
            <Button
              onClick={handleSave}
              disabled={!isVerified}
              className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold py-2.5 rounded-lg transition-colors mb-4 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isVerified ? '수정 완료' : '시작/끝 사진 필요'}
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
