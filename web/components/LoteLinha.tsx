"use client"

import * as React from "react"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"

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

interface LinhaLoteProps {
  linha: LinhaLote
  categorias: CategoriaOption[]
  onChange: (id: string, atualizacoes: Partial<LinhaLote>) => void
  onRemove: (id: string) => void
  loading: boolean
}

export function LinhaLote({
  linha,
  categorias,
  onChange,
  onRemove,
  loading,
}: LinhaLoteProps) {
  const categoriasDisponiveis = categorias.filter(
    (c) => c.tipo === linha.tipo && c.ativo,
  )

  return (
    <div className="flex flex-col gap-2 rounded-lg border border-input bg-card p-3">
      <div className="flex flex-col gap-1">
        <Label htmlFor={`categoria-${linha.id}`}>Categoria</Label>
        <Select
          value={linha.categoriaId?.toString() ?? ""}
          onValueChange={(valor) =>
            onChange(linha.id, {
              categoriaId: valor ? parseInt(valor) : null,
            })}
          disabled={loading}
        >
          <SelectTrigger
            className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
          >
            <SelectValue placeholder="Selecione uma categoria" />
          </SelectTrigger>
          <SelectContent>
            {categoriasDisponiveis.map((c) => (
              <SelectItem key={c.id} value={c.id.toString()}>
                {c.nome}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {linha.erro && (
          <p className="text-xs text-destructive mt-1">{linha.erro}</p>
        )}
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`tipo-${linha.id}`}>Tipo</Label>
        <Select
          value={linha.tipo}
          onValueChange={(valor) =>
            onChange(linha.id, {
              tipo: valor as TipoLancamento,
            })}
          disabled={loading}
        >
          <SelectTrigger
            id={`tipo-${linha.id}`}
            className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
          >
            <SelectValue placeholder={linha.tipo} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ENTRADA">Entrada</SelectItem>
            <SelectItem value="SAIDA">Saída</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`valor-${linha.id}`}>Valor</Label>
        <Input
          id={`valor-${linha.id}`}
          name={`valor-${linha.id}`}
          type="text"
          placeholder="0,00"
          value={linha.valor}
          onChange={(e) =>
            onChange(linha.id, {
              valor: e.target.value,
            })}
          disabled={loading}
          className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
        />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`data-${linha.id}`}>Data</Label>
        <Input
          id={`data-${linha.id}`}
          name={`data-${linha.id}`}
          type="date"
          value={linha.data}
          onChange={(e) =>
            onChange(linha.id, {
              data: e.target.value,
            })}
          disabled={loading}
          className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
        />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`descricao-${linha.id}`}>Descrição</Label>
        <Input
          id={`descricao-${linha.id}`}
          name={`descricao-${linha.id}`}
          type="text"
          placeholder="Descrição opcional"
          value={linha.descricao}
          onChange={(e) =>
            onChange(linha.id, {
              descricao: e.target.value,
            })}
          disabled={loading}
          className="h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
        />
      </div>
      <button
        type="button"
        onClick={() => onRemove(linha.id)}
        className="text-xs text-destructive hover:bg-destructive/10 rounded-lg px-2 py-1"
      >
        Remover linha
      </button>
    </div>
  )
}
