import { redirect } from "next/navigation";
import Link from "next/link";
import {
  ArrowLeft,
  FileText,
  UploadCloud,
  ListChecks,
  CheckCircle2,
  ChevronRight,
} from "lucide-react";
import { getMe, getCategorias, getImportacoes } from "@/lib/api";
import {
  Sidebar,
  SidebarContent,
  SidebarInset,
} from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";
import { ImportacaoClient } from "@/components/importacoes/ImportacaoClient";
import type { CategoriaOption } from "@/components/importacoes/RevisaoItens";

export const instant = false;

/**
 * Página de importação de PDFs (fase 5, 05-UI-SPEC §5.2). Server Component no
 * padrão de `web/app/page.tsx`: sessão via `getMe` (redirect para
 * /login?expirada=1), shell Sidebar+Header e o Client Component com a máquina
 * de estados (upload → revisão → confirmação → resultado + histórico).
 *
 * Sem `?mes=` — o mês vem dos documentos importados (fatura/extrato).
 */
export default async function ImportacoesPage() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  const [categorias, historico] = await Promise.all([
    getCategorias(),
    getImportacoes(0, 20),
  ]);

  const categoriasOptions: CategoriaOption[] = (categorias ?? []).map((c) => ({
    id: c.id,
    nome: c.nome,
    tipo: c.tipo,
    pessoal: c.pessoal,
    ativo: c.ativo,
  }));

  // Extrai contagem segura do histórico (suporta Page<T> do Spring Boot ou array)
  const totalImportacoes = Array.isArray(historico)
    ? historico.length
    : (historico?.totalElements ?? historico?.content?.length ?? 0);

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent role={usuario.role} />
      <SidebarInset>
        <div className="flex h-dvh w-full overflow-hidden bg-background text-foreground">
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header titulo="Importar PDFs" />

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
                      Importar PDFs
                    </h1>

                    <p className="hero__lead max-w-[55ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                      Envie faturas de cartão e extratos bancários em PDF. A
                      extração identifica a competência e monta os lançamentos
                      para você revisar antes de gravar.
                    </p>
                  </div>

                  {/* Bloco lateral: badges e guia visual do fluxo */}
                  <div className="flex shrink-0 flex-col items-start gap-3 lg:items-end">
                    <div className="flex flex-wrap items-center gap-2">
                      <Link
                        href="/lancamentos"
                        className="inline-flex items-center gap-1.5 rounded-full border border-border bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground transition-colors hover:border-ring hover:text-foreground"
                      >
                        <ArrowLeft className="h-3 w-3" />
                        <span>Lançamentos</span>
                      </Link>

                      <span className="inline-flex items-center gap-1.5 rounded-full border border-border/60 bg-background/40 px-3 py-1 text-xs font-medium text-muted-foreground">
                        <FileText className="h-3 w-3 text-primary" />
                        <span className="tabular-nums">
                          {totalImportacoes}{" "}
                          {totalImportacoes === 1
                            ? "documento processado"
                            : "documentos processados"}
                        </span>
                      </span>
                    </div>

                    <div className="flex w-full shrink-0 flex-col justify-center rounded-xl border border-border/60 bg-background/50 px-5 py-4 sm:w-auto">
                      <span className="mb-2.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                        Fluxo de processamento seguro
                      </span>

                      <ol className="flex flex-wrap items-center gap-3 text-xs">
                        <li className="flex items-center gap-1.5 font-medium text-foreground">
                          <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-primary/20 text-primary">
                            <UploadCloud className="h-3.5 w-3.5" />
                          </span>
                          <span>1. Envio</span>
                        </li>

                        <ChevronRight
                          aria-hidden="true"
                          className="h-3.5 w-3.5 shrink-0 text-muted-foreground/60"
                        />

                        <li className="flex items-center gap-1.5 font-medium text-foreground">
                          <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-muted text-foreground">
                            <ListChecks className="h-3.5 w-3.5" />
                          </span>
                          <span>2. Revisão</span>
                        </li>

                        <ChevronRight
                          aria-hidden="true"
                          className="h-3.5 w-3.5 shrink-0 text-muted-foreground/60"
                        />

                        <li className="flex items-center gap-1.5 font-medium text-foreground">
                          <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-muted text-foreground">
                            <CheckCircle2 className="h-3.5 w-3.5" />
                          </span>
                          <span>3. Gravação</span>
                        </li>
                      </ol>
                    </div>
                  </div>
                </div>
              </section>

              {/* 2. MÁQUINA DE ESTADOS: Envio → Revisão → Confirmação + Histórico */}
              <section
                data-screen="free"
                aria-label="Área de upload e histórico de importações"
                className="space-y-4"
              >
                <ImportacaoClient
                  categorias={categoriasOptions}
                  historicoInicial={historico}
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