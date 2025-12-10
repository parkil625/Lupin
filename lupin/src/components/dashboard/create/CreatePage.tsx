/**
 * CreatePage.tsx
 *
 * 피드 작성 페이지 컴포넌트
 * - 운동 인증 사진 업로드
 * - 인스타그램 스타일 textarea 사용
 * - 운동 시작/끝 사진 업로드
 */

import React, { useState, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { X, Zap, CheckCircle, Camera, Upload } from "lucide-react";
import { toast } from "sonner";
import { Feed } from "@/types/dashboard.types";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FeedContentInput } from "@/components/shared/FeedContent";

interface CreatePageProps {
  onCreatePost: (newFeed: Feed) => void;
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

export default function CreatePage({ onCreatePost }: CreatePageProps) {
  const [startImage, setStartImage] = useState<string | null>(null);
  const [endImage, setEndImage] = useState<string | null>(null);
  const [otherImages, setOtherImages] = useState<string[]>([]);
  const [workoutType, setWorkoutType] = useState<string>("");

  const uploadInputRef = useRef<HTMLInputElement>(null);

  // 피드 내용 (plain text)
  const [content, setContent] = useState("");

  const handleCreatePost = async () => {
    const images = [
      startImage,
      endImage,
      ...otherImages
    ].filter(Boolean) as string[];

    if (images.length === 0) {
      toast.error("최소 1개의 이미지를 업로드해주세요!");
      return;
    }

    if (!workoutType) {
      toast.error("운동 종류를 선택해주세요!");
      return;
    }

    if (!isVerified) {
      toast.error("운동 시작과 끝 사진이 필요합니다!");
      return;
    }

    // 임시 점수 계산 (추후 백엔드에서 사진 메타데이터 기반으로 계산)
    const workoutMinutes = 30; // 임시값
    const points = Math.floor(workoutMinutes / 5) * 5; // 5분당 5점

    const userName = localStorage.getItem("userName") || "사용자";
    const userId = Number(localStorage.getItem("userId")) || 0;
    const newFeed: Feed = {
      id: Date.now(),
      writerId: userId,
      writerName: userName,
      author: userName,
      activity: workoutType,
      points: points,
      content: content,
      images: images,
      likes: 0,
      comments: 0,
      time: "방금 전",
      createdAt: new Date().toISOString(),
      isMine: true,
    };

    onCreatePost(newFeed);
    toast.success("피드가 작성되었습니다!");

    // 초기화
    setStartImage(null);
    setEndImage(null);
    setOtherImages([]);
    setWorkoutType("");
    setContent("");
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
    <div className="h-full overflow-hidden flex">
      {/* Left Sidebar */}
      <div className="w-80 bg-white border-r border-gray-200 p-6 overflow-y-auto flex-shrink-0">
        <h2 className="text-xl font-black text-gray-900 mb-4">새 피드 작성</h2>

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
                input.onchange = (e) => handleFileSelect(e as unknown as React.ChangeEvent<HTMLInputElement>, 'start');
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
                input.onchange = (e) => handleFileSelect(e as unknown as React.ChangeEvent<HTMLInputElement>, 'end');
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
          onClick={handleCreatePost}
          disabled={!isVerified}
          className="w-full h-10 rounded-lg bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold text-sm border-0 shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Zap className="w-4 h-4 mr-1" />
          게시하기
        </Button>
      </div>

      {/* Right Editor */}
      <div className="w-[475px] bg-white flex-shrink-0 flex flex-col">
        <ScrollArea className="flex-1 w-[475px]" style={{ width: '475px', maxWidth: '475px' }}>
          <div className="p-4">
            <FeedContentInput
              value={content}
              onChange={setContent}
              placeholder="무슨 운동을 하셨나요? 오늘의 운동 기록을 남겨보세요 💪"
              rows={15}
            />
          </div>
        </ScrollArea>
      </div>
    </div>
  );
}
