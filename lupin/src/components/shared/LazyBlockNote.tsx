/**
 * LazyBlockNote.tsx
 *
 * BlockNote 에디터의 완전한 lazy loading
 * - useCreateBlockNote hook과 BlockNoteView를 모두 내부에서 처리
 * - 초기 번들에서 1.3MB 완전 제거
 * - hover 시 prefetch로 UX 개선
 */
import React, { Suspense } from "react";

// 로딩 스피너
const LoadingSpinner = () => (
  <div className="flex items-center justify-center p-4 min-h-[60px]">
    <div className="w-5 h-5 border-2 border-gray-200 border-t-[#C93831] rounded-full animate-spin" />
  </div>
);

// Prefetch 함수 - hover 시 호출
export const prefetchBlockNote = () => {
  import("@blocknote/mantine/style.css");
  import("@blocknote/mantine");
  import("@blocknote/react");
};

// 내부 BlockNote 컴포넌트 (lazy load 대상)
interface BlockNoteInternalProps {
  content?: string;
  editable?: boolean;
  theme?: "light" | "dark";
  className?: string;
  onChange?: (content: string) => void;
}

// Lazy loaded internal component
const BlockNoteInternal = React.lazy(() =>
  import("./BlockNoteInternal").then((m) => ({ default: m.BlockNoteInternal }))
);

// 외부에서 사용하는 컴포넌트
export function LazyBlockNoteView({
  content,
  editable = false,
  theme = "light",
  className,
  onChange,
}: BlockNoteInternalProps) {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <BlockNoteInternal
        content={content}
        editable={editable}
        theme={theme}
        className={className}
        onChange={onChange}
      />
    </Suspense>
  );
}

// 하위 호환성을 위한 타입 (editor prop을 받는 경우)
interface LazyBlockNoteViewWithEditorProps {
  editor: unknown;
  editable?: boolean;
  theme?: "light" | "dark";
  className?: string;
}

// 기존 코드 호환용 - editor를 직접 받는 버전
export function LazyBlockNoteViewWithEditor({
  editor,
  editable = false,
  theme = "light",
  className,
}: LazyBlockNoteViewWithEditorProps) {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <BlockNoteWithEditorInternal
        editor={editor}
        editable={editable}
        theme={theme}
        className={className}
      />
    </Suspense>
  );
}

// editor를 직접 받는 버전의 lazy component
const BlockNoteWithEditorInternal = React.lazy(() =>
  import("./BlockNoteInternal").then((m) => ({ default: m.BlockNoteWithEditor }))
);
