"use client"

import * as React from "react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { DeleteConfirmationDialog } from "@/components/DeleteConfirmationDialog"
import {
  ApiError,
  descartarImportacao,
  listarImportacoes,
  type ImportacaoHistorico,
  type ImportacaoHistoricoResumo,
} from "@/lib/api-client"
import { formatarDataIso, rotuloOrigem } from "@/components/importacoes/ImportacaoTipos"

/**
 * Histórico de importações do tenant (05-UI-SPEC §6.5): tabela densa no md+,
 * lista de blocos no mobile, descarte pendente com `DeleteConfirmationDialog`
 * (nunca `window.confirm`). Recarrega quando `refreshKey` muda (após uma
 * confirmação na página) e após cada descarte.
 */

const ESTADO: Record<string, { texto: string; variant: "success" | "warning" | "outline" | "destructive" }> = {
  ANALISADO: { texto: "Aguardando revisão", variant: "warning" },
  CONFIRMADO: { texto: "Confirmada", variant: "success" },
  DESCARTADO: { texto: "Descartada", variant: "outline" },
  DUPLICADO: { texto: "Duplicado", variant: "outline" },
  SEM_TEXTO: { texto: "Sem texto", variant: "outline" },
  ERRO: { texto: "Erro", variant: "destructive" },
}
type ImportacoesPendentesProps = {
  inicial: ImportacaoHistorico | null
  refreshKey: number
  onSessaoExpirada: () => void
  onRevisar: (id: number) => void
  carregandoRevisaoId?: number | null
}

