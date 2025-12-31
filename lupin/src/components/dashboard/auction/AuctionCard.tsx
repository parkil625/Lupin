import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { AuctionData } from "@/types/auction.types";
import { Gavel, Clock, Trophy, Calendar, Users, Award, Coins } from "lucide-react";
import { AuctionTimer } from "@/components/dashboard/auction/AuctionTimer"

interface AuctionCardProps {
    auction: AuctionData;
    isSelected?: boolean; // 선택 안 되는 경우도 있으므로 optional로 변경 권장
    onSelect?: (item: AuctionData) => void;
    onTimeEnd?: () => void;
    winnerName?: string;
    winningBid?: number;
    status?: 'ACTIVE' | 'SCHEDULED' | 'ENDED';
}

export const AuctionCard = ({
                                auction,
                                isSelected = false,
                                onSelect,
                                onTimeEnd,
                                winnerName,
                                winningBid,
                                status,
                            }: AuctionCardProps) => {

    // 1. props로 받은 status가 있으면 그걸 쓰고, 없으면 데이터 내부 status 사용
    const currentStatus = status || auction.status;
    const isEnded = currentStatus === "ENDED";
    const isScheduled = currentStatus === "SCHEDULED";
    const isActive = currentStatus === "ACTIVE";

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

    // 상태 배지 렌더링
    const getStatusBadge = () => {
        switch (currentStatus) {
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

    return (
        <Card
            className={`backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg transition-all 
        ${isSelected ? "ring-2 ring-[#C93831]" : ""}
        ${isScheduled ? "opacity-70 cursor-not-allowed" : "cursor-pointer"}
        ${isEnded ? "hover:shadow-md" : "hover:shadow-xl"} 
      `}
            onClick={() => {
                // 예정된 경매가 아니고, onSelect 함수가 있을 때만 실행
                if (!isScheduled && onSelect) {
                    onSelect(auction);
                }
            }}
        >
            <div className="p-6">
                <div className="flex gap-6">
                    {/* Image */}
                    <div className="flex-shrink-0 w-32 h-32 sm:w-40 sm:h-40 bg-gray-100 rounded-lg overflow-hidden">
                        <img
                            src={auction.item.imageUrl || "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400"}
                            alt={auction.item.itemName}
                            className={`w-full h-full object-cover ${isScheduled ? "grayscale" : ""}`}
                        />
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between mb-2">
                            <div className="flex-1">
                                <h3 className="text-xl font-black text-gray-900 mb-1 line-clamp-1">
                                    {auction.item.itemName}
                                </h3>
                                <p className="text-sm text-gray-600 font-medium line-clamp-2">
                                    {auction.item.description}
                                </p>
                                <div className="flex items-center gap-2 mt-2 text-xs text-gray-500 font-bold">
                                    <Calendar className="w-3 h-3" />
                                    {formatDate(auction.startTime)}
                                </div>
                            </div>
                            {getStatusBadge()}
                        </div>

                        {/* ✅ [수정] 상태에 따른 정보 표시 영역 */}
                        {isEnded ? (
                            // 1. 종료된 경매일 때 -> 낙찰자 정보 표시
                            <div className="mt-3 bg-yellow-50/50 p-3 rounded-lg border border-yellow-100">
                                <div className="flex justify-between items-center mb-1">
                                    <div className="flex items-center text-sm font-bold text-gray-600">
                                        <Award className="w-4 h-4 mr-1 text-yellow-600" />
                                        낙찰자
                                    </div>
                                    <span className="font-bold text-indigo-700">
                    {winnerName || auction.winnerName || "정보 없음"}
                  </span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <div className="flex items-center text-sm font-bold text-gray-600">
                                        <Coins className="w-4 h-4 mr-1 text-yellow-600" />
                                        낙찰가
                                    </div>
                                    <span className="font-bold text-gray-900">
                    {(winningBid || auction.currentPrice).toLocaleString()} P
                  </span>
                                </div>
                            </div>
                        ) : !isScheduled ? (
                            // 2. 진행 중일 때 -> 현재가 표시
                            <div className="grid grid-cols-1 gap-4 mt-4">
                                <div>
                                    <p className="text-xs text-gray-500 font-bold mb-1">현재가</p>
                                    <p className="text-2xl font-black text-[#C93831]">
                                        {auction.currentPrice.toLocaleString()}
                                        <span className="text-sm ml-1">P</span>
                                    </p>
                                </div>
                            </div>
                        ) : null}

                        {/* 하단 정보 (선택된 경우 타이머 표시) */}
                        <div className="flex items-center gap-4 mt-3">
                            {isSelected && isActive && (
                                <AuctionTimer auction={auction} onTimeEnd={onTimeEnd} />
                            )}
                            {/* 진행 중일 때만 입찰 수 표시 */}
                            {isActive && (
                                <div className="flex items-center gap-1 text-sm font-bold text-gray-600">
                                    <Users className="w-4 h-4" />
                                    {auction.totalBids || 0}번 입찰
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </Card>
    );
};