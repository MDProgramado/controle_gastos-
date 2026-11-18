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
} from "@/lib/api";
import { CategoriasClient } from "@/components/CategoriasClient";
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

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent />
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

                <div className="relative grid grid-cols-1 items-center gap-7 lg:grid-cols-8">
                  {/* Bloco 61.8%: Navegação, Contexto e Hierarquia Tipográfica */}
                  <div className="space-y-2">
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

                  {/* Bloco 38.2%: Resumo Estrutural Instantâneo */}
                  <div className="grid grid-cols-2 gap-3 rounded-xl border border-border/60 bg-background/50 p-3.5 sm:p-4">
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
            </div>
          </main>
        </div>
      </div>
      </SidebarInset>
    </Sidebar>
  );
}