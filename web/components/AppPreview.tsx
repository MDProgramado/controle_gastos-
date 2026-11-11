"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

type AppPreviewProps = {
  className?: string
}

export function AppPreview({ className }: AppPreviewProps) {
  return (
    <div
      data-edge-free
      data-enter-media
      className={cn(
        "relative group overflow-hidden rounded-2xl bg-card border border-border shadow-lg",
        className
      )}
    >
      {/* Barra de título da janela */}
      <div className="flex items-center gap-2 px-4 py-3 bg-muted/50 border-b border-border/50">
        <div className="flex gap-1.5">
          <span className="h-3 w-3 rounded-full bg-red-500" />
          <span className="h-3 w-3 rounded-full bg-yellow-500" />
          <span className="h-3 w-3 rounded-full bg-green-500" />
        </div>
        <span className="ml-2 text-xs text-muted-foreground font-medium">
          Controle Financeiro
        </span>
      </div>

      {/* Conteúdo do app dentro do frame */}
      <div className="p-6 sm:p-8 h-[400px] sm:h-[460px] overflow-hidden pb-4">
        <div className="flex flex-col h-full gap-4">
          {/* Header do app */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <svg className="h-6 w-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              <span className="font-semibold text-foreground">Controle Financeiro</span>
            </div>
            <div className="flex gap-2">
              <span className="h-8 px-3 text-sm font-medium text-muted-foreground hover:bg-muted/50 rounded-lg">
                Novo
              </span>
              <span className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary hover:bg-primary/20">
                <span className="text-[14px]">⋮</span>
              </span>
            </div>
          </div>

          {/* Saldo em destaque */}
          <div className="bg-primary/5 rounded-xl p-6 border border-primary/10">
            <div className="flex items-baseline gap-2">
              <span className="text-xl font-bold text-primary">R$ 12.840,50</span>
              <span className="ml-1 text-sm text-primary-foreground font-medium">
                Saldo disponível
              </span>
            </div>
            <p className="mt-2 text-sm text-muted-foreground">
              Atualizado em 10/11/2026 • Últimos 30 dias
            </p>
          </div>

          {/* Grid de resumo */}
          <div className="grid grid-cols-2 gap-4 mb-6">
            <div className="rounded-lg p-4 bg-muted/50 border border-border/50">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-muted-foreground">Entradas</p>
                  <p className="font-bold text-positive">R$ 8.500,00</p>
                </div>
                <button
                  type="button"
                  className="h-6 w-6 rounded-full bg-positive/10 flex items-center justify-center text-positive text-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  aria-label="Ver entradas"
                >
                  +
                </button>
              </div>
            </div>
            <div className="rounded-lg p-4 bg-muted/50 border border-border/50">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-muted-foreground">Saídas</p>
                  <p className="font-bold text-destructive">R$ 4.340,50</p>
                </div>
                <button
                  type="button"
                  className="h-6 w-6 rounded-full bg-destructive/10 flex items-center justify-center text-destructive text-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  aria-label="Ver saídas"
                >
                  −
                </button>
              </div>
            </div>
          </div>

          {/* Mini tabela de lançamentos recentes */}
          <div className="rounded-lg border border-border/50 overflow-hidden mb-4">
            <table className="w-full text-sm text-left">
              <thead>
                <tr className="bg-muted/30 border-b border-border/50">
                  <th className="px-3 py-2 text-xs font-semibold text-muted-foreground">
                    Data
                  </th>
                  <th className="px-3 py-2 text-xs font-semibold text-muted-foreground">
                    Descrição
                  </th>
                  <th className="px-3 py-2 text-xs font-semibold text-muted-foreground text-right">
                    Valor
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/50">
                <tr className="hover:bg-muted/50 transition-colors">
                  <td className="px-3 py-2 text-sm">10/11</td>
                  <td className="px-3 py-2 text-sm">Recebimento cliente</td>
                  <td className="px-3 py-2 text-sm text-right font-semibold text-positive">
                    + R$ 3.200,00
                  </td>
                </tr>
                <tr className="hover:bg-muted/50 transition-colors">
                  <td className="px-3 py-2 text-sm">09/11</td>
                  <td className="px-3 py-2 text-sm">Compra material escritório</td>
                  <td className="px-3 py-2 text-sm text-right font-semibold text-destructive">
                    − R$ 450,00
                  </td>
                </tr>
                <tr className="hover:bg-muted/50 transition-colors">
                  <td className="px-3 py-2 text-sm">08/11</td>
                  <td className="px-3 py-2 text-sm">Serviço prestado</td>
                  <td className="px-3 py-2 text-sm text-right font-semibold text-positive">
                    + R$ 1.800,00
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* Ação rápida */}
          <div className="mt-10 flex items-center justify-between pb-8">
            <span className="text-sm text-muted-foreground">
              Ações rápidas
            </span>
            <div className="flex gap-2">
              <span className="h-8 px-3 text-sm font-medium text-muted-foreground hover:bg-muted/50 rounded-lg">
                + Lançamento
              </span>
              <span className="h-8 px-3 text-sm font-medium text-muted-foreground hover:bg-muted/50 rounded-lg">
                Relatório
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Overlay sutil para indicar que é um preview */}
      <div className="absolute inset-0 pointer-events-none rounded-2xl border-t border-white/20" />
    </div>
  )
}
