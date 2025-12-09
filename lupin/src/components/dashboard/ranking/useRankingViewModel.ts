/**
 * useRankingViewModel.ts
 *
 * 랭킹 페이지의 비즈니스 로직과 상태 관리를 담당하는 커스텀 훅
 * - API 병렬 호출로 성능 최적화
 * - 파생 상태 useMemo로 관리
 */

import { useState, useEffect, useMemo } from "react";
import { userApi } from "@/api";
import { toast } from "sonner";
import { DEFAULT_VALUES } from "@/constants/rankingConstants";

export interface RankerData {
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

interface MyStats {
  activeDays: number;
  avgScore: number;
  streak: number;
}

interface UseRankingViewModelReturn {
  topRankers: RankerData[];
  belowRankers: RankerData[];
  statistics: Statistics;
  myStats: MyStats;
  loading: boolean;
  currentMonth: number;
}

export function useRankingViewModel(
  userId: number,
  profileImage: string | null
): UseRankingViewModelReturn {
  const [topRankers, setTopRankers] = useState<RankerData[]>([]);
  const [belowRankers, setBelowRankers] = useState<RankerData[]>([]);
  const [statistics, setStatistics] = useState<Statistics>({
    totalUsers: 0,
    activeUsersThisMonth: 0,
    averagePoints: 0,
  });
  const [myStats, setMyStats] = useState<MyStats>({
    activeDays: 0,
    avgScore: 0,
    streak: 0,
  });
  const [loading, setLoading] = useState(true);

  const currentMonth = useMemo(() => new Date().getMonth() + 1, []);

  useEffect(() => {
    const fetchRankingData = async () => {
      try {
        setLoading(true);

        // API 병렬 호출로 성능 최적화
        const [top10Response, statsResponse] = await Promise.all([
          userApi.getTopUsersByPoints(10),
          userApi.getStatistics(),
        ]);

        // 1~10등 매핑
        const top10Users = top10Response.map(
          (
            user: {
              id?: number;
              name?: string;
              points?: number;
              monthlyLikes?: number;
              profileImage?: string;
              avatar?: string;
              department?: string;
              activeDays?: number;
              avgScore?: number;
            },
            index: number
          ) => ({
            rank: index + 1,
            name: user.name || DEFAULT_VALUES.NAME,
            points: user.points || 0,
            monthlyLikes: user.monthlyLikes || 0,
            avatar: user.name ? user.name[0] : DEFAULT_VALUES.AVATAR_FALLBACK,
            profileImage:
              user.id === userId
                ? profileImage
                : user.avatar || user.profileImage,
            department: user.department || DEFAULT_VALUES.DEPARTMENT,
            activeDays: user.activeDays || 0,
            avgScore: user.avgScore || 0,
            isMe: user.id === userId,
          })
        );

        setTopRankers(top10Users);

        // 현재 사용자가 10등 이내인지 확인
        const isInTop10 = top10Users.some((u: { isMe?: boolean }) => u.isMe);

        if (isInTop10) {
          // 10등 이내면 belowRankers는 빈 배열
          setBelowRankers([]);
          const currentUser = top10Users.find(
            (u: { isMe?: boolean }) => u.isMe
          );
          if (currentUser) {
            setMyStats({
              activeDays: currentUser.activeDays,
              avgScore: currentUser.avgScore,
              streak: 0,
            });
          }
        } else {
          // 10등 밖이면 나를 중심으로 주변 3명 조회
          const contextResponse = await userApi.getUserRankingContext(userId);
          const contextUsers = contextResponse.map(
            (user: {
              id?: number;
              name?: string;
              points?: number;
              monthlyLikes?: number;
              profileImage?: string;
              avatar?: string;
              department?: string;
              activeDays?: number;
              avgScore?: number;
              rank?: number;
            }) => ({
              rank: user.rank || 0,
              name: user.name || DEFAULT_VALUES.NAME,
              points: user.points || 0,
              monthlyLikes: user.monthlyLikes || 0,
              avatar: user.name ? user.name[0] : DEFAULT_VALUES.AVATAR_FALLBACK,
              profileImage:
                user.id === userId
                  ? profileImage
                  : user.avatar || user.profileImage,
              department: user.department || DEFAULT_VALUES.DEPARTMENT,
              activeDays: user.activeDays || 0,
              avgScore: user.avgScore || 0,
              isMe: user.id === userId,
            })
          );
          setBelowRankers(contextUsers);

          const currentUser = contextUsers.find(
            (u: { isMe?: boolean }) => u.isMe
          );
          if (currentUser) {
            setMyStats({
              activeDays: currentUser.activeDays,
              avgScore: currentUser.avgScore,
              streak: 0,
            });
          }
        }

        // 전체 통계 설정
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
  }, [userId, profileImage, currentMonth]);

  return {
    topRankers,
    belowRankers,
    statistics,
    myStats,
    loading,
    currentMonth,
  };
}
