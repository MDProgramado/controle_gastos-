"use client"

import Link from "next/link"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { descreverResultadoConfirmacao } from "@/lib/importacao-resultado"

/**
 * Estado de sucesso da confirmação (05-UI-SPEC §6.3). A falha NÃO chega aqui:
 * ela volta para a revisão com a seleção preservada (retry idempotente).
 */
type ResultadoConfirmacaoProps = {
  /** IDs de lançamento gravados (soma dos documentos confirmados). */
  lancamentos: number
  /** IDs de conta a pagar gravados. */
  contasAPagar: number
  /** Itens que o usuário desmarcou (não foram gravados). */
  desmarcados: number
  falhas?: number
  erros?: string[]
  /** Ref do título de sucesso — foco programático na transição de estado. */
  tituloRef?: React.Ref<HTMLHeadingElement>
  onNova: () => void
  onRevisarPendentes?: () => void
}

export function ResultadoConfirmacao({
  lancamentos,
  contasAPagar,
  desmarcados,
  falhas = 0,
  erros = [],
  tituloRef,
  onNova,
  onRevisarPendentes,
}: ResultadoConfirmacaoProps) {
  const resumo = descreverResultadoConfirmacao({
    lancamentos,
    contas: contasAPagar,
    falhas,
    erros,
  });

  return (
    <div>
      <Alert variant="default">
        <AlertTitle ref={tituloRef} tabIndex={-1}>
          {resumo.titulo}
        </AlertTitle>
        <AlertDescription>{resumo.descricao}</AlertDescription>
      </Alert>

      {desmarcados > 0 && (
        <p className="mt-4 text-sm text-muted-foreground">
          {desmarcados} itens não foram importados porque você desmarcou.
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <Button asChild className="h-11 w-full sm:w-auto">
          <Link href="/lancamentos">Ver lançamentos</Link>
        </Button>
        {resumo.parcial && onRevisarPendentes && (
          <Button
            variant="outline"
            onClick={onRevisarPendentes}
            className="h-11 w-full sm:w-auto"
          >
            Revisar pendentes
          </Button>
        )}
        <Button variant="outline" onClick={onNova} className="h-11 w-full sm:w-auto">
          Nova importação
        </Button>
      </div>
    </div>
  )
}
