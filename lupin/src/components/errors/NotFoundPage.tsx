import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Home, ArrowLeft } from "lucide-react";

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    // ✅ 전체 화면을 꽉 채우는 컨테이너
    <div className="relative w-full h-screen overflow-hidden flex flex-col items-center justify-center text-center">
      {/* 🖼️ 배경 이미지 (화면 꽉 채우기 & 어둡게 처리) */}
      <div className="absolute inset-0 -z-10">
        <img
          src="/error-404-bg.webp" // 👈 public 폴더에 넣은 파일명!
          alt="Page Not Found"
          className="w-full h-full object-cover" // 화면 비율에 맞춰 꽉 채움
        />
        {/* 글씨 잘 보이게 검은색 필터 한 겹 씌우기 */}
        <div className="absolute inset-0 bg-black/40 backdrop-blur-[2px]" />
      </div>

      {/* 📝 텍스트 & 버튼 (흰색으로 변경) */}
      <div className="relative z-10 space-y-6 px-4 animate-in fade-in zoom-in duration-500">
        <h1 className="text-8xl font-black text-white tracking-tighter drop-shadow-lg">
          404
        </h1>
        <div className="space-y-2">
          <h2 className="text-3xl font-bold text-white drop-shadow-md">
            길을 잃으셨나요?
          </h2>
          <p className="text-gray-200 text-lg max-w-md mx-auto font-medium drop-shadow">
            요청하신 페이지를 찾을 수 없습니다.
            <br />
            주소를 다시 확인하거나 홈으로 돌아가주세요.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 justify-center pt-4">
          <Button
            onClick={() => navigate(-1)}
            variant="outline"
            size="lg"
            className="bg-white/10 border-white/20 text-white hover:bg-white/20 backdrop-blur-md"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            이전 페이지
          </Button>
          <Button
            onClick={() => navigate("/")}
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
