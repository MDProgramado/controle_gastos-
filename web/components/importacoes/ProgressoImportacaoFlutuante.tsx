"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { CheckCircle2, Loader2 } from "lucide-react"
import { Progress } from "@/components/ui/progress"
import { useImportacaoBackground } from "@/contexts/ImportacaoContext"

/**
 * Progresso da importação em background, fixo no canto superior direito,
 * um pouco abaixo do header. Visível em qualquer tela autenticada enquanto
 * a análise roda (ou quando fica pronta para aprovar). Oculta em /importacoes
 * (a página tem a própria barra) e em /login.
 */
export function ProgressoImportacaoFlutuante() {
  const pathname = usePathname()
  const { statusBg, progressoBg } = useImportacaoBackground()

  if (pathname === "/importacoes" || pathname === "/login") return null
  if (statusBg !== "analisando" && statusBg !== "concluido") return null

  if (statusBg === "concluido") {
    return (
      <div
        role="status"
        className="fixed right-4 top-20 z-40 w-[min(22rem,calc(100vw-2rem))] md:right-8"
      >
        <Link
          href="/importacoes"
          className="flex items-center gap-2 rounded-lg border border-emerald-500/40 bg-emerald-500/10 px-4 py-3 text-sm font-medium text-foreground shadow-lg backdrop-blur transition-colors hover:bg-emerald-500/20"
        >
          <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500" />
          <span>Importação pronta para aprovar</span>
        </Link>
      </div>
    )
  }

  const percentual = Math.round(progressoBg)

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed right-4 top-20 z-40 w-[min(22rem,calc(100vw-2rem))] md:right-8"
    >
      <Link
        href="/importacoes"
        className="flex items-center gap-3 rounded-lg border border-primary/40 bg-card/95 px-4 py-3 shadow-lg backdrop-blur transition-colors hover:bg-primary/10"
      >
        <Loader2 className="h-4 w-4 shrink-0 animate-spin text-primary" />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-foreground">
            Importando PDF…
          </span>
          <Progress value={progressoBg} className="mt-1.5 h-1.5" />
        </span>
        <span className="shrink-0 text-xs tabular-nums text-muted-foreground">
          {percentual}%
        </span>
      </Link>
    </div>
  )
}
