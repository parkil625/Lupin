/**
 * Auction.tsx
 *
 * 경매 페이지 메인 컴포넌트
 * - 리팩토링됨: 로직(hooks), UI(components), 타입(types) 분리
 */

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Clock } from "lucide-react";
import AnimatedBackground from "../shared/AnimatedBackground";
// [수정 1] placeBid 추가 import
import { getActiveAuction, getScheduledAuctions, placeBid, getUserPoints, getBidHistory } from "@/api/auctionApi";
// 분리된 컴포넌트 및 훅 import
import { AuctionData, BidHistory } from "@/types/auction.types";
import { useAuctionTimer } from "@/hooks/useAuctionTimer";
import { AuctionCard } from "./AuctionCard";
import { BiddingPanel } from "./BiddingPanel";

export default function Auction() {
  const [auctions, setAuctions] = useState<AuctionData[]>([]);
  const [scheduledAuctions, setScheduledAuctions] = useState<AuctionData[]>([]);
  const [selectedAuction, setSelectedAuction] = useState<AuctionData | null>(null);
  const [bidAmount, setBidAmount] = useState("");
  const [bidHistory, setBidHistory] = useState<BidHistory[]>([]);
  const [userPoints, setUserPoints] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  // 타이머 로직 훅 사용
  const { countdown, isOvertime } = useAuctionTimer(selectedAuction);

    // ▼ [SSE 연결 로직]
    useEffect(() => {
        if (!selectedAuction?.auctionId) return;

        const sseUrl = `${import.meta.env.VITE_API_URL || 'http://localhost:8081'}/api/auction/stream/${selectedAuction.auctionId}`;
        const eventSource = new EventSource(sseUrl);

        eventSource.onopen = () => {
            console.log("경매 SSE 연결 성공!");
        };

        eventSource.addEventListener("refresh", (e: MessageEvent) => {
            try {
                const data = JSON.parse(e.data);
                console.log("⚡ SSE 데이터 도착:", data);

                if (selectedAuction && selectedAuction.auctionId === data.auctionId) {
                    setSelectedAuction((prev) => {
                        if (!prev) return null;

                        // [수정 포인트] 무조건 true가 아니라, 실제 시간이 지났는지 체크하거나 기존 상태 유지
                        // 만약 'useAuctionTimer'가 시간을 기준으로 자동 판단한다면, 이 필드는 굳이 안 건드려도 됩니다.
                        // 확실하게 하기 위해 '현재 시간이 정규 시간을 지났는지'만 체크해봅니다.
                        const now = new Date();
                        const regularEnd = new Date(prev.regularEndTime);
                        const isActuallyOvertime = prev.overtimeStarted || (now >= regularEnd && !!data.newEndTime);

                        return {
                            ...prev,
                            currentPrice: data.currentPrice,

                            // 정규 시간은 유지 (골대 유지)
                            regularEndTime: prev.regularEndTime,

                            // 연장된 마감 시간 업데이트 (없으면 기존 유지)
                            overtimeEndTime: data.newEndTime || prev.overtimeEndTime,
                            overtimeStarted: isActuallyOvertime
                        };
                    });
                }

                // 목록 업데이트 로직 (동일하게 적용)
                setAuctions((prevAuctions) =>
                    prevAuctions.map((item) =>
                        item.auctionId === data.auctionId
                            ? {
                                ...item,
                                currentPrice: data.currentPrice,
                                // 목록에서도 연장 정보를 갱신하고 싶다면 아래 주석 해제
                                // overtimeEndTime: data.newEndTime || item.overtimeEndTime,
                            }
                            : item
                    )
                );

                // ... (입찰 내역 추가 및 포인트 갱신 코드는 기존과 동일)
                const newBidLog: BidHistory = {
                    id: Date.now(),
                    userId: 0,
                    userName: data.bidderName,
                    bidAmount: data.currentPrice,
                    bidTime: data.bidTime,
                    status: "ACTIVE"
                };
                setBidHistory((prev) => [newBidLog, ...prev]);
                fetchUserPoints();

            } catch (err) {
                console.error("SSE 파싱 에러:", err);
            }
        });

        eventSource.onerror = (err) => {
            console.error("SSE 연결 오류", err);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, [selectedAuction?.auctionId]); // 경매방 바뀔 때마다 재실행


  // 초기 데이터 로드
  useEffect(() => {
    fetchAuctions();
    fetchUserPoints();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 경매 선택 시 입찰 기록 조회 및 금액 초기화
  useEffect(() => {
    if (selectedAuction?.auctionId) {
      fetchBidHistory();
      // 가격이 바뀔 때만 입찰 금액 업데이트 (현재가 + 1원)
      setBidAmount((selectedAuction.currentPrice + 1).toString());
    }
  }, [selectedAuction?.auctionId, selectedAuction?.currentPrice]);

  /**
   * 경매 목록 조회 (진행 중 & 예정)
   */
  const fetchAuctions = async () => {
    try {
      setIsLoading(true);

      // 2. 진행 중인 경매와 예정된 경매를 병렬로 동시에 조회 (Promise.all 사용 권장)
      const [activeAuctionData, scheduledAuctionList] = await Promise.all([
        getActiveAuction().catch(() => null),       // 에러 발생 시 null 처리
        getScheduledAuctions().catch(() => [])      // 에러 발생 시 빈 배열 처리
      ]);

      // 3. 진행 중인 경매 상태 업데이트
      if (activeAuctionData) {
        setAuctions([activeAuctionData]);
        // 선택된 경매가 없으면 기본값으로 설정
        if (!selectedAuction || selectedAuction.auctionId === activeAuctionData.auctionId) {
          setSelectedAuction(activeAuctionData);
        }
      } else {
        setAuctions([]);
      }

      // 4. 예정된 경매 상태 업데이트
      if (scheduledAuctionList) {
        setScheduledAuctions(scheduledAuctionList);
      }

    } catch (error) {
      console.error("경매 목록 조회 실패:", error);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * 유저 포인트 조회
   */
const fetchUserPoints = async () => {
    // 1. 에러 처리를 위해 try-catch 사용 권장
    try {
        const data = await getUserPoints(); 
        
        if (data) {
            setUserPoints(data.totalPoints); 
        }
    } catch (error) {
        console.error("포인트 조회 실패", error);
        setUserPoints(0);
    }
};

  /**
   * 입찰 내역 조회
   */
  const fetchBidHistory = async () => {
try {
    const historyData = await getBidHistory();
    // API 데이터로 상태 업데이트
    setBidHistory(historyData); 
  } catch (error) {
    console.error("입찰 내역 조회 실패:", error);
    // 에러 발생 시 빈 배열 처리 (Mock Data가 남아있으면 안 됨)
    setBidHistory([]); 
  }
  };

  /**
   * 입찰 처리 핸들러
   */
  const handlePlaceBid = async () => {
    if (!selectedAuction) return;

    // 1. 입력값 검증 (숫자 변환 및 콤마 제거)
    const amount = parseInt(bidAmount.replace(/[^0-9]/g, ""));

    if (isNaN(amount)) {
      alert("올바른 금액을 입력해주세요.");
      return;
    }
    if (amount <= selectedAuction.currentPrice) {
      alert("현재가보다 높은 금액을 입찰해주세요.");
      return;
    }
    if (amount > userPoints) {
      alert("보유 포인트가 부족합니다.");
      return;
    }

    try {
      // 2. 실제 API 호출 연결
      await placeBid(selectedAuction.auctionId, amount);

      alert("입찰이 완료되었습니다!");

      // 3. 데이터 갱신 (경매 정보, 입찰 내역, 포인트)
      fetchAuctions(); // 현재가 갱신
      fetchBidHistory(); // 입찰 내역 갱신
      fetchUserPoints(); // 내 잔액 갱신

    } catch (error: unknown) {
      console.error("입찰 실패:", error);
      // 백엔드 에러 메시지가 있다면 보여주기
      const axiosError = error as { response?: { data?: { message?: string } } };
      const errorMessage = axiosError.response?.data?.message || "입찰에 실패했습니다.";
      alert(errorMessage);
    }
  };

  return (
    <div className="relative h-screen w-full flex flex-col overflow-hidden">
      <AnimatedBackground variant="member" />

      <ScrollArea className="flex-1 w-full h-full relative z-10">
        <div className="relative p-8">
          <div className="max-w-7xl mx-auto space-y-8">
            {/* Header */}
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-5xl font-black text-gray-900 mb-2">경매</h1>
                <p className="text-gray-600 font-bold">
                  매일 밤 10시, 포인트로 입찰하고 상품을 획득하세요
                </p>
              </div>
              <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg px-6 py-4">
                <div className="text-center">
                  <p className="text-sm text-gray-600 font-bold mb-1">
                    보유 포인트
                  </p>
                  <p className="text-2xl font-black text-[#C93831]">
                    {userPoints.toLocaleString()}
                    <span className="text-base ml-1">P</span>
                  </p>
                </div>
              </Card>
            </div>

            {/* Content Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Left Column: Auction List */}
              <div className="lg:col-span-2 space-y-6">

                {/* 1. 진행 중인 경매 섹션 */}
                <div>
                  <h2 className="text-2xl font-black text-gray-900 mb-4">
                    진행 중인 경매
                  </h2>
                  {isLoading ? (
                    <div className="h-48 rounded-xl animate-pulse bg-gray-200" />
                  ) : auctions.length > 0 ? (
                    <div className="space-y-4">
                      {auctions.map((auction) => (
                        <AuctionCard
                          key={auction.auctionId}
                          auction={auction}
                          isSelected={selectedAuction?.auctionId === auction.auctionId}
                          onSelect={setSelectedAuction}
                          countdown={countdown}
                          isOvertime={isOvertime}
                        />
                      ))}
                    </div>
                  ) : (
                    <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg p-12">
                      <div className="text-center text-gray-500">
                        <Clock className="w-12 h-12 mx-auto mb-3 text-gray-400" />
                        <p className="font-bold">진행 중인 경매가 없습니다</p>
                      </div>
                    </Card>
                  )}
                </div>

                {/* 2. 예정된 경매 섹션 */}
                {scheduledAuctions.length > 0 && (
                  <div>
                    <h2 className="text-2xl font-black text-gray-900 mb-4">
                      예정된 경매
                    </h2>
                    <div className="space-y-4">
                      {scheduledAuctions.map((auction) => (
                        <AuctionCard
                          key={auction.auctionId}
                          auction={auction}
                          isSelected={false}
                          onSelect={() => {}} // 예정된 경매는 선택 불가
                        />
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Right Column: Bidding Panel */}
              <div className="space-y-4">
                <h2 className="text-2xl font-black text-gray-900 mb-4">
                  입찰하기
                </h2>
                <BiddingPanel
                  selectedAuction={selectedAuction}
                  bidAmount={bidAmount}
                  setBidAmount={setBidAmount}
                  onPlaceBid={handlePlaceBid}
                  bidHistory={bidHistory}
                  userPoints={userPoints}
                />
              </div>
            </div>
          </div>
        </div>
      </ScrollArea>
    </div>
  );
}