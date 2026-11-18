"use client"

import * as React from "react"
import {
  CheckCircle2,
  Database,
  FileSearch,
  Loader2,
  Sparkles,
  UploadCloud,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"

type EtapaItem = {
  id: string
  titulo: string
  descricao: string
  icon: React.ComponentType<{ className?: string }>
}

const ETAPAS_ANALISE: EtapaItem[] = [
  {
    id: "envio",
    titulo: "1. Envio do lote",
    descricao: "Transferindo os PDFs para o servidor.",
    icon: UploadCloud,
  },
  {
    id: "extracao",
    titulo: "2. Leitura estrutural",
    descricao: "Extraindo competência, datas e valores.",
    icon: FileSearch,
  },
  {
    id: "ia",
    titulo: "3. Categorização e alertas",
    descricao: "Classificando lançamentos e checando duplicidades.",
    icon: Sparkles,
  },
]

const ETAPAS_CONFIRMACAO: EtapaItem[] = [
  {
    id: "validacao",
    titulo: "1. Validação dos itens",
    descricao: "Conferindo categorias e valores selecionados.",
    icon: CheckCircle2,
  },
  {
    id: "gravacao",
    titulo: "2. Gravando lançamentos",
    descricao: "Registrando lançamentos e contas a pagar.",
    icon: Database,
  },
]

type BarraProgressoImportacaoProps =
  | {
      modo: "analise"
      progresso: number
      totalArquivos: number
      arquivoAtualIndex: number
    }
  | {
      modo: "confirmacao"
      progresso: number
      documentosConcluidos: number
      totalDocumentos: number
      documentoAtualNome?: string
    }

export function BarraProgressoImportacao(props: BarraProgressoImportacaoProps) {
  const pct = Math.min(100, Math.max(0, Math.round(props.progresso)))

  const etapas = props.modo === "analise" ? ETAPAS_ANALISE : ETAPAS_CONFIRMACAO

  // Define qual sub-etapa está ativa com base no percentual atual
  const indiceAtivo = React.useMemo(() => {
    if (pct >= 100) return etapas.length
    if (props.modo === "analise") {
      if (pct < 20) return 0
      if (pct < 60) return 1
      return 2
    }
    return pct < 15 ? 0 : 1
  }, [pct, props.modo, etapas.length])

  const tituloPrincipal =
    props.modo === "analise"
      ? `Processando ${props.arquivoAtualIndex} de ${props.totalArquivos} ${
          props.totalArquivos === 1 ? "arquivo" : "arquivos"
        }`
      : `Gravando documento ${Math.min(
          props.documentosConcluidos + 1,
          props.totalDocumentos,
        )} de ${props.totalDocumentos}`

  const subtitulo =
    props.modo === "analise"
      ? "Isso pode levar alguns segundos dependendo do tamanho da fatura."
      : props.documentoAtualNome
        ? `Salvando: ${props.documentoAtualNome}`
        : "Finalizando gravação dos lançamentos…"

  return (
    <div
      role="status"
      aria-live="polite"
      className="rounded-2xl border border-border/70 bg-card p-5 shadow-xs sm:p-6"
    >
      {/* Topo: Etapa + Percentual */}
      <div className="mb-3 flex items-start justify-between gap-4">
        <div className="min-w-0 space-y-1">
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-[11px] uppercase tracking-wider">
              {props.modo === "analise" ? "Análise em andamento" : "Gravação em andamento"}
            </Badge>
          </div>
          <p className="text-base font-semibold text-foreground">{tituloPrincipal}</p>
          <p className="truncate text-xs text-muted-foreground">{subtitulo}</p>
        </div>

        <span className="shrink-0 rounded-full border border-border bg-background px-3 py-1 text-sm font-semibold tabular-nums text-foreground">
          {pct}%
        </span>
      </div>

      {/* Barra principal */}
      <Progress value={pct} className="h-2.5 w-full" />

      {/* Sub-etapas */}
      <ol
        className={`mt-5 grid grid-cols-1 gap-3 ${
          etapas.length === 3 ? "md:grid-cols-3" : "md:grid-cols-2"
        }`}
      >
        {etapas.map((etapa, idx) => {
          const Icon = etapa.icon
          const concluida = idx < indiceAtivo
          const ativa = idx === indiceAtivo

          return (
            <li
              key={etapa.id}
              className={`flex items-start gap-3 rounded-xl border p-3 transition-colors ${
                ativa
                  ? "border-primary/50 bg-primary/5"
                  : concluida
                    ? "border-border/60 bg-background/60 opacity-85"
                    : "border-border/40 bg-background/30 opacity-50"
              }`}
            >
              <span
                className={`mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg ${
                  concluida
                    ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
                    : ativa
                      ? "bg-primary/20 text-primary"
                      : "bg-muted text-muted-foreground"
                }`}
              >
                {concluida ? (
                  <CheckCircle2 className="h-4 w-4" />
                ) : ativa ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Icon className="h-4 w-4" />
                )}
              </span>

              <div className="space-y-0.5">
                <p className="text-xs font-semibold text-foreground">{etapa.titulo}</p>
                <p className="text-[11px] leading-relaxed text-muted-foreground">
                  {etapa.descricao}
                </p>
              </div>
            </li>
          )
        })}
      </ol>
    </div>
  )
}