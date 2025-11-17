/**
 * CreatePage.tsx
 *
 * 피드 작성 페이지 컴포넌트
 * - 운동 인증 사진 업로드
 * - 드래그 앤 드롭 이미지 업로드
 * - 운동 정보 입력 폼
 */

import React, { useState, useEffect, useRef } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { Upload, CheckCircle, X, Zap } from "lucide-react";
import { toast } from "sonner";
import { Feed } from "@/types/dashboard.types";

interface CreatePageProps {
  onCreatePost: (newFeed: Feed) => void;
}

export default function CreatePage({ onCreatePost }: CreatePageProps) {
  const [postImages, setPostImages] = useState<string[]>([]);
  const [postContent, setPostContent] = useState("");
  const [isWorkoutVerified, setIsWorkoutVerified] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleCreatePost = () => {
    if (postImages.length === 0 || !postContent) {
      toast.error("이미지와 내용을 모두 입력해주세요!");
      return;
    }

    if (!isWorkoutVerified) {
      toast.error("운동 인증이 필요합니다!");
      return;
    }

    const newFeed: Feed = {
      id: Date.now(),
      author: "김루핀",
      avatar: "김",
      activity: "운동",
      duration: "30분",
      points: 20,
      content: postContent,
      images: postImages,
      likes: 0,
      comments: 0,
      time: "방금 전",
      stats: { workout: "+20" },
      isMine: true,
      likedBy: []
    };

    onCreatePost(newFeed);
    toast.success("피드가 작성되었습니다!");
    setPostImages([]);
    setPostContent("");
    setIsWorkoutVerified(false);
  };

  useEffect(() => {
    if (postImages.length > 0) {
      const timer = setTimeout(() => {
        setIsWorkoutVerified(true);
      }, 1000);
      return () => clearTimeout(timer);
    } else {
      const timer = setTimeout(() => {
        setIsWorkoutVerified(false);
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [postImages]);

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = Array.from(e.dataTransfer.files);
    files.forEach(file => {
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (event) => {
          if (event.target?.result) {
            setPostImages(prev => [...prev, event.target!.result as string]);
          }
        };
        reader.readAsDataURL(file);
      }
    });
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    files.forEach(file => {
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (event) => {
          if (event.target?.result) {
            setPostImages(prev => [...prev, event.target!.result as string]);
          }
        };
        reader.readAsDataURL(file);
      }
    });
  };

  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">새 피드 작성</h1>
          <p className="text-gray-700 font-medium text-lg">운동 기록을 공유하세요</p>
        </div>

        <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
          <div className="p-8 space-y-6">
            {/* Image Upload Area - Wide with Drag & Drop */}
            <div className="space-y-3">
              <div className="flex items-center gap-4 mb-2">
                <Label className="text-base font-black text-gray-900">이미지</Label>
                {isWorkoutVerified && (
                  <Badge className="bg-green-500 text-white px-4 py-2 font-bold border-0">
                    <CheckCircle className="w-4 h-4 mr-1" />
                    운동 인증 완료
                  </Badge>
                )}
              </div>

              <div
                onDragEnter={handleDragEnter}
                onDragLeave={handleDragLeave}
                onDragOver={handleDragOver}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
                className={`w-full h-32 rounded-2xl border-2 border-dashed transition-all cursor-pointer ${
                  isDragging
                    ? 'border-[#C93831] bg-red-50'
                    : 'border-gray-300 hover:border-[#C93831] bg-white/50'
                }`}
              >
                <div className="h-full flex flex-col items-center justify-center gap-2">
                  <Upload className="w-8 h-8 text-gray-400" />
                  <span className="font-bold text-gray-600 text-sm">
                    클릭하거나 드래그하여 이미지 첨부
                  </span>
                </div>
              </div>

              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                multiple
                onChange={handleFileSelect}
                className="hidden"
              />
            </div>

            {/* Image Preview List - Fixed X button with higher z-index */}
            {postImages.length > 0 && (
              <ScrollArea className="max-h-40">
                <div className="flex gap-3 pb-2">
                  {postImages.map((img, idx) => (
                    <div key={idx} className="relative flex-shrink-0" style={{ width: '136px', height: '136px' }}>
                      <div className="w-32 h-32 rounded-xl overflow-hidden bg-gray-100">
                        <img src={img} alt={`Upload ${idx + 1}`} className="w-full h-full object-contain" />
                      </div>
                      <button
                        onClick={() => setPostImages(postImages.filter((_, i) => i !== idx))}
                        className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-[100] pointer-events-auto"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            )}

            {/* Content - Taller */}
            <div className="space-y-3">
              <Label className="text-base font-black text-gray-900">내용</Label>
              <Textarea
                placeholder="오늘의 운동은 어땠나요? 자세히 공유해주세요..."
                value={postContent}
                onChange={(e) => setPostContent(e.target.value)}
                className="min-h-[400px] rounded-2xl bg-white border-2 border-gray-200 focus:border-[#C93831] font-medium text-base resize-none transition-all"
              />
            </div>

            {/* Submit */}
            <Button
              onClick={handleCreatePost}
              disabled={!isWorkoutVerified}
              className="w-full h-16 rounded-2xl bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-black text-xl border-0 shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Zap className="w-6 h-6 mr-2" />
              게시하기
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
