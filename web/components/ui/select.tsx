import * as React from "react"
import * as SelectPrimitive from "@radix-ui/react-select"
import { cn } from "cn"

const Select = SelectPrimitive.Root

const SelectGroup = SelectPrimitive.Group

const SelectValue = SelectPrimitive.Value

const SelectTrigger = React.forwardRef<
  React.ElementRef<typeof SelectPrimitive.Trigger>,
  React.ComponentPropsWithoutRef<typeof SelectPrimitive.Trigger>
>(({ className, ...props }, ref) => (
  <SelectPrimitive.Trigger
    ref={ref}
    className={cn(
      "flex h-9 w-full items-center justify-between rounded-lg border border-input bg-transparent px-3 py-2 text-sm ring-offset-background file:border-0 file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:ring-ring focus-visible:ring-1 focus-visible:ring-disabled:opacity-50 disabled:pointer-events-none disabled:opacity-50",
      className
    )}
    {...props}
  />
))
SelectTrigger.displayName = SelectPrimitive.Trigger.displayName

const SelectContent = React.forwardRef<
  React.ElementRef<typeof SelectPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof SelectPrimitive.Content> & {
    position?: "popper" | "item"
  }
>(({ className, position = "popper", children, ...props }, ref) => (
  <SelectPrimitive.Portal>
    <SelectPrimitive.Content
      ref={ref}
      className={cn(
        "relative z-50 max-h-96 min-w-[8rem] overflow-hidden rounded-md border border-input bg-card px-2 py-2 text-sm text-foreground shadow-md data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2",
        position === "popper" &&
          "data-[side=bottom]:translate-y-1 data-[side=left]:-translate-x-1 data-[side=right]:translate-x-1 data-[side=top]:-translate-y-1",
        className
      )}
      {...props}
    >
      <SelectPrimitive.ScrollUpButton
        className="flex cursor-pointer items-center justify-center px-2 py-1 text-sm rounded-md text-sm text-muted-foreground"
      >
        <span className="m-0 h-5 w-5 shrink-0 round text-current">
          <span className="relative top-[12%] left-[25%] w-[10px] h-[10px] border border-current rounded-full" />
          <span className="absolute left-[25%] top-[65%] w-[10px] h-[10px] rounded-full border-transparent border-t-current rotate-45" />
        </span>
      </SelectPrimitive.ScrollUpButton>
      <SelectPrimitive.Viewport
        className={cn(
          "relative flex flex-col w-full min-h-[3rem] max-h-[96px] overflow-auto",
          position === "popper" && "m-1"
        )}
      >
        {children}
      </SelectPrimitive.Viewport>
      <SelectPrimitive.ScrollDownButton
        className="flex cursor-pointer items-center justify-center px-2 py-1 text-sm text-muted-foreground"
      >
        <span className="m-0 h-5 w-5 shrink-0 round text-current">
          <span className="relative top-[25%] left-[25%] w-[10px] h-[10px] border border-current rounded-full" />
          <span className="absolute top-[25%] left-[25%] w-[10px] h-[10px] rounded-full border-transparent border-t-current rotate-45" />
        </span>
      </SelectPrimitive.ScrollDownButton>
    </SelectPrimitive.Content>
  </SelectPrimitive.Portal>
))
SelectContent.displayName = SelectPrimitive.Content.displayName

const SelectItem = React.forwardRef<
  React.ElementRef<typeof SelectPrimitive.Item>,
  React.ComponentPropsWithoutRef<typeof SelectPrimitive.Item>
>(({ className, ...props }, ref) => (
  <SelectPrimitive.Item
    ref={ref}
    className={cn(
      "relative flex w-full cursor-pointer items-center rounded-sm px-2 py-1.5 text-sm outline-none transition-colors focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
      className
    )}
    {...props}
  >
    <span className="absolute left-1/2 w-px -translate-x-1/2 top-[6%] bottom-[6%] bg-border hidden" />
    <span className="flex items-center gap-2 text-left">
      <span className="absolute left-1/2 -translate-x-1/2 top-[12%] w-[10px] h-[10px] rounded-full border border-current hidden" />
      {props.children}
    </span>
  </SelectPrimitive.Item>
))
SelectItem.displayName = SelectPrimitive.Item.displayName

export {
  Select,
  SelectGroup,
  SelectValue,
  SelectTrigger,
  SelectContent,
  SelectItem,
}
