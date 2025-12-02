/**
 * FeedDetailContent.stories.tsx
 *
 * 피드 상세 콘텐츠 스토리북
 * - Dialog 없이 독립적으로 사용 가능한 피드 상세 뷰
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { FeedDetailContent } from "./FeedDetailContent";
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
    { type: "paragraph", content: [{ type: "text", text: "오늘 아침 5km 러닝 완료! 날씨가 좋아서 기분도 상쾌하네요 🏃‍♂️" }] }
  ]),
  images: [
    "https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=800",
    "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800",
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
    { type: "paragraph", content: [{ type: "text", text: "아침 요가로 하루를 시작합니다. 마음이 편안해지네요 🧘‍♀️" }] }
  ]),
  images: [],
  likes: 18,
  comments: 3,
  time: "6시간 전",
  createdAt: new Date().toISOString(),
};

// 래퍼 컴포넌트
function FeedDetailWrapper({ feed, isMine = false }: { feed: Feed; isMine?: boolean }) {
  const [imageIndex, setImageIndex] = useState(0);
  const [liked, setLiked] = useState(false);
  const [currentFeed, setCurrentFeed] = useState(feed);

  const handleLike = () => {
    setLiked(!liked);
    setCurrentFeed(prev => ({
      ...prev,
      likes: liked ? prev.likes - 1 : prev.likes + 1
    }));
  };

  return (
    <div className="p-8 bg-gray-100 min-h-screen flex items-center justify-center">
      <FeedDetailContent
        feed={currentFeed}
        currentImageIndex={imageIndex}
        onPrevImage={() => setImageIndex(Math.max(0, imageIndex - 1))}
        onNextImage={() => setImageIndex(Math.min(feed.images.length - 1, imageIndex + 1))}
        onEdit={(feed) => console.log("Edit:", feed)}
        onDelete={(feedId) => console.log("Delete:", feedId)}
        liked={liked}
        onLike={handleLike}
        isMine={isMine}
      />
    </div>
  );
}

const meta: Meta<typeof FeedDetailContent> = {
  title: "Dashboard/Feed/FeedDetailContent",
  component: FeedDetailContent,
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

// 다른 사람 피드 (좋아요 가능)
export const OtherUserFeed: Story = {
  render: () => <FeedDetailWrapper feed={mockFeedWithImages} isMine={false} />,
  parameters: {
    docs: {
      description: {
        story: "다른 사람의 피드. 좋아요 버튼이 활성화되어 있습니다.",
      },
    },
  },
};

// 내 피드 (수정/삭제 가능)
export const MyFeed: Story = {
  render: () => <FeedDetailWrapper feed={mockFeedWithImages} isMine={true} />,
  parameters: {
    docs: {
      description: {
        story: "내 피드. 우측 상단에 수정/삭제 메뉴가 표시됩니다.",
      },
    },
  },
};

// 이미지 없는 피드
export const WithoutImages: Story = {
  render: () => <FeedDetailWrapper feed={mockFeedNoImages} isMine={false} />,
};
