import { redirect } from "next/navigation";
import { getMe, getCategorias, getImportacoes } from "@/lib/api";
import { Sidebar } from "@/components/ui/sidebar";
import { SidebarContent } from "@/components/ui/sidebar";
import { SidebarInset } from "@/components/ui/sidebar";
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

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent />
      <SidebarInset>
        <div className="flex h-full w-full flex-col overflow-hidden">
          <Header titulo="Importar PDFs" />
          <main
            id="main-content"
            className="flex-1 overflow-y-auto bg-background px-4 py-6 md:px-8 lg:px-10"
          >
            <div className="mx-auto w-full max-w-[1280px]">
              {/* Hero — a primeira tela: título + lead (entra no carregamento) */}
              <section
                className="hero flex flex-col justify-start md:min-h-full"
                data-enter
                data-screen="free"
                aria-labelledby="hero-h"
              >
                <div className="w-full">
                  {/* No mobile o título já aparece no cabeçalho (a sidebar vira
                      drawer): o h1 fica só para leitores de tela. */}
                  <h1
                    id="hero-h"
                    className="sr-only text-2xl font-semibold tracking-tight text-foreground md:not-sr-only md:text-3xl"
                  >
                    Importar PDFs
                  </h1>
                  <p className="hero__lead mt-2 line-clamp-2 max-w-[52ch] text-sm text-muted-foreground">
                    Envie faturas de cartão e extratos bancários em PDF. A
                    análise monta os lançamentos do mês para você revisar antes
                    de gravar.
                  </p>
                </div>
              </section>

              {/* Máquina de estados: envio → revisão → confirmação + histórico */}
              <ImportacaoClient
                categorias={categoriasOptions}
                historicoInicial={historico}
              />
              {/* motion.js/moments.js só DEPOIS da hidratação deste segmento */}
              <KitBoot />
            </div>
          </main>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}
