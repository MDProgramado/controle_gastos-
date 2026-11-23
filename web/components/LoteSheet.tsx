"use client"

import * as React from "react"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Sheet } from "@/components/ui/sheet"
import { SheetContent } from "@/components/ui/sheet"
import { SheetHeader } from "@/components/ui/sheet"
import { SheetTitle } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { Spinner } from "@/components/Spinner"
import { normalizarValorParaApi, validarValorLote } from "@/lib/money"
import type { ResultadoCriacaoLote } from "@/lib/lancamentos"
import { LinhaLote as LinhaLoteRow } from "@/components/LoteLinha"
import type {
  CampoLote,
  CategoriaOption,
  ErroLinha,
  LinhaLote,
} from "@/components/lote-tipos"

/** Valida um campo isolado de uma linha (usado no onBlur). */
export function validarCampo(
  linha: Pick<LinhaLote, "categoriaId" | "tipo" | "valor" | "data">,
  campo: CampoLote,
): string | null {
  switch (campo) {
    case "categoria":
      return linha.categoriaId ? null : "Selecione uma categoria"
    case "tipo":
      return linha.tipo ? null : "Selecione o tipo"
    case "valor":
      return validarValorLote(linha.valor)
    case "data":
      return linha.data ? null : "Informe a data"
  }
}

/** Valida a linha inteira (usado ao clicar em Salvar lote). */
export function validarLinha(
  linha: Pick<LinhaLote, "categoriaId" | "tipo" | "valor" | "data">,
): ErroLinha | null {
  const campos: CampoLote[] = ["categoria", "valor", "data", "tipo"]
  for (const campo of campos) {
    const mensagem = validarCampo(linha, campo)
    if (mensagem) return { campo, mensagem }
  }
  return null
}

interface LoteSheetProps {
  open: boolean
  onOpenChange: (aberto: boolean) => void
  onSubmit: (
    linhas: Array<{
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }>,
  ) => Promise<ResultadoCriacaoLote>
  categorias: CategoriaOption[]
}

