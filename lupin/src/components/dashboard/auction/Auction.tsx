/**
 * Auction.tsx
 *
 * 경매 페이지 컴포넌트
 * - 매일 밤 10시 경매 시스템 (체스 초읽기 방식)
 * - 정규 시간 종료 후 초읽기 30초 (입찰 시마다 리셋)
 * - 현재가 0P부터 시작, 1P 단위로 입찰 가능
 */

import {useState, useEffect} from "react";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {ScrollArea} from "@/components/ui/scroll-area";
import {Badge} from "@/components/ui/badge";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import AnimatedBackground from "../shared/AnimatedBackground";
import {
    Gavel,
    Clock,
    TrendingUp,
    Trophy,
    AlertCircle,
    Users,
    Eye,
    Calendar,
} from "lucide-react";

interface AuctionItem {
    id: number;
    itemName: string;
    description: string;
    imageUrl?: string;
    currentPrice: number;
    startTime: string;
    regularEndTime: string; // 정규 종료 시간
    overtimeStarted: boolean; // 초읽기 시작 여부
    overtimeEndTime?: string; // 초읽기 종료 시간
    overtimeSeconds: number; // 초읽기 시간 (30초)
    status: "SCHEDULED" | "ACTIVE" | "ENDED" | "CANCELLED";
    winnerId?: number;
    totalBids: number;
    viewers: number;
}

interface BidHistory {
    id: number;
    userId: number;
    userName: string;
    bidAmount: number;
    bidTime: string;
    status: "ACTIVE" | "OUTBID" | "WINNING" | "LOST";
}

