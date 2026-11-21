"use client"

import * as React from "react"
import Image from "next/image"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { ChatPanel } from "@/components/ai/chat-panel"

/**
 * Botão flutuante no canto inferior esquerdo (ogum) que abre o chat da IA
 * em um painel independente da tela atual. Montado no layout raiz: a
 * conversa persiste ao navegar enquanto o componente não desmonta.
 * Oculta em /login (sem sessão).
 */
export function ChatFlutuante() {
  const pathname = usePathname()
  const [aberto, setAberto] = React.useState(false)

  if (pathname === "/login") return null

  return (
    <>
      {aberto && (
        <div
          role="dialog"
          aria-label="Chat com a IA"
          className="fixed bottom-20 left-4 z-40 flex h-[min(32rem,calc(100dvh-7rem))] w-[min(24rem,calc(100vw-2rem))] flex-col overflow-hidden rounded-xl border border-border bg-card shadow-2xl"
        >
          <ChatPanel
            onClose={() => setAberto(false)}
            className="h-full max-h-none flex-1 rounded-none border-0"
          />
        </div>
      )}

      <button
        type="button"
        onClick={() => setAberto((a) => !a)}
        aria-label={aberto ? "Fechar chat com a IA" : "Falar com a IA"}
        aria-expanded={aberto}
        aria-controls={aberto ? "chat-flutuante" : undefined}
        className={cn(
          "fixed bottom-4 left-4 z-40 flex h-12 w-12 items-center justify-center rounded-full border border-border bg-card shadow-lg transition-transform hover:scale-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
          aberto && "ring-2 ring-primary",
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
