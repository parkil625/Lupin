import { Card } from "../ui/card";
import { Badge } from "../ui/badge";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { TrendingUp, Users } from "lucide-react";

export default function Ranking() {
  const currentMonth = new Date().getMonth() + 1;

  const rankers = [
    { rank: 1, name: "이철수", points: 520, avatar: "이", badge: "🥇" },
    { rank: 2, name: "박영희", points: 480, avatar: "박", badge: "🥈" },
    { rank: 3, name: "최민수", points: 450, avatar: "최", badge: "🥉" },
    { rank: 4, name: "정수진", points: 420, avatar: "정" },
    { rank: 5, name: "강민호", points: 390, avatar: "강" },
    { rank: 6, name: "윤서연", points: 370, avatar: "윤" },
    { rank: 7, name: "장동건", points: 350, avatar: "장" },
    { rank: 8, name: "송혜교", points: 330, avatar: "송" },
    { rank: 9, name: "전지현", points: 310, avatar: "전" },
    { rank: 10, name: "현빈", points: 290, avatar: "현" },
    { rank: 12, name: "김루핀", points: 240, avatar: "김", isMe: true },
  ];

  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-7xl mx-auto">
        <div className="mb-6">
          <h1 className="text-5xl font-black text-gray-900 mb-2">
            {currentMonth}월 랭킹
          </h1>
          <p className="text-gray-700 font-medium text-lg">
            이번 달 TOP 운동왕은 누구?
          </p>
        </div>

        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-1.5">
            {rankers.map((ranker) => (
              <Card
                key={ranker.rank}
                className={`backdrop-blur-2xl border shadow-lg overflow-hidden transition-all hover:scale-[1.01] ${
                  ranker.isMe
                    ? "bg-gradient-to-r from-red-50/80 to-pink-50/80 border-[#C93831]"
                    : "bg-white/60 border-gray-200"
                }`}
              >
                <div className="p-2">
                  <div className="flex items-center gap-2">
                    <div className="text-xl font-black text-gray-900 w-8 text-center">
                      {ranker.badge || ranker.rank}
                    </div>

                    <Avatar className="w-8 h-8 border-2 border-white shadow-lg">
                      <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xs">
                        {ranker.avatar}
                      </AvatarFallback>
                    </Avatar>

                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-black text-sm text-gray-900">
                          {ranker.name}
                        </span>
                        {ranker.isMe && (
                          <Badge className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold border-0 text-xs">
                            나
                          </Badge>
                        )}
                      </div>
                      <div className="text-gray-600 font-bold text-xs">
                        {ranker.points}점
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
                    <span className="font-black text-xl text-[#C93831]">
                      18일
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">평균 점수</span>
                    <span className="font-black text-xl text-[#C93831]">
                      48
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">연속 기록</span>
                    <span className="font-black text-xl text-[#C93831]">
                      7일
                    </span>
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
                    <span className="font-black text-xl text-gray-900">
                      248명
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">
                      이번 달 활동
                    </span>
                    <span className="font-black text-xl text-gray-900">
                      220명
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-gray-700 font-medium">평균 점수</span>
                    <span className="font-black text-xl text-gray-900">
                      42점
                    </span>
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