export default function Auction() {
    const [auctions, setAuctions] = useState<AuctionItem[]>([]);
    const [scheduledAuctions, setScheduledAuctions] = useState<AuctionItem[]>([]);
    const [selectedAuction, setSelectedAuction] = useState<AuctionItem | null>(
        null
    );
    const [bidAmount, setBidAmount] = useState("");
    const [bidHistory, setBidHistory] = useState<BidHistory[]>([]);
    const [userPoints, setUserPoints] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [countdown, setCountdown] = useState<number>(0);
    const [isOvertime, setIsOvertime] = useState(false);

    useEffect(() => {
        fetchAuctions();
        fetchUserPoints();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (selectedAuction) {
            fetchBidHistory(selectedAuction.id);
            // 입찰 금액 자동 설정 (현재가 + 1P)
            setBidAmount((selectedAuction.currentPrice + 1).toString());
        }
    }, [selectedAuction]);

    // 카운트다운 타이머
    useEffect(() => {
        if (!selectedAuction || selectedAuction.status !== "ACTIVE") {
            setCountdown(0);
            setIsOvertime(false);
            return;
        }

        const calculateCountdown = () => {
            const now = Date.now();
            const regularEnd = new Date(selectedAuction.regularEndTime).getTime();

            // 정규 시간
            if (now < regularEnd) {
                setIsOvertime(false);
                const diff = Math.floor((regularEnd - now) / 1000);
                return Math.max(0, diff);
            }

            // 초읽기 모드
            if (selectedAuction.overtimeStarted && selectedAuction.overtimeEndTime) {
                setIsOvertime(true);
                const overtimeEnd = new Date(selectedAuction.overtimeEndTime).getTime();
                const diff = Math.floor((overtimeEnd - now) / 1000);
                return Math.max(0, diff);
            }

            // 정규 시간 종료 + 초읽기 미시작
            return 0;
        };

        setCountdown(calculateCountdown());

        const interval = setInterval(() => {
            const remaining = calculateCountdown();
            setCountdown(remaining);

            if (remaining <= 0) {
                clearInterval(interval);
            }
        }, 1000);

        return () => clearInterval(interval);
    }, [selectedAuction]);

    const fetchAuctions = async () => {
        try {
            // TODO: API 연동
            // const response = await auctionApi.getActiveAuctions();

            // Mock data - 진행 중인 경매
            const today = new Date();
            const todayStart = new Date(
                today.getFullYear(),
                today.getMonth(),
                today.getDate(),
                22,
                0,
                0
            );

            const activeAuctions: AuctionItem[] = [
                {
                    id: 1,
                    itemName: "Apple Watch Series 9 (45mm)",
                    description: "애플워치 시리즈9 GPS 45mm 미드나이트 알루미늄 케이스 (정가 599,000원)",
                    imageUrl:
                        "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=500&h=500&fit=crop",
                    currentPrice: 45,
                    startTime: todayStart.toISOString(),
                    regularEndTime: new Date(Date.now() + 1 * 60 * 1000).toISOString(), // 테스트용: 2분 후
                    overtimeStarted: false,
                    overtimeSeconds: 30,
                    status: "ACTIVE",
                    totalBids: 8,
                    viewers: 15,
                },
            ];

            // Mock data - 예정된 경매 (하루에 한 상품, 22:00 고정, 정규 시간 15분)
            const tomorrow = new Date(today);
            tomorrow.setDate(tomorrow.getDate() + 1);
            const tomorrowStart = new Date(
                tomorrow.getFullYear(),
                tomorrow.getMonth(),
                tomorrow.getDate(),
                22,
                0,
                0
            );
            const tomorrowEnd = new Date(
                tomorrow.getFullYear(),
                tomorrow.getMonth(),
                tomorrow.getDate(),
                22,
                15,
                0
            );

            const dayAfterTomorrow = new Date(today);
            dayAfterTomorrow.setDate(dayAfterTomorrow.getDate() + 2);
            const dayAfterStart = new Date(
                dayAfterTomorrow.getFullYear(),
                dayAfterTomorrow.getMonth(),
                dayAfterTomorrow.getDate(),
                22,
                0,
                0
            );
            const dayAfterEnd = new Date(
                dayAfterTomorrow.getFullYear(),
                dayAfterTomorrow.getMonth(),
                dayAfterTomorrow.getDate(),
                22,
                15,
                0
            );

            const scheduled: AuctionItem[] = [
                {
                    id: 2,
                    itemName: "LG 스탠바이미 Go (27인치)",
                    description: "LG 스탠바이미 Go 포터블 터치스크린 TV (정가 1,090,000원)",
                    imageUrl:
                        "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=500&h=500&fit=crop",
                    currentPrice: 0,
                    startTime: tomorrowStart.toISOString(),
                    regularEndTime: tomorrowEnd.toISOString(),
                    overtimeStarted: false,
                    overtimeSeconds: 30,
                    status: "SCHEDULED",
                    totalBids: 0,
                    viewers: 0,
                },
                {
                    id: 3,
                    itemName: "Apple AirPods Max",
                    description: "애플 에어팟 맥스 무선 헤드폰 스페이스 그레이 (정가 769,000원)",
                    imageUrl:
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&h=500&fit=crop",
                    currentPrice: 0,
                    startTime: dayAfterStart.toISOString(),
                    regularEndTime: dayAfterEnd.toISOString(),
                    overtimeStarted: false,
                    overtimeSeconds: 30,
                    status: "SCHEDULED",
                    totalBids: 0,
                    viewers: 0,
                },
                {
                    id: 4,
                    itemName: "Dyson 에어랩 컴플리트 롱",
                    description: "다이슨 에어랩 멀티 스타일러 컴플리트 롱 (정가 790,000원)",
                    imageUrl:
                        "https://images.unsplash.com/photo-1522338140262-f46f5913618a?w=500&h=500&fit=crop",
                    currentPrice: 0,
                    startTime: new Date(
                        dayAfterTomorrow.getFullYear(),
                        dayAfterTomorrow.getMonth(),
                        dayAfterTomorrow.getDate() + 1,
                        22,
                        0,
                        0
                    ).toISOString(),
                    regularEndTime: new Date(
                        dayAfterTomorrow.getFullYear(),
                        dayAfterTomorrow.getMonth(),
                        dayAfterTomorrow.getDate() + 1,
                        22,
                        15,
                        0
                    ).toISOString(),
                    overtimeStarted: false,
                    overtimeSeconds: 30,
                    status: "SCHEDULED",
                    totalBids: 0,
                    viewers: 0,
                },
            ];

            setAuctions(activeAuctions);
            setScheduledAuctions(scheduled);

            // 첫 진입 시 자동으로 진행 중인 경매 선택
            if (activeAuctions.length > 0 && !selectedAuction) {
                setSelectedAuction(activeAuctions[0]);
            }

            setIsLoading(false);
        } catch (error) {
            console.error("경매 목록 조회 실패:", error);
            setIsLoading(false);
        }
    };

    const fetchUserPoints = async () => {
        try {
            // TODO: API 연동
            setUserPoints(120);
        } catch (error) {
            console.error("사용자 포인트 조회 실패:", error);
        }
    };

    const fetchBidHistory = async (auctionId: number) => {
        console.log(`Fetching history for auction ID: ${auctionId}`);
        try {
            // TODO: API 연동
            setBidHistory([
                {
                    id: 1,
                    userId: 2,
                    userName: "김건강",
                    bidAmount: 45,
                    bidTime: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
                    status: "ACTIVE",
                },
                {
                    id: 2,
                    userId: 3,
                    userName: "이웰빙",
                    bidAmount: 42,
                    bidTime: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
                    status: "OUTBID",
                },
                {
                    id: 3,
                    userId: 4,
                    userName: "박헬스",
                    bidAmount: 38,
                    bidTime: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
                    status: "OUTBID",
                },
            ]);
        } catch (error) {
            console.error("입찰 내역 조회 실패:", error);
        }
    };

    const handlePlaceBid = async () => {
        if (!selectedAuction) return;

        const amount = parseInt(bidAmount);
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
            // TODO: API 연동
            alert("입찰이 완료되었습니다!");
            fetchAuctions();
            fetchBidHistory(selectedAuction.id);
            fetchUserPoints();
        } catch (error) {
            console.error("입찰 실패:", error);
            alert("입찰에 실패했습니다.");
        }
    };

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hours = String(date.getHours()).padStart(2, "0");
        const minutes = String(date.getMinutes()).padStart(2, "0");
        return `${year}.${month}.${day} ${hours}:${minutes}`;
    };

    const formatCountdown = (seconds: number) => {
        if (seconds >= 60) {
            const mins = Math.floor(seconds / 60);
            const secs = seconds % 60;
            return `${mins}:${String(secs).padStart(2, "0")}`;
        }
        return `${seconds}초`;
    };

    const getStatusBadge = (status: AuctionItem["status"]) => {
        switch (status) {
            case "ACTIVE":
                return (
                    <Badge className="bg-green-500 text-white border-0 font-bold">
                        <Gavel className="w-3 h-3 mr-1"/>
                        진행 중
                    </Badge>
                );
            case "SCHEDULED":
                return (
                    <Badge className="bg-blue-500 text-white border-0 font-bold">
                        <Clock className="w-3 h-3 mr-1"/>
                        예정
                    </Badge>
                );
            case "ENDED":
                return (
                    <Badge className="bg-gray-500 text-white border-0 font-bold">
                        <Trophy className="w-3 h-3 mr-1"/>
                        종료
                    </Badge>
                );
            default:
                return null;
        }
    };

    return (
        <div className="relative h-screen w-full flex flex-col overflow-hidden">
            <AnimatedBackground variant="member"/>

            <ScrollArea className="flex-1 w-full h-full relative z-10">
                <div className="relative p-8">
                    <div className="max-w-7xl mx-auto space-y-8">
                        {/* Header */}
                        <div className="flex items-center justify-between">
                            <div>
                                <h1 className="text-4xl font-black text-gray-900 mb-2">경매</h1>
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

                        {/* Auction Grid */}
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                            {/* Auction List */}
                            <div className="lg:col-span-2 space-y-6">
                                {/* 진행 중인 경매 */}
                                <div>
                                    <h2 className="text-2xl font-black text-gray-900 mb-4">
                                        진행 중인 경매
                                    </h2>
                                    {isLoading ? (
                                        <div className="h-48 rounded-xl animate-pulse bg-gray-200"/>
                                    ) : auctions.length > 0 ? (
                                        <div className="space-y-4">
                                            {auctions.map((auction) => (
                                                <Card
                                                    key={auction.id}
                                                    className={`backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg transition-all ${
                                                        selectedAuction?.id === auction.id
                                                            ? "ring-2 ring-[#C93831]"
                                                            : ""
                                                    }`}
                                                    onClick={() => setSelectedAuction(auction)}
                                                >
                                                    <div className="p-6">
                                                        <div className="flex gap-6">
                                                            {/* Image */}
                                                            <div
                                                                className="flex-shrink-0 w-32 h-32 sm:w-40 sm:h-40 bg-gray-100 rounded-lg overflow-hidden">
                                                                <img
                                                                    src={
                                                                        auction.imageUrl ||
                                                                        "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400"
                                                                    }
                                                                    alt={auction.itemName}
                                                                    className="w-full h-full object-cover"
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
                                                                        <div
                                                                            className="flex items-center gap-2 mt-2 text-xs text-gray-500 font-bold">
                                                                            <Calendar className="w-3 h-3"/>
                                                                            {formatDate(auction.startTime)}
                                                                        </div>
                                                                    </div>
                                                                    {getStatusBadge(auction.status)}
                                                                </div>

                                                                <div className="grid grid-cols-1 gap-4 mt-4">
                                                                    <div>
                                                                        <p className="text-xs text-gray-500 font-bold mb-1">
                                                                            현재가
                                                                        </p>
                                                                        <p className="text-2xl font-black text-[#C93831]">
                                                                            {auction.currentPrice.toLocaleString()}
                                                                            <span className="text-sm ml-1">P</span>
                                                                        </p>
                                                                    </div>
                                                                </div>

                                                                <div className="flex items-center gap-4 mt-3">
                                                                    {selectedAuction?.id === auction.id && (
                                                                        <div className={`flex items-center gap-1 text-sm font-bold ${
                                                                            isOvertime
                                                                                ? "text-red-600" // [수정됨] 초읽기(Overtime) 상태면 무조건 빨간색
                                                                                : "text-gray-600" // 평소에는 회색
                                                                        }`}>
                                                                            <Clock className="w-4 h-4" />
                                                                            {isOvertime
                                                                                ? `초읽기 ${countdown}초`
                                                                                : formatCountdown(countdown)
                                                                            }
                                                                        </div>
                                                                    )}
                                                                    <div
                                                                        className="flex items-center gap-1 text-sm font-bold text-gray-600">
                                                                        <Users className="w-4 h-4"/>
                                                                        {auction.totalBids}명 입찰
                                                                    </div>
                                                                    <div
                                                                        className="flex items-center gap-1 text-sm font-bold text-orange-600">
                                                                        <Eye className="w-4 h-4"/>
                                                                        {auction.viewers}명 시청 중
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </Card>
                                            ))}
                                        </div>
                                    ) : (
                                        <Card
                                            className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg p-12">
                                            <div className="text-center text-gray-500">
                                                <Clock className="w-12 h-12 mx-auto mb-3 text-gray-400"/>
                                                <p className="font-bold">진행 중인 경매가 없습니다</p>
                                            </div>
                                        </Card>
                                    )}
                                </div>

                                {/* 예정된 경매 */}
                                {scheduledAuctions.length > 0 && (
                                    <div>
                                        <h2 className="text-2xl font-black text-gray-900 mb-4">
                                            예정된 경매
                                        </h2>
                                        <div className="space-y-4">
                                            {scheduledAuctions.map((auction) => (
                                                <Card
                                                    key={auction.id}
                                                    className="bg-white border border-gray-200 shadow-sm opacity-70 cursor-not-allowed"
                                                >
                                                    <div className="p-6">
                                                        <div className="flex flex-col sm:flex-row gap-6">

                                                            {/* [수정됨] Image Section: 고정 사이즈 + 흑백 처리 */}
                                                            <div
                                                                className="flex-shrink-0 w-32 h-32 sm:w-40 sm:h-40 bg-gray-100 rounded-lg overflow-hidden">
                                                                <img
                                                                    src={
                                                                        auction.imageUrl ||
                                                                        "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400"
                                                                    }
                                                                    alt={auction.itemName}
                                                                    className="w-full h-full object-cover grayscale"
                                                                />
                                                            </div>

                                                            <div className="flex-1 min-w-0">
                                                                <div className="flex items-start justify-between mb-2">
                                                                    <div>
                                                                        <h3 className="text-xl font-black text-gray-900 mb-1">
                                                                            {auction.itemName}
                                                                        </h3>
                                                                        <p className="text-sm text-gray-600 font-medium">
                                                                            {auction.description}
                                                                        </p>
                                                                        <div
                                                                            className="flex items-center gap-2 mt-2 text-xs text-gray-500 font-bold">
                                                                            <Calendar className="w-3 h-3"/>
                                                                            {formatDate(auction.startTime)}
                                                                        </div>
                                                                    </div>
                                                                    {getStatusBadge(auction.status)}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </Card>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Bidding Panel */}
                            <div className="space-y-4">
                                <h2 className="text-2xl font-black text-gray-900 mb-4">
                                    입찰하기
                                </h2>

                                {selectedAuction && selectedAuction.status === "ACTIVE" ? (
                                    <>
                                        {/* Bidding Form */}
                                        <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg">
                                            <CardHeader>
                                                <CardTitle className="text-lg font-black">
                                                    {selectedAuction.itemName}
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
                                                    <p className="text-xs text-gray-500 mt-1 font-medium">
                                                        현재가보다 높은 금액을 입찰해주세요
                                                    </p>
                                                </div>

                                                <Button
                                                    onClick={handlePlaceBid}
                                                    className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold hover:shadow-lg"
                                                >
                                                    <TrendingUp className="w-4 h-4 mr-2"/>
                                                    입찰하기
                                                </Button>
                                            </CardContent>
                                        </Card>

                                        {/* Bid History */}
                                        <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg">
                                            <CardHeader>
                                                <CardTitle className="text-lg font-black">
                                                    입찰 내역
                                                </CardTitle>
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
                                                                            <Badge
                                                                                className="bg-green-500 text-white text-xs border-0 font-bold">
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
                                ) : (
                                    <Card className="backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg">
                                        <CardContent className="text-center py-12">
                                            {selectedAuction?.status === "SCHEDULED" ? (
                                                <>
                                                    <Clock className="w-12 h-12 mx-auto text-gray-400 mb-3"/>
                                                    <p className="text-sm text-gray-600 font-bold">
                                                        경매 시작 전입니다
                                                    </p>
                                                </>
                                            ) : selectedAuction?.status === "ENDED" ? (
                                                <>
                                                    <Trophy className="w-12 h-12 mx-auto text-gray-400 mb-3"/>
                                                    <p className="text-sm text-gray-600 font-bold">
                                                        경매가 종료되었습니다
                                                    </p>
                                                </>
                                            ) : (
                                                <>
                                                    <AlertCircle className="w-12 h-12 mx-auto text-gray-400 mb-3"/>
                                                    <p className="text-sm text-gray-600 font-bold">
                                                        입찰 가능한 경매가 없습니다
                                                    </p>
                                                </>
                                            )}
                                        </CardContent>
                                    </Card>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </ScrollArea>
        </div>
    );
}
