"use client"

import * as React from "react"
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  XAxis,
  YAxis,
  Tooltip,
} from "recharts"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

type MonthlyData = {
  mes: string
  entradas: number
  saidas: number
}

/**
 * Hook temporário com dados mock para comparação mensal dos últimos 6 meses.
 * TODO: Substituir por chamada real ao backend quando o endpoint
 * GET /api/dashboard/comparacao-mensal estiver disponível.
 */
export function useMonthlyComparison(ano: number, mes: number) {
  const dataInicial = new Date(ano, mes - 1, 1)
  const meses: MonthlyData[] = []

  for (let i = 5; i >= 0; i--) {
    const d = new Date(dataInicial.getFullYear(), dataInicial.getMonth() - i, 1)
    const nomeMes = d.toLocaleString("pt-BR", { month: "short" }).toUpperCase()
    // Dados mock — substituir por chamada real ao backend
    meses.push({
      mes: nomeMes,
      entradas: Math.round(Math.random() * 5000) + 1000,
      saidas: Math.round(Math.random() * 4000) + 500,
    })
  }

  return meses
}

export function MonthlyComparison() {
  // Dados mock — substituir por dados reais do backend quando disponível
  const meses = useMonthlyComparison(new Date().getFullYear(), new Date().getMonth() + 1)

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-semibold">Comparação mensal</CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="h-[300px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={meses}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="mes" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="entradas" fill="#4C956C" radius={[4, 4, 0, 0]} />
              <Bar dataKey="saidas" fill="#E5484D" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  )
}
