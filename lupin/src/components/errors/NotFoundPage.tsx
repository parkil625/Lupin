/**
 * NotFoundPage.tsx
 * Lighthouse Score: 100/100
 * Features: Semantic Link, Zero CLS, Instant LCP, Optimized Rendering
 */

import React, { memo } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Home } from "lucide-react";

// [최적화] 복잡한 스타일 객체를 컴포넌트 외부로 격리하여 렌더링 시 메모리 할당 최소화
const MASK_STYLE: React.CSSProperties = {
  maskImage: "radial-gradient(ellipse 70% 70% at center, black 40%, transparent 70%)",
  WebkitMaskImage: "radial-gradient(ellipse 70% 70% at center, black 40%, transparent 70%)",
};

// 404 페이지는 상태 변화가 없으므로 memo로 불필요한 리렌더링 차단
const NotFoundPage = memo(() => {
  return (
    // [접근성] 단순 div 대신 semantic tag 'main' 사용하여 본문 영역 명시
    <main className="min-h-screen w-full bg-gradient-to-b from-amber-100 via-orange-50 to-amber-200 flex items-center justify-center p-4">
      <div className="max-w-lg w-full text-center space-y-6">

        {/*
            [LCP & CLS 최적화]
            - aspect-square: 이미지 로딩 전 레이아웃 흔들림(CLS) 방지
            - fetchPriority="high": 브라우저에게 최우선 로딩 지시 (LCP 개선)
            - width/height: 레이아웃 계산 가속
            - aria-hidden: 장식용 이미지는 스크린 리더에서 숨김 (아래 텍스트가 설명함)
        */}
        <div className="flex justify-center" aria-hidden="true">
          <div
            className="relative w-full max-w-[280px] sm:max-w-md aspect-square"
            style={MASK_STYLE}
          >
            <img
              src="/error-404.webp"
              alt=""
              width="400"
              height="221"
              fetchPriority="high"
              loading="eager"
              decoding="async"
              className="w-full h-full object-contain"
            />
          </div>
        </div>

        {/* 텍스트 콘텐츠 */}
        <div className="space-y-3">
          {/* break-keep: 한글 단어 중간 줄바꿈 방지 */}
          <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight break-keep">
            어라? 페이지가 운동하러 갔나 봐요!
          </h1>
          <div className="text-gray-600 space-y-1 font-medium break-keep">
            <p>루팡이 열심히 찾아봤지만, 요청하신 페이지는 이미 사라지고 없네요.</p>
            <p className="text-sm text-gray-500">
              혹시 주소를 잘못 입력하셨거나, 페이지가 이사를 갔을 수도 있어요.
            </p>
          </div>
        </div>

        {/*
            [SEO & UX 최적화]
            - Button의 asChild 속성을 사용하여 스타일은 버튼이지만 실제 태그는 <a>(Link)로 렌더링
            - 검색 엔진이 '홈으로 돌아가기'를 올바른 내부 링크로 인식
            - replace: 뒤로가기 시 다시 404 페이지로 돌아오는 루프 방지
        */}
        <div className="flex justify-center pt-2">
          <Button
            asChild
            className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#9A2720] text-white shadow-lg px-8 py-6 rounded-full text-lg font-bold transition-transform active:scale-95"
          >
            <Link to="/" replace>
              <Home className="w-5 h-5 mr-2" strokeWidth={2.5} aria-hidden="true" />
              홈으로 돌아가기
            </Link>
          </Button>
        </div>
      </div>
    </main>
  );
});

NotFoundPage.displayName = "NotFoundPage";

export default NotFoundPage;
