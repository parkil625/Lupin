import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { AuctionData } from "@/types/auction.types";
import { Gavel, Clock, Trophy, Calendar, Users, Award, Coins } from "lucide-react";
import { AuctionTimer } from "@/components/dashboard/auction/AuctionTimer"
import { memo } from "react"; // ✅ memo 불러오기

interface AuctionCardProps {
    auction: AuctionData;
    isSelected?: boolean;
    onSelect?: (item: AuctionData) => void;
    onTimeEnd?: () => void;
    winnerName?: string;
    winningBid?: number;
    status?: 'ACTIVE' | 'SCHEDULED' | 'ENDED';
}

// ✅ [최적화 1] 날짜 포맷 함수를 컴포넌트 밖으로 이동 (불필요한 함수 재생성 방지)
const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}.${month}.${day} ${hours}:${minutes}`;
};

// ✅ [최적화 2] 변하지 않는 이미지 영역 (Memoization 적용)
// props(imageUrl, itemName 등)가 변하지 않으면 리렌더링되지 않음
const AuctionImage = memo(({ imageUrl, itemName, isScheduled }: { imageUrl?: string, itemName: string, isScheduled: boolean }) => (
    <div className="flex-shrink-0 w-32 h-32 sm:w-40 sm:h-40 bg-gray-100 rounded-lg overflow-hidden">
        <img
            src={imageUrl || "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400"}
            alt={itemName}
            className={`w-full h-full object-cover ${isScheduled ? "grayscale" : ""}`}
        />
    </div>
));
AuctionImage.displayName = "AuctionImage";

// ✅ [최적화 3] 변하지 않는 상품 텍스트 정보 영역 (Memoization 적용)
const AuctionItemInfo = memo(({ itemName, description, startTime }: { itemName: string, description: string, startTime: string }) => (
    <div className="flex-1">
        <h3 className="text-xl font-black text-gray-900 mb-1 line-clamp-1">
            {itemName}
        </h3>
        <p className="text-sm text-gray-600 font-medium line-clamp-2">
            {description}
        </p>
        <div className="flex items-center gap-2 mt-2 text-xs text-gray-500 font-bold">
            <Calendar className="w-3 h-3" />
            {formatDate(startTime)}
        </div>
    </div>
));
AuctionItemInfo.displayName = "AuctionItemInfo";

export const AuctionCard = ({
                                auction,
                                isSelected = false,
                                onSelect,
                                onTimeEnd,
                                winnerName,
                                winningBid,
                                status,
                            }: AuctionCardProps) => {

    const currentStatus = status || auction.status;
    const isEnded = currentStatus === "ENDED";
    const isScheduled = currentStatus === "SCHEDULED";
    const isActive = currentStatus === "ACTIVE";

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
        ${isEnded ? "hover:shadow-md" : "hover:shadow-xl"}
      `}
            onClick={() => {
                if (!isScheduled && onSelect) {
                    onSelect(auction);
                }
            }}
        >
            <div className="p-6">
                <div className="flex gap-6">
                    {/* ✅ 분리된 이미지 컴포넌트 사용 */}
                    <AuctionImage
                        imageUrl={auction.item.imageUrl}
                        itemName={auction.item.itemName}
                        isScheduled={isScheduled}
                    />

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between mb-2">
                            {/* ✅ 분리된 텍스트 정보 컴포넌트 사용 */}
                            <AuctionItemInfo
                                itemName={auction.item.itemName}
                                description={auction.item.description}
                                startTime={auction.startTime}
                            />
                            {/* 뱃지는 상태에 따라 변하므로 그대로 둠 */}
                            {getStatusBadge()}
                        </div>

                        {/* ✅ 아래 부분(가격, 타이머 등)은 실시간으로 자주 변하므로 여기서 렌더링 */}
                        {isEnded ? (
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

                        <div className="flex items-center gap-4 mt-3">
                            {isSelected && isActive && (
                                <AuctionTimer auction={auction} onTimeEnd={onTimeEnd} />
                            )}
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