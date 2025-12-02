/**
 * FeedDetailDialogHome.stories.tsx
 *
 * 피드 상세보기 다이얼로그 스토리북
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import FeedDetailDialogHome from "./FeedDetailDialogHome";
import { Feed } from "@/types/dashboard.types";
import { useState } from "react";

// 목 데이터 - 이미지 있는 피드
const mockFeedWithImages: Feed = {
  id: 1,
  writerId: 1,
  writerName: "김운동",
  author: "김운동",
  activity: "러닝",
  points: 150,
  content: JSON.stringify([
    { type: "paragraph", content: [{ type: "text", text: "오늘 아침 5km 러닝 완료! 날씨가 좋아서 기분도 상쾌하네요 🏃‍♂️" }] },
    { type: "paragraph", content: [{ type: "text", text: "매일 조금씩 거리를 늘려가고 있어요. 이번 주 목표는 매일 5km 달리기!" }] }
  ]),
  images: [
    "https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=800",
    "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800",
    "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=800",
  ],
  likes: 24,
  comments: 5,
  time: "2시간 전",
  calories: 320,
  createdAt: new Date().toISOString(),
};

// 목 데이터 - 이미지 없는 피드
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

// 목 데이터 - 수정된 피드
const mockFeedEdited: Feed = {
  id: 3,
  writerId: 3,
  writerName: "박피트",
  author: "박피트",
  activity: "웨이트",
  points: 200,
  content: JSON.stringify([
    { type: "paragraph", content: [{ type: "text", text: "오늘 상체 운동 완료. 벤치프레스 개인 기록 갱신! 💪" }] }
  ]),
  images: [
    "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
  ],
  likes: 42,
  comments: 8,
  time: "4시간 전",
  calories: 450,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

// 래퍼 컴포넌트 (상태 관리용)
function FeedDetailWrapper({ feed, initialOpen = true }: { feed: Feed; initialOpen?: boolean }) {
  const [open, setOpen] = useState(initialOpen);
  const [imageIndex, setImageIndex] = useState(0);

  return (
    <div className="p-8 bg-gray-100 min-h-screen">
      <button
        onClick={() => setOpen(true)}
        className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
      >
        피드 상세 열기
      </button>
      <FeedDetailDialogHome
        feed={feed}
        open={open}
        onOpenChange={setOpen}
        currentImageIndex={imageIndex}
        onPrevImage={() => setImageIndex(Math.max(0, imageIndex - 1))}
        onNextImage={() => setImageIndex(Math.min(feed.images.length - 1, imageIndex + 1))}
        onEdit={(feed) => console.log("Edit:", feed)}
        onDelete={(feedId) => console.log("Delete:", feedId)}
      />
    </div>
  );
}

const meta: Meta<typeof FeedDetailDialogHome> = {
  title: "Dashboard/Dialogs/FeedDetailDialogHome",
  component: FeedDetailDialogHome,
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
  render: () => <FeedDetailWrapper feed={mockFeedWithImages} />,
  parameters: {
    docs: {
      description: {
        story: "여러 이미지가 있는 피드 상세보기. 좌우 화살표로 이미지를 넘길 수 있습니다.",
      },
    },
  },
};

// 이미지 없는 피드
export const WithoutImages: Story = {
  render: () => <FeedDetailWrapper feed={mockFeedNoImages} />,
  parameters: {
    docs: {
      description: {
        story: "이미지가 없는 피드 상세보기",
      },
    },
  },
};

// 수정된 피드
export const Edited: Story = {
  render: () => <FeedDetailWrapper feed={mockFeedEdited} />,
  parameters: {
    docs: {
      description: {
        story: "수정된 피드 (수정 표시가 있음)",
      },
    },
  },
};

// 닫힌 상태로 시작
export const ClosedInitially: Story = {
  render: () => <FeedDetailWrapper feed={mockFeedWithImages} initialOpen={false} />,
  parameters: {
    docs: {
      description: {
        story: "닫힌 상태로 시작. 버튼 클릭하여 열기",
      },
    },
  },
};
