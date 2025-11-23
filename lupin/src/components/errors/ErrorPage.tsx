/**
 * 일반 에러 페이지 (500 등)
 */

import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Home, RefreshCw, AlertTriangle } from "lucide-react";

interface ErrorPageProps {
  title?: string;
  message?: string;
  showRefresh?: boolean;
}

export default function ErrorPage({
  title = "오류가 발생했습니다",
  message = "일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.",
  showRefresh = true,
}: ErrorPageProps) {
  const navigate = useNavigate();

  const handleRefresh = () => {
    window.location.reload();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4">
      <div className="max-w-md w-full text-center">
        {/* 에러 아이콘 */}
        <div className="mb-8 flex justify-center">
          <div className="w-24 h-24 rounded-full bg-red-100 flex items-center justify-center">
            <AlertTriangle className="w-12 h-12 text-[#C93831]" />
          </div>
        </div>

        {/* 메시지 */}
        <h2 className="text-2xl font-bold text-gray-900 mb-2">
          {title}
        </h2>
        <p className="text-gray-600 mb-8">
          {message}
        </p>

        {/* 버튼들 */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          {showRefresh && (
            <Button
              onClick={handleRefresh}
              variant="outline"
              className="flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              새로고침
            </Button>
          )}
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
