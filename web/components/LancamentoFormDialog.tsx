"use client"

import * as React from "react"
import { Dialog } from "@/components/ui/dialog"
import { DialogTrigger } from "@/components/ui/dialog"
import { DialogContent } from "@/components/ui/dialog"
import { DialogHeader } from "@/components/ui/dialog"
import { DialogTitle } from "@/components/ui/dialog"
import { DialogDescription } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { Spinner } from "@/components/Spinner"

type TipoLancamento = "ENTRADA" | "SAIDA"

type CategoriaOption = {
  id: number
  nome: string
  tipo: string
  pessoal: boolean
  ativo: boolean
}

type LancamentoFormDialogProps = {
  isOpen: boolean
  onClose: () => void
  onSubmit: (
    request: {
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    },
  ) => Promise<boolean>
  categorias: CategoriaOption[]
  modo: "criar" | "editar"
  lancamento?: {
    id: number
    categoriaId: number
    tipo: string
    valor: number
    data: string
    descricao: string
  }
}

const VALOR_RE = /^-?\d+(\.\d{1,2})?$/

export function LancamentoFormDialog({
  isOpen,
  onClose,
  onSubmit,
  categorias,
  modo,
  lancamento,
}: LancamentoFormDialogProps) {
  const [categoriaId, setCategoriaId] = React.useState<number | null>(
    lancamento?.categoriaId ?? null,
  )
  const [tipo, setTipo] = React.useState<TipoLancamento>(
    (lancamento?.tipo || "ENTRADA") as TipoLancamento,
  )
  const [valor, setValor] = React.useState(lancamento?.valor.toString() ?? "")
  const [data, setData] = React.useState(lancamento?.data ?? "")
  const [descricao, setDescricao] = React.useState(lancamento?.descricao ?? "")
  const [errosForm, setErrosForm] = React.useState<{
    categoriaId?: string
    tipo?: string
    valor?: string
    data?: string
  }>({})
  const [loading, setLoading] = React.useState(false)

  const chaveDialog = lancamento ? `editar-${lancamento.id}` : "novo"

  const categoriasFiltradas = categorias.filter(
    (c) => c.tipo === tipo && c.ativo,
  )

  function validarForm(): boolean {
    const novos: {
      categoriaId?: string
      tipo?: string
      valor?: string
      data?: string
    } = {}
    if (!categoriaId) novos.categoriaId = "Selecione uma categoria."
    if (!tipo) novos.tipo = "Selecione o tipo (entrada ou saída)."
    if (!data) novos.data = "Informe a data."
    else if (isNaN(new Date(data).getTime())) novos.data = "Informe uma data válida."
    if (!valor) {
      novos.valor = "Informe o valor."
    } else {
      const v = parseFloat(valor)
      if (!VALOR_RE.test(valor)) {
        novos.valor = "O valor aceita no máximo 2 casas decimais."
      } else if (v <= 0) {
        novos.valor = "O valor deve ser maior que zero."
      }
    }
    setErrosForm(novos)
    return Object.keys(novos).length === 0
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!validarForm()) return

    setLoading(true)
    try {
      const resultado = await onSubmit({
        categoriaId: categoriaId!,
        tipo,
        valor,
        data,
        descricao: descricao || undefined,
      })
      if (resultado) {
        onClose()
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogTrigger asChild>
        <button style={{ display: "none" }} aria-hidden="true" />
      </DialogTrigger>
      <DialogContent
        key={chaveDialog}
        className="w-full md:w-[480px] max-w-[95vw]"
      >
        <DialogHeader>
          <DialogTitle>
            {modo === "editar" ? "Editar lançamento" : "Novo lançamento"}
          </DialogTitle>
        </DialogHeader>
        <DialogDescription>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <Label htmlFor="categoria">Categoria</Label>
              <Select
                value={categoriaId?.toString() ?? ""}
                onValueChange={(valor) =>
                  setCategoriaId(valor ? parseInt(valor) : null)}
                disabled={loading}
                aria-invalid={errosForm.categoriaId ? true : undefined}
              >
                <SelectTrigger
                  className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
                >
                  <SelectValue placeholder="Selecione uma categoria" />
                </SelectTrigger>
                <SelectContent>
                  {categoriasFiltradas.map((c) => (
                    <SelectItem key={c.id} value={c.id.toString()}>
                      {c.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errosForm.categoriaId && (
                <p className="text-sm text-destructive mt-1">
                  {errosForm.categoriaId}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="tipo">Tipo</Label>
              <select
                id="tipo"
                name="tipo"
                value={tipo}
                onChange={(e) => setTipo(e.target.value as TipoLancamento)}
                disabled={loading}
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
                aria-invalid={errosForm.tipo ? true : undefined}
              >
                <option value="ENTRADA">Entrada</option>
                <option value="SAIDA">Saída</option>
              </select>
              {errosForm.tipo && (
                <p className="text-sm text-destructive mt-1">
                  {errosForm.tipo}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="valor">Valor</Label>
              <Input
                id="valor"
                name="valor"
                type="text"
                placeholder="0,00"
                value={valor}
                onChange={(e) => {
                  const v = e.target.value.replace(",", ".")
                  setValor(v)
                  if (errosForm.valor)
                    setErrosForm((atual) => ({ ...atual, valor: undefined }))
                }}
                disabled={loading}
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
                aria-invalid={errosForm.valor ? true : undefined}
              />
              {errosForm.valor && (
                <p className="text-sm text-destructive mt-1">
                  {errosForm.valor}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="data">Data</Label>
              <Input
                id="data"
                name="data"
                type="date"
                value={data}
                onChange={(e) => {
                  setData(e.target.value)
                  if (errosForm.data)
                    setErrosForm((atual) => ({ ...atual, data: undefined }))
                }}
                disabled={loading}
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
                aria-invalid={errosForm.data ? true : undefined}
              />
              {errosForm.data && (
                <p className="text-sm text-destructive mt-1">
                  {errosForm.data}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="descricao">Descrição</Label>
              <Input
                id="descricao"
                name="descricao"
                type="text"
                placeholder="Descrição do lançamento"
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                disabled={loading}
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
              />
            </div>

            <div className="flex items-center justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                variant="default"
                disabled={loading}
                className="h-11 w-auto font-semibold"
              >
                {loading ? (
                  <span className="flex items-center gap-1">
                    <Spinner className="mr-1" />
                    Salvando…
                  </span>
                ) : modo === "editar" ? "Salvar alterações" : "Salvar"
                }
              </Button>
            </div>
          </form>
        </DialogDescription>
      </DialogContent>
    </Dialog>
  )
}
