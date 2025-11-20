/**
 * EditFeedDialog.tsx
 *
 * 피드 수정 다이얼로그 컴포넌트
 * - 기존 피드 내용 수정
 * - BlockNote 에디터 사용
 * - 운동 시작/끝 사진 업로드
 */

import React, { useState, useEffect, useRef } from "react";
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
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { X, CheckCircle, Camera, Upload, Check, ChevronsUpDown } from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";

interface EditFeedDialogProps {
  feed: Feed | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (feedId: number, images: string[], content: string, workoutType: string, startImage: string | null, endImage: string | null) => void;
}

const WORKOUT_TYPES = [
  { value: "running", label: "런닝" },
  { value: "walking", label: "걷기" },
  { value: "cycling", label: "사이클" },
  { value: "swimming", label: "수영" },
  { value: "weight", label: "웨이트" },
  { value: "yoga", label: "요가" },
  { value: "pilates", label: "필라테스" },
  { value: "crossfit", label: "크로스핏" },
  { value: "hiking", label: "등산" },
  { value: "other", label: "기타" }
];

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
  const [comboboxOpen, setComboboxOpen] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const [hasChanges, setHasChanges] = useState(false);

  const uploadInputRef = useRef<HTMLInputElement>(null);
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

  const handleFileSelect = (
    e: React.ChangeEvent<HTMLInputElement>,
    type: 'start' | 'end' | 'upload'
  ) => {
    const files = type === 'upload' ? Array.from(e.target.files || []) : [e.target.files?.[0]].filter(Boolean) as File[];

    files.forEach(file => {
      if (!file.type.startsWith('image/')) return;

      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          const imageData = event.target.result as string;
          if (type === 'start') setStartImage(imageData);
          else if (type === 'end') setEndImage(imageData);
          else setOtherImages(prev => [...prev, imageData]);
        }
      };
      reader.readAsDataURL(file);
    });
  };

  const handleDrop = (e: React.DragEvent, type: 'start' | 'end' | 'upload') => {
    e.preventDefault();
    e.stopPropagation();

    const files = Array.from(e.dataTransfer.files);
    files.forEach(file => {
      if (!file.type.startsWith('image/')) return;

      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          const imageData = event.target.result as string;
          if (type === 'start') setStartImage(imageData);
          else if (type === 'end') setEndImage(imageData);
          else setOtherImages(prev => [...prev, imageData]);
        }
      };
      reader.readAsDataURL(file);
    });
  };

  const isVerified = startImage && endImage;

  // 저장 처리
  const handleSave = () => {
    if (!feed) return;

    if (!startImage || !endImage) {
      toast.error("운동 인증을 위해 시작 사진과 끝 사진을 모두 업로드해주세요!");
      return;
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
            <div className="mb-4">
              <Label className="text-xs font-bold text-gray-900 mb-2 block">운동 종류</Label>
              <Popover open={comboboxOpen} onOpenChange={setComboboxOpen}>
                <PopoverTrigger asChild>
                  <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={comboboxOpen}
                    className="w-full justify-between bg-white border-gray-300 text-sm"
                    onClick={() => {
                      console.log("Button clicked, current state:", comboboxOpen);
                      setComboboxOpen(!comboboxOpen);
                    }}
                  >
                    {workoutType
                      ? WORKOUT_TYPES.find((type) => type.value === workoutType)?.label || "값 없음"
                      : "운동 선택"}
                    <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-full p-0" style={{ width: "var(--radix-popover-trigger-width)" }}>
                  <Command>
                    <CommandInput placeholder="운동 검색..." />
                    <CommandList>
                      <CommandEmpty>운동을 찾을 수 없습니다.</CommandEmpty>
                      <CommandGroup>
                        {WORKOUT_TYPES.map((type) => (
                          <CommandItem
                            key={type.value}
                            value={type.value}
                            onSelect={(currentValue: string) => {
                              setWorkoutType(currentValue === workoutType ? "" : currentValue);
                              setComboboxOpen(false);
                            }}
                          >
                            <Check
                              className={cn(
                                "mr-2 h-4 w-4",
                                workoutType === type.value ? "opacity-100" : "opacity-0"
                              )}
                            />
                            {type.label}
                          </CommandItem>
                        ))}
                      </CommandGroup>
                    </CommandList>
                  </Command>
                </PopoverContent>
              </Popover>
            </div>

            {/* 2x2 Photo Grid */}
            <div className="grid grid-cols-2 gap-3 mb-4">
              {/* Start Image */}
              <div>
                <Label className="text-xs font-bold text-gray-700 mb-1.5 block">시작 사진</Label>
                <div
                  onClick={() => {
                    const input = document.createElement('input');
                    input.type = 'file';
                    input.accept = 'image/*';
                    input.onchange = (e) => handleFileSelect(e as any, 'start');
                    input.click();
                  }}
                  onDrop={(e) => handleDrop(e, 'start')}
                  onDragOver={(e) => e.preventDefault()}
                  className="relative aspect-square rounded-lg border-2 border-dashed border-gray-300 hover:border-[#C93831] bg-gray-50 cursor-pointer overflow-visible"
                >
                  {startImage ? (
                    <>
                      <img src={startImage} alt="Start" className="w-full h-full object-cover rounded-lg" />
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setStartImage(null);
                        }}
                        className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-50"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </>
                  ) : (
                    <div className="absolute inset-0 flex flex-col items-center justify-center gap-1">
                      <Camera className="w-6 h-6 text-gray-400" />
                    </div>
                  )}
                </div>
              </div>

              {/* End Image */}
              <div>
                <Label className="text-xs font-bold text-gray-700 mb-1.5 block">끝 사진</Label>
                <div
                  onClick={() => {
                    const input = document.createElement('input');
                    input.type = 'file';
                    input.accept = 'image/*';
                    input.onchange = (e) => handleFileSelect(e as any, 'end');
                    input.click();
                  }}
                  onDrop={(e) => handleDrop(e, 'end')}
                  onDragOver={(e) => e.preventDefault()}
                  className="relative aspect-square rounded-lg border-2 border-dashed border-gray-300 hover:border-[#C93831] bg-gray-50 cursor-pointer overflow-visible"
                >
                  {endImage ? (
                    <>
                      <img src={endImage} alt="End" className="w-full h-full object-cover rounded-lg" />
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setEndImage(null);
                        }}
                        className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-50"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </>
                  ) : (
                    <div className="absolute inset-0 flex flex-col items-center justify-center gap-1">
                      <Camera className="w-6 h-6 text-gray-400" />
                    </div>
                  )}
                </div>
              </div>

              {/* Other Images */}
              <div>
                <Label className="text-xs font-bold text-gray-700 mb-1.5 block">기타 사진</Label>
                <div className="relative aspect-square rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 overflow-visible">
                  {otherImages.length > 0 ? (
                    <>
                      <img src={otherImages[0]} alt="Other" className="w-full h-full object-cover rounded-lg" />
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setOtherImages(otherImages.filter((_, i) => i !== 0));
                        }}
                        className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-10"
                      >
                        <X className="w-3 h-3" />
                      </button>
                      {otherImages.length > 1 && (
                        <div className="absolute right-2 bottom-2 bg-[#C93831] text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center z-10">
                          +{otherImages.length - 1}
                        </div>
                      )}
                    </>
                  ) : (
                    <div className="absolute inset-0 flex items-center justify-center">
                      <span className="text-xs text-gray-400">없음</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Upload Cell */}
              <div>
                <Label className="text-xs font-bold text-gray-700 mb-1.5 block">업로드</Label>
                <div
                  onClick={() => uploadInputRef.current?.click()}
                  onDrop={(e) => handleDrop(e, 'upload')}
                  onDragOver={(e) => e.preventDefault()}
                  className="relative aspect-square rounded-lg border-2 border-dashed border-[#C93831] bg-red-50 hover:bg-red-100 cursor-pointer flex items-center justify-center"
                >
                  <Upload className="w-8 h-8 text-[#C93831]" />
                  <input
                    ref={uploadInputRef}
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={(e) => handleFileSelect(e, 'upload')}
                    className="hidden"
                  />
                </div>
              </div>
            </div>

            {/* Verification Badge */}
            {isVerified && (
              <Badge className="bg-green-500 text-white px-3 py-1.5 font-bold border-0 mb-4 w-full justify-center text-xs">
                <CheckCircle className="w-3 h-3 mr-1" />
                운동 인증 완료
              </Badge>
            )}

            {/* 작성 버튼 */}
            <Button
              onClick={handleSave}
              disabled={!isVerified}
              className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-semibold py-2.5 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isVerified ? '수정 완료' : '시작/끝 사진 필요'}
            </Button>
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
