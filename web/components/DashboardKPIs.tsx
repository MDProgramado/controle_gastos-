"use client"

import type { ReactNode } from "react"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

export const MONETARY = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
})

type DashboardKPIsProps = {
  totalEntradas: number
  totalSaidas: number
  saldo: number
  variacaoEntradas: number
  variacaoSaidas: number
  percentualEntradas: string
  percentualSaidas: string
  totalEntradasMesAnterior: number
  totalSaidasMesAnterior: number
  saldoMesAnterior: number
}

function percentualNum(p: string): number {
  return (
    Math.abs(Number(p.replace(/[^0-9,-]/g, "").replace(",", "."))) || 0
  )
}

/**
 * Linha de números do dashboard: stats editoriais (borda em cima, sem card) —
 * também é o momento `counter-rolls` da página (os dígitos rolam uma vez quando
 * a linha entra na tela; sem script os números ficam como escritos).
 */
export function DashboardKPIs({
  totalEntradas,
  totalSaidas,
  saldo,
  variacaoEntradas,
  variacaoSaidas,
  percentualEntradas,
  percentualSaidas,
  totalEntradasMesAnterior,
  totalSaidasMesAnterior,
  saldoMesAnterior,
}: DashboardKPIsProps) {
  const saldoPositivo = saldo >= 0

  const stats = [
    {
      label: "Saldo",
      valor: saldo,
      classe: saldoPositivo ? "text-positive" : "text-destructive",
      badge: null as ReactNode,
      legenda: `Saldo do mês anterior: ${MONETARY.format(saldoMesAnterior)}`,
      destaque: true,
    },
    {
      label: "Entradas",
      valor: totalEntradas,
      classe: "text-positive",
      destaque: false,
      badge: (
        <Badge
          variant="success"
          className={`text-xs font-semibold ${
            variacaoEntradas >= 0
              ? "text-positive bg-positive/10"
              : "text-destructive bg-destructive/10"
          }`}
        >
          {variacaoEntradas >= 0 ? "▲" : "▼"} {percentualNum(percentualEntradas)}%
        </Badge>
      ),
      legenda: `Mês anterior: ${MONETARY.format(totalEntradasMesAnterior)}`,
    },
    {
      label: "Saídas",
      valor: totalSaidas,
      classe: "text-destructive",
      badge: (
        <Badge
          variant="destructive"
          className={`text-xs font-semibold ${
            variacaoSaidas <= 0
              ? "text-positive bg-positive/10"
              : "text-destructive bg-destructive/10"
          }`}
        >
          {variacaoSaidas <= 0 ? "▼" : "▲"} {percentualNum(percentualSaidas)}%
        </Badge>
      ),
      legenda: `Mês anterior: ${MONETARY.format(totalSaidasMesAnterior)}`,
      destaque: false,
    },
  ]

  return (
    <div
      className="grid w-full grid-cols-1 gap-x-6 gap-y-8 md:grid-cols-3"
      data-moment="counter-rolls"
    >
      {stats.map((s) => (
        <div
          key={s.label}
          className={cn(
            "flex flex-col border-t border-border pt-4",
            // O Saldo vem em destaque no mobile: e o numero que o usuario
            // procura primeiro (1 coluna).
            s.destaque && "md:order-none",
          )}
        >
          <div className="flex items-center justify-between gap-2">
            <span
              className={cn(
                "font-medium text-muted-foreground",
                s.destaque ? "text-base text-foreground" : "text-sm",
              )}
            >
              {s.label}
            </span>
            {s.badge}
          </div>
          <p
            className={cn(
              "t-stat mt-3 font-semibold tracking-tight tabular-nums",
              s.destaque ? "text-3xl" : "text-xl",
              s.classe,
            )}
          >
            {MONETARY.format(s.valor)}
          </p>
          <p className="mt-1.5 text-sm text-muted-foreground">{s.legenda}</p>
        </div>
      ))}
    </div>
  )
}
