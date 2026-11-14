"use client"

import * as React from "react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import type { AlertaLista } from "@/components/importacoes/ImportacaoTipos"

/**
 * Lista de alertas da análise (05-UI-SPEC §6.2). O h2 "Alertas" + contador
 * ficam no pai (seção); aqui só o conteúdo. Alertas com alvo (ex.:
 * REVISAR_CATEGORIA) rolam e focam a linha do item ao clicar.
 */
type AlertasImportacaoProps = {
  alertas: AlertaLista[]
}

export function AlertasImportacao({ alertas }: AlertasImportacaoProps) {
  const focarItem = (itemId: number) => {
    const alvo = document.getElementById(`item-${itemId}`)
    if (!alvo) return
    alvo.scrollIntoView({ behavior: "smooth", block: "center" })
    alvo.focus()
  }

  if (alertas.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Nenhum alerta nesta importação.
      </p>
    )
  }

  return (
    <ul className="space-y-3">
      {alertas.map((alerta) => {
        const comAlvo = alerta.alvoItemId != null
        return (
          <li key={alerta.id}>
            <Alert
              variant={alerta.kind === "ERRO_DOC" ? "destructive" : "default"}
              // Alertas com alvo são clicáveis: rolam/focam o item na revisão.
              {...(comAlvo
                ? {
                    role: "button",
                    tabIndex: 0,
                    onClick: () => focarItem(alerta.alvoItemId as number),
                    onKeyDown: (e: React.KeyboardEvent) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault()
                        focarItem(alerta.alvoItemId as number)
                      }
                    },
                  }
                : {})}
              className={comAlvo ? "cursor-pointer" : undefined}
            >
              <div className="flex items-start justify-between gap-2">
                <AlertTitle>{alerta.titulo}</AlertTitle>
                {alerta.aviso && <Badge variant="warning">Aviso</Badge>}
              </div>
              <AlertDescription>{alerta.descricao}</AlertDescription>
            </Alert>
          </li>
        )
      })}
    </ul>
  )
}
