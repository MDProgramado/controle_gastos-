import * as React from "react"
import { cn } from "cn"

const Progress = React.forwardRef<
  React.ElementRef<"div">,
  React.HTMLAttributes<HTMLDivElement> & { value?: number }
>(({ className, value, ...props }, ref) => (
  <div
    ref={ref}
    role="progressbar"
    aria-valuenow={value}
    aria-valuemin={0}
    aria-valuemax={100}
    className={cn(
      "relative w-full h-2.5 overflow-hidden rounded-full border border-border bg-muted",
      className
    )}
    {...props}
  >
    <div
      className="flex-1 bg-primary"
      style={{ transform: `translateX(-${100 - (value ?? 0)}%)` }}
    />
  </div>
))
Progress.displayName = "Progress"

export { Progress }
