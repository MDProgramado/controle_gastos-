import * as React from "react"
import { cn } from "@/lib/utils"

function Chart({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="chart"
      className={cn("w-full h-[300px]", className)}
      {...props}
    />
  )
}

export { Chart }
