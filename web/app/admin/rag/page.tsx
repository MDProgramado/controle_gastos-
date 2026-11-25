import Link from "next/link";
import { redirect } from "next/navigation";
import { ArrowLeft, Database } from "lucide-react";
import { getMe } from "@/lib/api";
import { RagAdminClient } from "@/components/RagAdminClient";
import {
  Sidebar,
  SidebarContent,
  SidebarInset,
} from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";

export const instant = false;

export default async function RagAdminPage() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }
  if (usuario.role !== "ADMIN") {
    redirect("/");
  }

  return (
    <Sidebar>
      <SkipLink />
      <SidebarContent role={usuario.role} />
      <SidebarInset>
        <div className="flex h-dvh w-full overflow-hidden bg-background text-foreground">
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header titulo="Base de conhecimento" />

            <main
              id="main-content"
              className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6"
            >
              <div className="mx-auto flex w-full max-w-7xl flex-col gap-7 lg:gap-[2.618rem]">
                <section
                  className="hero relative overflow-hidden rounded-2xl border border-border/70 bg-card p-5 sm:p-[1.618rem]"
                  data-enter
                  data-screen="free"
                  aria-labelledby="rag-hero-h"
                >
                  <div
                    aria-hidden="true"
                    className="pointer-events-none absolute -right-24 -top-24 h-64 w-64 rounded-full bg-primary/15 blur-3xl"
                  />

                  <div className="relative flex w-full flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
                    <div className="min-w-0 max-w-xl flex-1 space-y-2">
                      <h1
                        id="rag-hero-h"
                        className="sr-only text-[1.618rem] font-semibold leading-tight tracking-tight text-foreground md:not-sr-only"
                      >
                        Base de conhecimento
                      </h1>
                      <p className="hero__lead max-w-[55ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                        Reindexe o corpus financeiro versionado para manter as
                        respostas da IA alinhadas às regras atuais.
                      </p>
                    </div>

                    <div className="flex shrink-0 flex-col items-start gap-3 lg:items-end">
                      <Link
                        href="/"
                        className="inline-flex items-center gap-1.5 rounded-full border border-border bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground transition-colors hover:border-ring hover:text-foreground"
                      >
                        <ArrowLeft className="h-3 w-3" />
                        Visão geral
                      </Link>
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-border/60 bg-background/40 px-3 py-1 text-xs font-medium text-muted-foreground">
                        <Database className="h-3 w-3 text-primary" />
                        Somente administradores
                      </span>
                    </div>
                  </div>
                </section>

                <section data-screen="free" aria-label="Reindexação">
                  <RagAdminClient />
                  <KitBoot />
                </section>
              </div>
            </main>
          </div>
        </div>
      </SidebarInset>
    </Sidebar>
  );
}
