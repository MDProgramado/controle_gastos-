"use client"

import * as React from "react"
import { Bell, User, Menu } from "lucide-react"
import { useSidebar } from "@/components/ui/sidebar"

interface HeaderProps {
  showNotification?: boolean
  showUser?: boolean
  onNotificationClick?: () => void
  onUserClick?: () => void
}

/**
 * Barra superior (header.nav do kit): ações à direita. A marca fica na sidebar
 * e o nome da página no <h1> dentro de <main> — a barra não repete nenhum dos
 * dois.
 */
export function Header({
  showNotification = true,
  showUser = true,
  onNotificationClick,
  onUserClick,
}: HeaderProps) {
  const { onMobileOpenChange } = useSidebar()

  return (
    <header className="nav flex items-center justify-end border-b border-border px-4 py-4">
      <div className="nav__actions flex items-center gap-2">
        <button
          type="button"
          onClick={() => onMobileOpenChange(true)}
          className="md:hidden p-2 rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          aria-label="Abrir menu"
        >
          <Menu className="h-5 w-5" />
        </button>
        {showNotification && (
          <button
            type="button"
            onClick={onNotificationClick}
            className="rounded-lg p-2 text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            aria-label="Notificações"
          >
            <Bell className="h-5 w-5" />
          </button>
        )}
        {showUser && (
          <button
            type="button"
            onClick={onUserClick}
            className="rounded-lg p-2 text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            aria-label="Perfil"
          >
            <User className="h-5 w-5" />
          </button>
        )}
      </div>
    </header>
  )
}
