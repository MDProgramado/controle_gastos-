"use client"

import * as React from "react"
import { Badge } from "@/components/ui/badge"
import { Checkbox } from "@/components/ui/checkbox"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { MONETARY } from "@/components/DashboardKPIs"
import { cn } from "@/lib/utils"
import {
  centavos,
  formatarDataIso,
  itemTemTag,
  rotuloOrigem,
  somarCentavos,
  type ItemRevisao,
  type SelecaoItem,
} from "@/components/importacoes/ImportacaoTipos"

/** Categoria do tenant (mesma shape de `LancamentosTable`). */
export type CategoriaOption = {
  id: number
  nome: string
  tipo: string
  pessoal: boolean
  ativo: boolean
}

type RevisaoItensProps = {
  itens: ItemRevisao[]
  selecao: Record<number, SelecaoItem>
  categorias: CategoriaOption[]
  onToggleItem: (itemId: number, incluir: boolean) => void
  onCategoriaChange: (itemId: number, categoriaId: number | null) => void
  disabled?: boolean
}

const GRUPOS = [
  {
    tipo: "RECEITA",
    titulo: "Receitas",
    badgeVariant: "success" as const,
    tipoCategoria: "ENTRADA",
    corValor: "text-positive",
    corSubtotal: "text-positive",
  },
  {
    tipo: "DESPESA_PAGA",
    titulo: "Despesas pagas",
    badgeVariant: "destructive" as const,
    tipoCategoria: "SAIDA",
    corValor: "text-destructive",
    corSubtotal: "text-destructive",
  },
  {
    tipo: "A_PAGAR",
    titulo: "Contas a pagar",
    badgeVariant: "outline" as const,
    tipoCategoria: "SAIDA",
    corValor: "text-foreground",
    corSubtotal: "text-foreground",
  },
] as const

/**
 * Itens para revisar (05-UI-SPEC §6.2): 3 grupos (Receitas / Despesas pagas /
 * Contas a pagar), linha de tabela densa no md+ (padrão `LancamentosTable`) e
 * bloco empilhado no mobile. Item desmarcado fica com `bg-muted/30` +
 * `opacity-60` — nunca sai da lista (permite religar).
 */
