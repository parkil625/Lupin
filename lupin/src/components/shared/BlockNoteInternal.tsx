/**
 * BlockNoteInternal.tsx
 *
 * BlockNote 실제 구현 (lazy load 대상)
 * 이 파일은 동적으로 import됨
 */
import { useMemo } from "react";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";

interface BlockNoteInternalProps {
  content?: string;
  editable?: boolean;
  theme?: "light" | "dark";
  className?: string;
  onChange?: (content: string) => void;
}

// content를 JSON으로 파싱하여 에디터 생성
export function BlockNoteInternal({
  content,
  editable = false,
  theme = "light",
  className,
  onChange,
}: BlockNoteInternalProps) {
  const initialContent = useMemo(() => {
    if (!content) return undefined;
    try {
      return JSON.parse(content);
    } catch {
      return [{ type: "paragraph" as const, content: [{ type: "text" as const, text: content, styles: {} }] }];
    }
  }, [content]);

  const editor = useCreateBlockNote({ initialContent });

  return (
    <BlockNoteView
      editor={editor}
      editable={editable}
      theme={theme}
      className={className}
      onChange={onChange ? () => onChange(JSON.stringify(editor.document)) : undefined}
    />
  );
}

// editor를 직접 받는 버전 (하위 호환성)
interface BlockNoteWithEditorProps {
  editor: ReturnType<typeof useCreateBlockNote>;
  editable?: boolean;
  theme?: "light" | "dark";
  className?: string;
  onChange?: () => void;
}

export function BlockNoteWithEditor({
  editor,
  editable = false,
  theme = "light",
  className,
  onChange,
}: BlockNoteWithEditorProps) {
  return (
    <BlockNoteView
      editor={editor}
      editable={editable}
      theme={theme}
      className={className}
      onChange={onChange}
    />
  );
}
