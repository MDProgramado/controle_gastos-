import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Tags, ArrowUpCircle, ArrowDownCircle } from "lucide-react";
import {
  getMe,
  getCategorias,
  type CategoriaData,
  criarCategoria,
  editarCategoria,
  desativarCategoria,
  getGrupos503020,
  salvarGrupo503020,
  type CategoriaGrupo503020Data,
} from "@/lib/api";
import { CategoriasClient } from "@/components/CategoriasClient";
import { Grupos503020Client } from "@/components/Grupos503020Client";
import type { Grupo503020 } from "@/lib/grupos-503020";
import {
  Sidebar,
  SidebarContent,
  SidebarInset,
} from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";

export const instant = false;

export default async function CategoriasPage() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  const categoriasResp = await getCategorias();
  const categorias: CategoriaData[] = categoriasResp ?? [];
  const gruposResp = await getGrupos503020();
  const grupos: CategoriaGrupo503020Data[] = gruposResp ?? [];

  // Métricas rápidas para leitura imediata (Redução de carga cognitiva)
  const ativas = categorias.filter((c) => c.ativo);
  const totalEntradas = ativas.filter(
    (c) => c.tipo?.toUpperCase() === "ENTRADA",
  ).length;
  const totalSaidas = ativas.filter(
    (c) => c.tipo?.toUpperCase() === "SAIDA",
  ).length;

  async function actionCriarCategoria(request: {
    nome: string;
    tipo: string;
    pessoal: boolean;
  }) {
    "use server";
    return criarCategoria(request);
  }

  async function actionEditarCategoria(
    id: number,
    request: { nome: string; tipo: string; pessoal: boolean },
  ) {
    "use server";
    return editarCategoria(id, request);
  }

  async function actionDesativarCategoria(id: number) {
    "use server";
    return desativarCategoria(id);
  }

  async function actionSalvarGrupo503020(
    categoriaId: number,
    grupo: Grupo503020,
  ) {
    "use server";
    return salvarGrupo503020(categoriaId, grupo);
  }

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent role={usuario.role} />
      <SidebarInset>
        <div className="flex h-dvh w-full overflow-hidden bg-background text-foreground">
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header titulo="Categorias" />

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
                {/* Brilho sutil da paleta no canto superior direito */}
                <div
                  aria-hidden="true"
                  className="pointer-events-none absolute -right-24 -top-24 h-64 w-64 rounded-full bg-primary/15 blur-3xl"
                />

                <div className="relative flex w-full flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
                  {/* Bloco texto: título e descrição com largura confortável */}
                  <div className="min-w-0 max-w-xl flex-1 space-y-2">
                    {/* Razão tipográfica 1.618rem (título) : 0.9375rem (subtítulo) */}
                    <h1
                      id="hero-h"
                      className="sr-only text-[1.618rem] font-semibold leading-tight tracking-tight text-foreground md:not-sr-only"
                    >
                      Categorias
                    </h1>

                    <p className="hero__lead max-w-[55ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                      Organize a estrutura de classificação das suas entradas e
                      saídas para manter os relatórios financeiros precisos.
                    </p>
                  </div>

                  {/* Bloco lateral: badges e resumo estrutural */}
                  <div className="flex shrink-0 flex-col items-start gap-3 lg:items-end">
                    <div className="flex flex-wrap items-center gap-2">
                      <Link
                        href="/"
                        className="inline-flex items-center gap-1.5 rounded-full border border-border bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground transition-colors hover:border-ring hover:text-foreground"
                      >
                        <ArrowLeft className="h-3 w-3" />
                        <span>Visão geral</span>
                      </Link>

                      <span className="inline-flex items-center gap-1.5 rounded-full border border-border/60 bg-background/40 px-3 py-1 text-xs font-medium text-muted-foreground">
                        <Tags className="h-3 w-3 text-primary" />
                        <span className="tabular-nums">
                          {ativas.length}{" "}
                          {ativas.length === 1 ? "ativa" : "ativas"}
                        </span>
                      </span>
                    </div>

                    <div className="grid w-auto min-w-[220px] grid-cols-2 gap-3 rounded-xl border border-border/60 bg-background/50 px-5 py-3.5">
                      <div className="space-y-1">
                        <span className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                          <ArrowUpCircle className="h-3.5 w-3.5 text-foreground" />
                          Entradas
                        </span>
                        <p className="text-xl font-semibold tabular-nums text-foreground">
                          {totalEntradas}
                        </p>
                      </div>

                      <div className="space-y-1 border-l border-border/60 pl-3">
                        <span className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                          <ArrowDownCircle className="h-3.5 w-3.5 text-primary" />
                          Saídas
                        </span>
                        <p className="text-xl font-semibold tabular-nums text-foreground">
                          {totalSaidas}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </section>

              {/* 2. ÁREA DE GERENCIAMENTO (Estatísticas + Listas de Categorias) */}
              <section
                data-screen="free"
                aria-labelledby="listas-h"
                className="space-y-4"
              >
                <div className="flex items-center justify-between">
                  <h2
                    id="listas-h"
                    className="text-xs font-semibold uppercase tracking-widest text-muted-foreground"
                  >
                    Estrutura de classificação
                  </h2>

                  <span className="text-xs text-muted-foreground">
                    Total cadastrado:{" "}
                    <strong className="font-medium tabular-nums text-foreground">
                      {categorias.length}
                    </strong>
                  </span>
                </div>

                <CategoriasClient
                  categorias={categorias}
                  userRole={usuario.role}
                  onCriar={actionCriarCategoria}
                  onEditar={actionEditarCategoria}
                  onDesativar={actionDesativarCategoria}
                />

                {/* motion.js/moments.js só DEPOIS da hidratação deste segmento */}
                <KitBoot />
              </section>

              <section
                data-screen="free"
                aria-labelledby="grupos-503020-h"
                className="space-y-4"
              >
                <h2
                  id="grupos-503020-h"
                  className="text-xs font-semibold uppercase tracking-widest text-muted-foreground"
                >
                  Planejamento 50/30/20
                </h2>
                <Grupos503020Client
                  itens={grupos}
                  podeEditar={usuario.role === "ADMIN"}
                  onSalvar={actionSalvarGrupo503020}
                />
              </section>
            </div>
          </main>
        </div>
      </div>
      </SidebarInset>
    </Sidebar>
  );
}