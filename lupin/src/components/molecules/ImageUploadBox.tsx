/**
 * ImageUploadBox - 이미지 업로드 박스 컴포넌트
 * Molecule: Label + 드래그앤드롭 영역 + 미리보기
 */

import React from "react";
import { Label } from "@/components/ui/label";
import { X, Camera, Upload } from "lucide-react";

interface ImageUploadBoxProps {
  label: string;
  image: string | null;
  onImageChange: (image: string | null) => void;
  onFileSelect: (file: File) => Promise<void>;
  variant?: "default" | "upload" | "display";
  showCount?: number;
  className?: string;
}

export default function ImageUploadBox({
  label,
  image,
  onImageChange,
  onFileSelect,
  variant = "default",
  showCount,
  className = "",
}: ImageUploadBoxProps) {
  const handleClick = () => {
    if (variant === "display") return;

    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*";
    if (variant === "upload") input.multiple = true;

    input.onchange = async (e) => {
      const files = Array.from((e.target as HTMLInputElement).files || []);
      for (const file of files) {
        if (file.type.startsWith("image/")) {
          await onFileSelect(file);
          if (variant !== "upload") break; // 단일 이미지면 첫 번째만
        }
      }
    };
    input.click();
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (variant === "display") return;

    const files = Array.from(e.dataTransfer.files);
    for (const file of files) {
      if (file.type.startsWith("image/")) {
        await onFileSelect(file);
        if (variant !== "upload") break; // 단일 이미지면 첫 번째만
      }
    }
  };

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
    onImageChange(null);
  };

  // 업로드 전용 박스
  if (variant === "upload") {
    return (
      <div className={className}>
        <Label className="text-xs font-bold text-gray-700 mb-1.5 block">
          {label}
        </Label>
        <div
          onClick={handleClick}
          onDrop={handleDrop}
          onDragOver={(e) => e.preventDefault()}
          className="relative aspect-square rounded-lg border-2 border-dashed border-[#C93831] bg-red-50 hover:bg-red-100 cursor-pointer flex items-center justify-center"
        >
          <Upload className="w-8 h-8 text-[#C93831]" />
        </div>
      </div>
    );
  }

  // 표시 전용 박스 (기타 사진)
  if (variant === "display") {
    return (
      <div className={className}>
        <Label className="text-xs font-bold text-gray-700 mb-1.5 block">
          {label}
        </Label>
        <div className="relative aspect-square rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 overflow-visible">
          {image ? (
            <>
              <img
                src={image}
                alt={label}
                className="w-full h-full object-cover rounded-lg"
              />
              <button
                onClick={handleRemove}
                className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-10 cursor-pointer"
              >
                <X className="w-3 h-3" />
              </button>
              {showCount && showCount > 1 && (
                <div className="absolute right-2 bottom-2 bg-[#C93831] text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center z-10">
                  +{showCount - 1}
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
    );
  }

  // 기본 업로드 박스
  return (
    <div className={className}>
      <Label className="text-xs font-bold text-gray-700 mb-1.5 block">
        {label}
      </Label>
      <div
        onClick={handleClick}
        onDrop={handleDrop}
        onDragOver={(e) => e.preventDefault()}
        className="relative aspect-square rounded-lg border-2 border-dashed border-gray-300 hover:border-[#C93831] bg-gray-50 cursor-pointer overflow-visible"
      >
        {image ? (
          <>
            <img
              src={image}
              alt={label}
              className="w-full h-full object-cover rounded-lg"
            />
            <button
              onClick={handleRemove}
              className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-50 cursor-pointer"
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
  );
}
