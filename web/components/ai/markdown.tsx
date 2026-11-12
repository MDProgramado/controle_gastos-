"use client"

import * as React from "react"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"

import { cn } from "@/lib/utils"

/**
 * Resposta da IA em Markdown (GFM: tabelas, listas, negrito, tachado).
 *
 * O react-markdown NAO interpreta HTML bruto (precisaria de `rehype-raw`,
 * que nao usamos), entao o texto devolvido pelo modelo nao injeta script
 * nem tags no chat. Todo o estilo vem dos tokens do tema.
 */
export function Markdown({
  className,
  children,
}: {
  className?: string
  children: string
}) {
  return (
    // O react-markdown v10 nao aceita className direto: o wrapper
    // carrega os estilos e o Markdown cuida do conteudo.
    <div
      className={cn(
        "text-sm leading-relaxed text-foreground",
        // espacamento entre blocos
        "[&>*:first-child]:mt-0 [&>*:last-child]:mb-0 [&>*+*]:mt-2",
        // enfase
        "[&_strong]:font-semibold [&_strong]:text-foreground",
        "[&_em]:italic",
        // listas
        "[&_ul]:list-disc [&_ul]:pl-5 [&_ul]:space-y-1",
        "[&_ol]:list-decimal [&_ol]:pl-5 [&_ol]:space-y-1",
        "[&_li]:pl-0",
        // codigo
        "[&_code]:rounded [&_code]:bg-muted [&_code]:px-1 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-xs",
        "[&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:border [&_pre]:border-border [&_pre]:bg-muted/50 [&_pre]:p-3 [&_pre]:text-xs",
        "[&_pre_code]:bg-transparent [&_pre_code]:p-0",
        // tabelas (GFM)
        "[&_table]:w-full [&_table]:border-collapse [&_table]:text-xs",
        "[&_th]:border [&_th]:border-border [&_th]:bg-muted/50 [&_th]:px-2 [&_th]:py-1.5 [&_th]:text-left [&_th]:font-semibold",
        "[&_td]:border [&_td]:border-border [&_td]:px-2 [&_td]:py-1.5 [&_td]:align-top",
        "[&_td]:tabular-nums",
        // citacao e separador
        "[&_blockquote]:border-l-2 [&_blockquote]:border-border [&_blockquote]:pl-3 [&_blockquote]:text-muted-foreground",
        "[&_hr]:border-border",
        // links
        "[&_a]:text-primary [&_a]:underline [&_a]:underline-offset-2 hover:[&_a]:opacity-80",
        className
      )}
    >
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{children}</ReactMarkdown>
    </div>
  )
}
