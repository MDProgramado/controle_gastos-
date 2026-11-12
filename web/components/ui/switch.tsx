"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import * as SwitchPrimitive from "@radix-ui/react-switch"

/**
 * Switch binário.
 *
 * O Root é o alvo de toque (h-10 w-12 = 40px de altura no mobile). A trilha
 * visual (24px) é um filho posicionado no centro do Root, para o controle
 * continuar legível sem exigir 40px de altura visível.
 *
 * O Thumb precisa ser filho do Root: sem ele o switch renderiza só a trilha
 * e a coluna Status fica visualmente vazia.
 */
const Switch = React.forwardRef<
  React.ElementRef<typeof SwitchPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof SwitchPrimitive.Root>
>(({ className, ...props }, ref) => (
  <SwitchPrimitive.Root
    data-slot="switch"
    className={cn(
      "group relative inline-flex h-10 w-12 shrink-0 cursor-pointer items-center rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
      className
    )}
    {...props}
    ref={ref}
  >
    <span
      aria-hidden="true"
      data-slot="switch-track"
      className="pointer-events-none absolute inset-x-0 top-1/2 h-6 -translate-y-1/2 rounded-full bg-input transition-colors group-data-[state=checked]:bg-primary"
    />
    <SwitchPrimitive.Thumb
      data-slot="switch-thumb"
      className="pointer-events-none absolute left-0.5 top-1/2 block size-5 -translate-y-1/2 rounded-full bg-background shadow-sm transition-transform group-data-[state=checked]:translate-x-6"
    />
  </SwitchPrimitive.Root>
))
Switch.displayName = SwitchPrimitive.Root.displayName

export { Switch }
