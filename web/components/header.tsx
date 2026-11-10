"use client"

import * as React from "react"
import { Bell, User } from "lucide-react"

interface HeaderProps {
  title: string
  showNotification?: boolean
  showUser?: boolean
  onNotificationClick?: () => void
  onUserClick?: () => void
}

export function Header({
  title,
  showNotification = true,
  showUser = true,
  onNotificationClick,
  onUserClick,
}: HeaderProps) {
  return (
    <header className="flex items-center justify-between px-3 py-4 border-b border-border">
      <div className="flex items-center gap-2">
        <h1 className="font-semibold text-lg text-foreground">{title}</h1>
      </div>
      <div className="flex items-center gap-2">
        {showNotification && (
          <button
            onClick={onNotificationClick}
            className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground"
            aria-label="Notificações"
          >
            <Bell className="h-5 w-5" />
          </button>
        )}
        {showUser && (
          <button
            onClick={onUserClick}
            className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground"
            aria-label="Usuário"
          >
            <User className="h-5 w-5" />
          </button>
        )}
      </div>
    </header>
  )
}
