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

const COLORS = [
  "var(--chart-1)",
  "var(--chart-2)",
  "var(--chart-3)",
  "var(--chart-4)",
  "var(--chart-5)",
]

const MONETARY = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
})

type CategoriaGasto = {
  id: number
  nome: string
  total: string
}

type DonutChartProps = {
  categorias: CategoriaGasto[]
  totalSaidas: string
}

export function DonutChart({
  categorias,
  totalSaidas,
}: DonutChartProps) {
  const totalSaidasNum = Number(totalSaidas) || 0
  const ordenadas = [...categorias].sort(
    (a, b) => Number(b.total) - Number(a.total)
  )
  const top5 = ordenadas.slice(0, 5)
  const outrosTotal = ordenadas
    .slice(5)
    .reduce((sum, c) => sum + Number(c.total), 0)

  const pieData = [
    ...top5.map((c, i) => ({
      name: c.nome,
      value: Number(c.total),
      fill: COLORS[i % COLORS.length],
    })),
    ...(outrosTotal > 0
      ? [{ name: "Outros", value: outrosTotal, fill: "var(--muted-foreground)" }]
      : []),
  ]

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="text-sm font-semibold">Por categoria</CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="flex flex-col items-center justify-center py-4">
          <div className="relative w-48 h-48">
            <Chart className="w-full h-full">
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
              {MONETARY.format(totalSaidasNum)}
            </span>
            <span className="text-xs text-muted-foreground">
              Total de saídas
            </span>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
