/**
 * EditFeedDialog.tsx
 *
 * 피드 수정 다이얼로그 컴포넌트
 * - 기존 피드 내용 수정
 * - BlockNote 에디터 사용
 * - 운동 시작/끝 사진 업로드
 */

import React, { useState, useEffect, useRef } from "react";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { X, CheckCircle, Camera, Upload } from "lucide-react";
import { Feed } from "@/types/dashboard.types";
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

interface EditFeedDialogProps {
  feed: Feed | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (feedId: number, images: string[], content: string, workoutType: string, startImage: string | null, endImage: string | null) => void;
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

  const uploadInputRef = useRef<HTMLInputElement>(null);

  const editor = useCreateBlockNote();

  // Feed가 변경되면 기존 데이터로 초기화
  useEffect(() => {
    if (feed) {
      setStartImage(feed.images[0] || null);
      setEndImage(feed.images[1] || null);
      setOtherImages(feed.images.slice(2) || []);
      setWorkoutType("런닝"); // 기본값
    }
  }, [feed]);

  // 다이얼로그 닫을 때 자동 저장
  useEffect(() => {
    if (!open && feed && (startImage || endImage || otherImages.length > 0)) {
      const images = [startImage, endImage, ...otherImages].filter(Boolean) as string[];
      const blocks = editor.document;
      const contentJson = JSON.stringify(blocks);
      onSave(feed.id, images, contentJson, workoutType, startImage, endImage);
    }
  }, [open]);

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

  if (!feed) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="!max-w-[795px] !w-[795px] h-[95vh] p-0 overflow-hidden backdrop-blur-3xl bg-white border border-gray-200 shadow-2xl !flex !gap-0" style={{ width: '795px', maxWidth: '795px' }}>
        <div className="flex h-full overflow-hidden w-full">
          {/* Left Sidebar */}
          <div className="w-80 bg-white border-r border-gray-200 p-6 overflow-y-auto flex-shrink-0">
            <h2 className="text-xl font-black text-gray-900 mb-4">피드 수정</h2>

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

            {/* Verification Badge */}
            {isVerified && (
              <Badge className="bg-green-500 text-white px-3 py-1.5 font-bold border-0 mb-4 w-full justify-center text-xs">
                <CheckCircle className="w-3 h-3 mr-1" />
                운동 인증 완료
              </Badge>
            )}

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
