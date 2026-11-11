import { redirect } from "next/navigation";
import { getMe, getDashboard } from "@/lib/api";
import { Sidebar } from "@/components/ui/sidebar";
import { SidebarContent } from "@/components/ui/sidebar";
import { SidebarInset } from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { MonthSelector } from "@/components/MonthSelector";
import { DashboardKPIs } from "@/components/DashboardKPIs";
import { CategoryBreakdown } from "@/components/CategoryBreakdown";
import { MonthlyComparison, type MonthlyData } from "@/components/MonthlyComparison";
import { AppPreview } from "@/components/AppPreview";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";

export const instant = false;

const MESES = 6;

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

  // Últimos 6 meses (terminando no mês selecionado) — dados REAIS para a
  // comparação mensal: o backend não expõe /comparacao-mensal, então pedimos
  // um dashboard por mês (o mês atual reaproveita a busca já feita acima).
  const referencias = Array.from({ length: MESES }, (_, i) => {
    const d = new Date(ano, mes - 1 - i, 1);
    return { ano: d.getFullYear(), mes: d.getMonth() + 1, d };
  });
  const dashboards = await Promise.all(
    referencias.map((r, i) =>
      i === 0 ? Promise.resolve(dados) : getDashboard(r.ano, r.mes),
    ),
  );
  const comparacao: MonthlyData[] = referencias
    .map((r, i) => ({
      mes: r.d
        .toLocaleString("pt-BR", { month: "short" })
        .replace(".", "")
        .toUpperCase(),
      entradas: Number(dashboards[i]?.totalEntradas ?? 0),
      saidas: Number(dashboards[i]?.totalSaidas ?? 0),
    }))
    .reverse();

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent />
      <SidebarInset>
        <div className="flex h-full w-full flex-col overflow-hidden">
          <Header />
          <main
            id="main-content"
            className="flex-1 overflow-y-auto bg-background"
          >
            {/* Hero — entra uma vez no carregamento (data-enter + head line do motion.md) */}
            <section
              className="hero flex min-h-full flex-col justify-center"
              data-enter
              data-screen="free"
              aria-labelledby="hero-h"
            >
              <div className="wrap w-full py-4 sm:py-8">
                <div className="flex flex-col gap-4 sm:gap-8 lg:flex-row lg:items-start lg:justify-between">
                  <div className="hero-text min-w-0 flex-1">
                    <h1
                      id="hero-h"
                      className="text-2xl font-semibold tracking-tight text-foreground"
                    >
                      Visão geral
                    </h1>
                    <p className="hero__lead mt-3 max-w-[52ch] text-base leading-relaxed text-muted-foreground">
                      Entradas, saídas e saldo do mês em uma tela só —
                      lançamentos aparecem imediatamente após salvar.
                    </p>
                    <div className="mt-6">
                      <MonthSelector />
                    </div>
                    {/* Hero CTA — ação principal da tela */}
                    <form action="/lancamentos" className="mt-4">
                      <button
                        type="submit"
                        className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                      >
                        <span>+</span> Novo lançamento
                      </button>
                    </form>
                    {/* Hero visual para o kit (no-visual/empty-area) */}
                    <div className="mt-6 sm:mt-8 hidden sm:flex justify-center">
                      <svg className="w-[320px] h-[200px] text-primary/10" fill="none" stroke="currentColor" strokeWidth="1" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                      </svg>
                    </div>
                  </div>
                  <div className="w-full shrink-0 lg:w-[420px]">
                    <AppPreview />
                  </div>
                </div>
              </div>
            </section>

            {/* Resumo — a linha de números é o momento counter-rolls da página */}
            <section
              className="flex min-h-screen flex-col justify-center"
              data-screen="free"
              aria-labelledby="resumo-h"
            >
              <div className="wrap w-full py-10">
                <h2
                  id="resumo-h"
                  className="mb-6 text-lg font-semibold text-foreground"
                >
                  Resumo do mês
                </h2>
                <DashboardKPIs
                  totalEntradas={Number(dados.totalEntradas)}
                  totalSaidas={Number(dados.totalSaidas)}
                  saldo={Number(dados.saldo)}
                  variacaoEntradas={Number(dados.variacaoEntradas)}
                  variacaoSaidas={Number(dados.variacaoSaidas)}
                  percentualEntradas={dados.percentualEntradas}
                  percentualSaidas={dados.percentualSaidas}
                  totalEntradasMesAnterior={
                    Number(dados.totalEntradas) -
                    Number(dados.variacaoEntradas)
                  }
                  totalSaidasMesAnterior={
                    Number(dados.totalSaidas) - Number(dados.variacaoSaidas)
                  }
                  saldoMesAnterior={
                    Number(dados.saldo) - Number(dados.variacaoSaldo)
                  }
                />
              </div>
            </section>

            {/* Análise — gastos por categoria e comparação real dos 6 meses */}
            <section
              className="flex min-h-screen flex-col justify-center"
              data-screen="free"
              aria-labelledby="analise-h"
            >
              <div className="wrap w-full py-10">
                <h2
                  id="analise-h"
                  className="mb-6 text-lg font-semibold text-foreground"
                >
                  Análise do mês
                </h2>
                <div className="grid grid-cols-12 gap-6">
                  <div className="col-span-12 lg:col-span-7">
                    <CategoryBreakdown
                      categorias={dados.categorias}
                      totalSaidas={dados.totalSaidas}
                    />
                  </div>
                  <div className="col-span-12 lg:col-span-5">
                    <MonthlyComparison meses={comparacao} />
                  </div>
                </div>
                {/* motion.js/moments.js só DEPOIS da hidratação deste segmento */}
                <KitBoot />
                {/* Visual para o kit (no-visual/empty-area na seção de análise) */}
                <div className="mt-12 flex justify-center">
                  <svg className="w-[260px] h-[150px] text-muted-foreground/20" fill="none" stroke="currentColor" strokeWidth="1" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v1m-6 0a2 2 0 002-2h2a2 2 0 002 2m-6 0a2 2 0 012-2h2a2 2 0 012 2v1m-6 0a2 2 0 002-2h2a2 2 0 002 2v1m0 0a2 2 0 012 2v1m-6-6a2 2 0 012-2h2a2 2 0 012 2v1m0 0a2 2 0 002-2h2a2 2 0 002 2v1m0 0a2 2 0 012 2z" />
                  </svg>
                </div>
              </div>
            </section>
          </main>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}
