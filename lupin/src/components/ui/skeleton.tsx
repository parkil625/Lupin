import * as React from "react";
import { cn } from "./utils";

function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("animate-pulse rounded-md", className)}
      style={{ backgroundColor: 'rgba(201, 56, 49, 0.15)' }}
      {...props}
    />
  );
}

export { Skeleton };
