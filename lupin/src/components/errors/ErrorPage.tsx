/**
 * 500 Internal Server Error 페이지
 * 서버 오류 발생 시 표시
 */

import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";

interface ErrorPageProps {
  title?: string;
  message?: string;
}

export default function ErrorPage({
  title = "서버가 오버트레이닝으로 퍼졌어요!",
  message = "루팡이 너무 열정적으로 데이터를 옮기다가 서버에 과부하가 걸린 것 같습니다.",
}: ErrorPageProps) {

  const handleRefresh = () => {
    window.location.reload();
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-200 via-slate-100 to-slate-300 flex items-center justify-center p-4">
      <div className="max-w-lg w-full text-center">
        {/* 루팡이 이미지 - 가장자리 페이드 효과 */}
        <div className="mb-6 flex justify-center">
          <div
            className="relative w-full max-w-md"
            style={{
              maskImage: "radial-gradient(ellipse 70% 70% at center, black 40%, transparent 70%)",
              WebkitMaskImage: "radial-gradient(ellipse 70% 70% at center, black 40%, transparent 70%)",
            }}
          >
            <img
              src="/error-500.png"
              alt="500 - 루팡이가 서버를 고치고 있어요"
              className="w-full"
            />
          </div>
        </div>

        {/* 메시지 */}
        <h1 className="text-2xl sm:text-3xl font-black text-gray-900 mb-3">
          {title}
        </h1>
        <p className="text-gray-600 mb-2">
          {message}
        </p>
        <p className="text-sm text-gray-500 mb-8">
          잠시만 기다려주시면 루팡이 금방 고쳐놓을게요! (아마도요...)
        </p>

        {/* 버튼 */}
        <div className="flex justify-center">
          <Button
            onClick={handleRefresh}
            className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#9A2720] text-white flex items-center gap-2 shadow-lg"
          >
            <RefreshCw className="w-4 h-4" />
            새로고침
          </Button>
        </div>
      </div>
    </div>
  );
}
