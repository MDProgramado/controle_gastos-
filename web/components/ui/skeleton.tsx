import * as React from "react"
import { cn } from "@/lib/utils"

const Skeleton = React.forwardRef<
  React.ElementRef<"div">,
  React.HTMLAttributes<HTMLDivElement> & { className?: string }
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      "animate-pulse rounded-full bg-muted",
      className
    )}
    {...props}
  />
))
Skeleton.displayName = "Skeleton"

export { Skeleton }
