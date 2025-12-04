import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { TrendingUp, Clock, Trophy, AlertCircle } from "lucide-react";
import { AuctionItem, BidHistory } from "@/types/auction.types";

interface BiddingPanelProps {
  selectedAuction: AuctionItem | null;
  bidAmount: string;
  setBidAmount: (amount: string) => void;
  onPlaceBid: () => void;
  bidHistory: BidHistory[];
  userPoints: number;
}

export const BiddingPanel = ({
  selectedAuction,
  bidAmount,
  setBidAmount,
  onPlaceBid,
  bidHistory,
  userPoints,
}: BiddingPanelProps) => {
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
            {selectedAuction.itemName}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="text-sm font-bold text-gray-700 mb-2 block">
              입찰 금액 (보유: {userPoints.toLocaleString()}P)
            </label>
            <Input
              type="number"
              placeholder="입찰 금액 입력"
              value={bidAmount}
              onChange={(e) => setBidAmount(e.target.value)}
              className="font-bold"
              onWheel={(e) => e.currentTarget.blur()}
            />
            <p className="text-xs text-gray-500 mt-1 font-medium">
              현재가({selectedAuction.currentPrice.toLocaleString()}P)보다 높은 금액을 입찰해주세요
            </p>
          </div>

          <Button
            onClick={onPlaceBid}
            className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold hover:shadow-lg"
          >
            <TrendingUp className="w-4 h-4 mr-2" />
            입찰하기
          </Button>
        </CardContent>
      </Card>

      {/* Bid History */}
      <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg">
        <CardHeader>
          <CardTitle className="text-lg font-black">입찰 내역</CardTitle>
        </CardHeader>
        <CardContent>
          {bidHistory.length > 0 ? (
            <ScrollArea className="h-[300px] pr-4">
              <div className="space-y-3">
                {bidHistory.map((bid) => (
                  <div
                    key={bid.id}
                    className="flex items-center justify-between py-2 border-b border-gray-200 last:border-0"
                  >
                    <div>
                      <p className="text-sm font-bold text-gray-900">
                        {bid.userName}
                      </p>
                      <p className="text-xs text-gray-500 font-medium">
                        {new Date(bid.bidTime).toLocaleString()}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-black text-[#C93831]">
                        {bid.bidAmount.toLocaleString()}P
                      </p>
                      {bid.status === "ACTIVE" && (
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