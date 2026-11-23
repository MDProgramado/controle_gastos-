"use client"

import * as React from "react"
import { Pie, PieChart, Cell, ResponsiveContainer } from "recharts"

import { Chart } from "@/components/ui/chart"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  agruparCategorias,
  formatarBRL,
  valoresParaGrafico,
  type CategoriaTotal,
} from "@/lib/format"
import { centavosAssinadosDeString, type ValorMonetario } from "@/lib/money"

const COLORS = [
  "var(--chart-1)",
  "var(--chart-2)",
  "var(--chart-3)",
  "var(--chart-4)",
  "var(--chart-5)",
]

const ZERO = BigInt(0)

type CategoriaGasto = CategoriaTotal

type DonutChartProps = {
  categorias: CategoriaGasto[]
  totalSaidas: ValorMonetario
}

export function DonutChart({
  categorias,
  totalSaidas,
}: DonutChartProps) {
  const totalSaidasCentavos =
    centavosAssinadosDeString(totalSaidas) ?? ZERO
  const agrupadas = agruparCategorias(categorias)

  const pieData = valoresParaGrafico(agrupadas, COLORS)

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">Por categoria</CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="flex flex-col items-center justify-center py-4">
          {/* Altura fixa: o grafico nao muda de tamanho com o conteudo. */}
          <div className="relative h-65 w-full md:h-80">
            <Chart className="h-full w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {pieData.map((c, i) => (
                      <Cell key={i} fill={c.fill} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            </Chart>
          </div>
          <div className="flex flex-col items-center justify-center pt-4 text-center">
            <span className="text-xl font-bold text-foreground">
              {formatarBRL(totalSaidasCentavos)}
            </span>
            <span className="text-xs text-muted-foreground">
              Total de saídas
            </span>
          </div>
        </div>
        {/* Legenda abaixo do grafico (nunca sobreposta). */}
        <ul className="flex w-full flex-wrap gap-x-4 gap-y-2 pt-4 text-xs text-muted-foreground">
          {pieData.map((c, i) => (
            <li key={`${c.name}-${i}`} className="flex items-center gap-1.5">
              <span
                className="h-2.5 w-2.5 shrink-0 rounded-sm"
                style={{ backgroundColor: c.fill }}
                aria-hidden="true"
              />
              <span className="truncate">{c.name}</span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
