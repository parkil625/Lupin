/**
 * Home.tsx
 *
 * 회원 대시보드 홈 페이지 컴포넌트
 * - 오늘의 활동, 챌린지, 최근 피드 표시
 * - 점수 현황 및 프로필 정보 표시
 */

import {useState, useEffect} from "react";
import {Card} from "@/components/ui/card";
import {Badge} from "@/components/ui/badge";
import {Avatar} from "@/components/ui/avatar";
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from "@/components/ui/tooltip";
import {
    Heart,
    MessageCircle,
    Sparkles,
    Flame,
    Award,
    User,
    Plus,
} from "lucide-react";
import {Feed} from "@/types/dashboard.types";
import AdPopupDialog from "../dialogs/AdPopupDialog";
import PrizeClaimDialog from "../dialogs/PrizeClaimDialog";
import LotteryResultDialog from "../dialogs/LotteryResultDialog";
import {userApi, feedApi, lotteryApi} from "@/api";

interface HomeProps {
    challengeJoined: boolean;
    handleJoinChallenge: () => void;
    profileImage: string | null;
    myFeeds: Feed[];
    setSelectedFeed: (feed: Feed) => void;
    setFeedImageIndex: (feedId: number, index: number) => void;
    setShowFeedDetailInHome: (show: boolean) => void;
    onCreateClick: () => void;
    refreshTrigger?: number;
}

const AD_POPUP_KEY = "adPopupHiddenUntil";
const SEEN_LOTTERY_RESULTS_KEY = "seenLotteryResults";

