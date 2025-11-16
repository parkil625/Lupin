/**
 * AnimatedBackground.tsx
 *
 * 대시보드 배경 애니메이션 컴포넌트
 * - 회원/의사 모드에 따라 다른 배경 스타일 제공
 * - 그라디언트 원형 요소들이 부드럽게 움직이는 애니메이션
 * - 화면 전체를 덮는 고정 위치 배경
 */

export default function AnimatedBackground({ variant = "member" }: { variant?: "member" | "doctor" }) {
  if (variant === "doctor") {
    return (
      <div className="fixed inset-0 -z-10 bg-gradient-to-br from-purple-100 via-pink-50 to-blue-100">
        <div className="absolute top-20 left-10 w-96 h-96 bg-gradient-to-br from-purple-300 to-pink-300 rounded-full blur-3xl opacity-40 animate-float"></div>
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-gradient-to-br from-blue-300 to-cyan-300 rounded-full blur-3xl opacity-40 animate-float-delayed"></div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 -z-10 bg-gradient-to-br from-purple-100 via-pink-50 to-blue-100">
      <div className="absolute top-20 left-10 w-96 h-96 bg-gradient-to-br from-purple-300 to-pink-300 rounded-full blur-3xl opacity-40 animate-float"></div>
      <div className="absolute bottom-20 right-10 w-96 h-96 bg-gradient-to-br from-blue-300 to-cyan-300 rounded-full blur-3xl opacity-40 animate-float-delayed"></div>
      <div className="absolute top-1/2 left-1/3 w-80 h-80 bg-gradient-to-br from-yellow-200 to-orange-300 rounded-full blur-3xl opacity-30 animate-pulse"></div>
      <div className="absolute bottom-1/3 right-1/4 w-72 h-72 bg-gradient-to-br from-green-200 to-emerald-300 rounded-full blur-3xl opacity-30 animate-float"></div>
      <div className="absolute top-1/3 right-10 w-64 h-64 bg-gradient-to-br from-red-200 to-pink-200 rounded-full blur-3xl opacity-25 animate-float"></div>
      <div className="absolute bottom-10 left-1/2 w-96 h-96 bg-gradient-to-br from-indigo-200 to-purple-200 rounded-full blur-3xl opacity-30 animate-float-delayed"></div>
    </div>
  );
}
