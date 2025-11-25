# 📚 Storybook 설정 가이드

## ✅ 완료된 작업

1. **Storybook 설치 완료**
   - Storybook v10.0.8 설치 및 실행 확인
   - 로컬: http://localhost:6006/
   - 네트워크: http://172.25.80.1:6006/

2. **Storybook 설정 파일 생성**
   - `.storybook/main.ts` - Storybook 메인 설정
   - `.storybook/preview.tsx` - 전역 데코레이터 및 파라미터 (JSX 지원)

3. **예제 스토리 생성**
   - `src/components/ui/button.stories.tsx` - Button 컴포넌트 스토리 (13개 변형)
   - `src/components/ui/card.stories.tsx` - Card 컴포넌트 스토리 (7개 예제)
   - `src/components/ui/input.stories.tsx` - Input 컴포넌트 스토리 (13개 타입)

---

## 🚀 Storybook 설치 및 실행

### ✅ Storybook 이미 설치됨
Storybook v10.0.8이 이미 설치되어 실행 중입니다.

### Storybook 실행
```bash
npm run storybook
```

브라우저가 자동으로 열리면서 http://localhost:6006 에서 Storybook이 실행됩니다.

### 3단계: 확인
좌측 사이드바에서 다음 스토리들을 확인하세요:
- **UI > Button** - 버튼 컴포넌트의 다양한 변형
- **UI > Card** - 카드 컴포넌트 예제
- **UI > Input** - 인풋 컴포넌트 타입별 예제

---

## 📝 Storybook 스토리 작성 가이드

### 기본 스토리 구조

```tsx
import type { Meta, StoryObj } from '@storybook/react';
import { YourComponent } from './YourComponent';

const meta = {
  title: 'Category/ComponentName',
  component: YourComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    // prop 타입 정의
  },
} satisfies Meta<typeof YourComponent>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    // 기본 props
  },
};
```

### 스토리 작성 예제

#### 1. 간단한 스토리
```tsx
export const Primary: Story = {
  args: {
    variant: 'primary',
    children: 'Click me',
  },
};
```

#### 2. 커스텀 렌더링
```tsx
export const WithIcon: Story = {
  render: () => (
    <Button>
      <Mail />
      Send Email
    </Button>
  ),
};
```

#### 3. 복합 스토리 (여러 컴포넌트)
```tsx
export const LoginForm: Story = {
  render: () => (
    <div className="space-y-4">
      <Input type="email" placeholder="Email" />
      <Input type="password" placeholder="Password" />
      <Button>Login</Button>
    </div>
  ),
};
```

---

## 🎨 더 많은 컴포넌트 스토리 추가하기

### 추가할 수 있는 컴포넌트들

프로젝트에 있는 다른 UI 컴포넌트들도 스토리를 추가할 수 있습니다:

```bash
src/components/ui/
├── accordion.tsx         → accordion.stories.tsx
├── alert.tsx             → alert.stories.tsx
├── avatar.tsx            → avatar.stories.tsx
├── badge.tsx             → badge.stories.tsx
├── checkbox.tsx          → checkbox.stories.tsx
├── dialog.tsx            → dialog.stories.tsx
├── dropdown-menu.tsx     → dropdown-menu.stories.tsx
├── select.tsx            → select.stories.tsx
├── switch.tsx            → switch.stories.tsx
├── tabs.tsx              → tabs.stories.tsx
└── tooltip.tsx           → tooltip.stories.tsx
```

### 스토리 파일 명명 규칙

- **파일명**: `ComponentName.stories.tsx`
- **위치**: 컴포넌트와 같은 폴더
- **title**: `'Category/ComponentName'`

---

## 🛠️ Storybook Addons

### 현재 설치된 Addons

1. **@storybook/addon-essentials** - 필수 애드온 번들
   - Docs: 자동 문서 생성
   - Controls: Props 동적 조작
   - Actions: 이벤트 핸들러 로깅
   - Viewport: 반응형 테스트
   - Backgrounds: 배경 색상 변경

2. **@storybook/addon-interactions** - 인터랙션 테스트

3. **@storybook/addon-a11y** - 접근성 테스트

### 추천 Addons (선택 사항)

```bash
# Figma 디자인과 비교
npm install -D @storybook/addon-designs

# 스토리북 성능 측정
npm install -D @storybook/addon-performance

# 다크모드 토글
npm install -D storybook-dark-mode
```

---

## 📐 현재 설정 요약

### `.storybook/main.ts`
- Vite 기반 React Storybook
- `@` alias 설정 (src 폴더)
- 모든 `.stories.tsx` 파일 자동 인식

### `.storybook/preview.ts`
- Tailwind CSS 자동 import
- 전역 padding 데코레이터
- 배경색 프리셋 (light, dark, gray)

---

## 💡 유용한 Storybook 명령어

```bash
# 개발 서버 시작
npm run storybook

# 정적 빌드 (배포용)
npm run build-storybook

# 빌드된 Storybook 미리보기
npx http-server storybook-static
```

---

## 🎯 다음 단계

1. **더 많은 UI 컴포넌트 스토리 작성**
   - Badge, Avatar, Dialog 등

2. **비즈니스 컴포넌트 스토리 작성**
   - `src/components/molecules/` 폴더의 컴포넌트들
   - SearchInput, WorkoutTypeSelect, ImageUploadBox 등

3. **복합 페이지 스토리 작성**
   - `src/components/dashboard/` 폴더의 페이지 컴포넌트들

4. **인터랙션 테스트 추가**
   - `@storybook/test` 사용하여 사용자 인터랙션 테스트

---

## 📚 참고 자료

- [Storybook 공식 문서](https://storybook.js.org/)
- [Storybook for React](https://storybook.js.org/docs/react/get-started/introduction)
- [Component Story Format (CSF)](https://storybook.js.org/docs/react/api/csf)
- [Storybook Addons](https://storybook.js.org/addons)

---

---

## 🎉 완료!

Storybook이 성공적으로 설치 및 실행되었습니다!

### 현재 사용 가능한 스토리
- **Button** (13개 변형) - 기본, Primary, Secondary, Outline, Ghost, Destructive, 아이콘 등
- **Card** (7개 예제) - 기본, 헤더/푸터, 로그인 폼, 프로필 카드 등
- **Input** (13개 타입) - Text, Email, Password, Search, Number, Date 등

### 다음 단계
추가 컴포넌트 스토리를 작성하고 싶다면 위 가이드를 참고하세요!

### 트러블슈팅
만약 에러가 발생한다면:
1. **포트 충돌**: `npm run storybook`이 이미 실행 중인지 확인 (포트 6006)
2. **캐시 문제**: Storybook 재시작 (`Ctrl+C` 후 `npm run storybook`)
3. **JSX 에러**: `.storybook/preview.tsx` 파일 확장자가 `.tsx`인지 확인
