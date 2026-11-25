import { Suspense } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getMe } from "@/lib/api";
import { urlApiServidor } from "@/lib/api-url";
import { API_TIMEOUT_MS } from "@/lib/bff-helpers";
import {
  Sidebar,
  SidebarContent,
  SidebarInset,
} from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { SkipLink } from "@/components/SkipLink";
import { ContasAPagarClient, type ContaAPagarItem } from "@/components/ContasAPagarClient";

async function getContasAPagar(): Promise<ContaAPagarItem[]> {
  const apiUrl = urlApiServidor();
  if (!apiUrl) return [];

  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("sess")?.value;
    if (!token) return [];

    const res = await fetch(`${apiUrl}/api/contas-a-pagar`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
      signal: AbortSignal.timeout(API_TIMEOUT_MS),
    });

    if (!res.ok) return [];
    return await res.json();
  } catch {
    return [];
  }
}

async function ListaContasAPagar() {
  const contas = await getContasAPagar();
  return <ContasAPagarClient contasIniciais={contas} />;
}

export default async function ContasAPagarPage() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent role={usuario.role} />
      <SidebarInset>
        <div className="flex h-dvh w-full overflow-hidden bg-background text-foreground">
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header titulo="Contas a Pagar" />

            <main
              id="main-content"
              className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6"
            >
              <div className="mx-auto flex w-full max-w-7xl flex-col gap-7 lg:gap-[2.618rem]">
                <h1 className="text-lg font-semibold text-foreground mb-6">
                  Contas a Pagar
                </h1>
                <p className="text-sm text-muted-foreground mb-6">
                  Compromissos futuros, boletos e faturas importadas
                </p>

                <Suspense
                  fallback={
                    <div className="text-sm text-muted-foreground">
                      Carregando contas a pagar...
                    </div>
                  }
                >
                  <ListaContasAPagar />
                </Suspense>
              </div>
            </main>
          </div>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}