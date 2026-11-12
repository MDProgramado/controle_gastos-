"use client"

import * as React from "react"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { X } from "lucide-react"
import type {
  CampoLote,
  CategoriaOption,
  ErroLinha,
  LinhaLote,
  TipoLancamento,
} from "@/components/lote-tipos"

interface LinhaLoteProps {
  linha: LinhaLote
  categorias: CategoriaOption[]
  onChange: (id: string, atualizacoes: Partial<LinhaLote>) => void
  onRemove: (id: string) => void
  /** Valida o campo quando ele perde o foco. */
  onBlurCampo: (id: string, campo: CampoLote) => void
  /** Limpa o erro do campo quando o usuario corrige. */
  onCorrigirCampo: (id: string, campo: CampoLote) => void
  /** Erro vigente da linha (com o campo que falhou). */
  erro?: ErroLinha
  loading: boolean
}

const CLASS_CAMPO =
  "h-11 w-full rounded-lg border border-input bg-transparent px-3 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"

export function LinhaLote({
  linha,
  categorias,
  onChange,
  onRemove,
  onBlurCampo,
  onCorrigirCampo,
  erro,
  loading,
}: LinhaLoteProps) {
  const categoriasDisponiveis = categorias.filter(
    (c) => c.tipo === linha.tipo && c.ativo,
  )

  // Mensagem de erro do campo (o erro da linha aponta o campo que falhou).
  const mensagemDe = (campo: CampoLote) =>
    erro?.campo === campo ? erro.mensagem : null

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-input bg-card p-4">
      {/* Categoria e Tipo lado a lado no md+ */}
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <div className="flex flex-col gap-1">
          <Label htmlFor={`categoria-${linha.id}`}>Categoria</Label>
          <Select
            value={linha.categoriaId?.toString() ?? ""}
            onValueChange={(valor) => {
              onCorrigirCampo(linha.id, "categoria")
              onChange(linha.id, {
                categoriaId: valor ? parseInt(valor) : null,
              })
            }}
            disabled={loading}
          >
            <SelectTrigger
              id={`categoria-${linha.id}`}
              onBlur={() => onBlurCampo(linha.id, "categoria")}
              className={CLASS_CAMPO}
              aria-invalid={mensagemDe("categoria") ? true : undefined}
              aria-describedby={
                mensagemDe("categoria") ? `erro-categoria-${linha.id}` : undefined
              }
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
          {mensagemDe("categoria") && (
            <p
              id={`erro-categoria-${linha.id}`}
              className="text-sm text-destructive"
            >
              {mensagemDe("categoria")}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <Label htmlFor={`tipo-${linha.id}`}>Tipo</Label>
          <Select
            value={linha.tipo}
            onValueChange={(valor) => {
              onCorrigirCampo(linha.id, "tipo")
              onChange(linha.id, {
                tipo: valor as TipoLancamento,
              })
            }}
            disabled={loading}
          >
            <SelectTrigger
              id={`tipo-${linha.id}`}
              onBlur={() => onBlurCampo(linha.id, "tipo")}
              className={CLASS_CAMPO}
              aria-invalid={mensagemDe("tipo") ? true : undefined}
              aria-describedby={
                mensagemDe("tipo") ? `erro-tipo-${linha.id}` : undefined
              }
            >
              <SelectValue placeholder={linha.tipo} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ENTRADA">Entrada</SelectItem>
              <SelectItem value="SAIDA">Saída</SelectItem>
            </SelectContent>
          </Select>
          {mensagemDe("tipo") && (
            <p id={`erro-tipo-${linha.id}`} className="text-sm text-destructive">
              {mensagemDe("tipo")}
            </p>
          )}
        </div>
      </div>

      {/* Valor e Data lado a lado no md+ */}
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <div className="flex flex-col gap-1">
          <Label htmlFor={`valor-${linha.id}`}>Valor</Label>
          <Input
            id={`valor-${linha.id}`}
            name={`valor-${linha.id}`}
            type="text"
            inputMode="decimal"
            placeholder="0,00"
            value={linha.valor}
            onChange={(e) => {
              onCorrigirCampo(linha.id, "valor")
              onChange(linha.id, {
                valor: e.target.value,
              })
            }}
            onBlur={() => onBlurCampo(linha.id, "valor")}
            disabled={loading}
            className={CLASS_CAMPO}
            aria-invalid={mensagemDe("valor") ? true : undefined}
            aria-describedby={
              mensagemDe("valor") ? `erro-valor-${linha.id}` : undefined
            }
          />
          {mensagemDe("valor") && (
            <p id={`erro-valor-${linha.id}`} className="text-sm text-destructive">
              {mensagemDe("valor")}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <Label htmlFor={`data-${linha.id}`}>Data</Label>
          <Input
            id={`data-${linha.id}`}
            name={`data-${linha.id}`}
            type="date"
            value={linha.data}
            onChange={(e) => {
              onCorrigirCampo(linha.id, "data")
              onChange(linha.id, {
                data: e.target.value,
              })
            }}
            onBlur={() => onBlurCampo(linha.id, "data")}
            disabled={loading}
            className={CLASS_CAMPO}
            aria-invalid={mensagemDe("data") ? true : undefined}
            aria-describedby={
              mensagemDe("data") ? `erro-data-${linha.id}` : undefined
            }
          />
          {mensagemDe("data") && (
            <p id={`erro-data-${linha.id}`} className="text-sm text-destructive">
              {mensagemDe("data")}
            </p>
          )}
        </div>
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
            })
          }
          disabled={loading}
          className={CLASS_CAMPO}
        />
      </div>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => onRemove(linha.id)}
          disabled={loading}
          className="flex h-10 items-center gap-1 rounded-lg px-2 text-sm text-destructive hover:bg-destructive/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
          <X className="h-4 w-4" />
          Remover linha
        </button>
      </div>
    </div>
  )
}
