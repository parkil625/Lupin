/**
 * rankingConstants.ts
 *
 * 랭킹 페이지 관련 상수 정의
 * - 매직 넘버/컬러 제거
 * - 유지보수성 향상
 */

export const RANKING_CONSTANTS = {
  TOP_RANKERS_COUNT: 10,
  HOVER_CARD_DELAY: {
    OPEN: 200,
    CLOSE: 100,
  },
} as const;

export const RANK_STYLES = {
  1: {
    crown: {
      color: "#FFD700", // Gold
      fill: "#FFD700",
      size: "w-8 h-8",
    },
    label: "1위",
  },
  2: {
    crown: {
      color: "#C0C0C0", // Silver
      fill: "#C0C0C0",
      size: "w-7 h-7",
    },
    label: "2위",
  },
  3: {
    crown: {
      color: "#CD7F32", // Bronze
      fill: "#CD7F32",
      size: "w-7 h-7",
    },
    label: "3위",
  },
} as const;

export const THEME_COLORS = {
  PRIMARY: "#C93831",
  PRIMARY_HOVER: "#B02F28",
  PRIMARY_LIGHT_ALPHA_15: "rgba(201, 56, 49, 0.15)",
  PRIMARY_LIGHT_ALPHA_80: "rgba(255, 245, 245, 0.8)",
} as const;

export const DEFAULT_VALUES = {
  NAME: "이름 없음",
  DEPARTMENT: "부서 미정",
  AVATAR_FALLBACK: "?",
} as const;
