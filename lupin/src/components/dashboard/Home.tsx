import { Card } from "../ui/card";
import { Badge } from "../ui/badge";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { Button } from "../ui/button";
import {
  Heart,
  MessageCircle,
  Sparkles,
  Flame,
  Award,
  Target,
  Zap,
} from "lucide-react";
import { Feed } from "@/types/dashboard.types";

interface HomeProps {
  challengeJoined: boolean;
  handleJoinChallenge: () => void;
  profileImage: string | null;
  myFeeds: Feed[];
  setSelectedFeed: (feed: Feed) => void;
  setFeedImageIndex: (feedId: number, index: number) => void;
  setShowFeedDetailInHome: (show: boolean) => void;
}

export default function Home({
  challengeJoined,
  handleJoinChallenge,
  profileImage,
  myFeeds,
  setSelectedFeed,
  setFeedImageIndex,
  setShowFeedDetailInHome,
}: HomeProps) {
  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Wellness Challenge Banner */}
        {!challengeJoined && (
          <Card className="backdrop-blur-2xl bg-white/70 border border-gray-200 shadow-xl overflow-hidden relative">
            <div className="p-6">
              <div className="flex items-center justify-between">
                <div className="space-y-2 flex-1">
                  <Badge className="bg-gradient-to-r from-[#C93831] to-pink-500 text-white px-4 py-1.5 font-bold border-0">
                    <Zap className="w-4 h-4 mr-1" />
                    진행중
                  </Badge>
                  <h2 className="text-3xl font-black text-gray-900">
                    웰빙 챌린지
                  </h2>
                  <p className="text-gray-700 font-medium">
                    오늘 오후 6시 시작 | 선착순 100명 특별 보상
                  </p>
                </div>

                <div className="relative w-48 h-48 flex-shrink-0">
                  <img
                    src="https://images.unsplash.com/photo-1762328500413-1a4cb2023059?w=400"
                    alt="Supplements"
                    className="w-full h-full object-contain"
                  />
                </div>

                <Button
                  onClick={handleJoinChallenge}
                  className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold px-6 py-5 rounded-2xl shadow-xl border-0 ml-6"
                >
                  참여하기
                </Button>
              </div>
            </div>
          </Card>
        )}

        {/* Profile Header */}
        <div className="p-8">
          <div className="flex items-start gap-8 mb-8">
            <Avatar className="w-40 h-40 border-4 border-white shadow-xl">
              {profileImage ? (
                <img
                  src={profileImage}
                  alt="Profile"
                  className="w-full h-full object-cover"
                />
              ) : (
                <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white text-5xl font-black">
                  김
                </AvatarFallback>
              )}
            </Avatar>

            <div className="flex-1">
              <h1 className="text-3xl font-black text-gray-900 mb-4">김루핀</h1>

              <div className="flex gap-8 mb-4">
                <div>
                  <div className="text-2xl font-black text-[#C93831]">
                    {myFeeds.length}
                  </div>
                  <div className="text-xs text-gray-600 font-bold">게시물</div>
                </div>
                <div>
                  <div className="text-2xl font-black text-[#C93831]">240</div>
                  <div className="text-xs text-gray-600 font-bold">총 점수</div>
                </div>
                <div>
                  <div className="text-2xl font-black text-[#C93831]">8</div>
                  <div className="text-xs text-gray-600 font-bold">추첨권</div>
                </div>
                <div>
                  <div className="text-2xl font-black text-[#C93831]">#12</div>
                  <div className="text-xs text-gray-600 font-bold">순위</div>
                </div>
              </div>

              <p className="text-gray-700 font-medium text-sm mb-3">
                🏃‍♂️ 건강한 습관 만들기
                <br />
                💪 매일 운동 챌린지 진행중
              </p>

              <div className="flex gap-2 flex-wrap">
                <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                  <Flame className="w-3 h-3 mr-1" />
                  7일 연속
                </Badge>
                <Badge className="bg-gradient-to-r from-purple-400 to-pink-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                  <Award className="w-3 h-3 mr-1" />
                  TOP 20
                </Badge>
                <Badge className="bg-gradient-to-r from-blue-400 to-cyan-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                  <Target className="w-3 h-3 mr-1" />
                  목표 달성
                </Badge>
              </div>
            </div>
          </div>
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
                <img
                  src={feed.images[0]}
                  alt={feed.activity}
                  className="w-full h-full object-cover"
                />

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
                      <Sparkles className="w-4 h-4 inline mr-1" />+{feed.points}
                      점
                    </div>
                  </div>
                </div>
              </Card>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
