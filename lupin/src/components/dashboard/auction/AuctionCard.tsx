import { Card } from "@/components/ui/card"; 
import { Badge } from "@/components/ui/badge"; 
import { AuctionItem } from "@/types/auction.types";
import { Gavel, Clock, Trophy, Calendar, Users, Eye } from "lucide-react";
// Badge 컴포넌트가 별도 파일이라면 import 경로를 수정하세요.
// import { Badge } from "@/components/ui/badge"; 

interface AuctionCardProps {
  auction: AuctionItem;
  isSelected: boolean;
  onSelect: (item: AuctionItem) => void;
  countdown?: number;     // 선택된 항목일 때만 전달됨
  isOvertime?: boolean;   // 선택된 항목일 때만 전달됨
}

export const AuctionCard = ({
  auction,
  isSelected,
  onSelect,
  countdown = 0,
  isOvertime = false,
}: AuctionCardProps) => {
  // 날짜 포맷 함수
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}.${month}.${day} ${hours}:${minutes}`;
  };

  // 카운트다운 포맷 함수
  const formatCountdown = (seconds: number) => {
    if (seconds >= 60) {
      const mins = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${mins}:${String(secs).padStart(2, "0")}`;
    }
    return `${seconds}초`;
  };

  // 상태 배지 렌더링
  const getStatusBadge = (status: AuctionItem["status"]) => {
    switch (status) {
      case "ACTIVE":
        return (
          <Badge className="bg-green-500 text-white border-0 font-bold">
            <Gavel className="w-3 h-3 mr-1" />
            진행 중
          </Badge>
        );
      case "SCHEDULED":
        return (
          <Badge className="bg-blue-500 text-white border-0 font-bold">
            <Clock className="w-3 h-3 mr-1" />
            예정
          </Badge>
        );
      case "ENDED":
        return (
          <Badge className="bg-gray-500 text-white border-0 font-bold">
            <Trophy className="w-3 h-3 mr-1" />
            종료
          </Badge>
        );
      default:
        return null;
    }
  };

  const isScheduled = auction.status === "SCHEDULED";

  return (
    <Card
      className={`backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg transition-all 
        ${isSelected ? "ring-2 ring-[#C93831]" : ""}
        ${isScheduled ? "opacity-70 cursor-not-allowed" : "cursor-pointer"}
      `}
      onClick={() => !isScheduled && onSelect(auction)}
    >
      <div className="p-6">
        <div className="flex gap-6">
          {/* Image */}
          <div className="flex-shrink-0 w-32 h-32 sm:w-40 sm:h-40 bg-gray-100 rounded-lg overflow-hidden">
            <img
              src={auction.imageUrl || "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400"}
              alt={auction.itemName}
              className={`w-full h-full object-cover ${isScheduled ? "grayscale" : ""}`}
            />
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between mb-2">
              <div className="flex-1">
                <h3 className="text-xl font-black text-gray-900 mb-1">
                  {auction.itemName}
                </h3>
                <p className="text-sm text-gray-600 font-medium">
                  {auction.description}
                </p>
                <div className="flex items-center gap-2 mt-2 text-xs text-gray-500 font-bold">
                  <Calendar className="w-3 h-3" />
                  {formatDate(auction.startTime)}
                </div>
              </div>
              {getStatusBadge(auction.status)}
            </div>

            {/* 가격 정보 (진행 중일 때만 크게 강조) */}
            {!isScheduled && (
              <div className="grid grid-cols-1 gap-4 mt-4">
                <div>
                  <p className="text-xs text-gray-500 font-bold mb-1">현재가</p>
                  <p className="text-2xl font-black text-[#C93831]">
                    {auction.currentPrice.toLocaleString()}
                    <span className="text-sm ml-1">P</span>
                  </p>
                </div>
              </div>
            )}

            {/* 하단 정보 (선택된 경우 타이머 표시) */}
            <div className="flex items-center gap-4 mt-3">
              {isSelected && auction.status === "ACTIVE" && (
                <div
                  className={`flex items-center gap-1 text-sm font-bold ${
                    isOvertime ? "text-red-600" : "text-gray-600"
                  }`}
                >
                  <Clock className="w-4 h-4" />
                  {isOvertime
                    ? `초읽기 ${countdown}초`
                    : formatCountdown(countdown)}
                </div>
              )}
              
              {!isScheduled && (
                <>
                  <div className="flex items-center gap-1 text-sm font-bold text-gray-600">
                    <Users className="w-4 h-4" />
                    {auction.totalBids}명 입찰
                  </div>
                  <div className="flex items-center gap-1 text-sm font-bold text-orange-600">
                    <Eye className="w-4 h-4" />
                    {auction.viewers}명 시청 중
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
};