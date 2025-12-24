import { Button } from "@/components/ui/button";
import { Home, RefreshCw } from "lucide-react";

export default function ErrorPage() {
  return (
    // ✅ 전체 화면을 꽉 채우는 컨테이너
    <div className="relative w-full h-screen overflow-hidden flex flex-col items-center justify-center text-center">
      {/* 🖼️ 배경 이미지 */}
      <div className="absolute inset-0 -z-10">
        <img
          src="/error-500-bg.webp" // 👈 public 폴더에 넣은 파일명!
          alt="Server Error"
          className="w-full h-full object-cover"
        />
        {/* 검은색 필터 */}
        <div className="absolute inset-0 bg-black/50 backdrop-blur-[2px]" />
      </div>

      {/* 📝 텍스트 & 버튼 */}
      <div className="relative z-10 space-y-6 px-4 animate-in fade-in zoom-in duration-500">
        <h1 className="text-8xl font-black text-white tracking-tighter drop-shadow-lg">
          500
        </h1>
        <div className="space-y-2">
          <h2 className="text-3xl font-bold text-white drop-shadow-md">
            시스템 오류가 발생했어요
          </h2>
          <p className="text-gray-200 text-lg max-w-md mx-auto font-medium drop-shadow">
            서버에 잠시 문제가 생겼습니다.
            <br />
            잠시 후 다시 시도해 주세요.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 justify-center pt-4">
          <Button
            onClick={() => window.location.reload()}
            variant="outline"
            size="lg"
            className="bg-white/10 border-white/20 text-white hover:bg-white/20 backdrop-blur-md"
          >
            <RefreshCw className="mr-2 h-4 w-4" />
            새로고침
          </Button>
          <Button
            onClick={() => (window.location.href = "/")}
            size="lg"
            className="bg-[#C93831] hover:bg-[#A62B25] text-white border-none shadow-lg"
          >
            <Home className="mr-2 h-4 w-4" />
            홈으로 가기
          </Button>
        </div>
      </div>
    </div>
  );
}
