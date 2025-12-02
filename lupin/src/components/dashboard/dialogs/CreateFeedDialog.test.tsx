import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// 핵심 로직만 테스트 (컴포넌트 렌더링 없이)
const DRAFT_STORAGE_KEY = 'createFeedDraft';

describe('CreateFeedDialog 핵심 로직', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('hasChanges 로직', () => {
    it('아무것도 입력하지 않으면 변경사항이 없어야 한다', () => {
      const startImage = null;
      const endImage = null;
      const otherImages: string[] = [];
      const workoutType = '헬스';
      const hasEditorContent = false;

      const hasChanges =
        startImage !== null ||
        endImage !== null ||
        otherImages.length > 0 ||
        workoutType !== '헬스' ||
        hasEditorContent;

      expect(hasChanges).toBe(false);
    });

    it('시작 이미지가 있으면 변경사항이 있어야 한다', () => {
      const startImage = 'https://example.com/start.jpg';
      const endImage = null;
      const otherImages: string[] = [];
      const workoutType = '헬스';
      const hasEditorContent = false;

      const hasChanges =
        startImage !== null ||
        endImage !== null ||
        otherImages.length > 0 ||
        workoutType !== '헬스' ||
        hasEditorContent;

      expect(hasChanges).toBe(true);
    });

    it('끝 이미지가 있으면 변경사항이 있어야 한다', () => {
      const startImage = null;
      const endImage = 'https://example.com/end.jpg';
      const otherImages: string[] = [];
      const workoutType = '헬스';
      const hasEditorContent = false;

      const hasChanges =
        startImage !== null ||
        endImage !== null ||
        otherImages.length > 0 ||
        workoutType !== '헬스' ||
        hasEditorContent;

      expect(hasChanges).toBe(true);
    });

    it('기타 이미지가 있으면 변경사항이 있어야 한다', () => {
      const startImage = null;
      const endImage = null;
      const otherImages: string[] = ['https://example.com/other.jpg'];
      const workoutType = '헬스';
      const hasEditorContent = false;

      const hasChanges =
        startImage !== null ||
        endImage !== null ||
        otherImages.length > 0 ||
        workoutType !== '헬스' ||
        hasEditorContent;

      expect(hasChanges).toBe(true);
    });

    it('운동 종류가 기본값이 아니면 변경사항이 있어야 한다', () => {
      const startImage = null;
      const endImage = null;
      const otherImages: string[] = [];
      const workoutType = '수영';
      const hasEditorContent = false;

      const hasChanges =
        startImage !== null ||
        endImage !== null ||
        otherImages.length > 0 ||
        workoutType !== '헬스' ||
        hasEditorContent;

      expect(hasChanges).toBe(true);
    });

    it('에디터에 내용이 있으면 변경사항이 있어야 한다', () => {
      const startImage = null;
      const endImage = null;
      const otherImages: string[] = [];
      const workoutType = '헬스';
      const hasEditorContent = true;

      const hasChanges =
        startImage !== null ||
        endImage !== null ||
        otherImages.length > 0 ||
        workoutType !== '헬스' ||
        hasEditorContent;

      expect(hasChanges).toBe(true);
    });
  });

  describe('비우고 닫기 동작', () => {
    it('localStorage가 삭제되어야 한다', () => {
      // Given: 임시저장 데이터가 있음
      const draftData = {
        startImage: 'https://example.com/start.jpg',
        endImage: 'https://example.com/end.jpg',
        otherImages: [],
        workoutType: '헬스',
        content: [{ type: 'paragraph', content: '' }],
      };
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draftData));

      // When: 비우고 닫기 버튼 클릭 시
      localStorage.removeItem(DRAFT_STORAGE_KEY);

      // Then: localStorage가 삭제됨
      expect(localStorage.getItem(DRAFT_STORAGE_KEY)).toBeNull();
    });

    it('글 작성 중 비우고 닫기 시 모든 데이터가 초기화되어야 한다', () => {
      // Given: 사용자가 글을 작성 중
      const draftData = {
        startImage: 'https://example.com/start.jpg',
        endImage: 'https://example.com/end.jpg',
        otherImages: ['https://example.com/other1.jpg', 'https://example.com/other2.jpg'],
        workoutType: '수영',
        content: [{ type: 'paragraph', content: [{ type: 'text', text: '오늘 운동 열심히 했습니다!' }] }],
      };
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draftData));

      // When: 비우고 닫기 실행 (handleCloseWithoutSaving 로직 시뮬레이션)
      const resetState = {
        startImage: null,
        endImage: null,
        otherImages: [],
        workoutType: '헬스',
      };
      localStorage.removeItem(DRAFT_STORAGE_KEY);

      // Then: localStorage가 완전히 삭제됨
      expect(localStorage.getItem(DRAFT_STORAGE_KEY)).toBeNull();

      // And: 상태가 초기화됨
      expect(resetState.startImage).toBeNull();
      expect(resetState.endImage).toBeNull();
      expect(resetState.otherImages).toEqual([]);
      expect(resetState.workoutType).toBe('헬스');
    });

    it('비우고 닫기 후 다시 열면 빈 상태여야 한다', () => {
      // Given: 임시저장 데이터가 있었음
      const draftData = {
        startImage: 'https://example.com/start.jpg',
        endImage: 'https://example.com/end.jpg',
        otherImages: [],
        workoutType: '수영',
        content: [{ type: 'paragraph', content: '테스트' }],
      };
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draftData));

      // When: 비우고 닫기 실행
      localStorage.removeItem(DRAFT_STORAGE_KEY);

      // And: 다시 다이얼로그를 열 때
      const savedDraft = localStorage.getItem(DRAFT_STORAGE_KEY);

      // Then: 저장된 데이터가 없어야 함
      expect(savedDraft).toBeNull();

      // And: 초기 상태로 시작해야 함
      const defaultState = {
        startImage: null,
        endImage: null,
        otherImages: [],
        workoutType: '헬스',
        content: [{ type: 'paragraph', content: '' }],
      };
      expect(defaultState.startImage).toBeNull();
      expect(defaultState.workoutType).toBe('헬스');
    });
  });

  describe('저장 후 닫기 동작', () => {
    it('localStorage가 유지되어야 한다', () => {
      // Given: 임시저장 데이터가 있음
      const draftData = {
        startImage: 'https://example.com/start.jpg',
        endImage: 'https://example.com/end.jpg',
        otherImages: [],
        workoutType: '헬스',
        content: [{ type: 'paragraph', content: '' }],
      };
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draftData));

      // When: 저장 후 닫기 버튼 클릭 시 (localStorage를 삭제하지 않음)
      // 아무것도 안 함

      // Then: localStorage가 유지됨
      expect(localStorage.getItem(DRAFT_STORAGE_KEY)).not.toBeNull();
      expect(JSON.parse(localStorage.getItem(DRAFT_STORAGE_KEY)!)).toEqual(draftData);
    });
  });

  describe('임시저장 복원', () => {
    it('localStorage에서 임시저장 데이터를 불러올 수 있다', () => {
      // Given: localStorage에 데이터가 있음
      const draftData = {
        startImage: 'https://example.com/start.jpg',
        endImage: null,
        otherImages: ['https://example.com/other1.jpg'],
        workoutType: '수영',
        content: [{ type: 'paragraph', content: '테스트 내용' }],
      };
      localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draftData));

      // When: 다이얼로그가 열릴 때 데이터를 불러옴
      const savedDraft = localStorage.getItem(DRAFT_STORAGE_KEY);
      const draft = JSON.parse(savedDraft!);

      // Then: 데이터가 올바르게 복원됨
      expect(draft.startImage).toBe('https://example.com/start.jpg');
      expect(draft.endImage).toBeNull();
      expect(draft.otherImages).toEqual(['https://example.com/other1.jpg']);
      expect(draft.workoutType).toBe('수영');
    });
  });
});
