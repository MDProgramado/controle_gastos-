import { redirect } from "next/navigation";
import {
  getMe,
  getCategorias,
  CategoriaData,
  criarCategoria,
  editarCategoria,
  desativarCategoria,
} from "@/lib/api";
import { CategoriasClient } from "@/components/CategoriasClient";
import { Sidebar } from "@/components/ui/sidebar";
import { SidebarContent } from "@/components/ui/sidebar";
import { SidebarInset } from "@/components/ui/sidebar";
import { Header } from "@/components/header";

export const instant = false;

export default async function CategoriasPage() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  const categoriasResp = await getCategorias();
  const categorias: CategoriaData[] = categoriasResp ?? [];

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
    <Sidebar>
      <SidebarContent />
      <SidebarInset>
        <div className="flex flex-col h-full w-full overflow-hidden">
          <Header title="Categorias" />
          <main className="flex-1 overflow-y-auto bg-background px-4 py-6">
            <div className="max-w-[1280px] mx-auto">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
                <div>
                  <h1 className="text-[28px] leading-[1.2] font-semibold text-foreground">
                    Categorias
                  </h1>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Gerencie as categorias de entrada e saída da empresa.
                  </p>
                </div>
              </div>

              <CategoriasClient
                categorias={categorias}
                userRole={usuario.role}
                onCriar={actionCriarCategoria}
                onEditar={actionEditarCategoria}
                onDesativar={actionDesativarCategoria}
              />
            </div>
          </main>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}