export function ImportacoesPendentes({
  inicial,
  refreshKey,
  onSessaoExpirada,
  onRevisar,
  carregandoRevisaoId = null,
}: ImportacoesPendentesProps) {
  const [lista, setLista] = React.useState<ImportacaoHistoricoResumo[]>(
    inicial?.content ?? [],
  )
  const [erroCarga, setErroCarga] = React.useState(false)
  const [msgSucesso, setMsgSucesso] = React.useState(false)
  const [erroDescarte, setErroDescarte] = React.useState<string | null>(null)
  const [alvo, setAlvo] = React.useState<ImportacaoHistoricoResumo | null>(null)
  const [ocupado, setOcupado] = React.useState(false)

  // Re-fetch quando o pai sinaliza mudança (confirmação nova no lote).
  React.useEffect(() => {
    let ativo = true
    void (async () => {
      const historico = await listarImportacoes(0, 20)
      if (!ativo) return
      if (historico === null) {
        if (inicial === null) setErroCarga(true)
        return
      }
      setLista(historico.content)
      setErroCarga(false)
    })()
    return () => {
      ativo = false
    }
    // inicial só alimenta o primeiro estado — mudanças vêm via refreshKey.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey])

  const descartar = async () => {
    if (!alvo) return
    setOcupado(true)
    try {
      await descartarImportacao(alvo.id)
      setLista((atual) => atual.filter((i) => i.id !== alvo.id))
      setMsgSucesso(true)
      setErroDescarte(null)
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        onSessaoExpirada()
        return
      }
      // Falha no descarte: mensagem do servidor (ex.: 409 já confirmada).
      setErroDescarte(
        e instanceof Error && e.message
          ? e.message
          : "Não foi possível descartar a importação. Tente novamente em instantes.",
      )
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div>
      {msgSucesso && (
        <Alert variant="default" className="mb-4">
          <AlertTitle>Importação descartada.</AlertTitle>
        </Alert>
      )}

      {erroDescarte && (
        <Alert variant="destructive" className="mb-4">
          <AlertDescription>{erroDescarte}</AlertDescription>
        </Alert>
      )}

      {erroCarga && (
        <Alert variant="destructive" className="mb-4">
          <AlertDescription>
            Não foi possível carregar os dados. Tente novamente em instantes.
          </AlertDescription>
        </Alert>
      )}

      {lista.length === 0 ? (
        <div className="rounded-lg border border-border p-12 text-center">
          <p className="text-sm text-muted-foreground">
            Nenhuma importação por aqui ainda.
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Envie a fatura do cartão ou o extrato bancário em PDF para começar.
          </p>
        </div>
      ) : (
        <>
          {/* Desktop (md+): tabela densa */}
          <div className="hidden overflow-hidden rounded-lg border border-border bg-card md:block">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-border">
                  <th scope="col" className="p-4 text-sm font-normal text-muted-foreground">Arquivo</th>
                  <th scope="col" className="p-4 text-sm font-normal text-muted-foreground">Origem</th>
                  <th scope="col" className="p-4 text-sm font-normal text-muted-foreground">Enviado em</th>
                  <th scope="col" className="p-4 text-sm font-normal text-muted-foreground">Estado</th>
                  <th scope="col" className="p-4 text-right text-sm font-normal text-muted-foreground">Ações</th>
                </tr>
              </thead>
              <tbody>
                {lista.map((importacao) => {
                  const estado = ESTADO[importacao.status] ?? { texto: importacao.status, variant: "outline" as const }
                  const pendente = importacao.status === "ANALISADO"
                  return (
                    <tr key={importacao.id} className="border-b border-border transition-colors last:border-b-0 hover:bg-muted/50">
                      <td className="max-w-[320px] p-4">
                        <p className="truncate text-sm font-medium text-foreground" title={importacao.nomeArquivo}>
                          {importacao.nomeArquivo}
                        </p>
                      </td>
                      <td className="p-4 text-sm text-muted-foreground">
                        {rotuloOrigem(importacao.tipoDocumento)}
                      </td>
                      <td className="p-4 text-sm text-muted-foreground tabular-nums">
                        {formatarDataIso(importacao.criadoEm)}
                      </td>
                      <td className="p-4">
                        <Badge variant={estado.variant}>{estado.texto}</Badge>
                      </td>
                      <td className="p-4 text-right">

                        {pendente && (
  <div className="flex items-center justify-end gap-2">
    <Button
      type="button"
      variant="outline"
      size="sm"
      onClick={() => onRevisar(importacao.id)}
      disabled={ocupado || carregandoRevisaoId !== null}
    >
      {carregandoRevisaoId === importacao.id ? "Abrindo…" : "Revisar"}
    </Button>
    <Button
      type="button"
      variant="ghost"
      size="sm"
      className="text-destructive hover:text-destructive"
      onClick={() => setAlvo(importacao)}
      disabled={ocupado || carregandoRevisaoId !== null}
    >
      Descartar
    </Button>
  </div>
)}


                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* Mobile: blocos empilhados */}
          <ul className="overflow-hidden rounded-lg border border-border bg-card md:hidden">
            {lista.map((importacao) => {
              const estado = ESTADO[importacao.status] ?? { texto: importacao.status, variant: "outline" as const }
              const pendente = importacao.status === "ANALISADO"
              return (
                <li key={importacao.id} className="border-b border-border p-4 last:border-b-0">
                  <p className="truncate text-sm font-medium text-foreground" title={importacao.nomeArquivo}>
                    {importacao.nomeArquivo}
                  </p>
                  <div className="mt-2 flex items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant="outline">{rotuloOrigem(importacao.tipoDocumento)}</Badge>
                      <Badge variant={estado.variant}>{estado.texto}</Badge>
                    </div>
                    <span className="text-xs text-muted-foreground tabular-nums">
                      {formatarDataIso(importacao.criadoEm)}
                    </span>
                  </div>
                  {pendente && (
  <div className="flex items-center justify-end gap-2">
    <Button
      type="button"
      variant="outline"
      size="sm"
      onClick={() => onRevisar(importacao.id)}
      disabled={ocupado || carregandoRevisaoId !== null}
    >
      {carregandoRevisaoId === importacao.id ? "Abrindo…" : "Revisar"}
    </Button>
    <Button
      type="button"
      variant="ghost"
      size="sm"
      className="text-destructive hover:text-destructive"
      onClick={() => setAlvo(importacao)}
      disabled={ocupado || carregandoRevisaoId !== null}
    >
      Descartar
    </Button>
  </div>
)}
                </li>
              )
            })}
          </ul>
        </>
      )}

      <DeleteConfirmationDialog
        isOpen={alvo !== null}
        onConfirm={descartar}
        onClose={() => setAlvo(null)}
        action="descartar"
        target={alvo?.nomeArquivo ?? "importação"}
        descricao="Tem certeza que deseja descartar esta importação pendente? Os itens revisados serão perdidos e o arquivo poderá ser enviado de novo."
      />
    </div>
  )
}
