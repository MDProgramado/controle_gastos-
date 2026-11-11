"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import * as SwitchPrimitive from "@radix-ui/react-switch"

const Switch = React.forwardRef<
  React.ElementRef<typeof SwitchPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof SwitchPrimitive.Root>
>(({ className, ...props }, ref) => (
  <SwitchPrimitive.Root
    data-slot="switch"
    className={cn(
      "peer inline-flex h-[1.15rem] w-8 shrink-0 items-center rounded-full border border-transparent shadow-xs transition-all outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=unchecked]:bg-input",
      className
    )}
    {...props}
    ref={ref}
  >
    {/* O Thumb precisa ser filho do Root: sem ele o switch renderiza só a
       trilha (32x16) e a coluna Status fica visualmente vazia. */}
    <SwitchPrimitive.Thumb
      data-slot="switch-thumb"
      className="pointer-events-none block size-4 rounded-full bg-background ring-0 transition-transform data-[state=checked]:translate-x-[calc(100%-2px)] data-[state=unchecked]:translate-x-0"
    />
  </SwitchPrimitive.Root>
))
Switch.displayName = SwitchPrimitive.Root.displayName

export { Switch }
