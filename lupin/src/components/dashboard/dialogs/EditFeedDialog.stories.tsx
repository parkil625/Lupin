/**
 * EditFeedDialog.stories.tsx
 *
 * 피드 수정 다이얼로그 스토리북
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import EditFeedDialog from "./EditFeedDialog";
import { Feed } from "@/types/dashboard.types";
import { useState } from "react";

// 목 데이터 - 이미지 있는 피드
const mockFeed: Feed = {
  id: 1,
  writerId: 1,
  writerName: "김운동",
  author: "김운동",
  activity: "러닝",
  points: 150,
  content: JSON.stringify([
    { type: "paragraph", content: [{ type: "text", text: "오늘 아침 5km 러닝 완료! 날씨가 좋아서 기분도 상쾌하네요." }] },
    { type: "paragraph", content: [{ type: "text", text: "매일 조금씩 거리를 늘려가고 있어요." }] }
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

// 목 데이터 - 이미지 많은 피드
const mockFeedManyImages: Feed = {
  id: 2,
  writerId: 1,
  writerName: "이헬스",
  author: "이헬스",
  activity: "헬스",
  points: 200,
  content: JSON.stringify([
    { type: "paragraph", content: [{ type: "text", text: "오늘 상체 운동 완료!" }] }
  ]),
  images: [
    "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
    "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800",
    "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800",
    "https://images.unsplash.com/photo-1581009146145-b5ef050c149a?w=800",
  ],
  likes: 42,
  comments: 8,
  time: "4시간 전",
  calories: 450,
  createdAt: new Date().toISOString(),
};

// 래퍼 컴포넌트 (상태 관리용)
function EditFeedWrapper({ feed, initialOpen = true }: { feed: Feed; initialOpen?: boolean }) {
  const [open, setOpen] = useState(initialOpen);

  const handleSave = (
    feedId: number,
    images: string[],
    content: string,
    workoutType: string,
    startImage: string | null,
    endImage: string | null
  ) => {
    console.log("Save:", { feedId, images, content, workoutType, startImage, endImage });
    alert(`피드 수정 완료!\nID: ${feedId}\n운동 종류: ${workoutType}`);
  };

  return (
    <div className="p-8 bg-gray-100 min-h-screen">
      <button
        onClick={() => setOpen(true)}
        className="px-4 py-2 bg-[#C93831] text-white rounded-lg hover:bg-[#B02F28]"
      >
        피드 수정하기
      </button>
      <EditFeedDialog
        feed={feed}
        open={open}
        onOpenChange={setOpen}
        onSave={handleSave}
      />
    </div>
  );
}

const meta: Meta<typeof EditFeedDialog> = {
  title: "Dashboard/Dialogs/EditFeedDialog",
  component: EditFeedDialog,
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

// 기본 상태 (열린 상태)
export const Default: Story = {
  render: () => <EditFeedWrapper feed={mockFeed} />,
  parameters: {
    docs: {
      description: {
        story: "피드 수정 다이얼로그 기본 상태. 기존 피드 내용이 로드되어 있습니다.",
      },
    },
  },
};

// 이미지 많은 피드
export const ManyImages: Story = {
  render: () => <EditFeedWrapper feed={mockFeedManyImages} />,
  parameters: {
    docs: {
      description: {
        story: "이미지가 많은 피드 수정",
      },
    },
  },
};

// 닫힌 상태로 시작
export const ClosedInitially: Story = {
  render: () => <EditFeedWrapper feed={mockFeed} initialOpen={false} />,
  parameters: {
    docs: {
      description: {
        story: "닫힌 상태로 시작. 버튼 클릭하여 열기",
      },
    },
  },
};

// 모바일 뷰포트
export const Mobile: Story = {
  render: () => <EditFeedWrapper feed={mockFeed} />,
  parameters: {
    viewport: {
      defaultViewport: "mobile1",
    },
    docs: {
      description: {
        story: "모바일 환경에서의 피드 수정 다이얼로그",
      },
    },
  },
};

// 태블릿 뷰포트
export const Tablet: Story = {
  render: () => <EditFeedWrapper feed={mockFeed} />,
  parameters: {
    viewport: {
      defaultViewport: "tablet",
    },
    docs: {
      description: {
        story: "태블릿 환경에서의 피드 수정 다이얼로그",
      },
    },
  },
};
