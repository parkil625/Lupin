/**
 * FeedV2.stories.tsx
 *
 * FeedV2 컴포넌트 스토리북 (리팩토링 버전)
 * - Feed.stories.tsx와 비교하면서 작업
 */

import type { Meta, StoryObj } from "@storybook/react";
import FeedViewV2 from "./FeedV2";

const meta: Meta<typeof FeedViewV2> = {
  title: "Dashboard/FeedV2 (리팩토링)",
  component: FeedViewV2,
  parameters: {
    layout: "fullscreen",
    backgrounds: { default: "gray" },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

// 기본
export const Default: Story = {
  render: () => (
    <div style={{ height: "100vh", width: "100%" }}>
      <FeedViewV2 />
    </div>
  ),
};
