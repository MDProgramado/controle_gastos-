import { cookies } from "next/headers";
import {
  Sidebar,
  SidebarContent,
  SidebarInset,
} from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { SkipLink } from "@/components/SkipLink";
import { ContasAPagarClient, type ContaAPagarItem } from "@/components/ContasAPagarClient";

const BACKEND_URL =
  process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

async function getContasAPagar(): Promise<ContaAPagarItem[]> {
  try {
    const cookieStore = await cookies();
    const token =
      cookieStore.get("sess")?.value ||
      cookieStore.get("access_token")?.value ||
      cookieStore.get("auth_token")?.value;

    const res = await fetch(`${BACKEND_URL}/contas-a-pagar`, {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      cache: "no-store",
    });

    if (!res.ok) return [];
    return await res.json();
  } catch {
    return [];
  }
}

export default async function ContasAPagarPage() {
  const contas = await getContasAPagar();

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent />
      <SidebarInset>
        <div className="flex h-dvh w-full overflow-hidden bg-background text-foreground">
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header titulo="Contas a Pagar" />

            <main className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6">
              <div className="mx-auto flex w-full max-w-7xl flex-col gap-7 lg:gap-[2.618rem]">
              <h1 className="text-lg font-semibold text-foreground mb-6">
                Contas a Pagar
              </h1>
              <p className="text-sm text-muted-foreground mb-6">
                Compromissos futuros, boletos e faturas importadas
              </p>
              <ContasAPagarClient contasIniciais={contas} />
            </div>
          </main>
        </div>
      </div>
      </SidebarInset>
    </Sidebar>
  );
}
