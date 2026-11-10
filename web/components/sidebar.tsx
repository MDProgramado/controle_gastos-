"use client"

import * as React from "react"
import { usePathname, useRouter } from "next/navigation"
import { cn } from "cn"
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

interface SidebarProps {
  isCollapsed?: boolean
}

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

export function Sidebar({ isCollapsed = false }: SidebarProps) {
  const pathname = usePathname()
  const router = useRouter()

  return (
    <>
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
            <SheetClose asChild>
              <button className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50">
                <ChevronLeft className="h-5 w-5" />
              </button>
            </SheetClose>
          </SheetHeader>
          <SheetFooter>
            <div className="flex flex-col gap-1 px-3 py-3 border-t border-border">
              <nav>
                {sidebarItems.map((item) => (
                  <SidebarItem
                    key={item.href}
                    item={item}
                    isActive={pathname === item.href}
                    isCollapsed={true}
                  />
                ))}
              </nav>
              <button
                onClick={() => {
                  document.cookie =
                    "sess=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
                  router.push("/login")
                }}
                className="flex items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 hover:text-destructive transition-colors justify-center"
              >
                <LogOut className="h-5 w-5 shrink-0" />
                <span>Sair</span>
              </button>
            </div>
          </SheetFooter>
        </SheetContent>
      </Sheet>

      {/* Desktop sidebar */}
      <aside
        className={cn(
          "hidden md:flex flex-col border-r border-b fixed top-0 left-0 h-screen z-40 transition-all duration-300",
          isCollapsed ? "w-16" : "w-64"
        )}
      >
        <div className="flex items-center justify-between px-3 py-4 border-b border-border">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-lg tracking-tight">Controle</span>
            <span className="text-muted-foreground text-xs font-semibold">Financeiro</span>
          </div>
          {!isCollapsed && (
            <button
              className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground"
              aria-label="Reduzir menu"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
          )}
        </div>
        <ScrollArea className="h-full">
          <nav className="flex flex-col gap-1 px-2 py-3">
            {sidebarItems.map((item) => (
              <SidebarItem
                key={item.href}
                item={item}
                isActive={pathname === item.href}
                isCollapsed={isCollapsed}
              />
            ))}
          </nav>
        </ScrollArea>
        <div className="flex flex-col gap-1 px-3 py-3 border-t border-border">
          <button
            onClick={() => {
              document.cookie =
                "sess=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
              router.push("/login")
            }}
            className="flex items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 hover:text-destructive transition-colors"
          >
            <LogOut className="h-5 w-5 shrink-0" />
            <span className={cn(isCollapsed ? "hidden" : "block")}>Sair</span>
          </button>
        </div>
      </aside>
    </>
  )
}
