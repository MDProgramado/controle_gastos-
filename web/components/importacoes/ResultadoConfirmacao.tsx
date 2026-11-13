"use client"

import Link from "next/link"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"

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
  /** Ref do título de sucesso — foco programático na transição de estado. */
  tituloRef?: React.Ref<HTMLHeadingElement>
  onNova: () => void
}

export function ResultadoConfirmacao({
  lancamentos,
  contasAPagar,
  desmarcados,
  tituloRef,
  onNova,
}: ResultadoConfirmacaoProps) {
  return (
    <div>
      <Alert variant="default">
        <AlertTitle ref={tituloRef} tabIndex={-1}>
          Importação confirmada
        </AlertTitle>
        <AlertDescription>
          {lancamentos} lançamentos e {contasAPagar} contas a pagar foram
          gravados. Nada mais será criado se você confirmar de novo.
        </AlertDescription>
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
        <Button variant="outline" onClick={onNova} className="h-11 w-full sm:w-auto">
          Nova importação
        </Button>
      </div>
    </div>
  )
}
