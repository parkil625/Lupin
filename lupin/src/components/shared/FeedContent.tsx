/**
 * FeedContent.tsx
 *
 * 피드 내용 입력/표시 컴포넌트 (인스타그램 스타일)
 * - 단순 textarea 입력
 * - URL 자동 링크
 * - 기존 BlockNote JSON도 텍스트로 변환해서 표시
 */
import React, { useMemo } from "react";

interface FeedContentDisplayProps {
  content: string;
  className?: string;
}

interface FeedContentInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
  className?: string;
}

/**
 * BlockNote JSON에서 텍스트만 추출
 */
function extractTextFromBlockNote(content: string): string {
  if (!content) return "";

  // 일반 텍스트면 그대로 반환
  if (!content.startsWith("[")) {
    return content;
  }

  try {
    const blocks = JSON.parse(content);
    const texts: string[] = [];

    const extractFromContent = (contentArray: unknown[]): string => {
      return contentArray
        .map((item: unknown) => {
          const typedItem = item as { type?: string; text?: string; content?: unknown[] };
          if (typedItem.type === "text") {
            return typedItem.text || "";
          }
          if (typedItem.content) {
            return extractFromContent(typedItem.content);
          }
          return "";
        })
        .join("");
    };

    for (const block of blocks) {
      if (block.content && Array.isArray(block.content)) {
        const text = extractFromContent(block.content);
        if (text) texts.push(text);
      }
    }

    return texts.join("\n");
  } catch {
    return content;
  }
}

/**
 * URL을 클릭 가능한 링크로 변환
 */
function formatContent(text: string): React.ReactNode[] {
  if (!text) return [];

  // URL 패턴
  const urlRegex = /(https?:\/\/[^\s]+)/g;

  const parts = text.split(urlRegex);

  return parts.map((part, index) => {
    // URL인 경우 링크로 변환
    if (urlRegex.test(part)) {
      // Reset lastIndex after test
      urlRegex.lastIndex = 0;
      return (
        <a
          key={index}
          href={part}
          target="_blank"
          rel="noopener noreferrer"
          className="text-[#C93831] hover:underline break-all"
          onClick={(e) => e.stopPropagation()}
        >
          {part.length > 50 ? `${part.slice(0, 50)}...` : part}
        </a>
      );
    }

    // 줄바꿈 처리
    return (
      <React.Fragment key={index}>
        {part.split("\n").map((line, lineIndex, arr) => (
          <React.Fragment key={lineIndex}>
            {line}
            {lineIndex < arr.length - 1 && <br />}
          </React.Fragment>
        ))}
      </React.Fragment>
    );
  });
}

/**
 * 피드 내용 표시 컴포넌트
 * - URL 자동 링크
 * - 줄바꿈 유지
 * - BlockNote JSON도 텍스트로 변환해서 표시
 */
export function FeedContentDisplay({ content, className = "" }: FeedContentDisplayProps) {
  const displayContent = useMemo(() => {
    const text = extractTextFromBlockNote(content);
    return formatContent(text);
  }, [content]);

  if (!content) return null;

  return (
    <p className={`text-gray-900 text-sm leading-relaxed whitespace-pre-wrap ${className}`}>
      {displayContent}
    </p>
  );
}

/**
 * 피드 내용 입력 컴포넌트
 * - 단순 textarea (아웃라인 없음)
 * - 전체 영역 채우기
 * - 글자 수 표시
 */
export function FeedContentInput({
  value,
  onChange,
  placeholder = "무슨 운동을 하셨나요?",
  maxLength = 2200,
  className = "",
}: FeedContentInputProps) {
  return (
    <div className={`relative h-full flex flex-col ${className}`}>
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        maxLength={maxLength}
        className="flex-1 w-full p-4 resize-none focus:outline-none text-sm bg-transparent"
      />
      <div className="absolute bottom-2 right-4 text-xs text-gray-400">
        {value.length}/{maxLength}
      </div>
    </div>
  );
}

/**
 * 기존 BlockNote content를 plain text로 변환 (마이그레이션용)
 */
export function convertBlockNoteToPlainText(content: string): string {
  return extractTextFromBlockNote(content);
}

export default FeedContentDisplay;
