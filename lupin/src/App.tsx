import { useState, useEffect } from "react";
import { Button } from "./components/ui/button";
import { Card, CardContent } from "./components/ui/card";
import { Badge } from "./components/ui/badge";
import {
  Heart,
  Sparkles,
  Trophy,
  Users,
  Dumbbell,
  Award,
  ArrowRight,
  Check,
  BarChart3,
  Search,
  Zap,
  Calendar,
  Target,
  MessageCircle,
  FileText,
  UserCircle
} from "lucide-react";
import { Toaster } from "./components/ui/sonner";
import Login from "./components/auth/Login";
import Dashboard from "./components/Dashboard";

export default function App() {
  const [showLogin, setShowLogin] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userType, setUserType] = useState<"member" | "doctor">("member");

  // Intersection Observer for diverse scroll animations
  useEffect(() => {
    const observerOptions = {
      threshold: 0.1,
      rootMargin: "0px 0px -100px 0px"
    };

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("animate-in");
        }
      });
    }, observerOptions);

    const elements = document.querySelectorAll("[class*='scroll-']");
    elements.forEach((el) => observer.observe(el));

    return () => observer.disconnect();
  }, [showLogin, isLoggedIn]);

  const handleLogin = (role: string) => {
    // 서버에서 받은 Role로 유저 타입 결정
    if (role === "DOCTOR") {
      setUserType("doctor");
    } else {
      setUserType("member");
    }
    setIsLoggedIn(true);
  };

  if (isLoggedIn) {
    return (
      <>
        <Dashboard onLogout={() => setIsLoggedIn(false)} userType={userType} />
        <Toaster />
      </>
    );
  }

  if (showLogin) {
    return (
      <Login 
        onBack={() => setShowLogin(false)} 
        onLogin={handleLogin}
      />
    );
  }

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Colorful Stained Background */}
      <div className="fixed inset-0 -z-10 bg-gradient-to-br from-purple-100 via-pink-50 to-blue-100">
        <div className="absolute top-20 left-10 w-96 h-96 bg-gradient-to-br from-purple-300 to-pink-300 rounded-full blur-3xl opacity-40 animate-float"></div>
        <div className="absolute top-60 right-20 w-80 h-80 bg-gradient-to-br from-blue-300 to-cyan-300 rounded-full blur-3xl opacity-40 animate-float-delayed"></div>
        <div className="absolute bottom-40 left-1/4 w-72 h-72 bg-gradient-to-br from-yellow-200 to-orange-300 rounded-full blur-3xl opacity-30 animate-pulse"></div>
        <div className="absolute bottom-20 right-1/3 w-96 h-96 bg-gradient-to-br from-green-200 to-emerald-300 rounded-full blur-3xl opacity-30 animate-float"></div>
        <div className="absolute top-1/3 right-1/4 w-80 h-80 bg-gradient-to-br from-red-200 to-pink-200 rounded-full blur-3xl opacity-25 animate-float"></div>
        <div className="absolute bottom-10 left-1/2 w-96 h-96 bg-gradient-to-br from-indigo-200 to-purple-200 rounded-full blur-3xl opacity-30 animate-float-delayed"></div>
      </div>

      {/* Header - Glassmorphism */}
      <header className="fixed top-0 w-full backdrop-blur-2xl bg-white/80 border-b border-gray-200 z-50">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <img src="/Lupin.png" alt="Lupin Logo" className="h-10 w-auto object-contain" />
          </div>
          <Button
            onClick={() => setShowLogin(true)}
            className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold border-0 shadow-xl rounded-2xl px-6"
          >
            로그인
          </Button>
        </div>
      </header>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-4 scroll-fade-up">
        <div className="container mx-auto max-w-6xl">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div className="space-y-6">
              <Badge className="bg-[#C93831] text-white hover:bg-[#B02F28] border-0 px-4 py-2 font-bold shadow-lg">
                <Sparkles className="w-4 h-4 mr-1" />
                직원 전용 헬스케어 플랫폼
              </Badge>
              <h1 className="text-6xl font-black text-gray-900 leading-tight">
                건강한 습관,<br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#C93831] to-pink-500">함께 만들어가요</span>
              </h1>
              <p className="text-xl text-gray-700 font-medium">
                운동을 기록하고 점수를 모으세요.<br />
                30점마다 추첨권을 받고 매일 당첨 기회를 잡으세요!
              </p>
              <div className="flex gap-4">
                <Button 
                  size="lg" 
                  onClick={() => setShowLogin(true)}
                  className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold text-lg px-8 py-6 rounded-2xl border-0 shadow-2xl hover:scale-105 transition-transform"
                >
                  시작하기
                  <ArrowRight className="w-5 h-5 ml-2" />
                </Button>
              </div>
              <div className="flex gap-8 pt-4">
                <div className="backdrop-blur-xl bg-white/40 rounded-2xl p-4 border border-white/60">
                  <div className="text-3xl font-black text-[#C93831]">30점</div>
                  <div className="text-sm text-gray-600 font-bold">추첨권 1개</div>
                </div>
                <div className="backdrop-blur-xl bg-white/40 rounded-2xl p-4 border border-white/60">
                  <div className="text-3xl font-black text-[#C93831]">매일</div>
                  <div className="text-sm text-gray-600 font-bold">추첨 진행</div>
                </div>
                <div className="backdrop-blur-xl bg-white/40 rounded-2xl p-4 border border-white/60">
                  <div className="text-3xl font-black text-[#C93831]">실시간</div>
                  <div className="text-sm text-gray-600 font-bold">랭킹</div>
                </div>
              </div>
            </div>
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-r from-red-200 to-pink-200 rounded-full blur-3xl opacity-30"></div>
              <img
                src="https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=600&q=75"
                alt="운동 활동"
                className="relative rounded-3xl shadow-2xl border-8 border-white/50 backdrop-blur-sm"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Service Introduction */}
      <section className="py-20 px-4 scroll-slide-right">
        <div className="container mx-auto max-w-6xl">
          <div className="text-center mb-16">
            <h2 className="text-5xl font-black text-gray-900 mb-4">Lupin이 특별한 이유</h2>
            <p className="text-xl text-gray-700 font-medium">운동 기록부터 추첨까지, 건강 관리가 즐거워집니다</p>
          </div>
          
          <div className="grid md:grid-cols-3 gap-8">
            <Card className="backdrop-blur-2xl bg-white/40 border border-white/60 shadow-xl hover:shadow-2xl transition-all hover:-translate-y-2">
              <CardContent className="p-8 space-y-4">
                <div className="w-14 h-14 bg-gradient-to-br from-[#C93831] to-pink-500 rounded-2xl flex items-center justify-center shadow-lg">
                  <Target className="w-7 h-7 text-white" />
                </div>
                <h3 className="text-2xl font-black text-gray-900">점수 획득 시스템</h3>
                <p className="text-gray-700 font-medium">
                  운동을 완료하고 점수를 획득하세요. 
                  30점마다 추첨권 1개를 자동으로 받아요!
                </p>
              </CardContent>
            </Card>

            <Card className="backdrop-blur-2xl bg-white/40 border border-white/60 shadow-xl hover:shadow-2xl transition-all hover:-translate-y-2">
              <CardContent className="p-8 space-y-4">
                <div className="w-14 h-14 bg-gradient-to-br from-purple-500 to-pink-500 rounded-2xl flex items-center justify-center shadow-lg">
                  <Trophy className="w-7 h-7 text-white" />
                </div>
                <h3 className="text-2xl font-black text-gray-900">매일 추첨</h3>
                <p className="text-gray-700 font-medium">
                  점수를 모아 추첨권을 받으세요. 
                  당일 획득한 추첨권으로 매일 추첨에 참여합니다!
                </p>
              </CardContent>
            </Card>

            <Card className="backdrop-blur-2xl bg-white/40 border border-white/60 shadow-xl hover:shadow-2xl transition-all hover:-translate-y-2">
              <CardContent className="p-8 space-y-4">
                <div className="w-14 h-14 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-2xl flex items-center justify-center shadow-lg">
                  <BarChart3 className="w-7 h-7 text-white" />
                </div>
                <h3 className="text-2xl font-black text-gray-900">실시간 랭킹</h3>
                <p className="text-gray-700 font-medium">
                  점수 기반 실시간 랭킹에서 
                  다른 동료들과 경쟁하고 성장하세요!
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* Activity Recording */}
      <section className="py-20 px-4 scroll-slide-left">
        <div className="container mx-auto max-w-6xl">
          <div className="text-center mb-16">
            <h2 className="text-5xl font-black text-gray-900 mb-4">활동 기록</h2>
            <p className="text-xl text-gray-700 font-medium">운동 후 활동 내역을 기록하고 점수를 획득하세요</p>
          </div>

          <div className="grid md:grid-cols-2 gap-12 items-center">
            <img
              src="https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&q=75"
              alt="운동 기록"
              className="rounded-3xl shadow-2xl border-8 border-white/50 backdrop-blur-sm"
            />
            <div className="space-y-6">
              <div className="flex items-center gap-3">
                <Dumbbell className="w-10 h-10 text-[#C93831]" />
                <h3 className="text-4xl font-black text-gray-900">간편한 운동 기록</h3>
              </div>
              <p className="text-xl text-gray-700 font-medium">
                운동이 끝나면 바로 기록하세요. 
                운동 시간과 종류에 따라 점수를 받을 수 있습니다.
              </p>
              <div className="space-y-4">
                <div className="flex items-center gap-3 backdrop-blur-xl bg-white/40 p-4 rounded-2xl border border-white/60">
                  <div className="w-8 h-8 bg-gradient-to-br from-[#C93831] to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                    <Check className="w-5 h-5 text-white" />
                  </div>
                  <span className="text-gray-900 font-bold">운동 종류와 시간 입력</span>
                </div>
                <div className="flex items-center gap-3 backdrop-blur-xl bg-white/40 p-4 rounded-2xl border border-white/60">
                  <div className="w-8 h-8 bg-gradient-to-br from-[#C93831] to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                    <Check className="w-5 h-5 text-white" />
                  </div>
                  <span className="text-gray-900 font-bold">즉시 점수 획득</span>
                </div>
                <div className="flex items-center gap-3 backdrop-blur-xl bg-white/40 p-4 rounded-2xl border border-white/60">
                  <div className="w-8 h-8 bg-gradient-to-br from-[#C93831] to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                    <Check className="w-5 h-5 text-white" />
                  </div>
                  <span className="text-gray-900 font-bold">30점마다 추첨권 자동 생성</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Key Features */}
      <section className="py-20 px-4 scroll-scale-in">
        <div className="container mx-auto max-w-6xl">
          <div className="text-center mb-16">
            <h2 className="text-5xl font-black text-gray-900 mb-4">핵심 기능</h2>
            <p className="text-xl text-gray-700 font-medium">다양한 기능으로 건강 관리를 쉽고 재미있게</p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[
              { icon: Users, title: "데일리 피드", desc: "하루 한 번 운동 관련 피드를 작성하고 공유하세요", color: "from-[#C93831] to-pink-500" },
              { icon: Heart, title: "좋아요", desc: "다른 사용자의 피드에 좋아요를 누르고 응원하세요", color: "from-pink-500 to-purple-500" },
              { icon: BarChart3, title: "랭킹", desc: "점수에 따른 실시간 랭킹을 확인하세요", color: "from-purple-500 to-blue-500" },
              { icon: Trophy, title: "추첨", desc: "30점마다 추첨권 1개가 자동 생성되어 당일 응모됩니다", color: "from-yellow-400 to-orange-500" },
              { icon: Search, title: "피드 검색", desc: "피드를 검색하고 필터링할 수 있습니다", color: "from-blue-500 to-cyan-500" },
              { icon: Calendar, title: "진료 예약", desc: "비대면 진료를 예약하고 관리할 수 있습니다", color: "from-cyan-500 to-teal-500" },
              { icon: MessageCircle, title: "비대면 진료", desc: "1:1 채팅을 통해 비대면으로 진료를 받을 수 있습니다", color: "from-teal-500 to-green-500" },
              { icon: FileText, title: "처방전 확인", desc: "받은 처방전을 확인하고 관리할 수 있습니다", color: "from-green-500 to-lime-500" },
              { icon: Zap, title: "웰빙 챌린지", desc: "정해진 시간에 진행되는 이벤트에 참여하세요", color: "from-orange-500 to-red-500" },
              { icon: UserCircle, title: "마이페이지", desc: "개인 건강 정보를 수정하고 관리할 수 있습니다", color: "from-indigo-500 to-purple-500" }
            ].map((feature, index) => (
              <Card key={index} className="backdrop-blur-2xl bg-white/40 border border-white/60 shadow-xl hover:shadow-2xl transition-all hover:-translate-y-2">
                <CardContent className="p-6 text-center space-y-4">
                  <div className={`w-14 h-14 bg-gradient-to-br ${feature.color} rounded-2xl flex items-center justify-center mx-auto shadow-lg`}>
                    <feature.icon className="w-7 h-7 text-white" />
                  </div>
                  <h3 className="text-xl font-black text-gray-900">{feature.title}</h3>
                  <p className="text-sm text-gray-700 font-medium">{feature.desc}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* How it Works */}
      <section className="py-20 px-4 scroll-bounce-in">
        <div className="container mx-auto max-w-6xl">
          <div className="text-center mb-16">
            <h2 className="text-5xl font-black text-gray-900 mb-4">어떻게 작동하나요?</h2>
            <p className="text-xl text-gray-700 font-medium">4단계로 시작하는 건강한 습관</p>
          </div>

          <div className="grid md:grid-cols-4 gap-6">
            {[
              { step: "1", title: "운동하기", desc: "좋아하는 운동을 하세요", icon: Dumbbell, color: "from-[#C93831] to-pink-500" },
              { step: "2", title: "활동 기록", desc: "운동 내역을 기록하세요", icon: Target, color: "from-purple-500 to-pink-500" },
              { step: "3", title: "점수 획득", desc: "점수와 추첨권을 받으세요", icon: Trophy, color: "from-yellow-400 to-orange-500" },
              { step: "4", title: "매일 추첨", desc: "당일 추첨에 자동 참여!", icon: Award, color: "from-blue-500 to-cyan-500" }
            ].map((item, index) => (
              <div key={index} className="text-center space-y-4">
                <div className={`w-20 h-20 bg-gradient-to-br ${item.color} text-white rounded-3xl flex items-center justify-center mx-auto text-3xl font-black shadow-2xl`}>
                  {item.step}
                </div>
                <item.icon className="w-10 h-10 text-[#C93831] mx-auto" />
                <h3 className="text-xl font-black text-gray-900">{item.title}</h3>
                <p className="text-sm text-gray-700 font-medium">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 scroll-zoom-in">
        <Card className="container mx-auto max-w-5xl backdrop-blur-2xl bg-gradient-to-br from-[#C93831]/90 to-[#B02F28]/90 border border-white/40 shadow-2xl">
          <CardContent className="p-16 text-center space-y-8 text-white">
            <h2 className="text-6xl font-black">지금 바로 시작하세요</h2>
            <p className="text-2xl text-red-100 font-medium">
              운동을 기록하고, 점수를 모으고, <br />
              매일 추첨의 기회를 잡으세요!
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Button 
                size="lg" 
                onClick={() => setShowLogin(true)}
                className="bg-white text-[#C93831] hover:bg-gray-50 font-bold text-xl px-10 py-7 rounded-2xl shadow-xl hover:scale-105 transition-transform"
              >
                <Sparkles className="w-6 h-6 mr-2" />
                로그인하고 시작하기
              </Button>
            </div>
            <div className="pt-8 flex items-center justify-center gap-12">
              <div className="text-center">
                <Award className="w-12 h-12 mx-auto mb-2 text-red-200" />
                <div className="text-sm text-red-100 font-bold">직원 전용</div>
              </div>
              <div className="text-center">
                <Heart className="w-12 h-12 mx-auto mb-2 text-red-200" />
                <div className="text-sm text-red-100 font-bold">무료 서비스</div>
              </div>
              <div className="text-center">
                <Trophy className="w-12 h-12 mx-auto mb-2 text-red-200" />
                <div className="text-sm text-red-100 font-bold">매일 추첨</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </section>

      {/* Footer */}
      <footer className="backdrop-blur-2xl bg-gray-900/95 text-gray-400 py-12 px-4 mt-20">
        <div className="container mx-auto max-w-6xl">
          <div className="grid md:grid-cols-3 gap-8 mb-8">
            <div className="space-y-4">
              <img src="/Lupin.png" alt="Lupin Logo" className="h-10 w-auto object-contain" />
              <p className="text-sm font-medium">
                건강한 습관, 함께 만들어가요
              </p>
            </div>
            <div>
              <h3 className="text-white font-black mb-4">회원 기능</h3>
              <ul className="space-y-2 text-sm font-medium">
                <li>활동 기록 & 점수 시스템</li>
                <li>데일리 피드 & 랭킹</li>
                <li>진료 예약 & 비대면 진료</li>
                <li>웰빙 챌린지 & 추첨</li>
              </ul>
            </div>
            <div>
              <h3 className="text-white font-black mb-4">의료진 기능</h3>
              <ul className="space-y-2 text-sm font-medium">
                <li>비대면 진료</li>
                <li>예약 관리</li>
                <li>회원 관리</li>
                <li>처방전 발급</li>
              </ul>
            </div>
          </div>
          <div className="pt-8 border-t border-gray-800 text-center text-sm font-medium">
            <p>© 2025 Lupin. All rights reserved.</p>
          </div>
        </div>
      </footer>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-20px); }
        }
        
        @keyframes float-delayed {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-30px); }
        }
        
        .animate-float {
          animation: float 6s ease-in-out infinite;
        }
        
        .animate-float-delayed {
          animation: float-delayed 8s ease-in-out infinite;
        }
        
        /* Sudden Appear Animations */
        .scroll-fade-up,
        .scroll-slide-right,
        .scroll-slide-left,
        .scroll-scale-in,
        .scroll-bounce-in,
        .scroll-zoom-in {
          opacity: 0;
        }
        
        /* Fade Up */
        .scroll-fade-up.animate-in {
          animation: fadeUpSudden 0.6s ease-out forwards;
        }
        
        @keyframes fadeUpSudden {
          0% { opacity: 0; transform: translateY(80px); }
          100% { opacity: 1; transform: translateY(0); }
        }
        
        /* Slide from Right */
        .scroll-slide-right.animate-in {
          animation: slideRightSudden 0.6s ease-out forwards;
        }
        
        @keyframes slideRightSudden {
          0% { opacity: 0; transform: translateX(-100px); }
          100% { opacity: 1; transform: translateX(0); }
        }
        
        /* Slide from Left */
        .scroll-slide-left.animate-in {
          animation: slideLeftSudden 0.6s ease-out forwards;
        }
        
        @keyframes slideLeftSudden {
          0% { opacity: 0; transform: translateX(100px); }
          100% { opacity: 1; transform: translateX(0); }
        }
        
        /* Scale In */
        .scroll-scale-in.animate-in {
          animation: scaleInSudden 0.5s ease-out forwards;
        }
        
        @keyframes scaleInSudden {
          0% { opacity: 0; transform: scale(0.7); }
          100% { opacity: 1; transform: scale(1); }
        }
        
        /* Bounce In */
        .scroll-bounce-in.animate-in {
          animation: bounceInSudden 0.7s cubic-bezier(0.68, -0.55, 0.265, 1.55) forwards;
        }
        
        @keyframes bounceInSudden {
          0% { opacity: 0; transform: scale(0.3) translateY(100px); }
          50% { opacity: 1; transform: scale(1.05); }
          100% { opacity: 1; transform: scale(1) translateY(0); }
        }
        
        /* Zoom In */
        .scroll-zoom-in.animate-in {
          animation: zoomInSudden 0.8s ease-out forwards;
        }
        
        @keyframes zoomInSudden {
          0% { opacity: 0; transform: scale(0.5); }
          100% { opacity: 1; transform: scale(1); }
        }
      `}</style>
    </div>
  );
}
