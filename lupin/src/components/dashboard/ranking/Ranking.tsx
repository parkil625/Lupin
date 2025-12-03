/**
 * Ranking.tsx
 *
 * 랭킹 페이지 컴포넌트
 * - 사용자 점수 기반 순위 표시
 * - 상위 랭커 하이라이트
 * - 실제 user 테이블 데이터 반영
 */

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  HoverCard,
  HoverCardTrigger,
  HoverCardContent,
} from "@/components/ui/hover-card";
import { TrendingUp, Users, User, Crown, Dumbbell, Heart } from "lucide-react";
import { userApi } from "@/api";
import { toast } from "sonner";

interface RankingProps {
  userId: number;
  profileImage: string | null;
}

interface RankerData {
  rank: number;
  name: string;
  points: number;
  monthlyLikes: number;
  avatar: string;
  profileImage?: string;
  department: string;
  activeDays: number;
  avgScore: number;
  isMe?: boolean;
}

interface Statistics {
  totalUsers: number;
  activeUsersThisMonth: number;
  averagePoints: number;
}

export default function Ranking({ userId, profileImage }: RankingProps) {
  const currentMonth = new Date().getMonth() + 1;
  const [topRankers, setTopRankers] = useState<RankerData[]>([]);
  const [belowRankers, setBelowRankers] = useState<RankerData[]>([]);
  const [statistics, setStatistics] = useState<Statistics>({
    totalUsers: 0,
    activeUsersThisMonth: 0,
    averagePoints: 0,
  });
  const [myStats, setMyStats] = useState({
    activeDays: 0,
    avgScore: 0,
    streak: 0,
  });
  const [loading, setLoading] = useState(true);

  // 데이터 로드
  useEffect(() => {
    const fetchRankingData = async () => {
      try {
        setLoading(true);

        // 1~10등 조회
        const top10Response = await userApi.getTopUsersByPoints(10);
        const top10Users = top10Response.map((user: { id?: number; name?: string; points?: number; monthlyLikes?: number; profileImage?: string; avatar?: string; department?: string; activeDays?: number; avgScore?: number }, index: number) => ({
          rank: index + 1,
          name: user.name || "이름 없음",
          points: user.points || 0,
          monthlyLikes: user.monthlyLikes || 0,
          avatar: user.name ? user.name[0] : "?",
          profileImage: user.id === userId ? profileImage : (user.avatar || user.profileImage),
          department: user.department || "부서 미정",
          activeDays: user.activeDays || 0,
          avgScore: user.avgScore || 0,
          isMe: user.id === userId,
        }));

        setTopRankers(top10Users);

        // 현재 사용자가 10등 이내인지 확인
        const isInTop10 = top10Users.some((u: { isMe?: boolean }) => u.isMe);

        if (isInTop10) {
          // 10등 이내면 belowRankers는 빈 배열
          setBelowRankers([]);
          const currentUser = top10Users.find((u: { isMe?: boolean }) => u.isMe);
          if (currentUser) {
            setMyStats({
              activeDays: currentUser.activeDays,
              avgScore: currentUser.avgScore,
              streak: 0,
            });
          }
        } else {
          // 10등 밖이면 나를 중심으로 주변 3명 조회 (나-1, 나, 나+1)
          const contextResponse = await userApi.getUserRankingContext(userId);
          const contextUsers = contextResponse.map((user: { id?: number; name?: string; points?: number; monthlyLikes?: number; profileImage?: string; avatar?: string; department?: string; activeDays?: number; avgScore?: number; rank?: number }) => ({
            rank: user.rank || 0,
            name: user.name || "이름 없음",
            points: user.points || 0,
            monthlyLikes: user.monthlyLikes || 0,
            avatar: user.name ? user.name[0] : "?",
            profileImage: user.id === userId ? profileImage : (user.avatar || user.profileImage),
            department: user.department || "부서 미정",
            activeDays: user.activeDays || 0,
            avgScore: user.avgScore || 0,
            isMe: user.id === userId,
          }));
          setBelowRankers(contextUsers);

          const currentUser = contextUsers.find((u: { isMe?: boolean }) => u.isMe);
          if (currentUser) {
            setMyStats({
              activeDays: currentUser.activeDays,
              avgScore: currentUser.avgScore,
              streak: 0,
            });
          }
        }

        // 전체 통계 조회
        const statsResponse = await userApi.getStatistics();
        setStatistics({
          totalUsers: statsResponse.totalUsers || 0,
          activeUsersThisMonth: statsResponse.activeUsersThisMonth || 0,
          averagePoints: statsResponse.averagePoints || 0,
        });
      } catch (error) {
        console.error("랭킹 데이터 로드 실패:", error);
        toast.error("랭킹 데이터를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchRankingData();
  }, [userId, profileImage]);

  // 로딩 중일 때 스켈레톤 렌더링
  if (loading) {
    return (
      <ScrollArea className="h-full">
        <div className="p-4 md:p-8">
          <div className="max-w-7xl mx-auto w-full">
            <div className="mb-4 md:mb-6">
              <h1 className="text-3xl md:text-5xl font-black text-gray-900 mb-2">
                {currentMonth}월 랭킹
              </h1>
              <p className="text-gray-700 font-medium text-base md:text-lg">
                이번 달 TOP 운동왕은 누구?
              </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-4 md:gap-8">
              <div className="lg:col-span-2 flex flex-col gap-2">
                {Array.from({ length: 10 }).map((_, i) => (
                  <div key={i} className="w-full rounded-xl animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)', height: '58px' }} />
                ))}
              </div>
              <div className="space-y-6">
                {/* 내 통계 스켈레톤 */}
                <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
                  <div className="p-6 space-y-4">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                      <div className="h-6 w-20 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                    </div>
                    <div className="space-y-3">
                      {Array.from({ length: 3 }).map((_, i) => (
                        <div key={i} className="flex justify-between items-center">
                          <div className="h-5 w-24 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                          <div className="h-7 w-12 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                        </div>
                      ))}
                    </div>
                  </div>
                </Card>
                {/* 전체 현황 스켈레톤 */}
                <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
                  <div className="p-6 space-y-4">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                      <div className="h-6 w-24 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                    </div>
                    <div className="space-y-3">
                      {Array.from({ length: 3 }).map((_, i) => (
                        <div key={i} className="flex justify-between items-center">
                          <div className="h-5 w-24 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                          <div className="h-7 w-12 rounded animate-pulse" style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }} />
                        </div>
                      ))}
                    </div>
                  </div>
                </Card>
              </div>
            </div>
          </div>
        </div>
      </ScrollArea>
    );
  }

  return (
    <ScrollArea className="h-full">
      <div className="p-4 md:p-8">
        <div className="max-w-7xl mx-auto w-full">
          <div className="mb-4 md:mb-6">
            <h1 className="text-3xl md:text-5xl font-black text-gray-900 mb-2">
              {currentMonth}월 랭킹
            </h1>
            <p className="text-gray-700 font-medium text-base md:text-lg">
              이번 달 TOP 운동왕은 누구?
            </p>
          </div>

          <div className="grid lg:grid-cols-3 gap-4 md:gap-8">
            <div className="lg:col-span-2 flex flex-col gap-2">
            {/* Top 10 Rankers */}
            {topRankers.map((ranker) => (
              <Card
                key={ranker.rank}
                className="backdrop-blur-2xl border shadow-lg overflow-hidden transition-all bg-white/60 border-gray-200"
              >
                <div className="px-7 py-1 flex items-center w-full">
                  <div className="flex items-center gap-4 w-full">
                    <div className="w-10 flex items-center justify-center">
                      {ranker.rank === 1 ? (
                        <Crown
                          className="w-8 h-8"
                          style={{ color: "#FFD700", fill: "#FFD700" }}
                        />
                      ) : ranker.rank === 2 ? (
                        <Crown
                          className="w-7 h-7"
                          style={{ color: "#C0C0C0", fill: "#C0C0C0" }}
                        />
                      ) : ranker.rank === 3 ? (
                        <Crown
                          className="w-7 h-7"
                          style={{ color: "#CD7F32", fill: "#CD7F32" }}
                        />
                      ) : (
                        <span className="text-2xl font-black text-gray-900">
                          {ranker.rank}
                        </span>
                      )}
                    </div>

                    <HoverCard openDelay={200} closeDelay={100}>
                      <HoverCardTrigger asChild>
                        <div>
                          <Avatar className="w-10 h-10 border-2 border-white shadow-lg bg-white cursor-pointer">
                            {ranker.profileImage ? (
                              <img
                                src={ranker.profileImage}
                                alt={ranker.name}
                                className="w-full h-full object-cover rounded-full"
                              />
                            ) : (
                              <AvatarFallback className="bg-white">
                                <User className="w-5 h-5 text-gray-400" />
                              </AvatarFallback>
                            )}
                          </Avatar>
                        </div>
                      </HoverCardTrigger>
                      <HoverCardContent className="w-80 bg-white/95 backdrop-blur-xl border border-gray-200">
                        <div className="flex gap-4">
                          <Avatar className="w-14 h-14 border-2 border-white shadow-lg bg-white">
                            {ranker.profileImage ? (
                              <img
                                src={ranker.profileImage}
                                alt={ranker.name}
                                className="w-full h-full object-cover rounded-full"
                              />
                            ) : (
                              <AvatarFallback className="bg-white">
                                <User className="w-7 h-7 text-gray-400" />
                              </AvatarFallback>
                            )}
                          </Avatar>
                          <div className="space-y-2 flex-1">
                            <h4 className="text-base font-black text-gray-900">
                              {ranker.name}
                            </h4>
                            <p className="text-sm text-gray-700 font-medium">
                              {ranker.department}
                            </p>
                            <div className="pt-1 space-y-1.5">
                              <div className="flex justify-between text-xs">
                                <span className="text-gray-600 font-medium">
                                  이번 달 활동
                                </span>
                                <span className="text-gray-900 font-bold">
                                  {ranker.activeDays}일
                                </span>
                              </div>
                              <div className="flex justify-between text-xs">
                                <span className="text-gray-600 font-medium">
                                  평균 점수
                                </span>
                                <span className="text-gray-900 font-bold">
                                  {ranker.avgScore}점
                                </span>
                              </div>
                              <div className="flex justify-between text-xs">
                                <span className="text-gray-600 font-medium">
                                  총 점수
                                </span>
                                <span className="text-[#C93831] font-bold">
                                  {ranker.points}점
                                </span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </HoverCardContent>
                    </HoverCard>

                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-black text-lg text-gray-900">
                          {ranker.name}
                        </span>
                      </div>
                      <div className="text-gray-600 font-bold text-sm flex items-center gap-3">
                        <span className="flex items-center gap-1">
                          <Dumbbell className="w-4 h-4" />
                          {ranker.points}
                        </span>
                        <span className="flex items-center gap-1">
                          <Heart className="w-4 h-4" />
                          {ranker.monthlyLikes}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            ))}

            {/* Separator - 10등 밖일 때만 표시 */}
            {!loading && belowRankers.length > 0 && (
            <div className="flex items-center justify-center py-3">
              <div className="flex flex-col gap-1">
                <div className="w-1.5 h-1.5 rounded-full bg-gray-400"></div>
                <div className="w-1.5 h-1.5 rounded-full bg-gray-400"></div>
                <div className="w-1.5 h-1.5 rounded-full bg-gray-400"></div>
              </div>
            </div>
            )}

            {/* 11~13등 영역 */}
            {!loading && belowRankers.map((ranker) => (
                <Card
                  key={ranker.rank}
                  className={`backdrop-blur-2xl border shadow-lg overflow-hidden transition-all ${
                    ranker.isMe
                      ? "bg-gradient-to-r from-red-50/80 to-pink-50/80 border-[#C93831]"
                      : "bg-white/60 border-gray-200"
                  }`}
                >
                  <div className="px-7 py-1 flex items-center w-full">
                    <div className="flex items-center gap-4 w-full">
                      <div className="w-10 flex items-center justify-center">
                        <span className="text-2xl font-black text-gray-900">
                          {ranker.rank}
                        </span>
                      </div>

                      <HoverCard openDelay={200} closeDelay={100}>
                        <HoverCardTrigger asChild>
                          <div>
                            <Avatar className="w-10 h-10 border-2 border-white shadow-lg bg-white cursor-pointer">
                              {ranker.profileImage ? (
                                <img
                                  src={ranker.profileImage}
                                  alt={ranker.name}
                                  className="w-full h-full object-cover rounded-full"
                                />
                              ) : (
                                <AvatarFallback className="bg-white">
                                  <User className="w-5 h-5 text-gray-400" />
                                </AvatarFallback>
                              )}
                            </Avatar>
                          </div>
                        </HoverCardTrigger>
                        <HoverCardContent className="w-80 bg-white/95 backdrop-blur-xl border border-gray-200">
                          <div className="flex gap-4">
                            <Avatar className="w-14 h-14 border-2 border-white shadow-lg bg-white">
                              {ranker.profileImage ? (
                                <img
                                  src={ranker.profileImage}
                                  alt={ranker.name}
                                  className="w-full h-full object-cover rounded-full"
                                />
                              ) : (
                                <AvatarFallback className="bg-white">
                                  <User className="w-7 h-7 text-gray-400" />
                                </AvatarFallback>
                              )}
                            </Avatar>
                            <div className="space-y-2 flex-1">
                              <h4 className="text-base font-black text-gray-900">
                                {ranker.name}
                              </h4>
                              <p className="text-sm text-gray-700 font-medium">
                                {ranker.department}
                              </p>
                              <div className="pt-1 space-y-1.5">
                                <div className="flex justify-between text-xs">
                                  <span className="text-gray-600 font-medium">
                                    이번 달 활동
                                  </span>
                                  <span className="text-gray-900 font-bold">
                                    {ranker.activeDays}일
                                  </span>
                                </div>
                                <div className="flex justify-between text-xs">
                                  <span className="text-gray-600 font-medium">
                                    평균 점수
                                  </span>
                                  <span className="text-gray-900 font-bold">
                                    {ranker.avgScore}점
                                  </span>
                                </div>
                                <div className="flex justify-between text-xs">
                                  <span className="text-gray-600 font-medium">
                                    총 점수
                                  </span>
                                  <span className="text-[#C93831] font-bold">
                                    {ranker.points}점
                                  </span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </HoverCardContent>
                      </HoverCard>

                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-black text-lg text-gray-900">
                            {ranker.name}
                          </span>
                        </div>
                        <div className="text-gray-600 font-bold text-sm flex items-center gap-3">
                          <span className="flex items-center gap-1">
                            <Dumbbell className="w-4 h-4" />
                            {ranker.points}
                          </span>
                          <span className="flex items-center gap-1">
                            <Heart className="w-4 h-4" />
                            {ranker.monthlyLikes}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </Card>
              ))}
          </div>

          <div className="space-y-6">
            <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
              <div className="p-6 space-y-4">
                <h3 className="text-xl font-black text-gray-900 flex items-center gap-2">
                  <TrendingUp className="w-6 h-6 text-[#C93831]" />내 통계
                </h3>

                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">
                      이번 달 활동
                    </span>
                    {loading ? (
                      <Skeleton className="h-7 w-12" />
                    ) : (
                      <span className="font-black text-xl text-[#C93831]">
                        {myStats.activeDays}일
                      </span>
                    )}
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">총 점수</span>
                    {loading ? (
                      <Skeleton className="h-7 w-12" />
                    ) : (
                      <span className="font-black text-xl text-[#C93831]">
                        {myStats.avgScore}
                      </span>
                    )}
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">연속 기록</span>
                    {loading ? (
                      <Skeleton className="h-7 w-12" />
                    ) : (
                      <span className="font-black text-xl text-[#C93831]">
                        {myStats.streak}일
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </Card>

            <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
              <div className="p-6 space-y-4">
                <h3 className="text-xl font-black text-gray-900 flex items-center gap-2">
                  <Users className="w-6 h-6 text-[#C93831]" />
                  전체 현황
                </h3>

                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">총 참여자</span>
                    {loading ? (
                      <Skeleton className="h-7 w-12" />
                    ) : (
                      <span className="font-black text-xl text-gray-900">
                        {statistics.totalUsers}명
                      </span>
                    )}
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">
                      이번 달 활동
                    </span>
                    {loading ? (
                      <Skeleton className="h-7 w-12" />
                    ) : (
                      <span className="font-black text-xl text-gray-900">
                        {statistics.activeUsersThisMonth}명
                      </span>
                    )}
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">평균 점수</span>
                    {loading ? (
                      <Skeleton className="h-7 w-12" />
                    ) : (
                      <span className="font-black text-xl text-gray-900">
                        {statistics.averagePoints}점
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
    </ScrollArea>
  );
}
