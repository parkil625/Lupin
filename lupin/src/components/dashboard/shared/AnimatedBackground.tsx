/**
 * AnimatedBackground.tsx
 *
 * 대시보드 배경 컴포넌트 - 미니멀 그라데이션
 * - 회원/의사 모드에 따라 다른 배경 스타일 제공
 * - 부드러운 메시 그라데이션
 * - 깔끔하고 심플한 디자인
 */

export default function AnimatedBackground({ variant = "member" }: { variant?: "member" | "doctor" }) {
  if (variant === "doctor") {
    return (
      <div className="fixed inset-0 -z-10">
        {/* 의사 모드 - 은은한 베이스 그라데이션 */}
        <div className="absolute inset-0 bg-gradient-to-br from-slate-100 via-blue-100 to-indigo-100"></div>

        {/* 부드러운 메시 그라데이션 효과 */}
        <div className="absolute inset-0">
          <div className="absolute top-0 left-0 w-[500px] h-[500px] bg-blue-200/70 rounded-full filter blur-[100px] animate-blob"></div>
          <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-purple-200/70 rounded-full filter blur-[100px] animate-blob animation-delay-2000"></div>
          <div className="absolute bottom-0 left-1/2 w-[500px] h-[500px] bg-indigo-200/70 rounded-full filter blur-[100px] animate-blob animation-delay-4000"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 -z-10">
      {/* 회원 모드 - 은은한 베이스 그라데이션 */}
      <div className="absolute inset-0 bg-gradient-to-br from-purple-100 via-blue-100 to-pink-100"></div>

      {/* 부드러운 메시 그라데이션 효과 */}
      <div className="absolute inset-0">
        <div className="absolute top-0 left-0 w-[600px] h-[600px] bg-purple-200/70 rounded-full filter blur-[100px] animate-blob"></div>
        <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-blue-200/70 rounded-full filter blur-[100px] animate-blob animation-delay-2000"></div>
        <div className="absolute bottom-0 left-1/2 w-[600px] h-[600px] bg-pink-200/70 rounded-full filter blur-[100px] animate-blob animation-delay-4000"></div>
      </div>
    </div>
  );
}
