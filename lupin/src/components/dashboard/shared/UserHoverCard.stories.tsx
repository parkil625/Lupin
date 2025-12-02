/**
 * UserHoverCard.stories.tsx
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { UserHoverCard } from "./UserHoverCard";

const meta: Meta<typeof UserHoverCard> = {
  title: "Dashboard/Shared/UserHoverCard",
  component: UserHoverCard,
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
  argTypes: {
    size: {
      control: "select",
      options: ["sm", "md", "lg"],
    },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    name: "김운동",
    department: "개발팀",
    activeDays: 15,
    avgScore: 85,
    points: 1250,
  },
};

export const Small: Story = {
  args: {
    name: "이헬스",
    department: "마케팅팀",
    activeDays: 22,
    avgScore: 92,
    points: 2100,
    size: "sm",
  },
};

export const Large: Story = {
  args: {
    name: "박피트",
    department: "디자인팀",
    activeDays: 30,
    avgScore: 78,
    points: 1800,
    size: "lg",
  },
};

export const NewUser: Story = {
  args: {
    name: "신입사원",
    department: "인사팀",
    activeDays: 0,
    avgScore: 0,
    points: 0,
  },
};
