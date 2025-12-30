import React from "react";
import { Label } from "@/components/ui/label";
import { X, Camera, Upload, Loader2 } from "lucide-react";

interface ImageUploadBoxProps {
  label: string;
  image: string | null;
  onImageChange: (image: string | null) => void;
  onFileSelect: (file: File) => Promise<void>;
  variant?: "default" | "upload" | "display";
  showCount?: number;
  className?: string;
  isLoading?: boolean; // [추가] 로딩 상태
}

export default function ImageUploadBox({
  label,
  image,
  onImageChange,
  onFileSelect,
  variant = "default",
  showCount,
  className = "",
  isLoading = false, // [추가] 기본값 false
}: ImageUploadBoxProps) {
  const handleClick = () => {
    if (variant === "display" || isLoading) return; // 로딩 중 클릭 방지

    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*";
    if (variant === "upload") input.multiple = true;

    input.onchange = async (e) => {
      const files = Array.from((e.target as HTMLInputElement).files || []);
      for (const file of files) {
        if (file.type.startsWith("image/")) {
          await onFileSelect(file);
          if (variant !== "upload") break;
        }
      }
    };
    input.click();
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (variant === "display" || isLoading) return;

    const files = Array.from(e.dataTransfer.files);
    for (const file of files) {
      if (file.type.startsWith("image/")) {
        await onFileSelect(file);
        if (variant !== "upload") break;
      }
    }
  };

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
    onImageChange(null);
  };

  // [추가] 로딩 오버레이 (프로그레스 역할)
  const LoadingOverlay = () => (
    <div className="absolute inset-0 z-50 flex flex-col items-center justify-center bg-white/80 backdrop-blur-sm rounded-lg cursor-wait">
      <Loader2 className="w-8 h-8 text-[#C93831] animate-spin mb-2" />
      <span className="text-[10px] font-bold text-[#C93831] animate-pulse">
        처리 중...
      </span>
    </div>
  );

  // 1. 업로드 전용 박스
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
          {isLoading ? (
            <LoadingOverlay />
          ) : (
            <Upload className="w-8 h-8 text-[#C93831]" />
          )}
        </div>
      </div>
    );
  }

  // 2. 표시 전용 박스
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
          {isLoading && <LoadingOverlay />}
        </div>
      </div>
    );
  }

  // 3. 기본 업로드 박스 (시작/끝 사진)
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
        {isLoading && <LoadingOverlay />}

        {image ? (
          <>
            <img
              src={image}
              alt={label}
              className="w-full h-full object-cover rounded-lg"
            />
            {!isLoading && (
              <button
                onClick={handleRemove}
                className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center hover:bg-red-600 shadow-lg z-10 cursor-pointer"
              >
                <X className="w-3 h-3" />
              </button>
            )}
          </>
        ) : (
          !isLoading && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-1">
              <Camera className="w-6 h-6 text-gray-400" />
            </div>
          )
        )}
      </div>
    </div>
  );
}
