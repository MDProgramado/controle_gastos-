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
import { Sidebar } from "@/components/sidebar";
import { Header } from "@/components/header";

type LancamentoDataDisplay = Omit<LancamentoData, "valor"> & { valor: number };

export const instant = false;

/**
 * Página /lancamentos (Server Component).
 * Valida sessão via getMe(), filtra lançamentos pelo mês da URL (?mes=YYYY-MM)
 * e carrega categorias para o formulário. Toda a interatividade (formulário,
 * diálogo de exclusão) vive no cliente via `LancamentosPageClient`.
 */
export default async function LancamentosPage({
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

  // Carrega lançamentos do mês + categorias (para o select do formulário) em paralelo.
  const [lancamentosResp, categorias] = await Promise.all([
    getLancamentos(ano, mes),
    fetchCategorias(),
  ]);

  const lancamentos: LancamentoDataDisplay[] = (lancamentosResp?.lancamentos ?? []).map(l => ({
    ...l,
    valor: Number(l.valor),
  }));
  const total = lancamentosResp?.total ?? lancamentos.length;

  // ── Server Actions (executam no servidor, têm acesso ao cookie "sess") ──
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

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      <Sidebar />
      <div className="flex flex-col h-full w-full overflow-hidden">
        <Header title="Lançamentos" />
        <main className="flex-1 overflow-y-auto bg-background px-4 py-6 md:px-24">
          <div className="max-w-[1280px] mx-auto">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
              <div>
                <h1 className="text-[28px] leading-[1.2] font-semibold text-foreground">
                  Lançamentos
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                  Listagem filtrada por mês. Valor positivo = entrada, negativo = saída.
                </p>
              </div>
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
          </div>
        </main>
      </div>
    </div>
  );
}

/**
 * Busca a lista de categorias do tenant (usada no select do formulário de
 * lançamento). Segue o padrão de autenticação de `getMe()`: cookie "sess" +
 * Bearer token.
 */
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
