import * as React from "react"
import * as SheetPrimitive from "@radix-ui/react-dialog"
import { X } from "lucide-react"
import { cn } from "@/lib/utils"

const Sheet = SheetPrimitive.Root

const SheetTrigger = SheetPrimitive.Trigger

const SheetClose = SheetPrimitive.Close

const SheetContent = React.forwardRef<
  React.ElementRef<typeof SheetPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof SheetPrimitive.Content> & {
    side?: "top" | "right" | "bottom" | "left"
    /** Botão X no canto superior direito (padrão: visível em todo sheet). */
    showCloseButton?: boolean
  }
>(({ className, side = "right", children, showCloseButton = true, ...props }, ref) => (
  <SheetPrimitive.Portal>
    <SheetPrimitive.Overlay
      className="fixed inset-0 z-50 bg-black/80"
    />
    <SheetPrimitive.Content
      ref={ref}
      className={cn(
        "fixed z-50 flex h-full flex-col bg-background text-foreground",
        side === "right"
          ? "right-0 border-l border-b md:shadow-md"
          : side === "left"
          ? "left-0 border-r border-b md:shadow-md"
          : side === "top"
          ? "top-0 border-b h-1/3 w-full"
          : "bottom-0 border-t border-b w-full",
        className
      )}
      {...props}
    >
      {children}
      {showCloseButton ? (
        <SheetPrimitive.Close
          className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          aria-label="Fechar"
        >
          <X className="h-5 w-5" />
        </SheetPrimitive.Close>
      ) : null}
    </SheetPrimitive.Content>
  </SheetPrimitive.Portal>
))
SheetContent.displayName = SheetPrimitive.Content.displayName

const SheetHeader = React.forwardRef<
  React.ElementRef<"div">,
  React.ComponentPropsWithRef<"div">
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("flex flex-col space-y-1 pr-12 text-center sm:text-left", className)}
    {...props}
  />
))
SheetHeader.displayName = "SheetHeader"

const SheetTitle = React.forwardRef<
  React.ElementRef<typeof SheetPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof SheetPrimitive.Title> & {
    className?: string
  }
>(({ className, ...props }, ref) => (
  <SheetPrimitive.Title
    ref={ref}
    className={cn("text-lg font-semibold", className)}
    {...props}
  />
))
SheetTitle.displayName = "SheetTitle"

const SheetFooter = React.forwardRef<
  React.ElementRef<"div">,
  React.ComponentPropsWithRef<"div">
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("flex flex-col space-y-1 text-center sm:text-left", className)}
    {...props}
  />
))
SheetFooter.displayName = "SheetFooter"

export {
  Sheet,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetFooter,
}
