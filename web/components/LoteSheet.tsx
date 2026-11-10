"use client"

import * as React from "react"
import { Sheet } from "@/components/ui/sheet"
import { SheetTrigger } from "@/components/ui/sheet"
import { SheetClose } from "@/components/ui/sheet"
import { SheetContent } from "@/components/ui/sheet"
import { SheetHeader } from "@/components/ui/sheet"
import { SheetFooter } from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { Spinner } from "@/components/Spinner"
import type { LinhaLote } from "@/components/LoteLinha"
import { LinhaLote as LinhaLoteRow } from "@/components/LoteLinha"

type TipoLancamento = "ENTRADA" | "SAIDA"

type CategoriaOption = {
  id: number
  nome: string
  tipo: string
  pessoal: boolean
  ativo: boolean
}

type LinhaLote = {
  id: string
  categoriaId: number | null
  tipo: TipoLancamento
  valor: string
  data: string
  descricao: string
  erro?: string
}

interface LoteSheetProps {
  onClose: () => void
  onSubmit: (
    linhas: Array<{
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }>,
  ) => Promise<boolean>
  categorias: CategoriaOption[]
}

export function LoteSheet({
  onClose,
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

  const removerLinha = (id: string) => {
    setLinhas(linhas.filter((l) => l.id !== id))
  }

  const atualizarLinha = (
    id: string,
    atualizacoes: Partial<LinhaLote>,
  ) => {
    setLinhas(linhas.map((l) => (l.id === id ? { ...l, ...atualizacoes } : l)))
  }

  const podeSalvar = linhas.some(
    (l) => l.categoriaId !== null && l.valor && l.tipo && l.data,
  )

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
    const novasLinhas = linhas.map((l) => {
      let erro: string | undefined
      if (!l.categoriaId) erro = "Selecione uma categoria"
      else if (!l.valor || !/^-?\d+(\.\d{1,2})?$/.test(l.valor))
        erro = "Valor inválido"
      else if (parseFloat(l.valor) <= 0) erro = "Valor deve ser maior que zero"
      else if (!l.data) erro = "Informe a data"
      else if (!l.tipo) erro = "Selecione o tipo"
      return { ...l, erro }
    })
    setLinhas(novasLinhas)
    if (novasLinhas.some((l) => l.erro)) return

    setLoading(true)
    try {
      const resultado = await onSubmit(
        linhas.map((l) => ({
          categoriaId: l.categoriaId!,
          tipo: l.tipo,
          valor: l.valor,
          data: l.data,
          descricao: l.descricao || undefined,
        })),
      )
      if (resultado) {
        onClose()
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Sheet>
      <SheetTrigger asChild>
        <button className="h-11 px-4 py-2 rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50">
          Lançar em lote
        </button>
      </SheetTrigger>
      <SheetContent side="right" className="w-[600px] max-w-[95vw]">
        <SheetHeader>
          <SheetClose asChild>
            <button className="p-2 rounded-lg text-muted-foreground hover:bg-accent/50">
              <span className="sr-only">Fechar</span>
            </button>
          </SheetClose>
        </SheetHeader>
        <SheetFooter className="p-6">
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
                  size="sm"
                  onClick={adicionarLinha}
                  disabled={loading}
                >
                  Adicionar linha
                </Button>
              </div>
              <div className="mt-1 flex flex-col gap-2 max-h-[400px] overflow-y-auto border border-input rounded-lg p-3 bg-muted/50">
                {linhas.length === 0 ? (
                  <div className="p-3 text-sm text-muted-foreground text-center">
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
                      loading={loading}
                    />
                  ))
                )}
              </div>
              {loading ? (
                <div className="flex items-center gap-2">
                  <Spinner className="mr-2" />
                  <span>Processando lote...</span>
                </div>
              ) : (
                <Button
                  type="button"
                  variant="default"
                  onClick={handleSubmit}
                  disabled={loading || !podeSalvar}
                  className="w-full"
                >
                  Salvar lote ({linhas.length} linha{linhas.length !== 1 ? "s" : ""})
                </Button>
              )}
            </div>
          </div>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}
