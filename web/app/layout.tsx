import type { Metadata } from "next";
import Script from "next/script";
import "./globals.css";
import { SidebarProvider } from "@/components/ui/sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";

export const metadata: Metadata = {
  title: "Controle Financeiro",
  description: "Controle de entradas e saídas da empresa",
};

/*
 * Linha do <head> do kit (design-taste-frontend, references/motion.md + moments.md):
 * - m-enter / m-enter-done: o hero espera a entrada (opacity 0 só nessa janela; o
 *   marcador de 700ms é a rede de segurança — sem motion.js o conteúdo aparece).
 * - mo-js / mo-safe: os moments do kit mostram o estado final se o módulo falhar.
 * Injetada por <Script strategy="beforeInteractive"> → sempre no <head>, antes do paint.
 */
const HEAD_LINE = [
  "document.documentElement.classList.add('m-enter');",
  "setTimeout(()=>document.documentElement.classList.add('m-enter-done'),700);",
  "document.documentElement.classList.add('mo-js');",
  "setTimeout(()=>document.documentElement.classList.add('mo-safe'),2500);",
].join("");

export default function RootLayout({ children }: LayoutProps<"/">) {
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
      <body>
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
          strategy="beforeInteractive"
          dangerouslySetInnerHTML={{ __html: HEAD_LINE }}
        />
        <TooltipProvider>
          <SidebarProvider>{children}</SidebarProvider>
        </TooltipProvider>
      </body>
    </html>
  );
}
