import { redirect } from "next/navigation";
import { getMe } from "@/lib/api";
import { getDashboard } from "@/lib/api";
import { Sidebar } from "@/components/ui/sidebar";
import { SidebarContent } from "@/components/ui/sidebar";
import { SidebarInset } from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { MonthSelector } from "@/components/MonthSelector";
import { DashboardKPIs } from "@/components/DashboardKPIs";
import { CategoryBreakdown } from "@/components/CategoryBreakdown";
import { MonthlyComparison } from "@/components/MonthlyComparison";
import { ChatPanel } from "@/components/ai/chat-panel";

export const instant = false;

export default async function Home({
  searchParams,
}: {
  searchParams: { [key: string]: string | string[] | undefined };
}) {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  const sp = await searchParams;

  const mesSelecionado = Array.isArray(sp.mes) ? sp.mes[0] : sp.mes;
  const [anoStr, mesStr] = (
    mesSelecionado ?? new Date().toISOString().slice(0, 7)
  ).split("-");
  const ano = parseInt(anoStr, 10);
  const mes = parseInt(mesStr, 10);

  const dados = await getDashboard(ano, mes);

  if (!dados) {
    redirect("/login?expirada=1");
  }

  return (
    <Sidebar>
      <SidebarContent />
      <SidebarInset>
        <div className="flex flex-col h-full w-full overflow-hidden">
          <Header title="Visão geral" />
          <main className="flex-1 overflow-y-auto bg-background px-4 py-6">
            <div className="max-w-[1280px] mx-auto">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
                <div>
                  <h1 className="text-2xl font-semibold text-foreground">
                    Visão geral
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
                percentualEntradas={dados.percentualEntradas}
                percentualSaidas={dados.percentualSaidas}
                totalEntradasMesAnterior={
                  Number(dados.totalEntradas) - Number(dados.variacaoEntradas)
                }
                totalSaidasMesAnterior={
                  Number(dados.totalSaidas) - Number(dados.variacaoSaidas)
                }
                saldoMesAnterior={Number(dados.saldo) - Number(dados.variacaoSaldo)}
              />

              <div className="mt-8 grid gap-6 md:grid-cols-3 lg:grid-cols-2">
                <CategoryBreakdown
                  categorias={dados.categorias}
                  totalSaidas={dados.totalSaidas}
                />
                <MonthlyComparison />
              </div>
            </div>
          </main>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}