export function LoteSheet({
  open,
  onOpenChange,
  onSubmit,
  categorias,
}: LoteSheetProps) {
  const [linhas, setLinhas] = React.useState<LinhaLote[]>([
    {
      id: crypto.randomUUID(),
      categoriaId: null,
      tipo: "ENTRADA",
      valor: "",
      data: new Date().toISOString().split("T")[0],
      descricao: "",
    },
  ])
  const [loading, setLoading] = React.useState(false)

  /**
   * Erro por linha e campo. Vive fora da linha para que a validacao
   * (onBlur e Salvar lote) escreva num unico lugar e o campo sozinho
   * exiba a mensagem.
   */
  const [erros, setErros] = React.useState<Record<string, ErroLinha>>({})
  const [erroGeral, setErroGeral] = React.useState<string | null>(null)

  const removerLinha = (id: string) => {
    setLinhas(linhas.filter((l) => l.id !== id))
    setErros((atuais) => {
      const copia = { ...atuais }
      delete copia[id]
      return copia
    })
  }

  const atualizarLinha = (
    id: string,
    atualizacoes: Partial<LinhaLote>,
  ) => {
    setLinhas(linhas.map((l) => (l.id === id ? { ...l, ...atualizacoes } : l)))
  }

  /**
   * Erro de uma linha: o primeiro campo invalido. Cada linha precisa de
   * categoria e valor preenchidos para o lote ser salvo.
   */
  const linhaInvalida = (l: LinhaLote) =>
    l.categoriaId === null || validarValorLote(l.valor) !== null

  const podeSalvar =
    linhas.length > 0 && linhas.every((l) => !linhaInvalida(l))

  /** Marca o erro de um campo quando ele perde o foco. */
  const validarNoBlur = (id: string, campo: CampoLote) => {
    const linha = linhas.find((l) => l.id === id)
    if (!linha) return
    const mensagem = validarCampo(linha, campo)
    setErros((atuais) => {
      const copia = { ...atuais }
      if (mensagem) {
        copia[id] = { campo, mensagem }
      } else if (atuais[id]?.campo === campo) {
        delete copia[id]
      }
      return copia
    })
  }

  /** Limpa o erro do campo quando o usuario o corrige. */
  const limparErroDoCampo = (id: string, campo: CampoLote) => {
    setErros((atuais) => {
      if (atuais[id]?.campo !== campo) return atuais
      const copia = { ...atuais }
      delete copia[id]
      return copia
    })
  }

  const adicionarLinha = () => {
    setLinhas([
      ...linhas,
      {
        id: crypto.randomUUID(),
        categoriaId: null,
        tipo: "ENTRADA",
        valor: "",
        data: new Date().toISOString().split("T")[0],
        descricao: "",
      },
    ])
  }

  async function handleSubmit() {
    const novosErros: Record<string, ErroLinha> = {}
    for (const l of linhas) {
      const erro = validarLinha(l)
      if (erro) novosErros[l.id] = erro
    }
    setErros(novosErros)
    setErroGeral(null)
    if (Object.keys(novosErros).length > 0) return

    const linhasParaApi: Array<{
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }> = []

    for (const l of linhas) {
      const valor = normalizarValorParaApi(l.valor)
      if (valor === null || l.categoriaId === null) {
        setErros((atuais) => ({
          ...atuais,
          [l.id]: { campo: "valor", mensagem: "Valor inválido" },
        }))
        return
      }
      linhasParaApi.push({
        categoriaId: l.categoriaId,
        tipo: l.tipo,
        valor,
        data: l.data,
        descricao: l.descricao || undefined,
      })
    }

    setLoading(true)
    try {
      const resultado = await onSubmit(linhasParaApi)
      if (!resultado.ok) {
        setErroGeral(resultado.mensagem)
        if (resultado.indiceLinha !== undefined) {
          const linha = linhas[resultado.indiceLinha]
          if (linha) {
            setErros((atuais) => ({
              ...atuais,
              [linha.id]: { campo: "valor", mensagem: resultado.mensagem },
            }))
          }
        }
        return
      }
      onOpenChange(false)
    } catch {
      setErroGeral("Não foi possível salvar o lote. Tente novamente.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="max-h-[90dvh] overflow-y-auto w-full md:max-w-[600px]">
        <SheetHeader>
          <SheetTitle>Lançar em lote</SheetTitle>
          {/* O X de fechar (canto superior direito) é o do SheetContent
              genérico (components/ui/sheet.tsx) — aqui só o título. */}
        </SheetHeader>
        <div className="flex-1 overflow-y-auto px-6 pb-6">
          <div className="flex flex-col gap-4">
            <div className="flex-1">
              <Label htmlFor="categoria-filtro">Filtrar categorias por tipo</Label>
              <div className="mt-1">
                <Select
                  value=""
                  onValueChange={() => {}}
                  disabled={loading}
                >
                  <SelectTrigger
                    className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
                  >
                    <SelectValue placeholder="Todas as categorias" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ENTRADA">Entrada</SelectItem>
                    <SelectItem value="SAIDA">Saída</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="flex-1">
              <Label htmlFor="lote-linhas">Linhas do lote</Label>
              <div className="mt-1 flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={adicionarLinha}
                  disabled={loading}
                  className="h-10"
                >
                  Adicionar linha
                </Button>
              </div>
              {/* Cada linha e um card unico (a borda dupla do wrapper +
                  a borda da linha sairam). */}
              <div className="mt-3 flex flex-col gap-3">
                {linhas.length === 0 ? (
                  <div className="rounded-lg border border-input p-4 text-center text-sm text-muted-foreground">
                    Nenhuma linha no lote. Adicione pelo menos uma.
                  </div>
                ) : (
                  linhas.map((linha) => (
                    <LinhaLoteRow
                      key={linha.id}
                      linha={linha}
                      categorias={categorias}
                      onChange={atualizarLinha}
                      onRemove={removerLinha}
                      onBlurCampo={validarNoBlur}
                      onCorrigirCampo={limparErroDoCampo}
                      erro={erros[linha.id]}
                      loading={loading}
                    />
                  ))
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Rodape fixo no fim do conteudo */}
        <div className="border-t border-border px-6 py-4">
          {erroGeral ? (
            <Alert variant="destructive" role="alert" className="mb-3">
              <AlertDescription>{erroGeral}</AlertDescription>
            </Alert>
          ) : null}
          {!podeSalvar && !loading ? (
            <p className="mb-2 text-sm text-muted-foreground">
              Preencha categoria e valor em todas as linhas.
            </p>
          ) : null}
          {loading ? (
            <div className="flex items-center gap-2">
              <Spinner className="mr-2" />
              <span>Processando lote...</span>
            </div>
          ) : (
            // aria-disabled (e nao disabled) para o botao continuar
            // clicavel: o clique dispara a validacao e mostra o erro no
            // campo, que e o que o requisito pede. Visualmente continua
            // desabilitado.
            <Button
              type="button"
              variant="default"
              onClick={handleSubmit}
              disabled={loading}
              aria-disabled={!podeSalvar}
              className="h-11 w-full md:w-auto aria-disabled:opacity-50 aria-disabled:cursor-not-allowed"
            >
              Salvar lote ({linhas.length} linha{linhas.length !== 1 ? "s" : ""})
            </Button>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
