"use client"

import * as React from "react"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import { Home, DollarSign, Layout, LogOut, ChevronLeft, ChevronRight } from "lucide-react"

const sidebarItems = [
  { href: "/", label: "Dashboard", icon: Home },
  { href: "/lancamentos", label: "Lançamentos", icon: DollarSign },
  { href: "/categorias", label: "Categorias", icon: Layout },
] as const

// ── Contexto (SidebarProvider) ──────────────────────────────────────────

interface SidebarContextType {
  collapsed: boolean
  onCollapseChange: (collapsed: boolean) => void
  mobileOpen: boolean
  onMobileOpenChange: (open: boolean) => void
}

const SidebarContext = React.createContext<SidebarContextType | null>(null)

interface SidebarProviderProps {
  children: React.ReactNode
  defaultCollapsed?: boolean
}

export function SidebarProvider({
  children,
  defaultCollapsed = false,
}: SidebarProviderProps) {
  const [collapsed, setCollapsed] = React.useState(defaultCollapsed)
  const [mobileOpen, setMobileOpen] = React.useState(false)

  return (
    <SidebarContext.Provider
      value={{
        collapsed,
        onCollapseChange: setCollapsed,
        mobileOpen,
        onMobileOpenChange: setMobileOpen,
      }}
    >
      {children}
    </SidebarContext.Provider>
  )
}

export function useSidebar() {
  const context = React.useContext(SidebarContext)
  if (!context) {
    throw new Error("useSidebar deve ser usado dentro de <SidebarProvider>")
  }
  return context
}

// ── Sidebar (shell flex container) ──────────────────────────────────────

interface SidebarProps {
  children: React.ReactNode
}

export function Sidebar({ children }: SidebarProps) {
  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      {children}
    </div>
  )
}

// ── Sair (botão de logout, apenas no rodapé da sidebar) ─────────────────

function SairButton({ collapsed }: { collapsed?: boolean }) {
  return (
    <button
      onClick={async () => {
        document.cookie =
          "sess=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
        try {
          await fetch("/api/auth/logout", { method: "POST" })
        } catch {
          // ignora erro de rede
        }
        window.location.replace("/login")
      }}
      className={cn(
        // Neutro em repouso, destrutivo no hover. h-10: alvo de toque.
        "flex h-10 items-center gap-3 rounded-lg border border-transparent px-3 text-sm font-medium text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:bg-destructive/20",
        collapsed ? "justify-center px-2" : ""
      )}
    >
      <LogOut className="h-5 w-5 shrink-0" />
      {collapsed ? null : <span>Sair</span>}
    </button>
  )
}

// ── SidebarItem ─────────────────────────────────────────────────────────

function SidebarItem({
  item,
  isActive,
  isCollapsed,
}: {
  item: (typeof sidebarItems)[number]
  isActive: boolean
  isCollapsed: boolean
}) {
  return (
    <a
      href={item.href}
      className={cn(
        // h-10: alvo de toque de 40px no mobile.
        "flex h-10 items-center gap-3 rounded-lg border border-transparent px-3 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
        isActive
          ? "bg-accent text-accent-foreground"
          : "text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground",
        isCollapsed && "justify-center px-2"
      )}
    >
      <item.icon className="h-5 w-5 shrink-0" />
      {!isCollapsed && <span>{item.label}</span>}
    </a>
  )
}

function SidebarNav({ collapsed }: { collapsed: boolean }) {
  const pathname = usePathname()
  return (
    <nav className="flex-1 flex flex-col gap-1 px-2 py-3">
      {sidebarItems.map((item) => (
        <SidebarItem
          key={item.href}
          item={item}
          isActive={pathname === item.href}
          isCollapsed={collapsed}
        />
      ))}
    </nav>
  )
}

// ── SidebarContent (sidebar fixo à esquerda / Sheet em mobile) ──────────

interface SidebarContentProps extends React.ComponentProps<"aside"> {
  className?: string
}

export function SidebarContent({
  className,
  ...props
}: SidebarContentProps) {
  const { collapsed, onCollapseChange, mobileOpen, onMobileOpenChange } = useSidebar()

  // Desktop: sidebar fixo no fluxo (não position:fixed para não sobrepor conteúdo)
  const desktop = (
    <aside
      className={cn(
        "hidden md:flex flex-col h-full border-r",
        collapsed ? "w-16" : "w-64",
        className
      )}
      {...props}
    >
      <div className="flex items-center justify-between px-3 py-4 border-b border-border">
        <div className="flex items-center gap-2">
          <span className="font-semibold text-lg tracking-tight">Controle</span>
          <span className="text-muted-foreground text-xs font-semibold">
            Financeiro
          </span>
        </div>
        <button
          onClick={() => onCollapseChange(!collapsed)}
          className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground"
          aria-label={collapsed ? "Expandir menu" : "Reduzir menu"}
        >
          {collapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <ChevronLeft className="h-4 w-4" />
          )}
        </button>
      </div>
      <div className="flex-1 overflow-y-auto">
        <SidebarNav collapsed={collapsed} />
      </div>
      <div className="flex flex-col gap-1 px-3 py-3 border-t border-border">
        <SairButton collapsed={collapsed} />
      </div>
    </aside>
  )

  // Mobile: Sheet (drawer) — não ocupa largura do conteúdo
  const mobile = (
    <Sheet open={mobileOpen} onOpenChange={onMobileOpenChange}>
      <SheetContent side="left" className="w-[280px]">
        <SheetHeader className="flex items-center justify-between">
          {/* SheetTitle (e nao um span): o Radix usa este elemento como
              rotulo acessivel do dialog. */}
          <SheetTitle className="px-3 py-4 text-lg font-semibold tracking-tight">
            Controle Financeiro
          </SheetTitle>
        </SheetHeader>
        <div className="flex-1 overflow-y-auto">
          {/* No drawer mobile os itens mostram o rótulo: o drawer tem 280px
              e o ícone sozinho não diz para onde vai. */}
          <SidebarNav collapsed={false} />
        </div>
        <div className="flex flex-col gap-1 border-t border-border px-3 py-3">
          <SairButton collapsed={false} />
        </div>
      </SheetContent>
    </Sheet>
  )

  return (
    <>
      {mobile}
      {desktop}
    </>
  )
}

// ── SidebarInset (conteúdo principal, flex-1, sem largura fixa) ─────────

interface SidebarInsetProps extends React.ComponentProps<"div"> {
  className?: string
}

export function SidebarInset({
  className,
  ...props
}: SidebarInsetProps) {
  return (
    <div className={cn("flex-1 overflow-hidden", className)} {...props} />
  )
}
