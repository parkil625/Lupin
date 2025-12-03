/**
 * useImageBrightness.ts
 *
 * 이미지 밝기를 분석하여 오버레이 아이콘 색상을 결정하는 훅
 * - 이미지 우측 하단 영역의 평균 밝기 계산
 * - 밝은 이미지: 검정 아이콘, 어두운 이미지: 흰색 아이콘
 */

import { useState, useEffect } from "react";

export type IconColor = "white" | "black";

export function useImageBrightness(imageUrl: string | undefined): IconColor {
  const [iconColor, setIconColor] = useState<IconColor>("white");

  useEffect(() => {
    if (!imageUrl) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- 이미지 없을 때 기본값 설정
      setIconColor("black");
      return;
    }

    const img = new Image();
    img.crossOrigin = "Anonymous";
    // CORS 캐시 우회를 위한 타임스탬프 추가
    const urlWithCacheBust = imageUrl.includes('?')
      ? `${imageUrl}&_t=${Date.now()}`
      : `${imageUrl}?_t=${Date.now()}`;
    img.src = urlWithCacheBust;

    img.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          setIconColor("white");
          return;
        }

        canvas.width = img.width;
        canvas.height = img.height;
        ctx.drawImage(img, 0, 0);

        // 우측 하단 영역의 밝기 계산 (아이콘이 위치한 부분)
        const sampleWidth = Math.min(100, img.width);
        const sampleHeight = Math.min(150, img.height);
        const x = img.width - sampleWidth;
        const y = img.height - sampleHeight;

        const imageData = ctx.getImageData(x, y, sampleWidth, sampleHeight);
        const data = imageData.data;

        let totalBrightness = 0;
        let totalAlpha = 0;
        const pixelCount = data.length / 4;

        for (let i = 0; i < data.length; i += 4) {
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];
          const a = data[i + 3];
          // Perceived brightness 공식
          const brightness = 0.299 * r + 0.587 * g + 0.114 * b;
          totalBrightness += brightness;
          totalAlpha += a;
        }

        const avgBrightness = totalBrightness / pixelCount;
        const avgAlpha = totalAlpha / pixelCount;

        // 투명한 배경이면 검정색, 아니면 평균 밝기에 따라 결정
        if (avgAlpha < 200) {
          setIconColor("black");
        } else {
          setIconColor(avgBrightness > 128 ? "black" : "white");
        }
      } catch {
        // CORS 에러 등의 경우 기본값 사용
        setIconColor("white");
      }
    };

    img.onerror = () => {
      setIconColor("white");
    };
  }, [imageUrl]);

  return iconColor;
}

export default useImageBrightness;
