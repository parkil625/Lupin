/**
 * CreateFeedDialog.stories.tsx
 *
 * 피드 작성 다이얼로그 스토리북
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import CreateFeedDialog from "./CreateFeedDialog";
import { useState } from "react";

// 래퍼 컴포넌트 (상태 관리용)
function CreateFeedWrapper({ initialOpen = true }: { initialOpen?: boolean }) {
  const [open, setOpen] = useState(initialOpen);

  const handleCreate = (
    images: string[],
    content: string,
    workoutType: string,
    startImage: string | null,
    endImage: string | null
  ) => {
    console.log("Create:", { images, content, workoutType, startImage, endImage });
    alert(`피드 작성 완료!\n운동 종류: ${workoutType}\n이미지 수: ${images.length}`);
  };

  return (
    <div className="p-8 bg-gray-100 min-h-screen">
      <button
        onClick={() => setOpen(true)}
        className="px-4 py-2 bg-[#C93831] text-white rounded-lg hover:bg-[#B02F28]"
      >
        피드 작성하기
      </button>
      <CreateFeedDialog
        open={open}
        onOpenChange={setOpen}
        onCreate={handleCreate}
      />
    </div>
  );
}

const meta: Meta<typeof CreateFeedDialog> = {
  title: "Dashboard/Dialogs/CreateFeedDialog",
  component: CreateFeedDialog,
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
  render: () => <CreateFeedWrapper />,
  parameters: {
    docs: {
      description: {
        story: "피드 작성 다이얼로그 기본 상태. 시작/끝 사진을 업로드하고 글을 작성할 수 있습니다.",
      },
    },
  },
};

// 닫힌 상태로 시작
export const ClosedInitially: Story = {
  render: () => <CreateFeedWrapper initialOpen={false} />,
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
  render: () => <CreateFeedWrapper />,
  parameters: {
    viewport: {
      defaultViewport: "mobile1",
    },
    docs: {
      description: {
        story: "모바일 환경에서의 피드 작성 다이얼로그",
      },
    },
  },
};

// 태블릿 뷰포트
export const Tablet: Story = {
  render: () => <CreateFeedWrapper />,
  parameters: {
    viewport: {
      defaultViewport: "tablet",
    },
    docs: {
      description: {
        story: "태블릿 환경에서의 피드 작성 다이얼로그",
      },
    },
  },
};
