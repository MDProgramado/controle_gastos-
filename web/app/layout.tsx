import type { Metadata } from "next";
import Script from "next/script";
import "./globals.css";
import { SidebarProvider } from "@/components/ui/sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ImportacaoProvider } from "@/contexts/ImportacaoContext";
import { ChatFlutuante } from "@/components/ai/ChatFlutuante";
import { getMe } from "@/lib/api";

export const instant = false;

export const metadata: Metadata = {
  title: "Controle Financeiro",
  description: "Controle de entradas e saídas da empresa",
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  const usuario = await getMe();

  return (
    <html
      lang="pt-BR"
      className="dark"
      data-type="software"
      data-radius="sharp"
      data-buttons="solid"
      data-motion="true"
      suppressHydrationWarning
    >
      <body className="min-h-dvh bg-background text-foreground antialiased overflow-x-hidden">
        <meta name="page-type" content="software" />
        <meta name="site-kind" content="software" />
        {/* camada de micro-interações do kit: motion.css DEPOIS do globals.css, motion.js carrega o entrance */}
        <link
          rel="stylesheet"
          href="/assets/motion/motion.css"
          precedence="taste-motion"
        />
        <Script
          id="taste-head-line"
          src="/assets/motion/head-line.js"
          strategy="beforeInteractive"
        />
        <TooltipProvider>
          <SidebarProvider>
            <ImportacaoProvider>{children}</ImportacaoProvider>
          </SidebarProvider>
        </TooltipProvider>
        <ChatFlutuante role={usuario?.role ?? null} />
      </body>
    </html>
  );
}
