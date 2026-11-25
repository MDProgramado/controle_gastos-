import { redirect } from "next/navigation";
import { cookies } from "next/headers";
import Link from "next/link";
import { ArrowLeft, Layers } from "lucide-react";
import { getMe, getLancamentos, type LancamentoData } from "@/lib/api";
import { urlApiServidor } from "@/lib/api-url";
import { API_TIMEOUT_MS } from "@/lib/bff-helpers";
import {
  actionCriarLancamento,
  actionCriarLote,
  actionEditarLancamento,
  actionExcluirLancamento,
} from "./actions";
import { LancamentosPageClient } from "@/components/LancamentosPageClient";
import {
  Sidebar,
  SidebarContent,
  SidebarInset,
} from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { MonthSelector } from "@/components/MonthSelector";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";

type LancamentoDataDisplay = LancamentoData;

export const instant = false;

interface PageProps {
  searchParams: Promise<{
    mes?: string;
    tipo?: string;
    categoriaId?: string;
  }>;
}

export default async function LancamentosPage({
  searchParams,
}: PageProps) {
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

  const [lancamentosResp, categorias] = await Promise.all([
    getLancamentos(ano, mes),
    fetchCategorias(),
  ]);

  const lancamentos: LancamentoDataDisplay[] = lancamentosResp?.lancamentos ?? [];
  const total = lancamentosResp?.total ?? lancamentos.length;

  const nomeMes = new Date(ano, mes - 1, 1)
    .toLocaleString("pt-BR", { month: "long" })
    .replace(/^./, (c) => c.toUpperCase());

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent role={usuario.role} />
      <SidebarInset>
        <div className="flex h-dvh w-full overflow-hidden bg-background text-foreground">
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header titulo="Lançamentos" />

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
                aria-labelledby="hero-lancamentos-h"
              >
                {/* Brilho sutil da paleta no canto superior direito */}
                <div
                  aria-hidden="true"
                  className="pointer-events-none absolute -right-24 -top-24 h-64 w-64 rounded-full bg-primary/15 blur-3xl"
                />

                <div className="flex w-full flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
                  {/* Bloco texto: Navegação de retorno e Contexto */}
                  <div className="min-w-0 flex-1 max-w-2xl">
                    <div className="flex flex-wrap items-center gap-2">
                      <Link
                        href="/"
                        className="inline-flex items-center gap-1.5 rounded-full border border-border bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground transition-colors hover:border-ring hover:text-foreground"
                      >
                        <ArrowLeft className="h-3 w-3" />
                        <span>Visão geral</span>
                      </Link>

                      <span className="inline-flex items-center gap-1.5 rounded-full border border-border/60 bg-background/40 px-3 py-1 text-xs font-medium text-muted-foreground">
                        <Layers className="h-3 w-3 text-primary" />
                        <span className="tabular-nums">
                          {total} {total === 1 ? "registro" : "registros"}
                        </span>
                      </span>
                    </div>

                    {/* Razão tipográfica 1.618rem (título) : 0.9375rem (subtítulo) */}
                    <h1
                      id="hero-lancamentos-h"
                      className="text-[1.618rem] font-semibold leading-tight tracking-tight text-foreground"
                    >
                      Lançamentos de {nomeMes}{" "}
                      <span className="font-normal text-muted-foreground">
                        {ano}
                      </span>
                    </h1>

                    <p className="max-w-[55ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                      Registre entradas, saídas ou importe movimentações em
                      lote. Cada alteração recalcula automaticamente o saldo do
                      mês.
                    </p>
                  </div>

                  {/* Bloco Filtro temporal alinhado à direita (Lei de Fitts) */}
                  <div className="w-full lg:w-auto shrink-0 flex flex-col gap-3 sm:flex-row sm:items-center lg:justify-end">
                    <MonthSelector
                      mesAtual={`${ano}-${String(mes).padStart(2, "0")}`}
                    />
                  </div>
                </div>
              </section>

              {/* 2. ÁREA OPERACIONAL (Resumo em counter-rolls + Tabela / Importação) */}
              <section
                data-screen="free"
                aria-labelledby="lista-h"
                className="space-y-4"
              >
                <div className="flex items-center justify-between">
                  <h2
                    id="lista-h"
                    className="text-xs font-semibold uppercase tracking-widest text-muted-foreground"
                  >
                    Movimentações do período
                  </h2>

                  <span className="text-xs text-muted-foreground">
                    Competência:{" "}
                    <strong className="font-medium text-foreground">
                      {nomeMes}/{ano}
                    </strong>
                  </span>
                </div>

                <LancamentosPageClient
                  lancamentos={lancamentos}
                  total={total}
                  categorias={categorias}
                  onCriar={actionCriarLancamento}
                  onEditar={actionEditarLancamento}
                  onExcluir={actionExcluirLancamento}
                  onCriarLote={actionCriarLote}
                />

                {/* motion.js/moments.js só DEPOIS da hidratação deste segmento */}
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

async function fetchCategorias(): Promise<
  Array<{
    id: number;
    nome: string;
    tipo: string;
    pessoal: boolean;
    ativo: boolean;
  }>
> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return [];
  const apiUrl = urlApiServidor();
  if (!apiUrl) return [];

  try {
    const res = await fetch(`${apiUrl}/api/categorias`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(API_TIMEOUT_MS),
    });
    if (!res.ok) return [];
    return (await res.json()) as Array<{
      id: number;
      nome: string;
      tipo: string;
      pessoal: boolean;
      ativo: boolean;
    }>;
  } catch {
    return [];
  }
}