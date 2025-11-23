/**
 * 이미지 메타데이터 추출 및 운동 계산 유틸리티
 */

import EXIF from "exif-js";

export interface ImageMetadata {
  dateTime: Date | null;
  latitude?: number;
  longitude?: number;
}

export interface WorkoutCalculation {
  durationMinutes: number;
  calories: number;
  score: number;
}

/**
 * 이미지 파일에서 EXIF 메타데이터 추출
 */
export function extractImageMetadata(file: File): Promise<ImageMetadata> {
  return new Promise((resolve) => {
    const reader = new FileReader();

    reader.onload = function(e) {
      const img = new Image();
      img.src = e.target?.result as string;

      img.onload = function() {
        EXIF.getData(img as unknown as string, function(this: typeof img) {
          const dateTimeStr = EXIF.getTag(this, "DateTimeOriginal") ||
                              EXIF.getTag(this, "DateTime");

          let dateTime: Date | null = null;

          if (dateTimeStr) {
            // EXIF 날짜 형식: "2024:01:15 10:30:00"
            const parts = dateTimeStr.split(" ");
            if (parts.length === 2) {
              const datePart = parts[0].replace(/:/g, "-");
              const timePart = parts[1];
              dateTime = new Date(`${datePart}T${timePart}`);
            }
          }

          // GPS 좌표 추출 (있는 경우)
          const latDMS = EXIF.getTag(this, "GPSLatitude");
          const lonDMS = EXIF.getTag(this, "GPSLongitude");
          const latRef = EXIF.getTag(this, "GPSLatitudeRef");
          const lonRef = EXIF.getTag(this, "GPSLongitudeRef");

          let latitude: number | undefined;
          let longitude: number | undefined;

          if (latDMS && lonDMS) {
            latitude = convertDMSToDD(latDMS, latRef);
            longitude = convertDMSToDD(lonDMS, lonRef);
          }

          resolve({ dateTime, latitude, longitude });
        });
      };

      img.onerror = () => resolve({ dateTime: null });
    };

    reader.onerror = () => resolve({ dateTime: null });
    reader.readAsDataURL(file);
  });
}

/**
 * DMS(도분초)를 십진수로 변환
 */
function convertDMSToDD(dms: number[], ref: string): number {
  const degrees = dms[0];
  const minutes = dms[1];
  const seconds = dms[2];

  let dd = degrees + minutes / 60 + seconds / 3600;

  if (ref === "S" || ref === "W") {
    dd = -dd;
  }

  return dd;
}

/**
 * 시작/끝 시간으로 운동 지표 계산
 */
export function calculateWorkoutMetrics(
  startTime: Date,
  endTime: Date,
  workoutType: string
): WorkoutCalculation {
  const durationMs = endTime.getTime() - startTime.getTime();
  const durationMinutes = Math.round(durationMs / (1000 * 60));

  // 운동 종류별 강도 값
  const intensityValues: Record<string, number> = {
    "산책": 0.5,
    "요가": 0.5,
    "스트레칭": 0.6,
    "필라테스": 0.6,
    "골프": 0.7,
    "빠른 걷기": 0.8,
    "헬스": 0.8,
    "배드민턴": 0.9,
    "탁구": 0.9,
    "자전거 타기": 1.0,
    "수영": 1.0,
    "등산": 1.0,
    "테니스": 1.1,
    "조깅": 1.2,
    "축구": 1.4,
    "농구": 1.4,
    "달리기": 1.5,
    "복싱": 1.5,
    "수영(빠르게)": 1.7,
    "자전거(빠르게)": 1.7,
    "HIIT": 1.7,
    "크로스핏": 1.7,
    "줄넘기": 1.8,
  };

  const intensity = intensityValues[workoutType] || 0.8;

  // 칼로리 계산: 강도 * 체중(kg) * 시간(hours) * 기본 MET 계수
  const weightKg = 65;
  const durationHours = durationMinutes / 60;
  const baseMET = intensity * 8; // 강도를 MET로 변환
  const calories = Math.round(baseMET * weightKg * durationHours);

  // 점수 계산: 시간(분) * 강도, 최대 30점
  const rawScore = durationMinutes * intensity;
  const score = Math.min(30, Math.round(rawScore));

  return {
    durationMinutes,
    calories,
    score,
  };
}

/**
 * 시작/끝 시간 유효성 검증
 */
export function validateWorkoutTimes(
  startTime: Date | null,
  endTime: Date | null
): { valid: boolean; error?: string } {
  if (!startTime || !endTime) {
    return { valid: false, error: "사진에서 시간 정보를 찾을 수 없습니다." };
  }

  if (startTime.getTime() >= endTime.getTime()) {
    return {
      valid: false,
      error: "시작 사진의 시간이 끝 사진보다 같거나 늦습니다. 올바른 사진을 업로드해주세요."
    };
  }

  // 최대 24시간 제한
  const maxDuration = 24 * 60 * 60 * 1000;
  if (endTime.getTime() - startTime.getTime() > maxDuration) {
    return {
      valid: false,
      error: "운동 시간이 24시간을 초과합니다. 올바른 사진을 업로드해주세요."
    };
  }

  return { valid: true };
}
