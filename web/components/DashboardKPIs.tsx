"use client";

import { Card } from "@/components/ui/card";
import { CardContent } from "@/components/ui/card";
import { CardDescription } from "@/components/ui/card";
import { CardFooter } from "@/components/ui/card";
import { CardHeader } from "@/components/ui/card";
import { CardTitle } from "@/components/ui/card";

const MONETARY = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

/** Formata a variação de um KPI.
 *
 * - Quando percentual === "—" (base do mês anterior zero): exibe apenas o
 *   valor absoluto, ex: "R$ 120,00".
 * - Caso contrário: exibe sinal +/-, valor absoluto e percentual, ex:
 *   "+R$ 120,00 (12,50%)" ou "-R$ 40,00 (-8,33%)".
 * Nunca exibe "NaN%" nem "Infinity%".
 */
export function formatarVariacao(absoluto: number, percentual: string): string {
  const abs = MONETARY.format(absoluto);
  if (percentual === "—") {
    return abs;
  }
  const sinal = absoluto >= 0 ? "+" : "-";
  return `${sinal}${abs} (${percentual}%)`;
}

/** Cor do valor do Saldo conforme o sinal. */
export function corSaldo(valor: number): string {
  if (valor > 0) return "text-green-600";
  if (valor < 0) return "text-red-600";
  return "text-muted-foreground";
}

/** Cor de uma variação conforme o sinal (verde/vermelho). */
export function corVariacao(valor: number): string {
  if (valor > 0) return "text-green-600";
  if (valor < 0) return "text-red-600";
  return "text-muted-foreground";
}

type DashboardKPIsProps = {
  totalEntradas: number;
  totalSaidas: number;
  saldo: number;
  variacaoEntradas: number;
  variacaoSaidas: number;
  variacaoSaldo: number;
  percentualEntradas: string;
  percentualSaidas: string;
  percentualSaldo: string;
};

export function DashboardKPIs({
  totalEntradas,
  totalSaidas,
  saldo,
  variacaoEntradas,
  variacaoSaidas,
  variacaoSaldo,
  percentualEntradas,
  percentualSaidas,
  percentualSaldo,
}: DashboardKPIsProps) {
  return (
    <section className="w-full max-w-4xl grid grid-cols-1 gap-4 sm:grid-cols-3">
      <Card className="border-l-4 border-l-green-600">
        <CardHeader>
          <CardTitle className="text-green-600">Entradas</CardTitle>
          <CardDescription className="text-lg font-medium text-foreground">
            {MONETARY.format(totalEntradas)}
          </CardDescription>
        </CardHeader>
        <CardFooter>
          {formatarVariacao(variacaoEntradas, percentualEntradas)}
        </CardFooter>
      </Card>

      <Card className="border-l-4 border-l-red-600">
        <CardHeader>
          <CardTitle className="text-red-600">Saídas</CardTitle>
          <CardDescription className="text-lg font-medium text-foreground">
            {MONETARY.format(totalSaidas)}
          </CardDescription>
        </CardHeader>
        <CardFooter>
          {formatarVariacao(variacaoSaidas, percentualSaidas)}
        </CardFooter>
      </Card>

      <Card className="border-l-4">
        <CardHeader>
          <CardTitle className={corSaldo(saldo)}>Saldo</CardTitle>
          <CardDescription className={`text-lg font-medium text-foreground ${corSaldo(saldo)}`}>
            {MONETARY.format(saldo)}
          </CardDescription>
        </CardHeader>
        <CardFooter>
          <span className={corVariacao(variacaoSaldo)}>
            {formatarVariacao(variacaoSaldo, percentualSaldo)}
          </span>
        </CardFooter>
      </Card>
    </section>
  );
}
