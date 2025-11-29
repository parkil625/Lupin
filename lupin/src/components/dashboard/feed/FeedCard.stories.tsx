/**
 * FeedCard.stories.tsx
 *
 * FeedCard 컴포넌트 단독 스토리북
 */

import type { Meta, StoryObj } from "@storybook/react";
import { FeedCard } from "./FeedCard";
import { Feed } from "@/types/dashboard.types";
import { useState } from "react";

// 목 데이터
const mockFeedWithImages: Feed = {
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
  time: "2시간 전",
  calories: 320,
  createdAt: new Date().toISOString(),
};

const mockFeedNoImages: Feed = {
  id: 2,
  writerId: 2,
  writerName: "이헬스",
  author: "이헬스",
  activity: "요가",
  points: 120,
  content: JSON.stringify([
    { type: "paragraph", content: "아침 요가로 하루를 시작합니다. 마음이 편안해지네요 🧘‍♀️" }
  ]),
  images: [],
  likes: 18,
  comments: 3,
  time: "6시간 전",
  createdAt: new Date().toISOString(),
};

const mockFeedEdited: Feed = {
  id: 3,
  writerId: 3,
  writerName: "박피트",
  author: "박피트",
  activity: "웨이트",
  points: 200,
  content: JSON.stringify([
    { type: "paragraph", content: "오늘 상체 운동 완료. 벤치프레스 개인 기록 갱신! 💪" }
  ]),
  images: [
    "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=500",
  ],
  likes: 42,
  comments: 8,
  time: "4시간 전",
  calories: 450,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

// 래퍼 컴포넌트 (상태 관리용)
function FeedCardWrapper({ feed: initialFeed, initialLiked = false }: { feed: Feed; initialLiked?: boolean }) {
  const [imageIndex, setImageIndex] = useState(0);
  const [liked, setLiked] = useState(initialLiked);
  const [feed, setFeed] = useState(initialFeed);

  const handleLike = () => {
    setLiked(!liked);
    setFeed(prev => ({
      ...prev,
      likes: liked ? prev.likes - 1 : prev.likes + 1
    }));
  };

  return (
    <div className="p-8 bg-gray-100 min-h-screen flex items-center justify-center">
      <FeedCard
        feed={feed}
        currentImageIndex={imageIndex}
        liked={liked}
        onImageIndexChange={(_, index) => setImageIndex(index)}
        onLike={handleLike}
      />
    </div>
  );
}

const meta: Meta<typeof FeedCard> = {
  title: "Dashboard/Feed/FeedCard",
  component: FeedCard,
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

// 이미지가 있는 피드
export const WithImages: Story = {
  render: () => <FeedCardWrapper feed={mockFeedWithImages} />,
};

// 이미지 여러 개
export const MultipleImages: Story = {
  render: () => <FeedCardWrapper feed={mockFeedWithImages} />,
  parameters: {
    docs: {
      description: {
        story: "여러 이미지가 있는 피드. 좌우 화살표로 넘길 수 있습니다.",
      },
    },
  },
};

// 이미지 없는 피드
export const WithoutImages: Story = {
  render: () => <FeedCardWrapper feed={mockFeedNoImages} />,
};

// 좋아요된 상태
export const Liked: Story = {
  render: () => <FeedCardWrapper feed={mockFeedWithImages} initialLiked={true} />,
};

// 수정된 피드
export const Edited: Story = {
  render: () => <FeedCardWrapper feed={mockFeedEdited} />,
  parameters: {
    docs: {
      description: {
        story: "수정된 피드 (수정 표시가 있음)",
      },
    },
  },
};
