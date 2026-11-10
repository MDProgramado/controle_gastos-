"use client"

import * as React from "react"
import { usePathname, useRouter } from "next/navigation"
import { cn } from "@/lib/utils"
import {
  Sheet,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
} from "@/components/ui/sheet"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Home, DollarSign, Layout, LogOut, Menu, ChevronLeft } from "lucide-react"

const sidebarItems = [
  { href: "/", label: "Dashboard", icon: Home },
  { href: "/lancamentos", label: "Lançamentos", icon: DollarSign },
  { href: "/categorias", label: "Categorias", icon: Layout },
] as const

// ── Contexto (SidebarProvider) ──────────────────────────────────────────

interface SidebarContextType {
  collapsed: boolean
  onCollapseChange: (collapsed: boolean) => void
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

  return (
    <SidebarContext.Provider value={{ collapsed, onCollapseChange: setCollapsed }}>
      {children}
    </SidebarContext.Provider>
  )
}

function useSidebar() {
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
  const router = useRouter()
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
        "flex items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 hover:text-destructive transition-colors",
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
        "flex items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-sm font-medium transition-colors",
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
    <nav className="flex flex-col gap-1 px-2 py-3">
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
  const { collapsed } = useSidebar()

  // Desktop: sidebar fixo no fluxo (não position:fixed para não sobrepor conteúdo)
  const desktop = (
    <aside
      className={cn(
        "hidden md:flex flex-col border-r h-screen",
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
        {!collapsed && (
          <button
            className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground"
            aria-label="Reduzir menu"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
        )}
      </div>
      <ScrollArea className="h-full">
        <SidebarNav collapsed={collapsed} />
      </ScrollArea>
      <div className="flex flex-col gap-1 px-3 py-3 border-t border-border">
        <SairButton collapsed={collapsed} />
      </div>
    </aside>
  )

  // Mobile: Sheet (drawer) — não ocupa largura do conteúdo
  const mobile = (
    <Sheet>
      <SheetTrigger asChild>
        <button
          className="md:hidden p-2 rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground"
          aria-label="Abrir menu"
        >
          <Menu className="h-5 w-5" />
        </button>
      </SheetTrigger>
      <SheetContent side="left" className="w-[256px]">
        <SheetHeader className="flex items-center justify-between">
          <div className="flex items-center gap-2 px-3 py-4">
            <span className="font-semibold text-lg tracking-tight">Controle</span>
            <span className="text-muted-foreground text-xs font-semibold">
              Financeiro
            </span>
          </div>
          <SheetClose asChild>
            <button className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50">
              <ChevronLeft className="h-5 w-5" />
            </button>
          </SheetClose>
        </SheetHeader>
        <ScrollArea className="h-full">
          <SidebarNav collapsed={true} />
        </ScrollArea>
        <div className="flex flex-col gap-1 px-3 py-3 border-t border-border">
          <SairButton collapsed={true} />
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
