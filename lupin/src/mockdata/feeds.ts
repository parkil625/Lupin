import { Feed } from "@/types/dashboard.types";

export const myFeeds: Feed[] = [
  {
    id: 1,
    author: "김루핀",
    avatar: "김",
    activity: "헬스 운동",
    duration: "60분",
    points: 30,
    content: "오늘 스쿼트 100kg 달성! 💪 꾸준히 해온 결과가 드디어 나타나네요. 작년에는 80kg도 힘들었는데 정말 뿌듯합니다!",
    images: [
      "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
      "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800"
    ],
    likes: 45,
    comments: 8,
    time: "3시간 전",
    stats: { strength: "+15", endurance: "+10" },
    isMine: true,
    likedBy: []
  },
  {
    id: 2,
    author: "김루핀",
    avatar: "김",
    activity: "러닝",
    duration: "30분",
    points: 20,
    content: "아침 러닝 5km 완주! ☀️ 날씨가 좋아서 기분도 최고입니다.",
    images: [
      "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800",
      "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800"
    ],
    likes: 32,
    comments: 5,
    time: "1일 전",
    stats: { cardio: "+20", calories: "320kcal" },
    isMine: true,
    likedBy: []
  },
  {
    id: 3,
    author: "김루핀",
    avatar: "김",
    activity: "요가",
    duration: "45분",
    points: 20,
    content: "요가로 하루 시작 🧘‍♀️ 몸과 마음이 한결 가벼워진 느낌!",
    images: ["https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800"],
    likes: 28,
    comments: 4,
    time: "2일 전",
    stats: { flexibility: "+25", mindfulness: "+30" },
    isMine: true,
    likedBy: []
  },
  {
    id: 4,
    author: "김루핀",
    avatar: "김",
    activity: "수영",
    duration: "40분",
    points: 25,
    content: "자유형 1km 달성! 🏊‍♂️",
    images: ["https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800"],
    likes: 20,
    comments: 3,
    time: "3일 전",
    stats: { cardio: "+20" },
    isMine: true,
    likedBy: []
  },
  {
    id: 5,
    author: "김루핀",
    avatar: "김",
    activity: "필라테스",
    duration: "50분",
    points: 25,
    content: "코어 운동 집중! 💪",
    images: ["https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800"],
    likes: 18,
    comments: 2,
    time: "4일 전",
    stats: { core: "+30" },
    isMine: true,
    likedBy: []
  }
];

export const allFeeds: Feed[] = [
  {
    id: 5,
    author: "이철수",
    avatar: "이",
    activity: "헬스 운동",
    duration: "60분",
    points: 30,
    content: "오늘도 데드리프트 120kg 성공! 💪 작년 이맘때는 80kg도 힘들었는데... 꾸준함이 정말 중요하다는 걸 느낍니다. 모두 파이팅!",
    images: [
      "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
      "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
      "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=800"
    ],
    likes: 124,
    comments: 23,
    time: "2시간 전",
    stats: { strength: "+15", endurance: "+10" },
    likedBy: []
  },
  {
    id: 6,
    author: "박영희",
    avatar: "박",
    activity: "아침 러닝",
    duration: "45분",
    points: 25,
    content: "한강 러닝 10km 완주 ☀️ 아침 공기가 정말 상쾌했어요. 오늘 하루도 화이팅!",
    images: [
      "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800",
      "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800"
    ],
    likes: 89,
    comments: 15,
    time: "5시간 전",
    stats: { cardio: "+20", calories: "520kcal" },
    likedBy: []
  },
  {
    id: 7,
    author: "최민수",
    avatar: "최",
    activity: "요가 클래스",
    duration: "50분",
    points: 20,
    content: "빈야사 플로우 클래스 완료! 🧘‍♂️ 몸과 마음이 한결 가벼워진 느낌. 스트레스 해소에 최고예요.",
    images: ["https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800"],
    likes: 67,
    comments: 12,
    time: "1일 전",
    stats: { flexibility: "+25", mindfulness: "+30" },
    likedBy: []
  }
];
