"use client"

import * as React from "react"

import { Card } from "@/components/ui/card"
import { CardContent } from "@/components/ui/card"
import { CardHeader } from "@/components/ui/card"
import { CardTitle } from "@/components/ui/card"

export type MonthlyData = {
  mes: string
  entradas: number
  saidas: number
}

type MonthlyComparisonProps = {
  meses: MonthlyData[]
}

const MONETARY = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
})

/**
 * Comparação dos últimos 6 meses com dados REAIS: `app/page.tsx` busca
 * `getDashboard` para cada mês e repassa aqui (o backend não expõe um
 * endpoint de comparação mensal — ver SUMMARY/TODO do backend).
 */
export function MonthlyComparison({ meses }: MonthlyComparisonProps) {
  const max = React.useMemo(
    () => Math.max(1, ...meses.flatMap((m) => [m.entradas, m.saidas])),
    [meses],
  )

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>Comparação mensal</CardTitle>
        <p className="text-sm text-muted-foreground">
          Entradas e saídas dos últimos 6 meses
        </p>
      </CardHeader>
      <CardContent>
        <div className="flex items-center gap-4 pb-4 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 bg-positive" aria-hidden="true" />
            Entradas
          </span>
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 bg-destructive" aria-hidden="true" />
            Saídas
          </span>
        </div>
        <div
          className="grid h-[260px] grid-cols-6 gap-2 sm:gap-3"
          role="img"
          aria-label={meses
            .map(
              (m) =>
                `${m.mes}: entradas ${MONETARY.format(m.entradas)}, saídas ${MONETARY.format(m.saidas)}`,
            )
            .join("; ")}
        >
          {meses.map((m) => (
            <div
              key={m.mes}
              className="flex h-full flex-col justify-end gap-1"
              title={`${m.mes} — entradas ${MONETARY.format(m.entradas)} / saídas ${MONETARY.format(m.saidas)}`}
            >
              <div className="flex h-full items-end justify-center gap-1">
                <div
                  className="w-1/2 max-w-[18px] bg-positive transition-[height] duration-500"
                  style={{ height: `${Math.max(2, (m.entradas / max) * 100)}%` }}
                />
                <div
                  className="w-1/2 max-w-[18px] bg-destructive transition-[height] duration-500"
                  style={{ height: `${Math.max(2, (m.saidas / max) * 100)}%` }}
                />
              </div>
              <span className="text-center text-[11px] font-medium text-muted-foreground">
                {m.mes}
              </span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

/** Formata BRL fora do componente (útil para quem monta os dados). */
export function formatBRL(value: number): string {
  return MONETARY.format(value)
}
