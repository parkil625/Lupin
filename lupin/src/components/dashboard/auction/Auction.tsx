/**
 * Auction.tsx
 *
 * 주요 수정 사항:
 * 1. fetchAuctions 함수가 '배경 업데이트' 모드를 지원하도록 수정 (silentRefresh 파라미터 추가)
 * -> 입찰 후 데이터 갱신 시 로딩 스켈레톤이 뜨지 않아 깜빡임이 사라집니다.
 * 2. useCallback을 사용하여 함수 재생성 방지
 */

import { useState, useEffect, useCallback } from "react";
import { Card } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Calendar, Clock, Trophy } from "lucide-react";
import AnimatedBackground from "../shared/AnimatedBackground";
import {
    getActiveAuction,
    getScheduledAuctions,
    placeBid,
    getUserPoints,
    getBidHistory,
    getEndedAuctions
} from "@/api/auctionApi";
import { AuctionData, BidHistory } from "@/types/auction.types";
import { AuctionCard } from "./AuctionCard";
import { BiddingPanel } from "./BiddingPanel";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { toast } from "sonner";

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

    /**
     * 유저 포인트 조회 (메모이제이션)
     */
    const fetchUserPoints = useCallback(async () => {
        try {
            const data = await getUserPoints();
            if (data) {
                setUserPoints(data.totalPoints);
            }
        } catch (error) {
            console.error("포인트 조회 실패", error);
        }
    }, []);

    /**
     * 경매 목록 조회 (진행 중 & 예정 & 종료)
     * @param silentRefresh true일 경우 로딩 스피너를 보여주지 않음 (깜빡임 방지)
     */
    const fetchAuctions = useCallback(async (silentRefresh = false) => {
        try {
            // ✅ [핵심] 조용히 갱신할 때는 로딩 상태를 켜지 않음
            if (!silentRefresh) {
                setIsLoading(true);
            }

            const [activeAuctionData, scheduledAuctionList, endedAuctionList] = await Promise.all([
                getActiveAuction().catch(() => null),
                getScheduledAuctions().catch(() => []),
                getEndedAuctions().catch(() => [])
            ]);

            await fetchUserPoints();

            // 진행 중인 경매 상태 업데이트
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
                    // 이미 선택된 경매가 있다면 정보를 갱신하되, 선택 상태는 유지
                    setSelectedAuction((prev) => {
                         if (!prev || prev.auctionId === activeAuctionData.auctionId) {
                             return activeAuctionData;
                         }
                         return prev;
                    });
                }
            } else {
                setAuctions([]);
                setSelectedAuction(null);
            }

            if (scheduledAuctionList) {
                setScheduledAuctions(scheduledAuctionList);
            }

            if (endedAuctionList) {
                setEndedAuctions(endedAuctionList);
            }

        } catch (error) {
            console.error("경매 목록 조회 실패:", error);
        } finally {
            if (!silentRefresh) {
                setIsLoading(false);
            }
        }
    }, [fetchUserPoints]);

    /**
     * 입찰 내역 조회
     */
    const fetchBidHistory = useCallback(async () => {
        try {
            const historyData = await getBidHistory();
            setBidHistory(historyData);
        } catch (error) {
            console.error("입찰 내역 조회 실패:", error);
            setBidHistory([]);
        }
    }, []);

    // ▼ [SSE 연결 로직]
    useEffect(() => {
        if (!selectedAuction?.auctionId) return;

        const isLocal = window.location.hostname === 'localhost';
        const baseUrl = isLocal ? 'http://localhost:8081' : window.location.origin;

        const sseUrl = `${baseUrl}/api/auction/stream/${selectedAuction.auctionId}`;
        const eventSource = new EventSource(sseUrl);

        eventSource.addEventListener("refresh", (e: MessageEvent) => {
            try {
                const data = JSON.parse(e.data);

                // 1. 현재 선택된 경매 상태 업데이트
                if (selectedAuction && selectedAuction.auctionId === data.auctionId) {
                    setSelectedAuction((prev) => {
                        if (!prev) return null;
                        const now = new Date();
                        const regularEnd = new Date(prev.regularEndTime);
                        const isActuallyOvertime = prev.overtimeStarted || (now >= regularEnd && !!data.newEndTime);

                        return {
                            ...prev,
                            currentPrice: data.currentPrice,
                            totalBids: data.totalBids,
                            regularEndTime: prev.regularEndTime,
                            overtimeEndTime: data.newEndTime || prev.overtimeEndTime,
                            overtimeStarted: isActuallyOvertime
                        };
                    });
                }

                setAuctions((prevAuctions) =>
                    prevAuctions.map((item) => {
                        if (item.auctionId === data.auctionId) {
                            const now = new Date();
                            const regularEnd = new Date(item.regularEndTime);
                            const isActuallyOvertime = item.overtimeStarted || (now >= regularEnd && !!data.newEndTime);

                            return {
                                ...item,
                                totalBids: data.totalBids,
                                currentPrice: data.currentPrice,
                                overtimeEndTime: data.newEndTime || item.overtimeEndTime,
                                overtimeStarted: isActuallyOvertime
                            };
                        }
                        return item;
                    })
                );

                // SSE로 입찰 내역 즉시 추가 (API 호출 없이 UI 반응성 향상)
                const newBidLog: BidHistory = {
                    id: Date.now(),
                    userId: 0,
                    userName: data.bidderName,
                    bidAmount: data.currentPrice,
                    bidTime: data.bidTime,
                    status: "ACTIVE"
                };

                setBidHistory((prev) => {
                    const updatedPrev = prev.map((bid) =>
                        bid.status === "ACTIVE"
                            ? { ...bid, status: "OUTBID" as const }
                            : bid
                    );
                    return [newBidLog, ...updatedPrev];
                });
                // 포인트는 내 포인트가 깎였을 수도 있으므로 갱신
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
    }, [selectedAuction?.auctionId, fetchUserPoints]);

    // 초기 데이터 로드 (첫 로딩이므로 로딩바 표시)
    useEffect(() => {
        fetchAuctions(false);
    }, [fetchAuctions]);

    // 경매 선택 시 입찰 기록 조회 및 금액 초기화
    useEffect(() => {
        if (selectedAuction?.auctionId) {
            fetchBidHistory();
        }
    }, [selectedAuction?.auctionId, fetchBidHistory]);

    // 현재 가격이 변경될 때 입찰 할 금액(Input) 업데이트
    useEffect(() => {
        if (selectedAuction?.currentPrice) {
            setBidAmount((selectedAuction.currentPrice + 1).toString());
        }
    }, [selectedAuction?.currentPrice]);


    /**
     * 입찰 처리 핸들러
     */
    const handlePlaceBid = async () => {
        if (!selectedAuction) return;

        const amount = parseInt(bidAmount.replace(/[^0-9]/g, ""));

        if (isNaN(amount)) {
            toast.error("올바른 금액을 입력해주세요.");
            return;
        }
        if (amount <= selectedAuction.currentPrice || amount < 0) {
            toast.error("현재가보다 높은 금액을 입찰해주세요.");
            return;
        }
        if (amount > userPoints) {
            toast.error("보유 포인트가 부족합니다.");
            return;
        }

        setIsBidding(true);

        try {
            await placeBid(selectedAuction.auctionId, amount);

            toast.success("입찰에 성공했습니다! 🎉", {
                description: `${amount.toLocaleString()}P 입찰 완료`,
                duration: 2000,
            });

            // ✅ [수정] true를 전달하여 로딩바 없이(깜빡임 없이) 데이터 갱신
            // SSE가 데이터를 주지만, 혹시 모를 동기화를 위해 백그라운드 갱신
            fetchAuctions(true);

            // fetchBidHistory(); // SSE가 처리하므로 굳이 즉시 호출 안해도 됨 (원하면 유지)
            fetchUserPoints();

        } catch (error) {
            console.error("입찰 실패:", error);
            const axiosError = error as { response?: { data?: string | { message?: string } } };
            const errorData = axiosError.response?.data;
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
                                                    // ✅ fetchAuctions는 이제 stable한 함수입니다.
                                                    onTimeEnd={() => fetchAuctions(true)}
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

                                {/* 2. 탭 영역 */}
                                <div className="mt-8">
                                    <Tabs defaultValue="scheduled" className="w-full">
                                        <TabsList className="grid w-full grid-cols-2 mb-4 bg-gray-100 p-1 rounded-xl">
                                            <TabsTrigger
                                                value="scheduled"
                                                className="rounded-lg data-[state=active]:bg-white data-[state=active]:shadow-sm font-bold"
                                            >
                                                <Calendar className="w-4 h-4 mr-2" />
                                                예정된 경매
                                            </TabsTrigger>
                                            <TabsTrigger
                                                value="ended"
                                                className="rounded-lg data-[state=active]:bg-white data-[state=active]:shadow-sm font-bold"
                                            >
                                                <Trophy className="w-4 h-4 mr-2 text-yellow-600" />
                                                종료된 경매
                                            </TabsTrigger>
                                        </TabsList>

                                        <TabsContent value="scheduled" className="space-y-4">
                                            {scheduledAuctions.length > 0 ? (
                                                scheduledAuctions.map((auction) => (
                                                    <AuctionCard
                                                        key={auction.auctionId}
                                                        auction={auction}
                                                        isSelected={false}
                                                        onSelect={() => { }}
                                                    />
                                                ))
                                            ) : (
                                                <div className="text-center py-12 text-gray-500 bg-gray-50 rounded-lg">
                                                    <p>예정된 경매가 없습니다.</p>
                                                </div>
                                            )}
                                        </TabsContent>

                                        <TabsContent value="ended" className="space-y-4">
                                            {endedAuctions.length > 0 ? (
                                                endedAuctions.map((auction) => (
                                                    <AuctionCard
                                                        key={auction.auctionId}
                                                        auction={auction}
                                                        status="ENDED"
                                                        winnerName={auction.winnerName}
                                                        winningBid={auction.currentPrice}
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