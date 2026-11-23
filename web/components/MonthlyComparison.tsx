import { Card } from "@/components/ui/card"
import { CardContent } from "@/components/ui/card"
import { CardHeader } from "@/components/ui/card"
import { CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { formatarBRL } from "@/lib/format"

export type MonthlyData = {
  mes: string
  entradas: bigint
  saidas: bigint
}

type MonthlyComparisonProps = {
  meses: MonthlyData[]
}

const ZERO = BigInt(0)
const UM = BigInt(1)
const CEM = BigInt(100)

/**
 * Comparação dos últimos 6 meses com dados REAIS: `app/page.tsx` busca
 * `getDashboard` para cada mês e repassa aqui (o backend não expõe um
 * endpoint de comparação mensal — ver SUMMARY/TODO do backend).
 */
export function MonthlyComparison({ meses }: MonthlyComparisonProps) {
  const max = meses
    .flatMap((m) => [m.entradas, m.saidas])
    .reduce((maior, valor) => (valor > maior ? valor : maior), UM)

  /**
   * Sem lançamentos no período: as barras ficariam com 2% de altura (o mínimo
   * do estilo inline) e o gráfico mostraria 6 tracinhos que não comunicam
   * nada. Nesse caso o card mostra o estado vazio — como o "Gastos por
   * categoria" faz quando não há saídas no mês.
   */
  const semMovimento = meses.every((m) => m.entradas === ZERO && m.saidas === ZERO)

  if (semMovimento) {
    return (
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Comparação mensal</CardTitle>
          <p className="text-sm text-muted-foreground">
            Entradas e saídas dos últimos 6 meses
          </p>
        </CardHeader>
        <CardContent className="flex flex-col items-center justify-center py-12 text-center">
          <p className="text-sm text-muted-foreground mb-4">
            Nenhum lançamento nos últimos 6 meses
          </p>
          <Button variant="outline" asChild>
            <a href="/lancamentos">Adicionar lançamento</a>
          </Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>Comparação mensal</CardTitle>
        <p className="text-sm text-muted-foreground">
          Entradas e saídas dos últimos 6 meses
        </p>
      </CardHeader>
      <CardContent>
        <div
          className="grid h-64 grid-cols-6 gap-2 md:h-80 sm:gap-1"
          role="img"
          aria-label={meses
            .map(
              (m) =>
                `${m.mes}: entradas ${formatarBRL(m.entradas)}, saídas ${formatarBRL(m.saidas)}`,
            )
            .join("; ")}
        >
          {meses.map((m) => (
            <div
              key={m.mes}
              className="flex h-full flex-col justify-end gap-1"
              title={`${m.mes} — entradas ${formatarBRL(m.entradas)} / saídas ${formatarBRL(m.saidas)}`}
            >
              <div className="flex h-full items-end justify-center gap-1">
                {/* Mês sem valor não desenha barra: um traço de 2% seria lido
                    como "teve movimento". Zero é zero. */}
                {m.entradas > ZERO && (
                  <div
                    className="w-1/2 max-w-4 bg-positive transition-[height] duration-500"
                    style={{ height: `${Number((m.entradas * CEM) / max)}%` }}
                  />
                )}
                {m.saidas > ZERO && (
                  <div
                    className="w-1/2 max-w-4 bg-destructive transition-[height] duration-500"
                    style={{ height: `${Number((m.saidas * CEM) / max)}%` }}
                  />
                )}
              </div>
              <span className="text-center text-[11px] font-medium text-muted-foreground">
                {m.mes}
              </span>
            </div>
          ))}
        </div>
        {/* Legenda abaixo do grafico (nunca sobreposta). */}
        <ul className="flex flex-wrap items-center gap-x-4 gap-y-2 pt-4 text-xs text-muted-foreground">
          <li className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 shrink-0 bg-positive" aria-hidden="true" />
            Entradas
          </li>
          <li className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 shrink-0 bg-destructive" aria-hidden="true" />
            Saídas
          </li>
        </ul>
      </CardContent>
    </Card>
  )
}
