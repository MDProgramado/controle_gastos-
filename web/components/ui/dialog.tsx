import * as React from "react"
import * as DialogPrimitive from "@radix-ui/react-dialog"
import { X } from "lucide-react"
import { cn } from "@/lib/utils"

const Dialog = DialogPrimitive.Root

const DialogTrigger = DialogPrimitive.Trigger

const DialogContent = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content> & {
    className?: string
    /** Botão X no canto superior direito (padrão: visível em todo dialog). */
    showCloseButton?: boolean
  }
>(({ className, children, showCloseButton = true, ...props }, ref) => (
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/80" />
    <DialogPrimitive.Content
      ref={ref}
      className={cn(
        // Mobile: largura com margem de 16px, altura maxima 90dvh e scroll
        // interno. No md+: largura maxima 480px, centralizado.
        "fixed left-[50%] top-[50%] z-50 flex max-h-[90dvh] w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%] flex-col overflow-hidden rounded-lg border border-border bg-card text-card-foreground shadow-lg md:w-full md:max-w-[480px]",
        className
      )}
      {...props}
    >
      {/* O conteudo rola por dentro; o X fica fora dele e sempre visivel. */}
      <div className="grid min-h-0 flex-1 gap-4 overflow-y-auto">{children}</div>
      {showCloseButton ? (
        <DialogPrimitive.Close
          className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          aria-label="Fechar"
        >
          <X className="h-5 w-5" />
        </DialogPrimitive.Close>
      ) : null}
    </DialogPrimitive.Content>
  </DialogPrimitive.Portal>
))
DialogContent.displayName = DialogPrimitive.Content.displayName

const DialogHeader = React.forwardRef<
  React.ElementRef<"div">,
  React.ComponentPropsWithRef<"div"> & {
    className?: string
  }
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("flex flex-col space-y-1 px-6 pt-6 pr-12 text-left", className)}
    {...props}
  />
))
DialogHeader.displayName = "DialogHeader"

const DialogDescription = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Description> & {
    className?: string
  }
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Description
    ref={ref}
    className={cn("text-sm text-muted-foreground", className)}
    {...props}
  />
))
DialogDescription.displayName = DialogPrimitive.Description.displayName

/**
 * Rodapé de ações do dialog. No mobile os botões ficam empilhados e
 * ocupa a largura toda; no md+ voltam para uma linha alinhada à direita.
 */
const DialogFooter = React.forwardRef<
  React.ElementRef<"div">,
  React.ComponentPropsWithRef<"div"> & {
    className?: string
  }
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      "flex flex-col-reverse gap-2 px-6 pb-6 text-sm sm:flex-row sm:justify-end",
      className
    )}
    {...props}
  />
))
DialogFooter.displayName = "DialogFooter"

/**
 * Corpo do dialog: o conteúdo entre o cabeçalho e o rodapé. Recebe o
 * respiro lateral; o respiro de cima é do DialogHeader.
 */
const DialogBody = React.forwardRef<
  React.ElementRef<"div">,
  React.ComponentPropsWithRef<"div"> & {
    className?: string
  }
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn("px-6", className)} {...props} />
))
DialogBody.displayName = "DialogBody"

const DialogTitle = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title> & {
    className?: string
  }
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Title
    ref={ref}
    className={cn("text-lg font-semibold", className)}
    {...props}
  />
))
DialogTitle.displayName = DialogPrimitive.Title.displayName

export {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogBody,
  DialogFooter,
}
