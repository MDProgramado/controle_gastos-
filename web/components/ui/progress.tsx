import * as React from "react"
import { cn } from "@/lib/utils"

const Progress = React.forwardRef<
  React.ElementRef<"div">,
  React.HTMLAttributes<HTMLDivElement> & { value?: number }
>(({ className, value, ...props }, ref) => {
  const valor = Math.min(100, Math.max(0, value ?? 0))

  return (
    <div
      ref={ref}
      role="progressbar"
      aria-valuenow={valor}
      aria-valuemin={0}
      aria-valuemax={100}
      className={cn(
        "relative h-2.5 w-full overflow-hidden rounded-full bg-muted",
        className
      )}
      {...props}
    >
      <div
        className="h-full rounded-full bg-primary transition-all duration-300 ease-out"
        style={{ width: `${valor}%` }}
      />
    </div>
  )
})
Progress.displayName = "Progress"

export { Progress }
