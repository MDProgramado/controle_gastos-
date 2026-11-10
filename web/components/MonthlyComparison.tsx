"use client"

import * as React from "react"

type MonthlyData = {
  mes: string
  entradas: number
  saidas: number
}

/**
 * Hook temporário com dados mock para comparação mensal dos últimos 6 meses.
 * TODO: Substituir por chamada real ao backend quando o endpoint
 * GET /api/dashboard/comparacao-mensal estiver disponível.
 * TODO: Remover dados mock e implementar busca real de comparacao mensal.
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
      entradas: 0,
      saidas: 0,
    })
  }

  return meses
}

export function MonthlyComparison() {
  // TODO: Substituir por dados reais do backend quando disponível
  const meses = useMonthlyComparison(new Date().getFullYear(), new Date().getMonth() + 1)

  return (
    <div>
      <h2 className="text-sm font-semibold">Comparação mensal</h2>
      <div className="h-[300px] w-full">
        {/* TODO: Implementar gráfico de barras com dados reais */}
        <div className="flex h-full items-center justify-center text-muted-foreground">
          Comparação mensal — aguardando dados do backend
        </div>
      </div>
    </div>
  )
}
