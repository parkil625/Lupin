import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import { Card, CardContent } from "./ui/card";
import { Badge } from "./ui/badge";
import {
    Heart, Sparkles, Trophy, Users, Dumbbell, Award, ArrowRight,
    Check, BarChart3, Search, Zap, Calendar, Target, MessageCircle, FileText, UserCircle
} from "lucide-react";

export default function LandingPage() {
    const navigate = useNavigate();

    useEffect(() => {
        const observerOptions = {
            threshold: 0.1,
            // 모바일 스크롤 속도를 고려하여 미리 로드되도록 마진 조정
            rootMargin: "0px 0px -50px 0px"
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("animate-in");
                    // ⚡️ 성능 최적화: 한 번 애니메이션 실행된 요소는 관찰 중단 (메모리 절약)
                    observer.unobserve(entry.target);
                }
            });
        }, observerOptions);

        // Hero 섹션을 제외한 나머지 스크롤 애니메이션 요소만 관찰
        const elements = document.querySelectorAll("[class*='scroll-']");
        elements.forEach((el) => observer.observe(el));

        return () => observer.disconnect();
    }, []);

    return (
        <div className="min-h-screen relative overflow-hidden">
            {/* 배경 그라디언트는 index.html body에서 처리 (FCP 최적화) */}

            {/* Header */}
            <header className="fixed top-0 w-full backdrop-blur-md bg-white/80 border-b border-gray-200 z-50 transition-all">
                <div className="container mx-auto px-4 py-3 md:py-4 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        {/* [CLS 방지] 로고 이미지에 width/height 명시 */}
                        <img
                            src="/Lupin.png"
                            alt="Lupin Logo"
                            width="100"
                            height="40"
                            className="h-8 md:h-10 w-auto object-contain"
                        />
                    </div>
                    <Button
                        onClick={() => navigate('/login')}
                        className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold border-0 shadow-xl rounded-2xl px-6 transition-transform active:scale-95"
                    >
                        로그인
                    </Button>
                </div>
            </header>

            {/* Hero Section */}
            {/* [LCP 최적화] Hero 섹션은 애니메이션 없이 즉시 렌더링되도록 'scroll-fade-up' 클래스 제거 */}
            <section className="pt-24 md:pt-32 pb-12 md:pb-20 px-4">
                <div className="container mx-auto max-w-6xl">
                    <div className="grid md:grid-cols-2 gap-8 md:gap-12 items-center">
                        <div className="space-y-4 md:space-y-6 order-2 md:order-1">
                            <Badge className="bg-[#C93831] text-white hover:bg-[#B02F28] border-0 px-3 md:px-4 py-1.5 md:py-2 font-bold shadow-lg text-xs md:text-sm inline-flex items-center">
                                <Sparkles className="w-3 h-3 md:w-4 md:h-4 mr-1" />
                                직원 전용 헬스케어 플랫폼
                            </Badge>
                            <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-black text-gray-900 leading-tight tracking-tight">
                                건강한 습관,<br />
                                <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#C93831] to-pink-500">함께 만들어가요</span>
                            </h1>
                            <p className="text-base md:text-xl text-gray-700 font-medium leading-relaxed">
                                운동을 기록하고 점수를 모으세요.<br />
                                동료들과 함께 건강한 습관을 만들어가요!
                            </p>
                            <div className="flex gap-4">
                                <Button
                                    size="lg"
                                    onClick={() => navigate('/login')}
                                    className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold text-sm md:text-lg px-6 md:px-8 py-4 md:py-6 rounded-2xl border-0 shadow-2xl hover:scale-105 transition-transform"
                                >
                                    시작하기
                                    <ArrowRight className="w-4 h-4 md:w-5 md:h-5 ml-2" />
                                </Button>
                            </div>

                            <div className="flex gap-3 md:gap-8 pt-2 md:pt-4">
                                {[
                                    { title: "피드", sub: "하루 1회" },
                                    { title: "실시간", sub: "랭킹" },
                                    { title: "비대면", sub: "진료" }
                                ].map((item, idx) => (
                                    <div key={idx} className="backdrop-blur-xl bg-white/40 rounded-xl md:rounded-2xl p-3 md:p-4 border border-white/60">
                                        <div className="text-xl md:text-3xl font-black text-[#C93831]">{item.title}</div>
                                        <div className="text-xs md:text-sm text-gray-600 font-bold">{item.sub}</div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Hero Image - LCP 핵심 요소 */}
                        <div className="relative order-1 md:order-2">
                            <div className="absolute inset-0 bg-gradient-to-r from-red-200 to-pink-200 rounded-full blur-3xl opacity-30"></div>
                            {/*
                                [최종 최적화]
                                1. src: 기본 이미지 (PC용)
                                2. srcSet: 화면 너비에 따라 모바일/PC 이미지 자동 선택
                                3. sizes: 브라우저에게 이미지 출력 크기 힌트 제공
                            */}
                            <img
                                src="/hero-desktop.webp"
                                srcSet="/hero-mobile.webp 400w, /hero-desktop.webp 800w"
                                sizes="(max-width: 768px) 100vw, 800px"
                                alt="운동하는 여성"
                                width="800"
                                height="600"
                                fetchPriority="high"
                                decoding="async"
                                className="relative rounded-2xl md:rounded-3xl shadow-2xl border-4 md:border-8 border-white/50 backdrop-blur-sm w-full h-auto aspect-[4/3] object-cover"
                            />
                        </div>
                    </div>
                </div>
            </section>

            {/* Service Introduction - 여기서부터 Lazy Loading 및 애니메이션 적용 */}
            <section className="py-12 md:py-20 px-4 scroll-fade-up">
                <div className="container mx-auto max-w-6xl">
                    <div className="text-center mb-8 md:mb-16">
                        <h2 className="text-3xl md:text-5xl font-black text-gray-900 mb-2 md:mb-4">Lupin이 특별한 이유</h2>
                        <p className="text-base md:text-xl text-gray-700 font-medium">운동 기록부터 비대면 진료까지, 건강 관리가 즐거워집니다</p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 md:gap-8">
                        {[
                            { icon: Target, title: "점수 획득 시스템", desc: "운동을 완료하고 점수를 획득하세요. 점수에 따라 실시간 랭킹이 변동됩니다!", color: "from-[#C93831] to-pink-500" },
                            { icon: MessageCircle, title: "비대면 진료", desc: "전문 의료진과 1:1 채팅으로 간편하게 비대면 진료를 받으세요!", color: "from-purple-500 to-pink-500" },
                            { icon: BarChart3, title: "실시간 랭킹", desc: "점수 기반 실시간 랭킹에서 다른 동료들과 경쟁하고 성장하세요!", color: "from-blue-500 to-cyan-500" }
                        ].map((card, i) => (
                            <Card key={i} className="backdrop-blur-2xl bg-white/40 border border-white/60 shadow-xl hover:shadow-2xl transition-all hover:-translate-y-2">
                                <CardContent className="p-6 md:p-8 space-y-3 md:space-y-4">
                                    <div className={`w-12 h-12 md:w-14 md:h-14 bg-gradient-to-br ${card.color} rounded-xl md:rounded-2xl flex items-center justify-center shadow-lg`}>
                                        <card.icon className="w-6 h-6 md:w-7 md:h-7 text-white" />
                                    </div>
                                    <h3 className="text-xl md:text-2xl font-black text-gray-900">{card.title}</h3>
                                    <p className="text-sm md:text-base text-gray-700 font-medium break-keep">
                                        {card.desc}
                                    </p>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                </div>
            </section>

            {/* Activity Recording */}
            <section className="py-12 md:py-20 px-4 scroll-slide-left">
                <div className="container mx-auto max-w-6xl">
                    <div className="text-center mb-8 md:mb-16">
                        <h2 className="text-3xl md:text-5xl font-black text-gray-900 mb-2 md:mb-4">활동 기록</h2>
                        <p className="text-base md:text-xl text-gray-700 font-medium">운동 후 활동 내역을 기록하고 점수를 획득하세요</p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8 md:gap-12 items-center">
                        {/* 
                            [성능 최적화] 스크롤 하단 이미지:
                            1. loading="lazy": 초기 로딩에서 제외
                            2. decoding="async": 메인 스레드 차단 방지
                        */}
                        <img
                            src="https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&q=80&fm=webp"
                            alt="운동 기록 화면"
                            width="800"
                            height="600"
                            loading="lazy"
                            decoding="async"
                            className="rounded-2xl md:rounded-3xl shadow-2xl border-4 md:border-8 border-white/50 backdrop-blur-sm w-full h-auto aspect-[4/3] object-cover"
                        />
                        <div className="space-y-4 md:space-y-6">
                            <div className="flex items-center gap-2 md:gap-3">
                                <Dumbbell className="w-8 h-8 md:w-10 md:h-10 text-[#C93831]" />
                                <h3 className="text-2xl md:text-4xl font-black text-gray-900">간편한 운동 기록</h3>
                            </div>
                            <p className="text-base md:text-xl text-gray-700 font-medium break-keep">
                                운동이 끝나면 바로 기록하세요. 운동 시간과 종류에 따라 점수를 받을 수 있습니다.
                            </p>
                            <div className="space-y-4">
                                {["운동 종류와 시간 입력", "즉시 점수 획득", "실시간 랭킹 반영"].map((text, idx) => (
                                    <div key={idx} className="flex items-center gap-3 backdrop-blur-xl bg-white/40 p-4 rounded-2xl border border-white/60">
                                        <div className="w-8 h-8 bg-gradient-to-br from-[#C93831] to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                                            <Check className="w-5 h-5 text-white" />
                                        </div>
                                        <span className="text-gray-900 font-bold">{text}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Key Features */}
            <section className="py-12 md:py-20 px-4 scroll-scale-in">
                <div className="container mx-auto max-w-6xl">
                    <div className="text-center mb-8 md:mb-16">
                        <h2 className="text-3xl md:text-5xl font-black text-gray-900 mb-2 md:mb-4">핵심 기능</h2>
                        <p className="text-base md:text-xl text-gray-700 font-medium">다양한 기능으로 건강 관리를 쉽고 재미있게</p>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-2 lg:grid-cols-3 gap-3 md:gap-6">
                        {[
                            { icon: Users, title: "데일리 피드", desc: "하루 한 번 운동 관련 피드를 작성하고 공유하세요", color: "from-[#C93831] to-pink-500" },
                            { icon: Heart, title: "좋아요", desc: "다른 사용자의 피드에 좋아요를 누르고 응원하세요", color: "from-pink-500 to-purple-500" },
                            { icon: BarChart3, title: "랭킹", desc: "점수에 따른 실시간 랭킹을 확인하세요", color: "from-purple-500 to-blue-500" },
                            { icon: Trophy, title: "배지", desc: "7일 연속 기록, TOP 10 등 특별 배지를 획득하세요", color: "from-yellow-400 to-orange-500" },
                            { icon: Search, title: "피드 검색", desc: "피드를 검색하고 필터링할 수 있습니다", color: "from-blue-500 to-cyan-500" },
                            { icon: Calendar, title: "진료 예약", desc: "비대면 진료를 예약하고 관리할 수 있습니다", color: "from-cyan-500 to-teal-500" },
                            { icon: MessageCircle, title: "비대면 진료", desc: "1:1 채팅을 통해 비대면으로 진료를 받을 수 있습니다", color: "from-teal-500 to-green-500" },
                            { icon: FileText, title: "처방전 확인", desc: "받은 처방전을 확인하고 관리할 수 있습니다", color: "from-green-500 to-lime-500" },
                            { icon: Zap, title: "포인트 시스템", desc: "활동에 따라 포인트를 획득하고 순위에 도전하세요", color: "from-orange-500 to-red-500" },
                            { icon: UserCircle, title: "마이페이지", desc: "개인 건강 정보를 수정하고 관리할 수 있습니다", color: "from-indigo-500 to-purple-500" }
                        ].map((feature, index) => (
                            <Card key={index} className="backdrop-blur-2xl bg-white/40 border border-white/60 shadow-xl hover:shadow-2xl transition-all hover:-translate-y-2">
                                <CardContent className="p-4 md:p-6 text-center space-y-2 md:space-y-4">
                                    <div className={`w-10 h-10 md:w-14 md:h-14 bg-gradient-to-br ${feature.color} rounded-xl md:rounded-2xl flex items-center justify-center mx-auto shadow-lg`}>
                                        <feature.icon className="w-5 h-5 md:w-7 md:h-7 text-white" />
                                    </div>
                                    <h3 className="text-base md:text-xl font-black text-gray-900">{feature.title}</h3>
                                    <p className="text-xs md:text-sm text-gray-700 font-medium hidden sm:block break-keep">{feature.desc}</p>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                </div>
            </section>

            {/* How it Works */}
            <section className="py-12 md:py-20 px-4 scroll-bounce-in">
                <div className="container mx-auto max-w-6xl">
                    <div className="text-center mb-8 md:mb-16">
                        <h2 className="text-3xl md:text-5xl font-black text-gray-900 mb-2 md:mb-4">어떻게 작동하나요?</h2>
                        <p className="text-base md:text-xl text-gray-700 font-medium">3단계로 시작하는 건강한 습관</p>
                    </div>

                    <div className="grid grid-cols-3 gap-3 md:gap-6">
                        {[
                            { step: "1", title: "운동하기", desc: "좋아하는 운동을 하세요", icon: Dumbbell, color: "from-[#C93831] to-pink-500" },
                            { step: "2", title: "활동 기록", desc: "운동 내역을 기록하세요", icon: Target, color: "from-purple-500 to-pink-500" },
                            { step: "3", title: "랭킹 경쟁", desc: "점수를 모아 순위에 도전!", icon: Trophy, color: "from-yellow-400 to-orange-500" }
                        ].map((item, index) => (
                            <div key={index} className="text-center space-y-2 md:space-y-4">
                                <div className={`w-14 h-14 md:w-20 md:h-20 bg-gradient-to-br ${item.color} text-white rounded-2xl md:rounded-3xl flex items-center justify-center mx-auto text-xl md:text-3xl font-black shadow-2xl`}>
                                    {item.step}
                                </div>
                                <item.icon className="w-6 h-6 md:w-10 md:h-10 text-[#C93831] mx-auto" />
                                <h3 className="text-sm md:text-xl font-black text-gray-900">{item.title}</h3>
                                <p className="text-xs md:text-sm text-gray-700 font-medium hidden sm:block">{item.desc}</p>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* CTA Section */}
            <section className="py-12 md:py-20 px-4 scroll-zoom-in">
                <Card className="container mx-auto max-w-5xl backdrop-blur-2xl bg-gradient-to-br from-[#C93831]/90 to-[#B02F28]/90 border border-white/40 shadow-2xl">
                    <CardContent className="p-8 md:p-16 text-center space-y-4 md:space-y-8 text-white">
                        <h2 className="text-3xl md:text-6xl font-black">지금 바로 시작하세요</h2>
                        <p className="text-base md:text-2xl text-red-100 font-medium">
                            운동을 기록하고, 점수를 모으고, <br />
                            동료들과 함께 건강해지세요!
                        </p>
                        <div className="flex flex-col sm:flex-row gap-4 justify-center">
                            <Button
                                size="lg"
                                onClick={() => navigate('/login')}
                                className="bg-white text-[#C93831] hover:bg-gray-50 font-bold text-base md:text-xl px-6 md:px-10 py-4 md:py-7 rounded-2xl shadow-xl hover:scale-105 transition-transform"
                            >
                                <Sparkles className="w-5 h-5 md:w-6 md:h-6 mr-2" />
                                로그인하고 시작하기
                            </Button>
                        </div>
                        <div className="pt-4 md:pt-8 flex items-center justify-center gap-6 md:gap-12">
                            {[
                                { Icon: Award, text: "직원 전용" },
                                { Icon: Heart, text: "무료 서비스" },
                                { Icon: Trophy, text: "실시간 랭킹" }
                            ].map((badge, idx) => (
                                <div key={idx} className="text-center">
                                    <badge.Icon className="w-8 h-8 md:w-12 md:h-12 mx-auto mb-1 md:mb-2 text-red-200" />
                                    <div className="text-xs md:text-sm text-red-100 font-bold">{badge.text}</div>
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>
            </section>

            {/* Footer */}
            <footer className="backdrop-blur-2xl bg-gray-900/95 text-gray-400 py-8 md:py-12 px-4 mt-12 md:mt-20">
                <div className="container mx-auto max-w-6xl">
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8 mb-6 md:mb-8">
                        <div className="space-y-3 md:space-y-4 text-center md:text-left">
                            <img src="/Lupin.png" alt="Lupin Logo" width="100" height="32" className="h-8 md:h-10 w-auto object-contain mx-auto md:mx-0" />
                            <p className="text-xs md:text-sm font-medium">건강한 습관, 함께 만들어가요</p>
                        </div>
                        <div className="text-center md:text-left">
                            <h3 className="text-white font-black mb-2 md:mb-4 text-sm md:text-base">회원 기능</h3>
                            <ul className="space-y-1 md:space-y-2 text-xs md:text-sm font-medium">
                                <li>활동 기록 & 점수 시스템</li>
                                <li>데일리 피드 & 랭킹</li>
                                <li>진료 예약 & 비대면 진료</li>
                                <li>포인트 시스템 & 랭킹</li>
                            </ul>
                        </div>
                        <div className="text-center md:text-left">
                            <h3 className="text-white font-black mb-2 md:mb-4 text-sm md:text-base">의료진 기능</h3>
                            <ul className="space-y-1 md:space-y-2 text-xs md:text-sm font-medium">
                                <li>비대면 진료</li>
                                <li>예약 관리</li>
                                <li>회원 관리</li>
                                <li>처방전 발급</li>
                            </ul>
                        </div>
                    </div>
                    <div className="pt-6 md:pt-8 border-t border-gray-800 text-center text-xs md:text-sm font-medium">
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
        
        /* 성능을 위해 will-change 추가 */
        .will-change-transform {
            will-change: transform;
        }

        /* Sudden Appear Animations - 초기 opacity 0 때문에 LCP가 늦어지는 것 방지 */
        .scroll-fade-up,
        .scroll-slide-right,
        .scroll-slide-left,
        .scroll-scale-in,
        .scroll-bounce-in,
        .scroll-zoom-in {
          opacity: 0;
          will-change: opacity, transform; /* 브라우저 힌트 제공 */
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