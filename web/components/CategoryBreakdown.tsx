"use client"

import * as React from "react"

import { Card } from "@/components/ui/card"
import { CardContent } from "@/components/ui/card"
import { CardHeader } from "@/components/ui/card"
import { CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Button } from "@/components/ui/button"
import { DonutChart } from "@/components/DonutChart"

const MONETARY = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
})

const PERCENTUAL = new Intl.NumberFormat("pt-BR", {
  style: "percent",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

type CategoriaGasto = {
  id: number
  nome: string
  total: string
}

type CategoryBreakdownProps = {
  categorias: CategoriaGasto[]
  totalSaidas: string
}

export function CategoryBreakdown({
  categorias,
  totalSaidas,
}: CategoryBreakdownProps) {
  const totalSaidasNum = Number(totalSaidas) || 0

  const ordenadas = [...categorias].sort(
    (a, b) => Number(b.total) - Number(a.total)
  )
  const top5 = ordenadas.slice(0, 5)
  const outrosTotal = ordenadas
    .slice(5)
    .reduce((sum, c) => sum + Number(c.total), 0)

  const hasData = totalSaidasNum > 0 && categorias.length > 0

  if (!hasData) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Gastos por categoria</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col items-center justify-center py-12 text-center">
          <p className="text-sm text-muted-foreground mb-4">
            Nenhuma saída neste mês
          </p>
          <Button variant="outline" asChild>
            <a href="/lancamentos">Adicionar lançamento</a>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const rows = top5.map((categoria) => {
    const percentual =
      totalSaidasNum > 0 ? Number(categoria.total) / totalSaidasNum : 0
    return {
      id: categoria.id,
      nome: categoria.nome,
      total: MONETARY.format(Number(categoria.total)),
      percentual: PERCENTUAL.format(percentual),
      progressValue: percentual * 100,
    }
  })

  const outrosRow = outrosTotal > 0
    ? {
        nome: "Outros",
        total: MONETARY.format(outrosTotal),
        percentual: PERCENTUAL.format(outrosTotal / totalSaidasNum),
        progressValue: (outrosTotal / totalSaidasNum) * 100,
      }
    : null

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>Gastos por categoria</CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="grid gap-6 md:flex md:flex-row md:items-start">
          <DonutChart categorias={categorias} totalSaidas={totalSaidas} />

          <div className="flex-1">
            <Card className="h-full">
              <CardHeader>
                <CardTitle className="text-sm font-semibold">Categorias</CardTitle>
              </CardHeader>
              <CardContent className="pt-0">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border">
                        <th className="text-left pb-2 text-muted-foreground">
                          Categoria
                        </th>
                        <th className="text-right pb-2 text-muted-foreground">
                          Valor
                        </th>
                        <th className="text-right pb-2 text-muted-foreground">
                          Percentual
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((row) => (
                        <tr key={row.id} className="border-b border-border/50">
                          <td className="py-3">{row.nome}</td>
                          <td className="py-3 text-right">
                            {row.total}
                          </td>
                          <td className="py-3 text-right">
                            <div className="flex items-center gap-2">
                              <Badge variant="secondary" className="w-[40px] text-right text-[10px]">
                                {row.percentual}
                              </Badge>
                              <Progress
                                value={row.progressValue}
                                className="h-1 w-full"
                                style={{ transform: "none" }}
                              />
                            </div>
                          </td>
                        </tr>
                      ))}
                      {outrosRow && (
                        <tr key="outros" className="border-b border-border/50">
                          <td className="py-3">Outros</td>
                          <td className="py-3 text-right">
                            {outrosRow.total}
                          </td>
                          <td className="py-3 text-right">
                            <div className="flex items-center gap-2">
                              <Badge variant="secondary" className="w-[40px] text-right text-[10px]">
                                {outrosRow.percentual}
                              </Badge>
                              <Progress
                                value={outrosRow.progressValue}
                                className="h-1 w-full"
                                style={{ transform: "none" }}
                              />
                            </div>
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
