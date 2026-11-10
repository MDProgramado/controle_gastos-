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
} from "@/lib/api";
import { LancamentosClient } from "@/components/LancamentosClient";
import { TopNav } from "@/components/TopNav";

type LancamentoDataDisplay = Omit<LancamentoData, "valor"> & { valor: number };

export const instant = false;

/**
 * Página /lancamentos (Server Component).
 * Valida sessão via getMe(), filtra lançamentos pelo mês da URL (?mes=YYYY-MM)
 * e carrega categorias para o formulário. Toda a interatividade (formulário,
 * diálogo de exclusão) vive no cliente via `LancamentosClient`.
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
  const [anoStr, mesStr] = (mesSelecionado ?? new Date().toISOString().slice(0, 7)).split("-");
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

  return (
    <div className="w-full">
      <TopNav activeRoute="/lancamentos" userRole={usuario.role} />
      <div className="w-full max-w-5xl mx-auto px-4 py-6">
        <LancamentosClient
        lancamentos={lancamentos}
        total={total}
        categorias={categorias}
        onCriar={actionCriarLancamento}
        onEditar={actionEditarLancamento}
        onExcluir={actionExcluirLancamento}
      />
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
