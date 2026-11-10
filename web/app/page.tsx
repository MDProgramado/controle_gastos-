import { redirect } from "next/navigation";
import { getMe } from "@/lib/api";
import { getDashboard } from "@/lib/api";
import { Sidebar } from "@/components/sidebar";
import { Header } from "@/components/header";
import { MonthSelector } from "@/components/MonthSelector";
import { DashboardKPIs } from "@/components/DashboardKPIs";
import { CategoryBreakdown } from "@/components/CategoryBreakdown";

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
  const [anoStr, mesStr] = (
    mesSelecionado ?? new Date().toISOString().slice(0, 7)
  ).split("-");
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
    <div className="flex h-screen w-full overflow-hidden bg-background">
      <Sidebar />
      <div className="flex flex-col h-full w-full overflow-hidden">
        <Header title="Dashboard" />
        <main className="flex-1 overflow-y-auto bg-background px-4 py-6 md:px-24">
          <div className="max-w-[1280px] mx-auto">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
              <div>
                <h1 className="text-[28px] leading-[1.2] font-semibold text-foreground">
                  Dashboard
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                  Entradas, saídas e saldo do mês selecionado
                </p>
              </div>
              <MonthSelector />
            </div>

            <DashboardKPIs
              totalEntradas={Number(dados.totalEntradas)}
              totalSaidas={Number(dados.totalSaidas)}
              saldo={Number(dados.saldo)}
              variacaoEntradas={Number(dados.variacaoEntradas)}
              variacaoSaidas={Number(dados.variacaoSaidas)}
              variacaoSaldo={Number(dados.variacaoSaldo)}
              percentualEntradas={dados.percentualEntradas}
              percentualSaidas={dados.percentualSaidas}
              percentualSaldo={dados.percentualSaldo}
            />

            <CategoryBreakdown
              categorias={dados.categorias}
              totalSaidas={dados.totalSaidas}
            />

            {dados.categorias.length === 0 && (
              <p className="mt-4 text-sm text-muted-foreground">
                Nenhum lançamento neste mês
              </p>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
