/**
 * Home.tsx
 *
 * 회원 대시보드 홈 페이지 컴포넌트
 * - 오늘의 활동, 최근 피드 표시
 * - 점수 현황 및 프로필 정보 표시
 */

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar } from "@/components/ui/avatar";
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
import { Feed } from "@/types/dashboard.types";
import { userApi, feedApi } from "@/api";

interface HomeProps {
  profileImage: string | null;
  myFeeds: Feed[];
  setSelectedFeed: (feed: Feed) => void;
  setFeedImageIndex: (feedId: number, index: number) => void;
  setShowFeedDetailInHome: (show: boolean) => void;
  onCreateClick: () => void;
  refreshTrigger?: number;
}

export default function Home({
  profileImage,
  myFeeds,
  setSelectedFeed,
  setFeedImageIndex,
  setShowFeedDetailInHome,
  onCreateClick,
  refreshTrigger,
}: HomeProps) {
  const [canPostToday, setCanPostToday] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [userStats, setUserStats] = useState({
    points: 0,
    rank: 0,
    has7DayStreak: false,
    isTop10: false,
    isTop100: false,
    name: "",
    monthlyLikes: 0,
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

        // 7일 연속 체크 (myFeeds가 7개 이상이고 연속인지 확인)
        const has7DayStreak = checkSevenDayStreak(myFeeds);

        const rank = myRanking?.rank || 999;
        setUserStats({
          points: user.currentPoints || 0,
          rank: rank,
          has7DayStreak,
          isTop10: rank <= 10,
          isTop100: rank <= 100,
          name: user.realName || localStorage.getItem("userName") || "사용자",
          monthlyLikes: user.monthlyLikes || 0,
        });
      } catch (error) {
        console.error("사용자 통계 로드 실패:", error);
      } finally {
        setIsLoading(false);
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
                  <User className="w-20 h-20 text-gray-400" />
                </div>
              )}
            </Avatar>

            <div className="flex-1">
              {isLoading ? (
                <div className="mb-4 rounded-lg animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)', width: '89px', height: '36px' }} />
              ) : (
                <h1 className="text-3xl font-black text-gray-900 mb-4">
                  {userStats.name}
                </h1>
              )}

              {isLoading ? (
                <div className="mb-4 rounded-lg animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)', width: '424px', height: '24px' }} />
              ) : (
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
                      좋아요{" "}
                    </span>
                    <span className="text-sm font-black text-[#C93831]">
                      {userStats.monthlyLikes}
                    </span>
                  </div>
                  <div>
                    <span className="text-sm text-gray-600 font-bold">순위 </span>
                    <span className="text-sm font-black text-[#C93831]">
                      #{userStats.rank || "-"}
                    </span>
                  </div>
                </div>
              )}

              {isLoading ? (
                <div className="flex gap-2">
                  <div className="rounded-md animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)', width: '85px', height: '28px' }} />
                  <div className="rounded-md animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)', width: '85px', height: '28px' }} />
                </div>
              ) : (
                <div className="flex gap-2 flex-wrap">
                  {userStats.has7DayStreak && (
                    <Badge className="bg-orange-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                      <Flame className="w-3 h-3 mr-1" />
                      7일 연속
                    </Badge>
                  )}
                  {userStats.isTop10 && (
                    <Badge className="bg-yellow-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                      <Award className="w-3 h-3 mr-1" />
                      TOP 10
                    </Badge>
                  )}
                  {!userStats.isTop10 && userStats.isTop100 && (
                    <Badge className="bg-purple-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                      <Award className="w-3 h-3 mr-1" />
                      TOP 100
                    </Badge>
                  )}
                </div>
              )}
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
                    <Plus className="w-5 h-5" />
                    만들기
                  </button>
                </TooltipTrigger>
                <TooltipContent side="top" sideOffset={8}>
                  <p>
                    {canPostToday
                      ? "피드 작성"
                      : "하루에 한 번만 피드를 작성할 수 있습니다."}
                  </p>
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
                <Card className="h-full overflow-hidden backdrop-blur-xl bg-white/60 border border-gray-200 shadow-lg hover:shadow-2xl transition-all relative">
                  <div className="w-full h-full bg-white">
                    {feed.images && feed.images.length > 0 ? (
                      <img
                        src={feed.images[0]}
                        alt={feed.activity}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-100 to-gray-200">
                        <div className="text-center p-4">
                          <Sparkles className="w-12 h-12 mx-auto text-gray-400 mb-2" />
                          <p className="text-sm font-bold text-gray-600">
                            {feed.activity}
                          </p>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="absolute inset-0 bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-sm">
                    <div className="text-center text-white space-y-2">
                      <div className="flex items-center justify-center gap-4">
                        <span className="flex items-center gap-1 font-bold text-base">
                          <Heart className="w-5 h-5" />
                          {feed.likes}
                        </span>
                        <span className="flex items-center gap-1 font-bold text-base">
                          <MessageCircle className="w-5 h-5" />
                          {feed.comments}
                        </span>
                      </div>
                      <div className="text-sm font-bold">
                        <Sparkles className="w-4 h-4 inline mr-1" />+
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
    </div>
  );
}
