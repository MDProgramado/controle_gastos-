import { redirect } from "next/navigation";
import Link from "next/link";
import { Plus, ArrowUpRight, Sparkles, ShieldCheck, AlertTriangle } from "lucide-react";
import { getMe, getDashboard } from "@/lib/api";
import { centavosAssinadosDeString } from "@/lib/money";
import { progressoPercentual } from "@/lib/format";
import {
  Sidebar,
  SidebarContent,
  SidebarInset,
} from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { MonthSelector } from "@/components/MonthSelector";
import { DashboardKPIs } from "@/components/DashboardKPIs";
import { CategoryBreakdown } from "@/components/CategoryBreakdown";
import {
  MonthlyComparison,
  type MonthlyData,
} from "@/components/MonthlyComparison";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";

export const instant = false;

const MESES = 6;
const ZERO = BigInt(0);

function centavos(valor: string | undefined): bigint {
  return centavosAssinadosDeString(valor ?? "") ?? ZERO;
}

interface PageProps {
  searchParams: Promise<{
    mes?: string;
    tipo?: string;
    categoriaId?: string;
  }>;
}

export default async function Home({ searchParams }: PageProps) {
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

  // Últimos 6 meses (terminando no mês selecionado)
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
      entradas: centavos(dashboards[i]?.totalEntradas),
      saidas: centavos(dashboards[i]?.totalSaidas),
    }))
    .reverse();

  const totalEntradas = centavos(dados.totalEntradas);
  const totalSaidas = centavos(dados.totalSaidas);
  const saldo = centavos(dados.saldo);
  const variacaoEntradas = centavos(dados.variacaoEntradas);
  const variacaoSaidas = centavos(dados.variacaoSaidas);
  const variacaoSaldo = centavos(dados.variacaoSaldo);

  const taxaComprometimento =
    totalEntradas > ZERO
      ? Math.round(progressoPercentual(totalSaidas, totalEntradas))
      : 0;
  const alertaComprometimento = taxaComprometimento >= 80;

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent />
      <SidebarInset>
        <div className="flex h-dvh w-full overflow-hidden bg-background text-foreground">
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header titulo="Visão geral" />

            <main
              id="main-content"
              className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6"
            >
            {/* Container principal com ritmo vertical baseado em Phi (1.618rem / 2.618rem) */}
            <div className="mx-auto flex w-full max-w-7xl flex-col gap-7 lg:gap-[2.618rem]">
              
              {/* 1. CABEÇALHO DE COMANDO (Proporção Áurea interna 1.618fr : 1fr) */}
              <section
                className="hero relative overflow-hidden rounded-2xl border border-border/70 bg-card p-5 sm:p-[1.618rem]"
                data-enter
                data-screen="free"
                aria-labelledby="hero-h"
              >
                {/* Luz ambiental sutil da paleta no canto superior direito */}
                <div
                  aria-hidden="true"
                  className="pointer-events-none absolute -right-24 -top-24 h-64 w-64 rounded-full bg-primary/15 blur-3xl"
                />

                <div className="flex w-full flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
                  {/* Bloco texto: Identidade e Contexto */}
                  <div className="min-w-0 flex-1 max-w-2xl">
                    <div className="inline-flex items-center gap-2 rounded-full border border-border bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground">
                      <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                      <span>Painel Executivo</span>
                    </div>

                    {/* Razão tipográfica 1.618rem (título) : 1rem (subtítulo) */}
                    <h1
                      id="hero-h"
                      className="sr-only text-[1.618rem] font-semibold leading-tight tracking-tight text-foreground md:not-sr-only"
                    >
                      Visão geral
                    </h1>

                    <p className="max-w-[55ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                      Acompanhe entradas, saídas e a saúde do seu fluxo mensal.
                      Novos lançamentos atualizam os indicadores em tempo real.
                    </p>
                  </div>

                  {/* Bloco Controles de Ação (Lei de Fitts - alvos amplos e agrupados) */}
                  <div className="w-full lg:w-auto shrink-0 flex flex-col gap-3 sm:flex-row sm:items-center lg:justify-end">
                    <MonthSelector
                      mesAtual={`${ano}-${String(mes).padStart(2, "0")}`}
                    />

                    <Link
                      href="/lancamentos"
                      className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-medium text-primary-foreground shadow-sm transition-all duration-200 hover:bg-primary/90 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
                    >
                      <Plus className="h-4 w-4" />
                      <span>Novo lançamento</span>
                    </Link>
                  </div>
                </div>
              </section>

              {/* 2. KPIs PRINCIPAIS + INDICADOR VISUAL DE SAÚDE FINANCEIRA */}
              <section
                data-screen="free"
                aria-labelledby="resumo-h"
                className="space-y-4"
              >
                <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
                  <h2
                    id="resumo-h"
                    className="text-xs font-semibold uppercase tracking-widest text-muted-foreground"
                  >
                    Resumo do período
                  </h2>

                  {/* Feedback visual de comprometimento da renda (Redução de carga cognitiva) */}
                  {totalEntradas > ZERO && (
                    <div className="flex items-center gap-3 rounded-lg border border-border/60 bg-card/60 px-3 py-1.5 text-xs">
                      <span className="flex items-center gap-1.5 text-muted-foreground">
                        {alertaComprometimento ? (
                          <AlertTriangle className="h-3.5 w-3.5 text-primary" />
                        ) : (
                          <ShieldCheck className="h-3.5 w-3.5 text-foreground" />
                        )}
                        Renda comprometida:
                      </span>

                      <div className="flex items-center gap-2">
                        <div className="h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                          <div
                            className={`h-full rounded-full transition-all duration-500 ${
                              alertaComprometimento
                                ? "bg-primary"
                                : "bg-foreground"
                            }`}
                            style={{ width: `${taxaComprometimento}%` }}
                          />
                        </div>
                        <strong className="font-semibold tabular-nums text-foreground">
                          {taxaComprometimento}%
                        </strong>
                      </div>
                    </div>
                  )}
                </div>

                <DashboardKPIs
                  totalEntradas={totalEntradas}
                  totalSaidas={totalSaidas}
                  saldo={saldo}
                  variacaoEntradas={variacaoEntradas}
                  variacaoSaidas={variacaoSaidas}
                  percentualEntradas={dados.percentualEntradas}
                  percentualSaidas={dados.percentualSaidas}
                  totalEntradasMesAnterior={
                    totalEntradas - variacaoEntradas
                  }
                  totalSaidasMesAnterior={totalSaidas - variacaoSaidas}
                  saldoMesAnterior={saldo - variacaoSaldo}
                />
              </section>

              {/* 3. ANÁLISE GRÁFICA EM PROPORÇÃO ÁUREA (61.8% Evolução | 38.2% Categorias) */}
              <section
                data-screen="free"
                aria-labelledby="analise-h"
                className="w-full min-w-0 space-y-4"
              >
                <div className="flex items-center justify-between">
                  <h2
                    id="analise-h"
                    className="text-xs font-semibold uppercase tracking-widest text-muted-foreground"
                  >
                    Análise estratégica
                  </h2>

                  <Link
                    href="/lancamentos"
                    className="group inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                  >
                    <Sparkles className="h-3.5 w-3.5 text-primary transition-transform group-hover:scale-110" />
                    <span>Explorar com IA</span>
                    <ArrowUpRight className="h-3.5 w-3.5 transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
                  </Link>
                </div>

                {/* Grid de comparação mensal e categorias: empilhado no mobile, lado a lado no desktop xl+ */}
                <div className="grid w-full min-w-0 grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
                  <div className="w-full min-w-0">
                    <MonthlyComparison meses={comparacao} />
                  </div>
                  <div className="w-full min-w-0">
                    <CategoryBreakdown
                      categorias={dados.categorias}
                      totalSaidas={dados.totalSaidas}
                    />
                  </div>
                </div>

                <KitBoot />
              </section>

            </div>
          </main>
        </div>
      </div>
      </SidebarInset>
    </Sidebar>
  );
}