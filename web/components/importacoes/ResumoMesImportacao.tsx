"use client"

import { Badge } from "@/components/ui/badge"
import { MONETARY } from "@/components/DashboardKPIs"
import { cn } from "@/lib/utils"
import { formatarDataIso } from "@/components/importacoes/ImportacaoTipos"

/**
 * Stat editorial do resumo do mês (05-UI-SPEC §6.2): mesmo padrão de
 * `DashboardKPIs` (borda superior, sem card), 4 colunas no md+. Os valores
 * chegam JÁ recalculados a partir dos itens marcados (soma em centavos
 * inteiros — regra do centavo).
 */
type ResumoMesImportacaoProps = {
  /** Números em reais (centavos/100 no chamador). */
  totalRecebido: number
  totalPago: number
  totalComprometido: number
  saldoPrevisto: number
  fatura: {
    /** null quando o lote não tem fatura de cartão (linha não aparece). */
    total: number | null
    /** ISO yyyy-MM-dd. */
    dataVencimento: string | null
    /** |diferença| em reais quando a soma marcada não bate com a fatura (> R$ 0,05). */
    divergencia: number | null
  }
}

const STATS = [
  {
    label: "Total recebido",
    legenda: "Receitas identificadas no período",
    chave: "recebido" as const,
    cor: "text-positive",
  },
  {
    label: "Total já pago",
    legenda: "Despesas já quitadas",
    chave: "pago" as const,
    cor: "text-destructive",
  },
  {
    label: "Total comprometido a pagar",
    legenda: "Contas a pagar em aberto",
    chave: "comprometido" as const,
    cor: "text-foreground",
  },
  {
    label: "Saldo previsto",
    legenda: "Receitas menos despesas pagas",
    chave: "saldo" as const,
    cor: "text-positive",
  },
]

export function ResumoMesImportacao({
  totalRecebido,
  totalPago,
  totalComprometido,
  saldoPrevisto,
  fatura,
}: ResumoMesImportacaoProps) {
  const valores: Record<"recebido" | "pago" | "comprometido" | "saldo", number> = {
    recebido: totalRecebido,
    pago: totalPago,
    comprometido: totalComprometido,
    saldo: saldoPrevisto,
  }

  return (
    <div>
      <div className="grid w-full grid-cols-1 gap-x-6 gap-y-8 md:grid-cols-4">
        {STATS.map((stat) => {
          const valor = valores[stat.chave]
          // Saldo previsto muda de cor: positivo / negativo / zero.
          const cor =
            stat.chave === "saldo"
              ? valor > 0
                ? "text-positive"
                : valor < 0
                  ? "text-destructive"
                  : "text-muted-foreground"
              : stat.cor
          return (
            <div key={stat.label} className="flex flex-col border-t border-border pt-4">
              <span className="text-sm font-medium text-muted-foreground">
                {stat.label}
              </span>
              <p className={cn("mt-2 text-xl font-semibold tabular-nums", cor)}>
                {MONETARY.format(valor)}
              </p>
              <span className="mt-1 text-sm text-muted-foreground">{stat.legenda}</span>
            </div>
          )
        })}
      </div>

      {fatura.total !== null && (
        <div className="mt-6 flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2 border-t border-border pt-4">
          <div>
            <span className="text-sm font-medium text-muted-foreground">
              Total da fatura
            </span>
            <p className="mt-2 text-xl font-semibold tabular-nums text-foreground">
              {MONETARY.format(fatura.total)}
            </p>
          </div>
          <div className="flex items-center gap-3">
            {fatura.dataVencimento && (
              <span className="text-sm text-muted-foreground">
                Vencimento: {formatarDataIso(fatura.dataVencimento)}
              </span>
            )}
            {fatura.divergencia !== null && (
              <Badge variant="warning">
                Divergência de {MONETARY.format(fatura.divergencia)}
              </Badge>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
