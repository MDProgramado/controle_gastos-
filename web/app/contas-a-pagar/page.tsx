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

type ParamsBusca = Record<string, string | string[] | undefined>;

type FiltrosContas = {
  pago?: boolean;
  vencimentoDe?: string;
  vencimentoAte?: string;
};

type ResultadoContas = {
  contas: ContaAPagarItem[];
  erro: string | null;
};

function valorUnico(params: ParamsBusca, chave: string): string | undefined {
  const valor = params[chave];
  return Array.isArray(valor) ? valor[0] : valor;
}

function dataValida(valor: string | undefined): valor is string {
  return !!valor && /^\d{4}-\d{2}-\d{2}$/.test(valor) && !Number.isNaN(Date.parse(valor));
}

function filtrosDaBusca(params: ParamsBusca): FiltrosContas {
  const pago = valorUnico(params, "pago");
  const vencimentoDe = valorUnico(params, "vencimentoDe");
  const vencimentoAte = valorUnico(params, "vencimentoAte");

  return {
    pago: pago === "true" ? true : pago === "false" ? false : undefined,
    vencimentoDe: dataValida(vencimentoDe) ? vencimentoDe : undefined,
    vencimentoAte: dataValida(vencimentoAte) ? vencimentoAte : undefined,
  };
}

async function getContasAPagar(filtros: FiltrosContas): Promise<ResultadoContas> {
  const apiUrl = urlApiServidor();
  if (!apiUrl) {
    return { contas: [], erro: "API não configurada." };
  }

  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return { contas: [], erro: "Sessão expirada. Entre novamente." };
  }

  const url = new URL("/api/contas-a-pagar", apiUrl);
  if (filtros.pago !== undefined) url.searchParams.set("pago", String(filtros.pago));
  if (filtros.vencimentoDe) url.searchParams.set("vencimentoDe", filtros.vencimentoDe);
  if (filtros.vencimentoAte) url.searchParams.set("vencimentoAte", filtros.vencimentoAte);

  try {
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(API_TIMEOUT_MS),
    });

    if (!res.ok) {
      let detalhe: string | undefined;
      try {
        const corpo = (await res.json()) as { detail?: string; erro?: string };
        detalhe = corpo.detail ?? corpo.erro;
      } catch {
        detalhe = undefined;
      }
      const erro =
        res.status === 401
          ? "Sessão expirada. Entre novamente."
          : res.status >= 502
          ? "Não foi possível consultar as contas a pagar."
          : detalhe ?? "Não foi possível carregar as contas a pagar.";
      return { contas: [], erro };
    }

    return { contas: (await res.json()) as ContaAPagarItem[], erro: null };
  } catch {
    return { contas: [], erro: "Não foi possível consultar as contas a pagar." };
  }
}

async function ListaContasAPagar({ searchParams }: { searchParams: Promise<ParamsBusca> }) {
  const { contas, erro } = await getContasAPagar(filtrosDaBusca(await searchParams));

  return (
    <div className="space-y-4">
      {erro && (
        <p
          role="alert"
          className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          {erro}
        </p>
      )}
      <ContasAPagarClient contasIniciais={contas} />
    </div>
  );
}

export default async function ContasAPagarPage({
  searchParams,
}: {
  searchParams: Promise<ParamsBusca>;
}) {
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
                  <ListaContasAPagar searchParams={searchParams} />
                </Suspense>
              </div>
            </main>
          </div>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}
