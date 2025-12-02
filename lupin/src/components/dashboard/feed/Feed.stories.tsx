/**
 * Feed.stories.tsx
 *
 * FeedCard 컴포넌트 스토리북
 * - 다양한 상태의 피드 카드 미리보기
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import FeedView from "./Feed";
import { Feed } from "@/types/dashboard.types";
import { useRef, useState } from "react";

// 목 데이터
const mockFeeds: Feed[] = [
  {
    id: 1,
    writerId: 1,
    writerName: "김운동",
    author: "김운동",
    activity: "러닝",
    points: 150,
    content: JSON.stringify([
      { type: "paragraph", content: "오늘 아침 5km 러닝 완료! 날씨가 좋아서 기분도 상쾌하네요 🏃‍♂️" }
    ]),
    images: [
      "https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=500",
      "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=500",
    ],
    likes: 24,
    comments: 5,
    calories: 320,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    time: "2시간 전",
  },
  {
    id: 2,
    writerId: 2,
    writerName: "이헬스",
    author: "이헬스",
    activity: "웨이트",
    points: 200,
    content: JSON.stringify([
      { type: "paragraph", content: "오늘 상체 운동 완료. 벤치프레스 개인 기록 갱신했습니다! 💪" }
    ]),
    images: [
      "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=500",
    ],
    likes: 42,
    comments: 8,
    calories: 450,
    createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
    time: "4시간 전",
  },
  {
    id: 3,
    writerId: 3,
    writerName: "박피트",
    author: "박피트",
    activity: "요가",
    points: 120,
    content: JSON.stringify([
      { type: "paragraph", content: "아침 요가로 하루를 시작합니다. 마음이 편안해지네요 🧘‍♀️" }
    ]),
    images: [],
    likes: 18,
    comments: 3,
    createdAt: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
    time: "6시간 전",
  },
];

// 래퍼 컴포넌트 (상태 관리용)
function FeedWrapper({ feeds }: { feeds: Feed[] }) {
  const [searchQuery, setSearchQuery] = useState("");
  const [showSearch, setShowSearch] = useState(false);
  const [feedImageIndices, setFeedImageIndices] = useState<Record<number, number>>({});
  const [likedFeeds, setLikedFeeds] = useState<Set<number>>(new Set());
  const feedContainerRef = useRef<HTMLDivElement>(null);
  const [scrollToFeedId, setScrollToFeedId] = useState<number | null>(null);

  const getFeedImageIndex = (feedId: number) => feedImageIndices[feedId] || 0;
  const setFeedImageIndex = (feedId: number, index: number) => {
    setFeedImageIndices(prev => ({ ...prev, [feedId]: index }));
  };
  const hasLiked = (feedId: number) => likedFeeds.has(feedId);
  const handleLike = (feedId: number) => {
    setLikedFeeds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(feedId)) {
        newSet.delete(feedId);
      } else {
        newSet.add(feedId);
      }
      return newSet;
    });
  };

  return (
    <div style={{ height: "800px", width: "100%" }}>
      <FeedView
        allFeeds={feeds}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        showSearch={showSearch}
        setShowSearch={setShowSearch}
        getFeedImageIndex={getFeedImageIndex}
        setFeedImageIndex={setFeedImageIndex}
        hasLiked={hasLiked}
        handleLike={handleLike}
        feedContainerRef={feedContainerRef}
        scrollToFeedId={scrollToFeedId}
        setScrollToFeedId={setScrollToFeedId}
        loadMoreFeeds={() => {}}
        hasMoreFeeds={false}
        isLoadingFeeds={false}
      />
    </div>
  );
}

const meta: Meta<typeof FeedView> = {
  title: "Dashboard/Feed",
  component: FeedView,
  parameters: {
    layout: "fullscreen",
    backgrounds: {
      default: "gray",
    },
  },
  tags: ["autodocs"],
};

export default meta;
type Story = StoryObj<typeof meta>;

// 기본 피드 목록
export const Default: Story = {
  render: () => <FeedWrapper feeds={mockFeeds} />,
};

// 이미지가 있는 피드만
export const WithImages: Story = {
  render: () => <FeedWrapper feeds={mockFeeds.filter(f => f.images.length > 0)} />,
};

// 이미지가 없는 피드
export const WithoutImages: Story = {
  render: () => <FeedWrapper feeds={mockFeeds.filter(f => f.images.length === 0)} />,
};

// 단일 피드
export const SingleFeed: Story = {
  render: () => <FeedWrapper feeds={[mockFeeds[0]]} />,
};

// 빈 상태
export const Empty: Story = {
  render: () => <FeedWrapper feeds={[]} />,
};

// 로딩 상태
export const Loading: Story = {
  render: () => {
    const feedContainerRef = useRef<HTMLDivElement>(null);
    return (
      <div style={{ height: "800px", width: "100%" }}>
        <FeedView
          allFeeds={[]}
          searchQuery=""
          setSearchQuery={() => {}}
          showSearch={false}
          setShowSearch={() => {}}
          getFeedImageIndex={() => 0}
          setFeedImageIndex={() => {}}
          hasLiked={() => false}
          handleLike={() => {}}
          feedContainerRef={feedContainerRef}
          scrollToFeedId={null}
          setScrollToFeedId={() => {}}
          loadMoreFeeds={() => {}}
          hasMoreFeeds={true}
          isLoadingFeeds={true}
        />
      </div>
    );
  },
};
