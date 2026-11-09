import { redirect } from "next/navigation";
import { getMe, getCategorias, CategoriaData, criarCategoria, editarCategoria, desativarCategoria } from "@/lib/api";
import { CategoriasClient } from "@/components/CategoriasClient";
import { TopNav } from "@/components/TopNav";

export const instant = false;

/**
 * Página /categorias (Server Component).
 * Valida sessão via getMe(), obtém o role do usuário, carrega categorias
 * server-side. Toda a interatividade (formulário, diálogo de desativação)
 * vive no cliente via `CategoriasClient`.
 */
export default async function CategoriasPage() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  const categoriasResp = await getCategorias();
  const categorias: CategoriaData[] = categoriasResp ?? [];

  // ── Server Actions (executam no servidor, têm acesso ao cookie "sess") ──
  async function actionCriarCategoria(
    request: { nome: string; tipo: string; pessoal: boolean },
  ) {
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
    <div className="w-full">
      <TopNav activeRoute="/categorias" userRole={usuario.role} />
      <div className="w-full max-w-5xl mx-auto px-4 py-6">
        <CategoriasClient
        categorias={categorias}
        userRole={usuario.role}
        onCriar={actionCriarCategoria}
        onEditar={actionEditarCategoria}
        onDesativar={actionDesativarCategoria}
      />
    </div>
  </div>
  );
}
