"use client"

import * as React from "react"
import Image from "next/image"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { ChatPanel } from "@/components/ai/chat-panel"
import { acoesDoAssistente, type PapelAssistente } from "@/lib/assistente"

export function ChatFlutuante({ role = null }: { role?: PapelAssistente }) {
  const pathname = usePathname()
  const [chatAberto, setChatAberto] = React.useState(false)
  const [menuAberto, setMenuAberto] = React.useState(false)
  const botaoRef = React.useRef<HTMLButtonElement>(null)
  const acoes = React.useMemo(() => acoesDoAssistente(role), [role])

  React.useEffect(() => {
    if (!menuAberto) return

    const aoTeclar = (evento: KeyboardEvent) => {
      if (evento.key !== "Escape") return
      setMenuAberto(false)
      botaoRef.current?.focus()
    }

    window.addEventListener("keydown", aoTeclar)
    return () => window.removeEventListener("keydown", aoTeclar)
  }, [menuAberto])

  if (pathname === "/login") return null

  return (
    <>
      {chatAberto && (
        <div
          role="dialog"
          aria-label="Chat com a IA"
          className="fixed bottom-20 right-3 z-40 flex h-[min(32rem,calc(100dvh-7rem))] w-[min(24rem,calc(100vw-2rem))] flex-col overflow-hidden rounded-xl border border-border bg-card shadow-2xl"
        >
          <ChatPanel
            onClose={() => setChatAberto(false)}
            className="h-full max-h-none flex-1 rounded-none border-0"
          />
        </div>
      )}

      {menuAberto && (
        <div
          id="acoes-assistente"
          aria-label="Ações do assistente"
          className="fixed bottom-20 right-3 z-40 w-[min(18rem,calc(100vw-2rem))] rounded-xl border border-border bg-card p-2 shadow-xl"
        >
          {acoes.map((acao) =>
            acao.abreChat ? (
              <button
                key={acao.id}
                type="button"
                onClick={() => {
                  setChatAberto(true)
                  setMenuAberto(false)
                }}
                className="flex w-full items-center rounded-md px-3 py-2 text-left text-sm text-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {acao.label}
              </button>
            ) : (
              <Link
                key={acao.id}
                href={acao.href ?? "#"}
                onClick={() => setMenuAberto(false)}
                className="flex w-full items-center rounded-md px-3 py-2 text-sm text-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {acao.label}
              </Link>
            ),
          )}
        </div>
      )}

      <button
        ref={botaoRef}
        type="button"
        onClick={() => setMenuAberto((aberto) => !aberto)}
        aria-label={
          menuAberto
            ? "Fechar menu do assistente Ogum"
            : "Abrir menu do assistente Ogum"
        }
        aria-expanded={menuAberto}
        aria-controls={menuAberto ? "acoes-assistente" : undefined}
        className={cn(
          "fixed bottom-4 right-3 z-40 flex h-12 w-12 items-center justify-center rounded-full border border-border bg-card shadow-lg transition-transform duration-200 hover:scale-105 active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
          menuAberto && "ring-2 ring-primary",
        )}
      >
        <Image
          src="/ogum.svg"
          alt=""
          width={32}
          height={32}
          className="h-8 w-8"
        />
      </button>
    </>
  )
}
