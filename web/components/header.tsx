"use client"

import * as React from "react"
import { Bell, User, Menu } from "lucide-react"
import { useSidebar } from "@/components/ui/sidebar"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { Loader2, CheckCircle2 } from "lucide-react"
import { useImportacaoBackground } from "@/contexts/ImportacaoContext"
interface HeaderProps {
  /**
   * Título da página. No mobile é o único título visível no topo (a sidebar
   * vira drawer, então o nome da página vive aqui); no md+ o <h1> do hero,
   * logo abaixo, cumpre esse papel e o cabeçalho não o repete.
   */
  titulo?: string
  showNotification?: boolean
  showUser?: boolean
  onNotificationClick?: () => void
  onUserClick?: () => void
}

/**
 * Barra superior (header.nav do kit): hambúrguer à esquerda, título da página
 * ao centro, ações à direita. Alvos de toque com h-10 w-10 (40px) no mobile.
 */
export function Header({
  titulo,
  showNotification = true,
  showUser = true,
  onNotificationClick,
  onUserClick,
}: HeaderProps) {
  const { onMobileOpenChange } = useSidebar()
  const pathname = usePathname()
const { statusBg, progressoBg } = useImportacaoBackground()

  return (
    <header className="nav relative flex items-center justify-between gap-3 border-b border-border px-4 min-h-16 py-3 md:px-8 lg:px-10">
      <button
        type="button"
        onClick={() => onMobileOpenChange(true)}
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 md:hidden"
        aria-label="Abrir menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* Título centralizado: absoluto para ficar de verdade no centro do
          cabeçalho, independente da largura dos ícones das pontas. No md+
          o <h1> do hero, logo abaixo, cumpre esse papel e o cabeçalho não
          repete o texto. */}
      {titulo ? (
        <span className="pointer-events-none absolute left-4 right-4 truncate text-center text-xl font-semibold tracking-tight text-foreground md:hidden">
          {titulo}
        </span>
      ) : null}

{pathname !== "/importacoes" && statusBg === "analisando" && (
  <Link
    href="/importacoes"
    className="inline-flex items-center gap-2 rounded-full border border-primary/40 bg-primary/10 px-3 py-1 text-xs font-medium text-foreground transition-colors hover:bg-primary/20"
  >
    <Loader2 className="h-3.5 w-3.5 animate-spin text-primary" />
    <span>Importando PDF ({Math.round(progressoBg)}%)…</span>
  </Link>
)}

{pathname !== "/importacoes" && statusBg === "concluido" && (
  <Link
    href="/importacoes"
    className="inline-flex items-center gap-2 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-foreground transition-colors hover:bg-emerald-500/20"
  >
    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
    <span>Importação pronta para aprovar</span>
  </Link>
)}
      <div className="nav__actions ml-auto flex shrink-0 items-center gap-1">
        {showNotification && (
          <button
            type="button"
            onClick={onNotificationClick}
            className="flex h-10 w-10 items-center justify-center rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            aria-label="Notificações"
          >
            <Bell className="h-5 w-5" />
          </button>
        )}
        {showUser && (
          <button
            type="button"
            onClick={onUserClick}
            className="flex h-10 w-10 items-center justify-center rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            aria-label="Perfil"
          >
            <User className="h-5 w-5" />
          </button>
        )}
      </div>
    </header>
  )
}
