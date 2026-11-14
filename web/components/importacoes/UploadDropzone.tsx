"use client"

import * as React from "react"
import { FileUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/**
 * Área de envio dos PDFs (05-UI-SPEC §6.1): clicável, arrastar-e-soltar e
 * acessível por teclado (role=button, Enter/Space). O input de arquivo é
 * sr-only e aceita só PDF (nunca o curinga de todos os tipos).
 */
export const ID_LIMITES = "upload-limites"

type UploadDropzoneProps = {
  /** Arquivos escolhidos (pelo clique ou pelo drop) — validação no pai. */
  onArquivosSelecionados: (arquivos: FileList | File[]) => void
  disabled?: boolean
}

export function UploadDropzone({
  onArquivosSelecionados,
  disabled = false,
}: UploadDropzoneProps) {
  const inputRef = React.useRef<HTMLInputElement>(null)
  const [arrastando, setArrastando] = React.useState(false)

  const abrirSeletor = () => {
    if (disabled) return
    inputRef.current?.click()
  }

  return (
    <div
      role="button"
      tabIndex={disabled ? -1 : 0}
      aria-label="Selecionar PDFs"
      aria-describedby={ID_LIMITES}
      aria-disabled={disabled}
      onClick={abrirSeletor}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault()
          abrirSeletor()
        }
      }}
      onDragOver={(e) => {
        e.preventDefault()
        if (!disabled) setArrastando(true)
      }}
      onDragLeave={() => setArrastando(false)}
      onDrop={(e) => {
        e.preventDefault()
        setArrastando(false)
        if (disabled) return
        onArquivosSelecionados(e.dataTransfer.files)
      }}
      className={cn(
        "flex min-h-[200px] cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed border-border bg-card px-4 py-8 text-center transition-colors md:min-h-[160px]",
        arrastando && "border-primary bg-primary/5",
        disabled && "cursor-not-allowed opacity-60",
      )}
    >
      <FileUp aria-hidden="true" className="h-8 w-8 text-muted-foreground" />
      <p className="text-base font-semibold text-foreground">
        {arrastando ? "Solte os arquivos para adicionar" : "Arraste os PDFs aqui"}
      </p>
      <p className="text-sm text-muted-foreground">
        ou clique para escolher arquivos
      </p>
      <Button
        type="button"
        variant="outline"
        onClick={(e) => {
          e.stopPropagation()
          abrirSeletor()
        }}
        disabled={disabled}
      >
        Selecionar PDFs
      </Button>
      <input
        ref={inputRef}
        type="file"
        accept="application/pdf,.pdf"
        multiple
        className="sr-only"
        onClick={(e) => e.stopPropagation()}
        onChange={(e) => {
          if (e.target.files && e.target.files.length > 0) {
            onArquivosSelecionados(e.target.files)
          }
          e.target.value = "" // permite re-selecionar o mesmo arquivo
        }}
      />
    </div>
  )
}
