import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * 상대적 시간 표시 함수
 * 주어진 날짜를 현재 시간 기준으로 "방금 전", "5분 전", "3일 전" 등으로 변환
 */
/**
 * BlockNote JSON 콘텐츠를 텍스트로 변환
 */
export function parseBlockNoteContent(content: string): string {
  if (!content) return "";

  try {
    const blocks = JSON.parse(content);
    if (Array.isArray(blocks)) {
      return blocks
        .map((block: { content?: string | { text?: string }[] }) => {
          if (typeof block.content === "string") {
            return block.content;
          }
          if (Array.isArray(block.content)) {
            return block.content
              .map((item: string | { text?: string }) => (typeof item === "string" ? item : item.text || ""))
              .join("");
          }
          return "";
        })
        .join("\n");
    }
    return content;
  } catch {
    // JSON 파싱 실패시 원본 반환
    return content;
  }
}

const S3_BUCKET = "lupin-storage";
const S3_REGION = "ap-northeast-2";
const S3_BASE_URL = `https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com`;

/**
 * S3 키를 전체 URL로 변환
 * 이미 http로 시작하는 URL이면 그대로 반환
 */
export function getS3Url(s3Key: string | undefined | null): string {
  if (!s3Key) return "";
  if (s3Key.startsWith("http://") || s3Key.startsWith("https://")) {
    return s3Key;
  }
  return `${S3_BASE_URL}/${s3Key}`;
}

export function getRelativeTime(date: Date | string): string {
  const now = new Date();
  const targetDate = typeof date === "string" ? new Date(date) : date;
  const diffMs = now.getTime() - targetDate.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);
  const diffWeeks = Math.floor(diffDays / 7);
  const diffMonths = Math.floor(diffDays / 30);
  const diffYears = Math.floor(diffDays / 365);

  if (diffSeconds < 60) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  if (diffHours < 24) return `${diffHours}시간 전`;
  if (diffDays < 7) return `${diffDays}일 전`;
  if (diffWeeks < 4) return `${diffWeeks}주 전`;
  if (diffMonths < 12) return `${diffMonths}개월 전`;
  return `${diffYears}년 전`;
}
