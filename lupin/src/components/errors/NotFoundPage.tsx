/**
 * 404 Not Found 페이지
 * 페이지를 찾을 수 없을 때 표시
 */

import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Home } from "lucide-react";

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gradient-to-b from-amber-100 via-orange-50 to-amber-200 flex items-center justify-center p-4">
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
              src="/error-404.png"
              alt="404 - 루팡이가 페이지를 찾고 있어요"
              className="w-full"
            />
          </div>
        </div>

        {/* 메시지 */}
        <h1 className="text-2xl sm:text-3xl font-black text-gray-900 mb-3">
          어라? 페이지가 운동하러 갔나 봐요!
        </h1>
        <p className="text-gray-600 mb-2">
          루팡이 열심히 찾아봤지만, 요청하신 페이지는 이미 사라지고 없네요.
        </p>
        <p className="text-sm text-gray-500 mb-8">
          혹시 주소를 잘못 입력하셨거나, 페이지가 이사를 갔을 수도 있어요.
        </p>

        {/* 버튼 */}
        <div className="flex justify-center">
          <Button
            onClick={() => navigate("/")}
            className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#9A2720] text-white flex items-center gap-2 shadow-lg"
          >
            <Home className="w-4 h-4" />
            홈으로 돌아가기
          </Button>
        </div>
      </div>
    </div>
  );
}
