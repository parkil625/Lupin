/**
 * Auction.tsx
 *
 * 경매 페이지 메인 컴포넌트
 * - 리팩토링됨: 로직(hooks), UI(components), 타입(types) 분리
 */

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import {Calendar, Clock, Trophy} from "lucide-react";
import AnimatedBackground from "../shared/AnimatedBackground";
import { getActiveAuction, getScheduledAuctions, placeBid, getUserPoints, getBidHistory,getEndedAuctions } from "@/api/auctionApi";
// 분리된 컴포넌트 및 훅 import
import { AuctionData, BidHistory } from "@/types/auction.types";
import { useAuctionTimer } from "@/hooks/useAuctionTimer";
import { AuctionCard } from "./AuctionCard";
import { BiddingPanel } from "./BiddingPanel";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {toast} from "sonner";

export default function Auction() {
  const [auctions, setAuctions] = useState<AuctionData[]>([]);
  const [scheduledAuctions, setScheduledAuctions] = useState<AuctionData[]>([]);
  const [endedAuctions, setEndedAuctions] = useState<AuctionData[]>([]);
  const [selectedAuction, setSelectedAuction] = useState<AuctionData | null>(null);
  const [bidAmount, setBidAmount] = useState("");
  const [bidHistory, setBidHistory] = useState<BidHistory[]>([]);
  const [userPoints, setUserPoints] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isBidding, setIsBidding] = useState(false);


    // ▼ [SSE 연결 로직]
    useEffect(() => {
        if (!selectedAuction?.auctionId) return;

        // [수정 1] 주소 찾는 방식을 useNotificationSse.ts와 통일
        const isLocal = window.location.hostname === 'localhost';
        const baseUrl = isLocal ? 'http://localhost:8081' : window.location.origin;


        const sseUrl = `${baseUrl}/api/auction/stream/${selectedAuction.auctionId}`;

        const eventSource = new EventSource(sseUrl);

        eventSource.addEventListener("refresh", (e: MessageEvent) => {
            try {
                const data = JSON.parse(e.data);
                console.log("SSE 데이터 도착:", data);

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
                            totalBids: data.totalBids,
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
                                totalBids: data.totalBids,
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
                    userId: 0, // 필요 시 실제 ID나 0 유지
                    userName: data.bidderName,
                    bidAmount: data.currentPrice,
                    bidTime: data.bidTime,
                    status: "ACTIVE" // 새 입찰은 무조건 '최고가' 상태
                };

                setBidHistory((prev) => {
                    // 기존 내역 중 'ACTIVE'(최고가) 상태였던 것들을 모두 'OUTBID'로 변경
                    const updatedPrev = prev.map((bid) =>
                        bid.status === "ACTIVE"
                            ? { ...bid, status: "OUTBID" as const }
                            : bid
                    );
                    // 새 입찰을 맨 앞에 추가
                    return [newBidLog, ...updatedPrev];
                });
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
        }
    }, [selectedAuction?.auctionId]);

    // 2. 현재 가격이 변경될 때 입찰 할 금액(Input)만 업데이트합니다. (서버 재요청 방지)
    useEffect(() => {
        if (selectedAuction?.currentPrice) {
            setBidAmount((selectedAuction.currentPrice + 1).toString());
        }
    }, [selectedAuction?.currentPrice]);


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
     * 경매 목록 조회 (진행 중 & 예정 & 내 포인트)
     */
    const fetchAuctions = async () => {
        try {
            setIsLoading(true);

            // 1. [순서 변경] 경매 정보부터 먼저 가져옵니다.
            // (이 요청이 처리되는 동안 서버에서 낙찰/정산 로직이 돌 시간을 벋니다.)
            const [activeAuctionData, scheduledAuctionList, endedAuctionList] = await Promise.all([
                getActiveAuction().catch(() => null),
                getScheduledAuctions().catch(() => []),
                getEndedAuctions().catch(() => [])
            ]);

            // 2. [핵심] 경매 조회가 끝난 '후에' 포인트를 조회합니다.
            // 이제 서버가 정산을 마쳤을 확률이 훨씬 높습니다.
            await fetchUserPoints();

            // 3. 진행 중인 경매 상태 업데이트 (좀비 경매 방지 로직)
            if (activeAuctionData) {
                const now = new Date();
                const regularEnd = new Date(activeAuctionData.regularEndTime);
                const overtimeBuffer = (activeAuctionData.overtimeSeconds ?? 30) * 1000;

                const effectiveEndTime = (activeAuctionData.overtimeStarted && activeAuctionData.overtimeEndTime)
                    ? new Date(activeAuctionData.overtimeEndTime)
                    : new Date(regularEnd.getTime() + overtimeBuffer);

                if (now > effectiveEndTime) {
                    setAuctions([]);
                    setSelectedAuction(null);
                } else {
                    setAuctions([activeAuctionData]);
                    if (!selectedAuction || selectedAuction.auctionId === activeAuctionData.auctionId) {
                        setSelectedAuction(activeAuctionData);
                    }
                }
            } else {
                setAuctions([]);
                setSelectedAuction(null);
            }

            // 4. 예정된 경매 상태 업데이트
            if (scheduledAuctionList) {
                setScheduledAuctions(scheduledAuctionList);
            }

            if (endedAuctionList) {
                setEndedAuctions(endedAuctionList);
            }

        } catch (error) {
            console.error("경매 목록 조회 실패:", error);
        } finally {
            setIsLoading(false);
        }
    };

    // 타이머 로직 훅 사용
    const { countdown, isOvertime } = useAuctionTimer(selectedAuction, fetchAuctions);



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

      // 1. 입력값 검증
      const amount = parseInt(bidAmount.replace(/[^0-9]/g, ""));

      if (isNaN(amount)) {
          toast.error("올바른 금액을 입력해주세요."); // alert 대신 toast
          return;
      }
      if (amount <= selectedAuction.currentPrice || amount < 0) {
          toast.error("현재가보다 높은 금액을 입찰해주세요."); // alert 대신 toast
          return;
      }
      if (amount > userPoints) {
          toast.error("보유 포인트가 부족합니다."); // alert 대신 toast
          return;
      }

      // [로딩 시작] 버튼 비활성화
      setIsBidding(true);

      try {
          // 2. 실제 API 호출
          await placeBid(selectedAuction.auctionId, amount);

          // 성공 알림
          toast.success("입찰에 성공했습니다! 🎉", {
              description: `${amount.toLocaleString()}P 입찰 완료`,
              duration: 2000,
          });

          // 3. 데이터 갱신
          fetchAuctions();
          fetchBidHistory();
          fetchUserPoints();

      } catch (error) { // : any 제거 (기본적으로 unknown 타입이 됨)
          console.error("입찰 실패:", error);

          // 1. 에러를 '우리가 예상하는 모양'으로 잠깐 변신시킵니다 (Type Assertion)
          // "이 에러는 response 안에 data가 있을 수도 있는 객체야!" 라고 알려주는 겁니다.
          const axiosError = error as { response?: { data?: string | { message?: string } } };

          // 2. 이제 안전하게 데이터를 꺼낼 수 있습니다.
          const errorData = axiosError.response?.data;

          // 3. 메시지가 문자열("실패!")인지 객체({message: "실패!"})인지 확인해서 추출
          const errorMessage = typeof errorData === 'string'
              ? errorData
              : errorData?.message || "입찰에 실패했습니다.";

          toast.error(errorMessage);
      } finally {
          setIsBidding(false);
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
                      매일 밤 10시, 포인트로 입찰하고 상품을 획득하세요!
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

                  {/* 2. [변경] 탭 영역 (예정된 경매 <-> 종료된 경매) */}
                  <div className="mt-8">
                      <Tabs defaultValue="scheduled" className="w-full">
                          <TabsList className="grid w-full grid-cols-2 mb-4 bg-gray-100 p-1 rounded-xl">
                              <TabsTrigger
                                  value="scheduled"
                                  className="rounded-lg data-[state=active]:bg-white data-[state=active]:shadow-sm font-bold"
                              >
                                  <Calendar className="w-4 h-4 mr-2"/>
                                  예정된 경매
                              </TabsTrigger>
                              <TabsTrigger
                                  value="ended"
                                  className="rounded-lg data-[state=active]:bg-white data-[state=active]:shadow-sm font-bold"
                              >
                                  <Trophy className="w-4 h-4 mr-2 text-yellow-600"/>
                                  종료된 경매
                              </TabsTrigger>
                          </TabsList>

                          {/* 탭 1: 예정된 경매 */}
                          <TabsContent value="scheduled" className="space-y-4">
                              {scheduledAuctions.length > 0 ? (
                                  scheduledAuctions.map((auction) => (
                                      <AuctionCard
                                          key={auction.auctionId}
                                          auction={auction}
                                          isSelected={false}
                                          onSelect={() => {}}
                                      />
                                  ))
                              ) : (
                                  <div className="text-center py-12 text-gray-500 bg-gray-50 rounded-lg">
                                      <p>예정된 경매가 없습니다.</p>
                                  </div>
                              )}
                          </TabsContent>

                          {/* 탭 2: 종료된 경매 */}
                          <TabsContent value="ended" className="space-y-4">
                              {endedAuctions.length > 0 ? (
                                  endedAuctions.map((auction) => (
                                      <AuctionCard
                                          key={auction.auctionId}
                                          auction={auction}
                                          status="ENDED" // 카드에 종료 상태 명시
                                          winnerName={auction.winnerName} // 낙찰자 이름 전달
                                          winningBid={auction.currentPrice} // 낙찰가 전달
                                      />
                                  ))
                              ) : (
                                  <div className="text-center py-12 text-gray-500 bg-gray-50 rounded-lg">
                                      <p>아직 종료된 경매 기록이 없습니다.</p>
                                  </div>
                              )}
                          </TabsContent>
                      </Tabs>
                  </div>
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
                  isBidding={isBidding}
                />
              </div>
            </div>
          </div>
        </div>
      </ScrollArea>
    </div>
  );
}