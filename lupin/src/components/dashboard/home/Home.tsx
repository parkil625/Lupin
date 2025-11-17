/**
 * Home.tsx
 *
 * 회원 대시보드 홈 페이지 컴포넌트
 * - 오늘의 활동, 챌린지, 최근 피드 표시
 * - 점수 현황 및 프로필 정보 표시
 */

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar } from "@/components/ui/avatar";
import {
  Heart,
  MessageCircle,
  Sparkles,
  Flame,
  Award,
  User,
} from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import AdPopupDialog from "../dialogs/AdPopupDialog";

interface HomeProps {
  challengeJoined: boolean;
  handleJoinChallenge: () => void;
  profileImage: string | null;
  myFeeds: Feed[];
  setSelectedFeed: (feed: Feed) => void;
  setFeedImageIndex: (feedId: number, index: number) => void;
  setShowFeedDetailInHome: (show: boolean) => void;
}

const AD_POPUP_KEY = "adPopupHiddenUntil";

export default function Home({
  handleJoinChallenge,
  profileImage,
  myFeeds,
  setSelectedFeed,
  setFeedImageIndex,
  setShowFeedDetailInHome,
}: HomeProps) {
  const [showAdPopup, setShowAdPopup] = useState(false);

  useEffect(() => {
    // 개발/테스트 모드: URL에 ?showAd=true가 있으면 강제로 표시
    const urlParams = new URLSearchParams(window.location.search);
    const forceShowAd = urlParams.get('showAd') === 'true';

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
        console.log("광고 팝업 숨김 중 (남은 시간:", Math.floor((hiddenTime - Date.now()) / 1000 / 60), "분)");
        console.log("테스트하려면 URL에 ?showAd=true를 추가하거나 콘솔에서 localStorage.removeItem('adPopupHiddenUntil')을 실행하세요");
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
                <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-200 to-gray-300">
                  <User className="w-20 h-20 text-gray-400" />
                </div>
              )}
            </Avatar>

            <div className="flex-1">
              <h1 className="text-3xl font-black text-gray-900 mb-4">김루핀</h1>

              <div className="flex gap-8 mb-4">
                <div>
                  <span className="text-sm text-gray-600 font-bold">게시물 </span>
                  <span className="text-sm font-black text-[#C93831]">{myFeeds.length}</span>
                </div>
                <div>
                  <span className="text-sm text-gray-600 font-bold">이번 달 점수 </span>
                  <span className="text-sm font-black text-[#C93831]">30</span>
                </div>
                <div>
                  <span className="text-sm text-gray-600 font-bold">추첨권 </span>
                  <span className="text-sm font-black text-[#C93831]">1</span>
                </div>
                <div>
                  <span className="text-sm text-gray-600 font-bold">순위 </span>
                  <span className="text-sm font-black text-[#C93831]">#12</span>
                </div>
              </div>

              <div className="flex gap-2 flex-wrap">
                <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                  <Flame className="w-3 h-3 mr-1" />
                  7일 연속
                </Badge>
                <Badge className="bg-gradient-to-r from-purple-400 to-pink-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                  <Award className="w-3 h-3 mr-1" />
                  TOP 20
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

      {/* 광고 팝업 */}
      <AdPopupDialog
        open={showAdPopup}
        onClose={handleCloseAdPopup}
        onDontShowFor24Hours={handleDontShowFor24Hours}
        onJoinChallenge={handleJoinChallengeFromPopup}
      />
    </div>
  );
}