export function RevisaoItens({
  itens,
  selecao,
  categorias,
  onToggleItem,
  onCategoriaChange,
  disabled = false,
}: RevisaoItensProps) {
  if (itens.length === 0) {
    return (
      <div className="rounded-lg border border-border bg-card p-6">
        <p className="text-sm font-medium text-foreground">
          Nenhum lançamento identificado.
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          A IA não encontrou transações nos PDFs enviados. Confira se o arquivo
          tem texto selecionável e tente de novo.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {GRUPOS.map((grupo) => (
        <GrupoItens
          key={grupo.tipo}
          grupo={grupo}
          itens={itens}
          selecao={selecao}
          categorias={categorias}
          onToggleItem={onToggleItem}
          onCategoriaChange={onCategoriaChange}
          disabled={disabled}
        />
      ))}
    </div>
  )
}

type Grupo = (typeof GRUPOS)[number]

type GrupoItensProps = {
  grupo: Grupo
  itens: ItemRevisao[]
  selecao: Record<number, SelecaoItem>
  categorias: CategoriaOption[]
  onToggleItem: (itemId: number, incluir: boolean) => void
  onCategoriaChange: (itemId: number, categoriaId: number | null) => void
  disabled: boolean
}

function GrupoItens({
  grupo,
  itens,
  selecao,
  categorias,
  onToggleItem,
  onCategoriaChange,
  disabled,
}: GrupoItensProps) {
  const itensGrupo = React.useMemo(
    () => itens.filter((i) => i.tipo === grupo.tipo),
    [itens, grupo.tipo],
  )
  // Categorias do tenant compatíveis com o tipo do item (ENTRADA/SAIDA).
  const categoriasDoGrupo = React.useMemo(
    () => categorias.filter((c) => c.tipo === grupo.tipoCategoria),
    [categorias, grupo.tipoCategoria],
  )

  if (itensGrupo.length === 0) return null

  // Subtotal do grupo: soma dos itens MARCADOS (centavos inteiros — sem drift).
  const subtotalCentavos = somarCentavos(
    itensGrupo
      .filter((i) => selecao[i.id]?.incluir)
      .map((i) => i.valor),
  )

  return (
    <div className="rounded-lg border border-border bg-card p-4 md:p-6">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h3 className="text-base font-semibold text-foreground">{grupo.titulo}</h3>
          <Badge variant={grupo.badgeVariant}>{itensGrupo.length}</Badge>
        </div>
        <span className={cn("text-sm tabular-nums", grupo.corSubtotal)}>
          {MONETARY.format(centavos(subtotalCentavos))}
        </span>
      </div>

      {/* Desktop (md+): linha de tabela densa */}
      <table className="mt-4 hidden w-full text-left md:table">
        <thead>
          <tr className="border-b border-border">
            <th scope="col" className="sr-only">Incluir</th>
            <th scope="col" className="pb-2 pr-2 text-sm font-normal text-muted-foreground">Data</th>
            <th scope="col" className="pb-2 pr-2 text-sm font-normal text-muted-foreground">Descrição</th>
            <th scope="col" className="pb-2 pr-2 text-sm font-normal text-muted-foreground">Categoria</th>
            <th scope="col" className="pb-2 text-right text-sm font-normal text-muted-foreground">Valor</th>
          </tr>
        </thead>
        <tbody>
          {itensGrupo.map((item) => (
            <LinhaTabela
              key={item.id}
              item={item}
              selecao={selecao[item.id]}
              categoriasDoGrupo={categoriasDoGrupo}
              onToggleItem={onToggleItem}
              onCategoriaChange={onCategoriaChange}
              disabled={disabled}
              grupo={grupo}
            />
          ))}
        </tbody>
      </table>

      {/* Mobile: bloco empilhado por item (sem overflow horizontal) */}
      <div className="mt-2 md:hidden">
        {itensGrupo.map((item) => (
          <BlocoMobile
            key={item.id}
            item={item}
            selecao={selecao[item.id]}
            categoriasDoGrupo={categoriasDoGrupo}
            onToggleItem={onToggleItem}
            onCategoriaChange={onCategoriaChange}
            disabled={disabled}
            grupo={grupo}
          />
        ))}
      </div>
    </div>
  )
}

/* ============================ linha / bloco ============================ */

type LinhaProps = {
  item: ItemRevisao
  selecao: SelecaoItem | undefined
  categoriasDoGrupo: CategoriaOption[]
  onToggleItem: (itemId: number, incluir: boolean) => void
  onCategoriaChange: (itemId: number, categoriaId: number | null) => void
  disabled: boolean
  grupo: Grupo
}

function estadoDoItem(item: ItemRevisao, selecao: SelecaoItem | undefined) {
  const incluir = selecao?.incluir ?? false
  const descricao = item.descricao ?? "Item sem descrição"
  const badges = (
    <div className="mt-1 flex flex-wrap items-center gap-1.5">
      <Badge variant="outline">{rotuloOrigem(item.origem)}</Badge>
      {item.tipo === "A_PAGAR" && item.parcelaAtual != null && item.parcelaTotal != null && (
        <Badge variant="outline">
          Parcela {item.parcelaAtual}/{item.parcelaTotal}
        </Badge>
      )}
      {incluir && !selecao?.manual && itemTemTag(item.alerta, "REVISAR_CATEGORIA") && (
        <Badge variant="warning">Revisar categoria</Badge>
      )}
      {itemTemTag(item.alerta, "GASTO_ATIPICO") && (
        <Badge variant="warning">Gasto atípico</Badge>
      )}
      {(itemTemTag(item.alerta, "DUPLICIDADE") ||
        itemTemTag(item.alerta, "DUPLICADO_EXTERNO")) && (
        <Badge variant="warning">Possível duplicata</Badge>
      )}
    </div>
  )
  return { incluir, descricao, badges }
}

function CheckboxItem({
  item,
  incluir,
  onToggleItem,
  disabled,
}: {
  item: ItemRevisao
  incluir: boolean
  onToggleItem: (itemId: number, incluir: boolean) => void
  disabled: boolean
}) {
  return (
    <label className="flex min-h-[44px] min-w-[44px] cursor-pointer items-center justify-center py-2 pr-2 pl-1">
      <Checkbox
        checked={incluir}
        onCheckedChange={(v) => onToggleItem(item.id, v === true)}
        disabled={disabled}
        aria-label={`Incluir ${item.descricao ?? "item sem descrição"} de ${MONETARY.format(item.valor)} na importação`}
      />
    </label>
  )
}

function SelectCategoria({
  item,
  selecao,
  categoriasDoGrupo,
  descricao,
  onCategoriaChange,
  disabled,
}: {
  item: ItemRevisao
  selecao: SelecaoItem | undefined
  categoriasDoGrupo: CategoriaOption[]
  descricao: string
  onCategoriaChange: (itemId: number, categoriaId: number | null) => void
  disabled: boolean
}) {
  return (
    <Select
      value={selecao?.categoriaId != null ? String(selecao.categoriaId) : ""}
      onValueChange={(valor) => onCategoriaChange(item.id, Number(valor))}
      disabled={disabled}
    >
      <SelectTrigger
        aria-label={`Categoria de ${descricao}`}
        className="h-8 w-full min-w-[160px] md:max-w-[220px]"
      >
        <SelectValue placeholder="Selecionar" />
      </SelectTrigger>
      <SelectContent>
        {categoriasDoGrupo.map((c) => (
          <SelectItem key={c.id} value={String(c.id)}>
            {c.nome}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

function HelperSemCategoria({ visivel }: { visivel: boolean }) {
  if (!visivel) return null
  return (
    <p className="mt-1 text-sm text-muted-foreground">
      Escolha uma categoria para incluir este item.
    </p>
  )
}

/** Linha de tabela (md+): célula com div para o desfoque de item desmarcado. */
function LinhaTabela({
  item,
  selecao,
  categoriasDoGrupo,
  onToggleItem,
  onCategoriaChange,
  disabled,
  grupo,
}: LinhaProps) {
  const { incluir, descricao, badges } = estadoDoItem(item, selecao)
  const semCategoria = incluir && !selecao?.categoriaId
  return (
    <tr
      id={`item-${item.id}`}
      tabIndex={-1}
      className={cn(
        "border-b border-border outline-none transition-colors last:border-b-0 focus-visible:ring-2 focus-visible:ring-ring/50",
        !incluir && "bg-muted/30 [&>td>div]:opacity-60",
      )}
    >
      <td className="py-2 pl-1 pr-2 align-middle">
        <CheckboxItem item={item} incluir={incluir} onToggleItem={onToggleItem} disabled={disabled} />
      </td>
      <td className="py-2 pr-2 align-middle">
        <div className="text-sm text-muted-foreground tabular-nums">
          {formatarDataIso(item.data)}
        </div>
      </td>
      <td className="max-w-[260px] py-2 pr-2 align-middle">
        <div>
          <p className="truncate text-sm text-foreground" title={item.descricao ?? undefined}>
            {item.descricao ?? "—"}
          </p>
          {badges}
          <HelperSemCategoria visivel={semCategoria} />
        </div>
      </td>
      <td className="py-2 pr-2 align-middle">
        <div>
          <SelectCategoria
            item={item}
            selecao={selecao}
            categoriasDoGrupo={categoriasDoGrupo}
            descricao={descricao}
            onCategoriaChange={onCategoriaChange}
            disabled={disabled}
          />
        </div>
      </td>
      <td className="py-2 text-right align-middle">
        <div className={cn("text-sm font-semibold tabular-nums", grupo.corValor)}>
          {MONETARY.format(item.valor)}
        </div>
      </td>
    </tr>
  )
}

/** Bloco do mobile: checkbox + data na 1ª linha, descrição, select, valor. */
function BlocoMobile({
  item,
  selecao,
  categoriasDoGrupo,
  onToggleItem,
  onCategoriaChange,
  disabled,
  grupo,
}: LinhaProps) {
  const { incluir, descricao, badges } = estadoDoItem(item, selecao)
  const semCategoria = incluir && !selecao?.categoriaId
  return (
    <div
      id={`item-${item.id}`}
      tabIndex={-1}
      className={cn(
        "flex items-start gap-1 border-b border-border px-1 py-3 outline-none last:border-b-0 focus-visible:ring-2 focus-visible:ring-ring/50",
        !incluir && "bg-muted/30",
      )}
    >
      <CheckboxItem item={item} incluir={incluir} onToggleItem={onToggleItem} disabled={disabled} />
      <div className={cn("min-w-0 flex-1", !incluir && "opacity-60")}>
        <p className="truncate text-sm text-foreground" title={item.descricao ?? undefined}>
          {item.descricao ?? "—"}
        </p>
        <p className="mt-0.5 text-xs text-muted-foreground tabular-nums">
          {formatarDataIso(item.data)}
        </p>
        {badges}
        <div className="mt-2">
          <SelectCategoria
            item={item}
            selecao={selecao}
            categoriasDoGrupo={categoriasDoGrupo}
            descricao={descricao}
            onCategoriaChange={onCategoriaChange}
            disabled={disabled}
          />
        </div>
        <HelperSemCategoria visivel={semCategoria} />
      </div>
      <span
        className={cn(
          "shrink-0 text-sm font-semibold tabular-nums",
          grupo.corValor,
          !incluir && "opacity-60",
        )}
      >
        {MONETARY.format(item.valor)}
      </span>
    </div>
  )
}
