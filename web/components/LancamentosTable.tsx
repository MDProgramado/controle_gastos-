"use client"

import * as React from "react"
import { Input } from "@/components/ui/input"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Label } from "@/components/ui/label"
import { Pencil, Trash2 } from "lucide-react"
import { MONETARY } from "@/components/DashboardKPIs"

type LancamentoData = {
  id: number
  tipo: string
  valor: number
  data: string
  descricao: string
  categoriaNome: string
  categoriaTipo: string
}

type CategoriaOption = {
  id: number
  nome: string
  tipo: string
  pessoal: boolean
  ativo: boolean
}

type LancamentosTableProps = {
  lancamentos: LancamentoData[]
  total: number
  categorias: CategoriaOption[]
  ano: number | null
  mes: number | null
  tipo: string | null
  categoriaId: number | null
  busca: string
  onChangeFiltros: (f: { ano?: number | null; mes?: number | null; tipo?: string | null; categoriaId?: number | null; busca?: string }) => void
  onPageChange: (pagina: number) => void
  loading: boolean
  error: string | null
  onRetry: () => void
  onEditar: (l: LancamentoData) => void
  onExcluir: (id: number) => void
}

export function LancamentosTable({
  lancamentos,
  total,
  categorias,
  ano,
  mes,
  tipo,
  categoriaId,
  busca,
  onChangeFiltros,
  onPageChange,
  loading,
  error,
  onRetry,
  onEditar,
  onExcluir,
}: LancamentosTableProps) {
  const PAGE_SIZE = 10
  const totalPaginas = Math.max(1, Math.ceil(total / PAGE_SIZE))
  const paginaAtual = 1
  const paginados = lancamentos.slice(0, PAGE_SIZE)

  if (loading) {
    return (
      <div className="rounded-lg border border-input bg-card shadow-sm">
        <div className="p-6 flex flex-col gap-4">
          <div className="h-8 w-1/3 rounded-md bg-muted animate-pulse" />
          <div className="h-8 w-1/2 rounded-md bg-muted animate-pulse" />
          <div className="h-8 w-3/4 rounded-md bg-muted animate-pulse" />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-lg border border-input bg-card shadow-sm p-6">
        <Alert variant="destructive" role="alert" className="mb-4">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
        <Button variant="outline" onClick={onRetry}>
          Tentar novamente
        </Button>
      </div>
    )
  }

  if (lancamentos.length === 0) {
    return (
      <div className="rounded-lg border border-input bg-card shadow-sm">
        <div className="p-12 text-center">
          <p className="text-sm text-muted-foreground">
            Nenhum lançamento neste período
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="flex-1">
          <Label htmlFor="ano-mes">Mês</Label>
          <div className="mt-1">
            <Input
              id="ano-mes"
              type="month"
              value={`${ano?.toString().padStart(4, "0")}-${mes?.toString().padStart(2, "0")}`}
              onChange={(e) => {
                const [a, m] = e.target.value.split("-")
                onChangeFiltros({ ano: a ? parseInt(a) : null, mes: m ? parseInt(m) : null })
              }}
              disabled={loading}
              className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
            />
          </div>
        </div>
        <div className="flex-1">
          <Label htmlFor="tipo-filtro">Tipo</Label>
          <div className="mt-1">
            <Select
              value={tipo ?? ""}
              onValueChange={(valor) => onChangeFiltros({ tipo: valor || null })}
              disabled={loading}
            >
              <SelectTrigger
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
              >
                <SelectValue placeholder="Todos os tipos" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ENTRADA">Entrada</SelectItem>
                <SelectItem value="SAIDA">Saída</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <div className="flex-1">
          <Label htmlFor="categoria-filtro">Categoria</Label>
          <div className="mt-1">
            <Select
              value={categoriaId?.toString() ?? ""}
              onValueChange={(valor) => onChangeFiltros({ categoriaId: valor ? parseInt(valor) : null })}
              disabled={loading}
            >
              <SelectTrigger
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
              >
                <SelectValue placeholder="Todas as categorias" />
              </SelectTrigger>
              <SelectContent>
                {categorias.map((c) => (
                  <SelectItem key={c.id} value={c.id.toString()}>
                    {c.nome}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        <div className="flex-1">
          <Label htmlFor="busca">Descrição</Label>
          <div className="mt-1">
            <Input
              id="busca"
              type="text"
              placeholder="Buscar por descrição..."
              value={busca}
              onChange={(e) => onChangeFiltros({ busca: e.target.value || undefined })}
              disabled={loading}
              className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
            />
          </div>
        </div>
      </div>

      <div className="rounded-lg border border-input bg-card shadow-sm">
        {/* Mobile: a tabela vira lista de cards (a coluna Ações era cortada).
            No md+ a tabela de verdade, com scroll horizontal se precisar. */}
        <div className="divide-y divide-border md:hidden">
          {paginados.map((l) => renderCard(l, onEditar, onExcluir, loading))}
        </div>

        <div className="hidden md:block">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead>
                <tr className="border-b bg-muted text-muted-foreground">
                  <th className="p-4">Data</th>
                  <th className="p-4">Descrição</th>
                  <th className="p-4">Categoria</th>
                  <th className="p-4">Tipo</th>
                  <th className="p-4 text-right">Valor</th>
                  <th className="p-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {paginados.map((l) => renderRow(l, onEditar, onExcluir, loading))}
              </tbody>
            </table>
          </div>
        </div>
        <div className="border-t border-border px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex-1 text-sm text-muted-foreground">
            Exibindo {Math.min(total, PAGE_SIZE)} de {total} lançamentos
          </div>
          <div className="flex flex-col sm:flex-row gap-2">
            <Button variant="outline" size="sm" disabled={paginaAtual <= 1 || loading} onClick={() => onPageChange(paginaAtual - 1)} className="h-10">
              Anterior
            </Button>
            <Button variant="outline" size="sm" disabled={paginaAtual >= totalPaginas || loading} onClick={() => onPageChange(paginaAtual + 1)} className="h-10">
              Próximo
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}

/** Badge de tipo (Entrada/Saída) — compartilhado pela linha e pelo card. */
function TipoBadge({ tipo }: { tipo: string }) {
  const ehEntrada = tipo === "ENTRADA"
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs font-semibold ${
        ehEntrada
          ? "border-positive text-positive bg-positive/10"
          : "border-destructive text-destructive bg-destructive/10"
      }`}
    >
      {tipo}
    </span>
  )
}

/**
 * Card do lançamento no mobile: Descrição em destaque, Categoria e Tipo
 * numa linha e as ações (Editar / Excluir) com alvos de 40px.
 */
function renderCard(
  l: LancamentoData,
  onEditar: (l: LancamentoData) => void,
  onExcluir: (id: number) => void,
  loading: boolean,
) {
  const ehEntrada = l.tipo === "ENTRADA"
  const categoriaNome = l.categoriaNome || "—"
  return (
    <div key={l.id} data-lancamento-card className="flex flex-col gap-3 p-4">
      <div className="min-w-0">
        <p className="break-words font-medium text-foreground">
          {l.descricao || "—"}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          {l.data} · {categoriaNome}
        </p>
      </div>

      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <TipoBadge tipo={l.tipo} />
          <span
            className={`font-semibold tabular-nums ${
              ehEntrada ? "text-positive" : "text-destructive"
            }`}
          >
            {ehEntrada ? "+" : "-"}
            {MONETARY.format(Math.abs(l.valor))}
          </span>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onEditar(l)}
            disabled={loading}
            className="h-10 w-10 p-0"
            aria-label="Editar lançamento"
          >
            <Pencil className="h-4 w-4" />
          </Button>
          <Button
            type="button"
            variant="destructive"
            size="sm"
            onClick={() => onExcluir(l.id)}
            disabled={loading}
            className="h-10 w-10 p-0"
            aria-label="Excluir lançamento"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}

function renderRow(
  l: LancamentoData,
  onEditar: (l: LancamentoData) => void,
  onExcluir: (id: number) => void,
  loading: boolean,
) {
  const ehEntrada = l.tipo === "ENTRADA"
  const categoriaNome = l.categoriaNome || "—"
  return (
    <tr key={l.id} className="hover:bg-muted/50 transition-colors">
      <td className="p-4">{l.data}</td>
      <td className="p-4 break-words">{l.descricao || "—"}</td>
      <td className="p-4 break-words">{categoriaNome}</td>
      <td className="p-4">
        <TipoBadge tipo={l.tipo} />
      </td>
      <td className="p-4 text-right font-semibold">
        <span className={ehEntrada ? "text-positive" : "text-destructive"}>
          {ehEntrada ? "+" : "-"}
          {MONETARY.format(Math.abs(l.valor))}
        </span>
      </td>
      <td className="p-4 text-right">
        <div className="flex items-center justify-end gap-2">
          <Button type="button" variant="outline" size="sm" onClick={() => onEditar(l)} disabled={loading} className="h-10 px-2 text-xs">
            Editar
          </Button>
          <Button type="button" variant="destructive" size="sm" onClick={() => onExcluir(l.id)} disabled={loading} className="h-10 px-2 text-xs">
            Excluir
          </Button>
        </div>
      </td>
    </tr>
  )
}
