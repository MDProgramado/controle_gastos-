"use client"

import { X } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/Spinner"
import {
  formatarBytes,
  rotuloOrigemArquivo,
  type ArquivoValido,
} from "@/components/importacoes/ImportacaoTipos"

/**
 * Lista de arquivos do envio (05-UI-SPEC §6.1): uma linha por arquivo com
 * tamanho, origem inferida, estado (aria-live) e ação de remover.
 * Estados "pronto/ignorado/duplicado/erro" por documento ficam na revisão —
 * aqui a lista só vive no estado de envio (Aguardando / Analisando).
 */
export type EstadoArquivo =
  | "aguardando"
  | "analisando"
  | "pronto"
  | "ignorado"
  | "erro"

type ImportFileListProps = {
  arquivos: ArquivoValido[]
  estados: Record<string, EstadoArquivo>
  onRemover: (id: string) => void
  disabled?: boolean
}

const ESTADO_BADGE: Record<
  EstadoArquivo,
  { texto: string; variant: "default" | "outline" | "success" | "warning" | "destructive"; animando?: boolean }
> = {
  aguardando: { texto: "Aguardando", variant: "outline" },
  analisando: { texto: "Analisando", variant: "default", animando: true },
  pronto: { texto: "Pronto", variant: "success" },
  ignorado: { texto: "Ignorado", variant: "warning" },
  erro: { texto: "Erro", variant: "destructive" },
}

export function ImportFileList({
  arquivos,
  estados,
  onRemover,
  disabled = false,
}: ImportFileListProps) {
  return (
    <ul className="rounded-lg border border-border bg-card">
      {arquivos.map((arquivo) => {
        const badge = ESTADO_BADGE[estados[arquivo.id] ?? "aguardando"]
        return (
          <li
            key={arquivo.id}
            className="flex items-center justify-between gap-4 border-b border-border px-4 py-3 last:border-b-0"
          >
            <div className="flex min-w-0 items-center gap-3">
              <div className="min-w-0">
                <p
                  className="truncate text-sm font-medium text-foreground"
                  title={arquivo.file.name}
                >
                  {arquivo.file.name}
                </p>
                <div className="mt-1 flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">
                    {formatarBytes(arquivo.file.size)}
                  </span>
                  <Badge variant="outline">
                    {rotuloOrigemArquivo(arquivo.origem)}
                  </Badge>
                </div>
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <span aria-live="polite">
                <Badge variant={badge.variant}>
                  {badge.animando ? (
                    <>
                      <Spinner className="size-3" /> {badge.texto}
                    </>
                  ) : (
                    badge.texto
                  )}
                </Badge>
              </span>
              <Button
                type="button"
                variant="ghost"
                size="icon-xs"
                onClick={() => onRemover(arquivo.id)}
                disabled={disabled}
                aria-label={`Remover ${arquivo.file.name}`}
              >
                <X aria-hidden="true" />
              </Button>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
