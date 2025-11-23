/**
 * 404 Not Found 페이지
 */

import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Home, ArrowLeft, Search } from "lucide-react";

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4">
      <div className="max-w-md w-full text-center">
        {/* 404 숫자 */}
        <div className="relative mb-8">
          <h1 className="text-[150px] font-black text-gray-200 leading-none select-none">
            404
          </h1>
          <div className="absolute inset-0 flex items-center justify-center">
            <Search className="w-20 h-20 text-[#C93831] opacity-80" />
          </div>
        </div>

        {/* 메시지 */}
        <h2 className="text-2xl font-bold text-gray-900 mb-2">
          페이지를 찾을 수 없습니다
        </h2>
        <p className="text-gray-600 mb-8">
          요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.
          <br />
          URL을 다시 확인해주세요.
        </p>

        {/* 버튼들 */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Button
            onClick={() => navigate(-1)}
            variant="outline"
            className="flex items-center gap-2"
          >
            <ArrowLeft className="w-4 h-4" />
            이전 페이지
          </Button>
          <Button
            onClick={() => navigate("/")}
            className="bg-[#C93831] hover:bg-[#B02F28] text-white flex items-center gap-2"
          >
            <Home className="w-4 h-4" />
            홈으로 가기
          </Button>
        </div>
      </div>
    </div>
  );
}
