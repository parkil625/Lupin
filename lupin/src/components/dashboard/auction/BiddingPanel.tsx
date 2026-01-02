// src/components/dashboard/auction/BiddingPanel.tsx

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { TrendingUp, Clock, Trophy, AlertCircle } from "lucide-react";
import { AuctionData, BidHistory } from "@/types/auction.types";

interface BiddingPanelProps {
  selectedAuction: AuctionData | null;
  bidAmount: string;
  setBidAmount: (amount: string) => void;
  onPlaceBid: () => void;
  bidHistory: BidHistory[];
  userPoints: number;
  isBidding: boolean;
}

export const BiddingPanel = ({
  selectedAuction,
  bidAmount,
  setBidAmount,
  onPlaceBid,
  bidHistory,
  isBidding,
}: BiddingPanelProps) => {
    const currentUserId = Number(localStorage.getItem("userId"));

    // ✅ [핵심 수정] 데이터를 받자마자 금액 내림차순으로 '줄 세우기'를 먼저 합니다.
    // 원본 배열을 건드리지 않기 위해 [...bidHistory]로 복사 후 정렬합니다.
    const sortedHistory = [...bidHistory].sort((a, b) => b.bidAmount - a.bidAmount);

    const addAmount = (amountToAdd: number) => {
        const current = parseInt(bidAmount.replace(/[^0-9]/g, "")) || 0;
        setBidAmount((current + amountToAdd).toString());
    };

  if (!selectedAuction || selectedAuction.status !== "ACTIVE") {
    return (
      <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg">
        <CardContent className="text-center py-12">
          {selectedAuction?.status === "SCHEDULED" ? (
            <>
              <Clock className="w-12 h-12 mx-auto text-gray-400 mb-3" />
              <p className="text-sm text-gray-600 font-bold">경매 시작 전입니다</p>
            </>
          ) : selectedAuction?.status === "ENDED" ? (
            <>
              <Trophy className="w-12 h-12 mx-auto text-gray-400 mb-3" />
              <p className="text-sm text-gray-600 font-bold">경매가 종료되었습니다</p>
            </>
          ) : (
            <>
              <AlertCircle className="w-12 h-12 mx-auto text-gray-400 mb-3" />
              <p className="text-sm text-gray-600 font-bold">
                입찰 가능한 경매가 없습니다
              </p>
            </>
          )}
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      {/* Bidding Form */}
      <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg mb-4">
        <CardHeader>
          <CardTitle className="text-lg font-black">
            {selectedAuction.item.itemName}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="text-sm font-bold text-gray-700 mb-2 block">
              입찰 금액
            </label>
            <Input
              type="number"
              placeholder="입찰 금액 입력"
              value={bidAmount}
              onChange={(e) => setBidAmount(e.target.value)}
              className="font-bold"
              onWheel={(e) => e.currentTarget.blur()}
            />
              <div className="flex gap-2 mt-2 mb-4">
                  {[100, 500, 1000].map((amt) => (
                      <button
                          key={amt}
                          onClick={() => addAmount(amt)}
                          className="px-3 py-1 text-xs font-bold bg-gray-100 text-gray-600 rounded-full hover:bg-gray-200 transition hover:cursor-pointer"
                      >
                          +{amt.toLocaleString()}
                      </button>
                  ))}
              </div>
            <p className="text-xs text-gray-500 mt-1 font-medium">
              현재가({selectedAuction.currentPrice.toLocaleString()}P)보다 높은 금액을 입찰해주세요
            </p>
          </div>

            <Button
                onClick={onPlaceBid}
                disabled={isBidding} // 로딩 중 클릭 방지
                className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold hover:shadow-lg disabled:opacity-70 hover:cursor-pointer"
            >
                {isBidding ? (
                    <span className="animate-spin mr-2">⏳</span>
                ) : (
                    <TrendingUp className="w-4 h-4 mr-2" />
                )}
                {isBidding ? "처리 중..." : "입찰하기"}
            </Button>
        </CardContent>
      </Card>

      {/* Bid History */}
      <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg">
        <CardHeader>
          <CardTitle className="text-lg font-black">입찰 내역</CardTitle>
        </CardHeader>
        <CardContent>
          {sortedHistory.length > 0 ? (
            <ScrollArea className="h-[300px] pr-4">
              <div className="space-y-3">
                {/* ✅ [수정] 정렬된 sortedHistory를 사용합니다. */}
                {sortedHistory.map((bid, index) => (
                  <div
                    key={bid.id}
                    className="flex items-center justify-between py-2 border-b border-gray-200 last:border-0"
                  >
                    <div>
                        <p className="text-sm font-bold text-gray-900">
                            {bid.userName}
                            {bid.userId === currentUserId && (
                                <span className="text-[#C93831] ml-1 text-xs">(나)</span>
                            )}
                        </p>
                      <p className="text-xs text-gray-500 font-medium">
                        {new Date(bid.bidTime).toLocaleString()}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-black text-[#C93831]">
                        {bid.bidAmount.toLocaleString()}P
                      </p>

                      {/* ✅ [수정] 서버 상태를 무시하고, 정렬된 목록의 첫 번째(1등)에게만 배지를 줍니다. */}
                      {index === 0 && (
                        <Badge className="bg-green-500 text-white text-xs border-0 font-bold">
                          최고가
                        </Badge>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          ) : (
            <div className="text-center py-8">
              <p className="text-sm text-gray-500 font-medium">
                아직 입찰 내역이 없습니다
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </>
  );
};