import { describe, it, expect } from 'vitest';
import { isPastDate, formatDateToString, formatDateTime } from './utils';
import { AVAILABLE_TIMES, DEPARTMENT_NAMES, STATUS_CONFIG } from './constants';

describe('Medical 컴포넌트 퍼포먼스 최적화', () => {
  describe('상수 분리 최적화', () => {
    it('예약 가능 시간이 상수로 분리되어 있어야 한다', () => {
      expect(AVAILABLE_TIMES).toBeDefined();
      expect(AVAILABLE_TIMES.length).toBeGreaterThan(0);
      expect(AVAILABLE_TIMES).toContain('09:00');
    });

    it('진료과 매핑이 상수로 분리되어 있어야 한다', () => {
      expect(DEPARTMENT_NAMES).toBeDefined();
      expect(DEPARTMENT_NAMES['internal']).toBe('내과');
    });

    it('상태 설정이 상수로 분리되어 있어야 한다', () => {
      expect(STATUS_CONFIG).toBeDefined();
      expect(STATUS_CONFIG.SCHEDULED).toBeDefined();
      expect(STATUS_CONFIG.SCHEDULED.label).toBe('진료 예정');
    });
  });

  describe('유틸리티 함수 분리', () => {
    it('isPastDate 함수가 정상 작동해야 한다', () => {
      const pastDate = new Date('2020-01-01');
      const futureDate = new Date('2099-12-31');

      expect(isPastDate(pastDate)).toBe(true);
      expect(isPastDate(futureDate)).toBe(false);
    });

    it('formatDateToString 함수가 정상 작동해야 한다', () => {
      const date = new Date('2024-03-15');
      const formatted = formatDateToString(date);

      expect(formatted).toMatch(/^\d{4}-\d{2}-\d{2}$/);
      expect(formatted).toContain('2024');
    });

    it('formatDateTime 함수가 정상 작동해야 한다', () => {
      const date = new Date('2024-03-15');
      const time = '14:30';
      const formatted = formatDateTime(date, time);

      expect(formatted).toContain('2024');
      expect(formatted).toContain('14:30');
      expect(formatted).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
    });
  });
});
