import { redirect } from "next/navigation";
import { cookies } from "next/headers";
import {
  getMe,
  getLancamentos,
  type LancamentoData,
  type LancamentoRequest,
  criarLancamento,
  editarLancamento,
  excluirLancamento,
  criarLote,
} from "@/lib/api";
import { LancamentosPageClient } from "@/components/LancamentosPageClient";
import { Sidebar } from "@/components/ui/sidebar";
import { SidebarContent } from "@/components/ui/sidebar";
import { SidebarInset } from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";

type LancamentoDataDisplay = Omit<LancamentoData, "valor"> & { valor: number };

export const instant = false;

export default async function LancamentosPage({
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

  const [lancamentosResp, categorias] = await Promise.all([
    getLancamentos(ano, mes),
    fetchCategorias(),
  ]);

  const lancamentos: LancamentoDataDisplay[] = (lancamentosResp?.lancamentos ?? []).map(l => ({
    ...l,
    valor: Number(l.valor),
  }));
  const total = lancamentosResp?.total ?? lancamentos.length;

  async function actionCriarLancamento(request: Omit<LancamentoRequest, "valor"> & { valor: number }) {
    "use server";
    const result = await criarLancamento({ ...request, valor: String(request.valor) });
    return result ? { ...result, valor: Number(result.valor) } : null;
  }

  async function actionEditarLancamento(
    id: number,
    request: Omit<LancamentoRequest, "valor"> & { valor: number },
  ) {
    "use server";
    const result = await editarLancamento(id, { ...request, valor: String(request.valor) });
    return result ? { ...result, valor: Number(result.valor) } : null;
  }

  async function actionExcluirLancamento(id: number) {
    "use server";
    return excluirLancamento(id);
  }

  async function actionCriarLote(
    linhas: Array<{
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }>,
  ) {
    "use server";
    const result = await criarLote({ linhas });
    return result !== null;
  }

  const nomeMes = new Date(ano, mes - 1, 1)
    .toLocaleString("pt-BR", { month: "long" })
    .replace(/^./, (c) => c.toUpperCase());

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent />
      <SidebarInset>
        <div className="flex flex-col h-full w-full overflow-hidden">
          <Header />
          <main id="main-content" className="flex-1 overflow-y-auto bg-background">
            {/* Hero — a primeira tela: título + lead (entra no carregamento) */}
            <section
              className="hero flex min-h-full flex-col justify-center"
              data-enter
              data-screen="free"
              aria-labelledby="hero-h"
            >
              <div className="wrap w-full py-8">
                <h1
                  id="hero-h"
                  className="text-2xl font-semibold tracking-tight text-foreground"
                >
                  Lançamentos
                </h1>
                <p className="hero__lead mt-3 max-w-[52ch] text-base leading-relaxed text-muted-foreground">
                  Listagem filtrada por mês. Valor positivo = entrada, negativo
                  = saída.
                </p>
              </div>
            </section>

            {/* Conteúdo — resumo em counter-rolls + tabela do mês */}
            <section
              className="flex min-h-screen flex-col justify-center"
              data-screen="free"
              aria-labelledby="lista-h"
            >
              <div className="wrap w-full py-10">
                <h2 id="lista-h" className="sr-only">
                  Lançamentos de {nomeMes}
                </h2>
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
              </div>
            </section>
          </main>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}

async function fetchCategorias(): Promise<
  Array<{ id: number; nome: string; tipo: string; pessoal: boolean; ativo: boolean }>
> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return [];

  try {
    const res = await fetch(`${process.env.API_URL}/api/categorias`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
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
