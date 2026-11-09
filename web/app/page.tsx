import { redirect } from "next/navigation";
import { getMe } from "@/lib/api";
import { getDashboard } from "@/lib/api";
import { LogoutButton } from "@/components/LogoutButton";
import { MonthSelector } from "@/components/MonthSelector";
import { DashboardKPIs } from "@/components/DashboardKPIs";

// Cache Components (Next 16): a sessão é lida por request (cookies) — a rota
// é dinâmica por natureza e bloqueia no servidor (docs: "Allow blocking route").
export const instant = false;

/**
 * Página dashboard (Server Component): valida a sessão via `getMe()`, busca os
 * dados agregados de `GET /api/dashboard` e renderiza os KPIs + seletor de mês.
 * Sem sessão → redirect /login?expirada=1.
 */
export default async function Home({
  searchParams,
}: {
  searchParams: { [key: string]: string | string[] | undefined };
}) {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  // Mês selecionado via ?mes=YYYY-MM; padrão = mês corrente.
  const mesSelecionado = Array.isArray(searchParams.mes)
    ? searchParams.mes[0]
    : searchParams.mes;
  const [anoStr, mesStr] = (mesSelecionado ?? new Date().toISOString().slice(0, 7)).split("-");
  const ano = parseInt(anoStr, 10);
  const mes = parseInt(mesStr, 10);

  const dados = await getDashboard(ano, mes);

  // Sem dados (sessão expirada na chamada ao dashboard): trata como não
  // autenticado → redirect. Mês sem lançamentos retorna zeros do backend,
  // então dados não será null aqui — apenas totais zerados.
  if (!dados) {
    redirect("/login?expirada=1");
  }

  return (
    <main className="flex min-h-dvh w-full flex-col items-center justify-center px-4 py-12">
      <div className="w-full max-w-4xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
          <div>
            <h1 className="text-[20px] leading-[1.2] font-semibold text-foreground">
              Dashboard
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Entradas, saídas e saldo do mês selecionado
            </p>
          </div>
          <MonthSelector />
        </div>

        <DashboardKPIs
          totalEntradas={dados.totalEntradas}
          totalSaidas={dados.totalSaidas}
          saldo={dados.saldo}
          variacaoEntradas={dados.variacaoEntradas}
          variacaoSaidas={dados.variacaoSaidas}
          variacaoSaldo={dados.variacaoSaldo}
          percentualEntradas={dados.percentualEntradas}
          percentualSaidas={dados.percentualSaidas}
          percentualSaldo={dados.percentualSaldo}
        />

        <div className="mt-8 flex items-center gap-4">
          <LogoutButton />
        </div>
      </div>
    </main>
  );
}