export default function Home({
                                 handleJoinChallenge,
                                 profileImage,
                                 myFeeds,
                                 setSelectedFeed,
                                 setFeedImageIndex,
                                 setShowFeedDetailInHome,
                                 onCreateClick,
                                 refreshTrigger,
                             }: HomeProps) {
    const [showAdPopup, setShowAdPopup] = useState(false);
    const [showPrizeClaim, setShowPrizeClaim] = useState(false);
    const [showLoseResult, setShowLoseResult] = useState(false);
    const [prizeClaimData, setPrizeClaimData] = useState({
        ticketId: 0,
        prizeAmount: "100만원"
    });
    const [canPostToday, setCanPostToday] = useState(true);
    const [userStats, setUserStats] = useState({
        points: 0,
        lotteryTickets: 0,
        rank: 0,
        has7DayStreak: false,
        isTop10: false,
        isTop100: false,
        name: "",
    });

    // 7일 연속 체크 함수
    const checkSevenDayStreak = (feeds: Feed[]) => {
        if (feeds.length < 7) return false;

        // 피드를 날짜순으로 정렬 (최신순)
        const sortedFeeds = [...feeds].sort(
            (a, b) => new Date(b.time).getTime() - new Date(a.time).getTime()
        );

        // 최근 7일 연속 체크
        const today = new Date();
        for (let i = 0; i < 7; i++) {
            const targetDate = new Date(today);
            targetDate.setDate(today.getDate() - i);

            const hasPostOnDate = sortedFeeds.some((feed) => {
                const feedDate = new Date(feed.time);
                return feedDate.toDateString() === targetDate.toDateString();
            });

            if (!hasPostOnDate) return false;
        }

        return true;
    };

    // 테스트 URL 콘솔 출력
    useEffect(() => {
        console.log("=== 추첨 테스트 URL ===");
        console.log("수동 추첨 실행:", `${window.location.origin}/?runDraw=true`);
        console.log("당첨 팝업 테스트:", `${window.location.origin}/?testWin=true`);
        console.log("낙첨 팝업 테스트:", `${window.location.origin}/?testLose=true`);
        console.log("========================");
    }, []);

    // 추첨 결과 확인 (당첨 + 낙첨)
    useEffect(() => {
        const checkLotteryResults = async () => {
            try {
                const userId = parseInt(localStorage.getItem("userId") || "1");

                // URL 파라미터로 테스트 모드 체크
                const urlParams = new URLSearchParams(window.location.search);

                // 수동 추첨 실행
                if (urlParams.get("runDraw") === "true") {
                    try {
                        await lotteryApi.runManualDraw();
                        // URL에서 파라미터 제거
                        window.history.replaceState({}, '', window.location.pathname);
                        // 추첨 후 바로 결과 확인 계속 진행
                    } catch (error) {
                        console.error("추첨 실행 실패:", error);
                        alert("추첨 실행에 실패했습니다.");
                        return;
                    }
                }

                if (urlParams.get("testWin") === "true") {
                    const prize = urlParams.get("prize") || "100만원";
                    setPrizeClaimData({ticketId: 1, prizeAmount: prize});
                    setShowPrizeClaim(true);
                    return;
                }
                if (urlParams.get("testLose") === "true") {
                    setShowLoseResult(true);
                    return;
                }

                // 모든 추첨권 조회
                const allTickets = await lotteryApi.getAllTickets(userId);
                const claims = await lotteryApi.getPrizeClaims(userId);

                // 이미 수령 신청한 티켓 ID 목록
                const claimedTicketIds = claims.map((c: any) => c.lotteryTicket?.id);

                // 이미 본 추첨 결과 ID 목록
                const seenResults = JSON.parse(localStorage.getItem(SEEN_LOTTERY_RESULTS_KEY) || "[]");

                // 사용된 티켓 중 결과가 있는 것들
                const ticketsWithResults = allTickets.filter(
                    (ticket: any) => ticket.isUsed === "Y" && ticket.winResult
                );

                // 미수령 당첨 티켓 찾기 (우선순위 높음)
                const unclaimedWinTicket = ticketsWithResults.find(
                    (ticket: any) =>
                        !ticket.winResult.includes("낙첨") &&
                        !claimedTicketIds.includes(ticket.id)
                );

                if (unclaimedWinTicket) {
                    const prizeAmount = unclaimedWinTicket.winResult?.includes("100만원")
                        ? "100만원"
                        : "50만원";
                    setPrizeClaimData({
                        ticketId: unclaimedWinTicket.id,
                        prizeAmount
                    });
                    setShowPrizeClaim(true);
                    return;
                }

                // 안 본 낙첨 결과 찾기
                const unseenLoseTicket = ticketsWithResults.find(
                    (ticket: any) =>
                        ticket.winResult.includes("낙첨") &&
                        !seenResults.includes(ticket.id)
                );

                if (unseenLoseTicket) {
                    setShowLoseResult(true);
                    // 본 것으로 표시
                    const newSeenResults = [...seenResults, unseenLoseTicket.id];
                    localStorage.setItem(SEEN_LOTTERY_RESULTS_KEY, JSON.stringify(newSeenResults));
                }
            } catch (error) {
                console.error("추첨 결과 확인 실패:", error);
            }
        };

        checkLotteryResults();
    }, []);

    // 사용자 통계 로드
    useEffect(() => {
        const fetchUserStats = async () => {
            try {
                const userId = parseInt(localStorage.getItem("userId") || "1");

                // 사용자 정보 조회
                const user = await userApi.getUserById(userId);

                // 랭킹 컨텍스트 조회 (현재 사용자의 순위 포함)
                const rankingContext = await userApi.getUserRankingContext(userId);
                const myRanking = rankingContext.find((r: any) => r.id === userId);

                // 미사용 추첨권 개수 조회
                const ticketData = await lotteryApi.getUnusedTicketCount(userId);

                // 7일 연속 체크 (myFeeds가 7개 이상이고 연속인지 확인)
                const has7DayStreak = checkSevenDayStreak(myFeeds);

                const rank = myRanking?.rank || 999;
                setUserStats({
                    points: user.currentPoints || 0,
                    lotteryTickets: ticketData.count || 0,
                    rank: rank,
                    has7DayStreak,
                    isTop10: rank <= 10,
                    isTop100: rank <= 100,
                    name: user.realName || localStorage.getItem("userName") || "사용자",
                });
            } catch (error) {
                console.error("사용자 통계 로드 실패:", error);
            }
        };

        fetchUserStats();
    }, [myFeeds]);

    // 오늘 피드 작성 가능 여부 확인
    useEffect(() => {
        const checkCanPost = async () => {
            try {
                const userId = parseInt(localStorage.getItem("userId") || "1");
                const canPost = await feedApi.canPostToday(userId);
                setCanPostToday(canPost);
            } catch (error) {
                console.error("피드 작성 가능 여부 확인 실패:", error);
            }
        };

        checkCanPost();
    }, [myFeeds, refreshTrigger]); // myFeeds가 변경되거나 refreshTrigger가 변경되면 다시 확인

    useEffect(() => {
        // 개발/테스트 모드: URL에 ?showAd=true가 있으면 강제로 표시
        const urlParams = new URLSearchParams(window.location.search);
        const forceShowAd = urlParams.get("showAd") === "true";

        if (forceShowAd) {
            console.log("광고 팝업 강제 표시 (테스트 모드)");
            localStorage.removeItem(AD_POPUP_KEY);
            const timer = setTimeout(() => {
                setShowAdPopup(true);
            }, 500);
            return () => clearTimeout(timer);
        }

        // 페이지 로드 시 광고 팝업 표시 여부 확인
        const hiddenUntil = localStorage.getItem(AD_POPUP_KEY);
        if (hiddenUntil) {
            const hiddenTime = parseInt(hiddenUntil);
            if (Date.now() < hiddenTime) {
                // 아직 숨김 시간이 유효함
                console.log(
                    "광고 팝업 숨김 중 (남은 시간:",
                    Math.floor((hiddenTime - Date.now()) / 1000 / 60),
                    "분)"
                );
                console.log(
                    "테스트하려면 URL에 ?showAd=true를 추가하거나 콘솔에서 localStorage.removeItem('adPopupHiddenUntil')을 실행하세요"
                );
                return;
            } else {
                // 숨김 시간이 만료됨, localStorage에서 제거
                localStorage.removeItem(AD_POPUP_KEY);
            }
        }
        // 1초 후 광고 팝업 표시
        const timer = setTimeout(() => {
            console.log("광고 팝업 표시");
            setShowAdPopup(true);
        }, 1000);
        return () => clearTimeout(timer);
    }, []);

    const handleCloseAdPopup = () => {
        setShowAdPopup(false);
    };

    const handleDontShowFor24Hours = () => {
        const hideUntil = Date.now() + 24 * 60 * 60 * 1000; // 24시간 후
        localStorage.setItem(AD_POPUP_KEY, hideUntil.toString());
        setShowAdPopup(false);
    };

    const handleJoinChallengeFromPopup = () => {
        handleJoinChallenge();
        setShowAdPopup(false);
    };

    return (
        <div className="h-full overflow-auto p-8">
            <div className="max-w-6xl mx-auto space-y-8">
                {/* Profile Header */}
                <div className="p-8">
                    <div className="flex items-start gap-8 mb-8">
                        <Avatar className="w-40 h-40 border-4 border-white shadow-xl bg-gray-100">
                            {profileImage ? (
                                <img
                                    src={profileImage}
                                    alt="Profile"
                                    className="w-full h-full object-cover"
                                />
                            ) : (
                                <div className="w-full h-full flex items-center justify-center bg-white">
                                    <User className="w-20 h-20 text-gray-400"/>
                                </div>
                            )}
                        </Avatar>

                        <div className="flex-1">
                            <h1 className="text-3xl font-black text-gray-900 mb-4">
                                {userStats.name}
                            </h1>

                            <div className="flex gap-8 mb-4">
                                <div>
                                    <span className="text-sm text-gray-600 font-bold">피드 </span>
                                    <span className="text-sm font-black text-[#C93831]">
                    {myFeeds.length}
                  </span>
                                </div>
                                <div>
                  <span className="text-sm text-gray-600 font-bold">
                    이번 달 점수{" "}
                  </span>
                                    <span className="text-sm font-black text-[#C93831]">
                    {userStats.points}
                  </span>
                                </div>
                                <div>
                  <span className="text-sm text-gray-600 font-bold">
                    추첨권{" "}
                  </span>
                                    <span className="text-sm font-black text-[#C93831]">
                    {userStats.lotteryTickets}
                  </span>
                                </div>
                                <div>
                  <span className="text-sm text-gray-600 font-bold">
                    현재 점수{" "}
                  </span>
                                    <span className="text-sm font-black text-yellow-600">
                    {userStats.points % 30}
                  </span>
                                    <span className="text-xs text-gray-500 ml-1">
                    (+{30 - (userStats.points % 30) || 30}점 → 추첨권)
                  </span>
                                </div>
                                <div>
                                    <span className="text-sm text-gray-600 font-bold">순위 </span>
                                    <span className="text-sm font-black text-[#C93831]">
                    #{userStats.rank || "-"}
                  </span>
                                </div>
                            </div>

                            <div className="flex gap-2 flex-wrap">
                                {userStats.has7DayStreak && (
                                    <Badge
                                        className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                                        <Flame className="w-3 h-3 mr-1"/>
                                        7일 연속
                                    </Badge>
                                )}
                                {userStats.isTop10 && (
                                    <Badge
                                        className="bg-gradient-to-r from-yellow-400 via-yellow-500 to-yellow-600 text-white px-3 py-1.5 font-bold border-0 text-xs">
                                        <Award className="w-3 h-3 mr-1"/>
                                        TOP 10
                                    </Badge>
                                )}
                                {!userStats.isTop10 && userStats.isTop100 && (
                                    <Badge
                                        className="bg-gradient-to-r from-purple-400 to-pink-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                                        <Award className="w-3 h-3 mr-1"/>
                                        TOP 100
                                    </Badge>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Posts Section */}
                <div>
                    {/* Posts Header */}
                    <div className="flex items-center justify-between mb-6 px-8">
                        <h2 className="text-2xl font-black text-gray-900">피드</h2>
                        <TooltipProvider>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button
                                        onClick={canPostToday ? onCreateClick : undefined}
                                        disabled={!canPostToday}
                                        className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-all font-bold ${
                                            canPostToday
                                                ? "bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white hover:shadow-lg cursor-pointer"
                                                : "bg-gray-300 text-gray-500 cursor-not-allowed"
                                        }`}
                                    >
                                        <Plus className="w-5 h-5"/>
                                        만들기
                                    </button>
                                </TooltipTrigger>
                                <TooltipContent side="top" sideOffset={8}>
                                    <p>{canPostToday ? "피드 작성" : "하루에 한 번만 피드를 작성할 수 있습니다."}</p>
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    </div>

                    {/* Posts Grid */}
                    <div className="grid grid-cols-5 gap-3">
                        {myFeeds.map((feed) => (
                            <div
                                key={feed.id}
                                className="cursor-pointer group aspect-[3/4]"
                                onClick={() => {
                                    setSelectedFeed(feed);
                                    setFeedImageIndex(feed.id, 0);
                                    setShowFeedDetailInHome(true);
                                }}
                            >
                                <Card
                                    className="h-full overflow-hidden backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg hover:shadow-2xl transition-all relative">
                                    <div className="w-full h-full bg-white">
                                        {feed.images && feed.images.length > 0 ? (
                                            <img
                                                src={feed.images[0]}
                                                alt={feed.activity}
                                                className="w-full h-full object-cover"
                                            />
                                        ) : (
                                            <div
                                                className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-100 to-gray-200">
                                                <div className="text-center p-4">
                                                    <Sparkles className="w-12 h-12 mx-auto text-gray-400 mb-2"/>
                                                    <p className="text-sm font-bold text-gray-600">
                                                        {feed.activity}
                                                    </p>
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    <div
                                        className="absolute inset-0 bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-sm">
                                        <div className="text-center text-white space-y-2">
                                            <div className="flex items-center justify-center gap-4">
                        <span className="flex items-center gap-1 font-bold text-base">
                          <Heart className="w-5 h-5"/>
                            {feed.likes}
                        </span>
                                                <span className="flex items-center gap-1 font-bold text-base">
                          <MessageCircle className="w-5 h-5"/>
                                                    {feed.comments}
                        </span>
                                            </div>
                                            <div className="text-sm font-bold">
                                                <Sparkles className="w-4 h-4 inline mr-1"/>+
                                                {feed.points}점
                                            </div>
                                        </div>
                                    </div>
                                </Card>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* 광고 팝업 */}
            <AdPopupDialog
                open={showAdPopup}
                onClose={handleCloseAdPopup}
                onDontShowFor24Hours={handleDontShowFor24Hours}
                onJoinChallenge={handleJoinChallengeFromPopup}
            />

            {/* 당첨 팝업 */}
            <PrizeClaimDialog
                open={showPrizeClaim}
                onClose={() => setShowPrizeClaim(false)}
                prizeAmount={prizeClaimData.prizeAmount}
                ticketId={prizeClaimData.ticketId}
            />

            {/* 낙첨 결과 팝업 */}
            <LotteryResultDialog
                open={showLoseResult}
                onClose={() => setShowLoseResult(false)}
            />
        </div>
    );
}
