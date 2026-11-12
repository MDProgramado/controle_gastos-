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
      <SkipLink />
      <SidebarContent />
      <SidebarInset>
        <div className="flex flex-col h-full w-full overflow-hidden">
          <Header titulo="Categorias" />
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
                    drawer): o h1 fica só para leitores de tela, evitando
                    repetir o mesmo texto duas vezes na tela. */}
                <h1
                  id="hero-h"
                  className="sr-only text-2xl font-semibold tracking-tight text-foreground md:not-sr-only md:text-3xl"
                >
                  Categorias
                </h1>
                <p className="hero__lead mt-2 line-clamp-2 max-w-[52ch] text-sm text-muted-foreground">
                  Gerencie as categorias de entrada e saída da empresa.
                </p>
              </div>
            </section>

            {/* Conteúdo — estatísticas (counter-rolls) + listas */}
            <section
              className="flex flex-col justify-start py-10 md:min-h-screen md:justify-center"
              data-screen="free"
              aria-labelledby="listas-h"
            >
              <div className="w-full">
                <h2 id="listas-h" className="sr-only">
                  Listas de categorias
                </h2>
                <CategoriasClient
                  categorias={categorias}
                  userRole={usuario.role}
                  onCriar={actionCriarCategoria}
                  onEditar={actionEditarCategoria}
                  onDesativar={actionDesativarCategoria}
                />
                {/* motion.js/moments.js só DEPOIS da hidratação deste segmento */}
                <KitBoot />
              </div>
            </section>
            </div>
          </main>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}
