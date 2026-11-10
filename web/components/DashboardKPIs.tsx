"use client"

import { Card } from "@/components/ui/card"
import { CardContent } from "@/components/ui/card"
import { CardHeader } from "@/components/ui/card"
import { CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"

const MONETARY = new Intl.NumberFormat("pt-BR", {
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

  return (
    <section className="w-full grid gap-4 sm:grid-cols-3">
      {/* Saldo - primeiro card, em destaque */}
      <Card className="col-span-1 sm:col-span-3">
        <CardHeader className="flex flex-row items-center justify-between py-4">
          <CardTitle className="text-sm font-semibold text-muted-foreground">
            Saldo
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-0">
          <div className="flex items-baseline gap-2">
            <span className={`text-4xl font-bold ${saldoPositivo ? "text-positive" : "text-destructive"}`}>
              {MONETARY.format(saldo)}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Saldo do mês anterior: {MONETARY.format(saldoMesAnterior)}
          </p>
        </CardContent>
      </Card>

      {/* Entradas */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-sm font-semibold text-muted-foreground">
            Entradas
          </CardTitle>
          <Badge
            variant="success"
            className={`text-xs font-semibold ${
              variacaoEntradas >= 0 ? "text-positive bg-positive/10" : "text-destructive bg-destructive/10"
            }`}
          >
            {variacaoEntradas >= 0 ? "▲" : "▼"} {Math.abs(
              Number(percentualEntradas.replace(/[^0-9,-]/g, "").replace(",", ".")) || 0
            )}%
          </Badge>
        </CardHeader>
        <CardContent className="pt-0">
          <div className="flex items-baseline gap-1">
            <span className="text-2xl font-bold text-positive">
              {MONETARY.format(totalEntradas)}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Mês anterior: {MONETARY.format(totalEntradasMesAnterior)}
          </p>
        </CardContent>
      </Card>

      {/* Saídas */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-sm font-semibold text-muted-foreground">
            Saídas
          </CardTitle>
          <Badge
            variant="destructive"
            className={`text-xs font-semibold ${
              variacaoSaidas <= 0 ? "text-positive bg-positive/10" : "text-destructive bg-destructive/10"
            }`}
          >
            {variacaoSaidas <= 0 ? "▼" : "▲"} {Math.abs(
              Number(percentualSaidas.replace(/[^0-9,-]/g, "").replace(",", ".")) || 0
            )}%
          </Badge>
        </CardHeader>
        <CardContent className="pt-0">
          <div className="flex items-baseline gap-1">
            <span className="text-2xl font-bold text-destructive">
              {MONETARY.format(totalSaidas)}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Mês anterior: {MONETARY.format(totalSaidasMesAnterior)}
          </p>
        </CardContent>
      </Card>
    </section>
  )
}
