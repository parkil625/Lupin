import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { parseBlockNoteContent, getRelativeTime, cn } from "./utils";

describe("parseBlockNoteContent", () => {
  it("should return empty string for empty content", () => {
    expect(parseBlockNoteContent("")).toBe("");
    expect(parseBlockNoteContent(null as unknown as string)).toBe("");
    expect(parseBlockNoteContent(undefined as unknown as string)).toBe("");
  });

  it("should parse BlockNote JSON with string content", () => {
    const content = JSON.stringify([
      { type: "paragraph", content: "Hello World" },
    ]);
    expect(parseBlockNoteContent(content)).toBe("Hello World");
  });

  it("should parse BlockNote JSON with array content containing text objects", () => {
    const content = JSON.stringify([
      {
        type: "paragraph",
        content: [{ type: "text", text: "Hello " }, { type: "text", text: "World" }],
      },
    ]);
    expect(parseBlockNoteContent(content)).toBe("Hello World");
  });

  it("should join multiple blocks with newlines", () => {
    const content = JSON.stringify([
      { type: "paragraph", content: "First line" },
      { type: "paragraph", content: "Second line" },
    ]);
    expect(parseBlockNoteContent(content)).toBe("First line\nSecond line");
  });

  it("should return original content for non-JSON string", () => {
    const plainText = "This is plain text";
    expect(parseBlockNoteContent(plainText)).toBe(plainText);
  });

  it("should return original content for non-array JSON", () => {
    const content = JSON.stringify({ type: "paragraph", content: "Hello" });
    expect(parseBlockNoteContent(content)).toBe(content);
  });

  it("should handle blocks with empty content", () => {
    const content = JSON.stringify([
      { type: "paragraph", content: [] },
      { type: "paragraph" },
    ]);
    expect(parseBlockNoteContent(content)).toBe("\n");
  });
});

describe("getRelativeTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2025-01-15T12:00:00Z"));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should return "방금 전" for times less than 60 seconds ago', () => {
    const date = new Date("2025-01-15T11:59:30Z");
    expect(getRelativeTime(date)).toBe("방금 전");
  });

  it('should return "X분 전" for times less than 60 minutes ago', () => {
    const date = new Date("2025-01-15T11:55:00Z");
    expect(getRelativeTime(date)).toBe("5분 전");
  });

  it('should return "X시간 전" for times less than 24 hours ago', () => {
    const date = new Date("2025-01-15T09:00:00Z");
    expect(getRelativeTime(date)).toBe("3시간 전");
  });

  it('should return "X일 전" for times less than 7 days ago', () => {
    const date = new Date("2025-01-12T12:00:00Z");
    expect(getRelativeTime(date)).toBe("3일 전");
  });

  it('should return "X주 전" for times less than 4 weeks ago', () => {
    const date = new Date("2025-01-01T12:00:00Z");
    expect(getRelativeTime(date)).toBe("2주 전");
  });

  it('should return "X개월 전" for times less than 12 months ago', () => {
    const date = new Date("2024-11-15T12:00:00Z");
    expect(getRelativeTime(date)).toBe("2개월 전");
  });

  it('should return "X년 전" for times more than 12 months ago', () => {
    const date = new Date("2023-01-15T12:00:00Z");
    expect(getRelativeTime(date)).toBe("2년 전");
  });

  it("should accept string date format", () => {
    expect(getRelativeTime("2025-01-15T11:55:00Z")).toBe("5분 전");
  });
});

describe("cn", () => {
  it("should merge class names", () => {
    expect(cn("foo", "bar")).toBe("foo bar");
  });

  it("should handle conditional classes", () => {
    expect(cn("foo", false && "bar", "baz")).toBe("foo baz");
  });

  it("should merge tailwind classes correctly", () => {
    expect(cn("p-4", "p-2")).toBe("p-2");
    expect(cn("text-red-500", "text-blue-500")).toBe("text-blue-500");
  });
});
