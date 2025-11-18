/**
 * CreateFeedDialog.tsx
 *
 * 피드 작성 다이얼로그 컴포넌트
 * - 새 피드 작성
 * - BlockNote 에디터 사용
 * - 운동 시작/끝 사진 업로드
 */

import React, { useState, useEffect, useRef } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { X, CheckCircle, Camera, Upload } from "lucide-react";
import { toast } from "sonner";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";

interface CreateFeedDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (images: string[], content: string, workoutType: string, startImage: string | null, endImage: string | null) => void;
}

const WORKOUT_TYPES = [
  "런닝",
  "걷기",
  "사이클",
  "수영",
  "웨이트",
  "요가",
  "필라테스",
  "크로스핏",
  "등산",
  "기타"
];

const DRAFT_STORAGE_KEY = "createFeedDraft";

export default function CreateFeedDialog({
  open,
  onOpenChange,
  onCreate,
}: CreateFeedDialogProps) {
  const [startImage, setStartImage] = useState<string | null>(null);
  const [endImage, setEndImage] = useState<string | null>(null);
  const [otherImages, setOtherImages] = useState<string[]>([]);
  const [workoutType, setWorkoutType] = useState<string>("런닝");

  const uploadInputRef = useRef<HTMLInputElement>(null);

  const editor = useCreateBlockNote({
    initialContent: [
      {
        type: "paragraph",
        content: "",
      },
    ],
  });

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
          setWorkoutType(draft.workoutType || "런닝");

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
    setWorkoutType("런닝");
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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="!max-w-[795px] !w-[795px] h-[95vh] p-0 overflow-hidden backdrop-blur-3xl bg-white border border-gray-200 shadow-2xl !flex !gap-0" style={{ width: '795px', maxWidth: '795px' }}>
        <DialogHeader className="sr-only">
          <DialogTitle>피드 작성</DialogTitle>
          <DialogDescription>
            새로운 피드를 작성할 수 있습니다.
          </DialogDescription>
        </DialogHeader>
        <div className="flex h-full overflow-hidden w-full">
          {/* Left Sidebar */}
          <div className="w-80 bg-white border-r border-gray-200 p-6 overflow-y-auto flex-shrink-0">
            <h2 className="text-xl font-black text-gray-900 mb-4">피드 작성</h2>

            {/* Workout Type */}
            <div className="mb-4">
              <Label className="text-xs font-bold text-gray-900 mb-2 block">운동 종류</Label>
              <Select value={workoutType} onValueChange={setWorkoutType}>
                <SelectTrigger className="w-full bg-white border-gray-300 text-sm">
                  <SelectValue placeholder="운동 선택" />
                </SelectTrigger>
                <SelectContent>
                  {WORKOUT_TYPES.map(type => (
                    <SelectItem key={type} value={type}>{type}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
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
                    <div className="absolute inset-0 flex items-center justify-center p-2">
                      <div className="relative w-full h-full">
                        {otherImages.slice(0, 3).map((img, idx) => (
                          <div
                            key={idx}
                            className="absolute w-16 h-16 rounded-md border-2 border-white shadow-md overflow-visible"
                            style={{
                              left: `${idx * 12}px`,
                              top: `${idx * 8}px`,
                              zIndex: 3 - idx
                            }}
                          >
                            <img src={img} alt={`Other ${idx}`} className="w-full h-full object-cover rounded-md" />
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setOtherImages(otherImages.filter((_, i) => i !== idx));
                              }}
                              className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-50"
                            >
                              <X className="w-2.5 h-2.5" />
                            </button>
                          </div>
                        ))}
                        {otherImages.length > 1 && (
                          <div className="absolute right-2 bottom-2 bg-[#C93831] text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center z-10">
                            {otherImages.length}
                          </div>
                        )}
                      </div>
                    </div>
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

            {/* Submit Button */}
            <Button
              onClick={handleSubmit}
              className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold hover:shadow-lg transition-all mb-4"
            >
              작성
            </Button>

            {/* Verification Badge */}
            {isVerified && (
              <Badge className="bg-green-500 text-white px-3 py-1.5 font-bold border-0 w-full justify-center text-xs">
                <CheckCircle className="w-3 h-3 mr-1" />
                운동 인증 완료
              </Badge>
            )}
          </div>

          {/* Right Editor */}
          <div className="w-[475px] bg-white flex-shrink-0 flex flex-col">
            <ScrollArea className="flex-1 w-[475px]" style={{ width: '475px', maxWidth: '475px' }}>
              <style>{`
                .bn-editor {
                  max-width: 443px !important;
                  width: 443px !important;
                }
                .bn-container {
                  max-width: 475px !important;
                  width: 475px !important;
                }
                .bn-block-content {
                  max-width: 443px !important;
                }
                .bn-inline-content {
                  word-wrap: break-word !important;
                  overflow-wrap: break-word !important;
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
  );
}
