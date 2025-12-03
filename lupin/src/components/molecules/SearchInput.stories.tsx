import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import SearchInput from './SearchInput';

/**
 * SearchInput 컴포넌트는 검색 기능을 제공하는 molecule 컴포넌트입니다.
 * 검색 아이콘, Clear 버튼, 자동완성 제안 기능을 포함합니다.
 *
 * ## 사용 방법
 * ```tsx
 * import SearchInput from '@/components/molecules/SearchInput';
 *
 * const [query, setQuery] = useState('');
 * <SearchInput
 *   value={query}
 *   onChange={setQuery}
 *   placeholder="검색..."
 * />
 * ```
 */
const meta = {
  title: 'Molecules/SearchInput',
  component: SearchInput,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    value: {
      control: 'text',
      description: '검색어',
    },
    placeholder: {
      control: 'text',
      description: 'Placeholder 텍스트',
    },
  },
  decorators: [
    (Story) => (
      <div style={{ width: '500px' }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof SearchInput>;

export default meta;
type Story = StoryObj<typeof meta>;

/**
 * 기본 SearchInput
 */
export const Default: Story = {
  args: {
    value: '',
    onChange: () => {},
    placeholder: '검색...',
  },
  render: () => {
    const [value, setValue] = useState('');
    return (
      <SearchInput
        value={value}
        onChange={setValue}
        placeholder="검색..."
      />
    );
  },
};

/**
 * 초기값이 있는 SearchInput
 */
export const WithInitialValue: Story = {
  args: {
    value: '러닝',
    onChange: () => {},
    placeholder: '운동 검색',
  },
  render: () => {
    const [value, setValue] = useState('러닝');
    return (
      <SearchInput
        value={value}
        onChange={setValue}
        placeholder="운동 검색"
      />
    );
  },
};

/**
 * 자동완성 제안 기능
 */
export const WithSuggestions: Story = {
  args: {
    value: '',
    onChange: () => {},
    placeholder: '운동 종류를 검색하세요',
  },
  render: () => {
    const [value, setValue] = useState('');
    const workoutSuggestions = [
      '러닝',
      '수영',
      '사이클',
      '헬스',
      '요가',
      '필라테스',
      '크로스핏',
      '등산',
      '테니스',
      '배드민턴',
    ];

    return (
      <SearchInput
        value={value}
        onChange={setValue}
        placeholder="운동 종류를 검색하세요"
        suggestions={workoutSuggestions}
      />
    );
  },
};

/**
 * 사용자 검색 예시
 */
export const UserSearch: Story = {
  args: {
    value: '',
    onChange: () => {},
    placeholder: '이름 또는 이메일로 검색',
  },
  render: () => {
    const [value, setValue] = useState('');
    const userSuggestions = [
      '김직원 (user01@company.com)',
      '박의사 (doctor01@company.com)',
      '이간호사 (nurse01@company.com)',
      '최관리자 (admin01@company.com)',
    ];

    return (
      <div className="space-y-2">
        <label className="text-sm font-medium">사용자 검색</label>
        <SearchInput
          value={value}
          onChange={setValue}
          placeholder="이름 또는 이메일로 검색"
          suggestions={userSuggestions}
        />
      </div>
    );
  },
};

/**
 * 진료 기록 검색 예시
 */
export const MedicalRecordSearch: Story = {
  args: {
    value: '',
    onChange: () => {},
    placeholder: '증상을 입력하세요',
  },
  render: () => {
    const [value, setValue] = useState('');
    const medicalSuggestions = [
      '고혈압',
      '당뇨',
      '감기',
      '두통',
      '복통',
      '알레르기',
      '피부염',
      '관절염',
    ];

    return (
      <div className="space-y-2">
        <label className="text-sm font-medium">증상 검색</label>
        <SearchInput
          value={value}
          onChange={setValue}
          placeholder="증상을 입력하세요"
          suggestions={medicalSuggestions}
        />
      </div>
    );
  },
};

/**
 * 피드 검색 예시
 */
export const FeedSearch: Story = {
  args: {
    value: '',
    onChange: () => {},
    placeholder: '피드 검색 (해시태그, 내용)',
  },
  render: () => {
    const [value, setValue] = useState('');

    return (
      <div className="w-full max-w-2xl p-6 bg-gray-50 rounded-lg">
        <SearchInput
          value={value}
          onChange={setValue}
          placeholder="피드 검색 (해시태그, 내용)"
          className="w-full"
        />

        {value && (
          <div className="mt-4 text-sm text-gray-600">
            &quot;{value}&quot; 검색 결과...
          </div>
        )}
      </div>
    );
  },
};

/**
 * 다양한 placeholder
 */
export const DifferentPlaceholders: Story = {
  args: {
    value: '',
    onChange: () => {},
    placeholder: '검색...',
  },
  render: () => {
    const [value1, setValue1] = useState('');
    const [value2, setValue2] = useState('');
    const [value3, setValue3] = useState('');

    return (
      <div className="space-y-4 w-full">
        <SearchInput
          value={value1}
          onChange={setValue1}
          placeholder="운동 종류 검색..."
        />
        <SearchInput
          value={value2}
          onChange={setValue2}
          placeholder="사용자 이름 검색..."
        />
        <SearchInput
          value={value3}
          onChange={setValue3}
          placeholder="태그 검색..."
        />
      </div>
    );
  },
};
