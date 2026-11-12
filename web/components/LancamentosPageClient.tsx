"use client"

import * as React from "react"
import { LancamentosTable } from "@/components/LancamentosTable"
import { LancamentoFormDialog } from "@/components/LancamentoFormDialog"
import { LoteSheet } from "@/components/LoteSheet"
import { ChatPanel } from "@/components/ai/chat-panel"
import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/Spinner"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { DeleteConfirmationDialog } from "@/components/DeleteConfirmationDialog"

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

type LancamentosPageClientProps = {
  lancamentos: LancamentoData[]
  total: number
  categorias: CategoriaOption[]
  onCriar: (
    request: {
      categoriaId: number
      tipo: string
      valor: number
      data: string
      descricao?: string
    },
  ) => Promise<LancamentoData | null>
  onEditar: (
    id: number,
    request: {
      categoriaId: number
      tipo: string
      valor: number
      data: string
      descricao?: string
    },
  ) => Promise<LancamentoData | null>
  onExcluir: (id: number) => Promise<boolean>
  onCriarLote: (
    linhas: Array<{
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }>,
  ) => Promise<boolean>
}

export function LancamentosPageClient({
  lancamentos: lancamentosIniciais,
  total,
  categorias,
  onCriar,
  onEditar,
  onExcluir,
  onCriarLote,
}: LancamentosPageClientProps) {
  // Filtros
  const [ano, setAno] = React.useState<number | null>(null)
  const [mes, setMes] = React.useState<number | null>(null)
  const [tipo, setTipo] = React.useState<string | null>(null)
  const [categoriaId, setCategoriaId] = React.useState<number | null>(null)
  const [busca, setBusca] = React.useState("")

  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  // Dialog de criar/editar
  const [formAberto, setFormAberto] = React.useState(false)
  const [modo, setModo] = React.useState<"criar" | "editar">("criar")
  const [editandoLancamento, setEditandoLancamento] = React.useState<
    LancamentoData | null
  >(null)

  // Sheet de lote

  // Dialog de exclusão
  const [excluirId, setExcluirId] = React.useState<number | null>(null)
  const [excluirAberto, setExcluirAberto] = React.useState(false)

  const resetForm = React.useCallback(() => {
    setModo("criar")
    setEditandoLancamento(null)
    setFormAberto(false)
  }, [])

  const abrirCriar = React.useCallback(() => {
    setModo("criar")
    setEditandoLancamento(null)
    setFormAberto(true)
  }, [])

  const abrirEditar = React.useCallback((l: LancamentoData) => {
    setModo("editar")
    setEditandoLancamento(l)
    setFormAberto(true)
  }, [])

  const handleChangeFiltros = React.useCallback(
    (filtros: {
      ano?: number | null
      mes?: number | null
      tipo?: string | null
      categoriaId?: number | null
      busca?: string
    }) => {
      if (filtros.ano !== undefined) setAno(filtros.ano)
      if (filtros.mes !== undefined) setMes(filtros.mes)
      if (filtros.tipo !== undefined) setTipo(filtros.tipo)
      if (filtros.categoriaId !== undefined)
        setCategoriaId(filtros.categoriaId)
      if (filtros.busca !== undefined) setBusca(filtros.busca)
    },
    [],
  )

  const handlePageChange = React.useCallback(() => {}, [])

  const handleRetry = React.useCallback(() => {
    setError(null)
  }, [])

  const handleCriar = React.useCallback(
    async (request: {
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }): Promise<boolean> => {
      setLoading(true)
      setError(null)
      try {
        const resultado = await onCriar({
          categoriaId: request.categoriaId,
          tipo: request.tipo,
          valor: parseFloat(request.valor),
          data: request.data,
          descricao: request.descricao,
        })
        if (resultado) {
          resetForm()
          // Atualiza a lista local
          // (em um app real, isso viria do servidor via revalidação)
          window.location.reload()
          return true
        } else {
          setError("Erro ao criar lançamento. Tente novamente.")
          return false
        }
      } finally {
        setLoading(false)
      }
    },
    [onCriar, resetForm],
  )

  const handleEditar = React.useCallback(
    async (request: {
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }): Promise<boolean> => {
      if (!editandoLancamento) return false
      setLoading(true)
      setError(null)
      try {
        const resultado = await onEditar(editandoLancamento.id, {
          categoriaId: request.categoriaId,
          tipo: request.tipo,
          valor: parseFloat(request.valor),
          data: request.data,
          descricao: request.descricao,
        })
        if (resultado) {
          resetForm()
          window.location.reload()
          return true
        } else {
          setError("Erro ao editar lançamento. Tente novamente.")
          return false
        }
      } finally {
        setLoading(false)
      }
    },
    [onEditar, editandoLancamento, resetForm],
  )

  const handleExcluirConfirmar = React.useCallback(async () => {
    if (!excluirId) return
    setLoading(true)
    setError(null)
    try {
      const resultado = await onExcluir(excluirId)
      if (resultado) {
        setExcluirAberto(false)
        setExcluirId(null)
        window.location.reload()
      } else {
        setError("Erro ao excluir lançamento. Tente novamente.")
      }
    } finally {
      setLoading(false)
    }
  }, [onExcluir, excluirId])

  const handleCriarLote = React.useCallback(
    async (linhas: Array<{
      categoriaId: number
      tipo: string
      valor: string
      data: string
      descricao?: string
    }>): Promise<boolean> => {
      setLoading(true)
      setError(null)
      try {
        const resultado = await onCriarLote(linhas)
        if (resultado) {
          window.location.reload()
          return true
        } else {
          setError("Erro ao salvar lote. Tente novamente.")
          return false
        }
      } finally {
        setLoading(false)
      }
    },
    [onCriarLote],
  )

  // Resumo real do mês (soma dos lançamentos recebidos) — o momento
  // counter-rolls da página: os números rolam uma vez ao entrar na tela.
  const resumo = React.useMemo(() => {
    let entradas = 0
    let saidas = 0
    for (const l of lancamentosIniciais) {
      if (l.tipo === "ENTRADA") entradas += l.valor
      else saidas += Math.abs(l.valor)
    }
    return { entradas, saidas, saldo: entradas - saidas }
  }, [lancamentosIniciais])

  const resumoFmt = React.useMemo(
    () =>
      new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL",
      }),
    [],
  )

  return (
    <div className="space-y-6">
      {/* Botão de ação principal */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="default"
            onClick={abrirCriar}
            disabled={loading}
            className="h-11"
          >
            <span className="flex items-center gap-1">
              {
                loading
                  ? (
                    <span className="flex items-center gap-1">
                      <Spinner className="mr-1" />
                      Criando…
                    </span>
                  )
                  : (
                    <>
                      <span className="mr-1">+</span> Novo lançamento
                    </>
                  )
              }
            </span>
          </Button>
        </div>
        {error && (
          <Alert variant="destructive" role="alert" className="mb-4">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
      </div>

      {/* Resumo do mês — counter-rolls (números reais da listagem).
          1 coluna no mobile, 3 no md+. O botão de filtro fica à direita
          do valor (não embaixo, centralizado). */}
      <div
        className="grid grid-cols-1 gap-x-6 gap-y-6 md:grid-cols-3"
        data-moment="counter-rolls"
      >
        <div className="flex flex-col border-t border-border pt-3">
          <span className="text-xs font-medium text-muted-foreground">
            Entradas
          </span>
          <div className="mt-1.5 flex items-baseline justify-between gap-3">
            <p className="t-stat text-2xl font-semibold tabular-nums text-positive">
              {resumoFmt.format(resumo.entradas)}
            </p>
            <button
              type="button"
              className="shrink-0 text-xs text-primary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              onClick={() => setTipo("ENTRADA")}
              aria-label="Filtrar apenas entradas"
            >
              Filtrar
            </button>
          </div>
        </div>
        <div className="flex flex-col border-t border-border pt-3">
          <span className="text-xs font-medium text-muted-foreground">
            Saídas
          </span>
          <div className="mt-1.5 flex items-baseline justify-between gap-3">
            <p className="t-stat text-2xl font-semibold tabular-nums text-destructive">
              {resumoFmt.format(resumo.saidas)}
            </p>
            <button
              type="button"
              className="shrink-0 text-xs text-primary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              onClick={() => setTipo("SAIDA")}
              aria-label="Filtrar apenas saídas"
            >
              Filtrar
            </button>
          </div>
        </div>
        <div className="flex flex-col border-t border-border pt-3">
          <span className="text-xs font-medium text-muted-foreground">
            Saldo
          </span>
          <div className="mt-1.5 flex items-baseline justify-between gap-3">
            <p
              className={`t-stat text-2xl font-semibold tabular-nums ${
                resumo.saldo >= 0 ? "text-positive" : "text-destructive"
              }`}
            >
              {resumoFmt.format(resumo.saldo)}
            </p>
            <button
              type="button"
              className="shrink-0 text-xs text-primary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              onClick={() => setTipo(null)}
              aria-label="Mostrar todas"
            >
              Todas
            </button>
          </div>
        </div>
      </div>

      {/* Tabela de lançamentos + chat. No mobile tudo empilhado (1 coluna);
          no lg+ o chat vira coluna lateral ao lado da tabela. */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <div className="lg:col-span-8">
          <LancamentosTable
            lancamentos={lancamentosIniciais}
            total={total}
            categorias={categorias}
            ano={ano}
            mes={mes}
            tipo={tipo}
            categoriaId={categoriaId}
            busca={busca}
            onChangeFiltros={handleChangeFiltros}
            onPageChange={handlePageChange}
            loading={loading}
            error={error}
            onRetry={handleRetry}
            onEditar={abrirEditar}
            onExcluir={(id) => {
              setExcluirId(id)
              setExcluirAberto(true)
            }}
          />
        </div>

        {/* Chat com IA — no mobile ocupa a largura toda abaixo da tabela;
            no lg+ vira coluna lateral ao lado dela. */}
        <div className="lg:col-span-4">
          <ChatPanel />
        </div>
      </div>

      {/* Dialog de criar/editar */}
      <LancamentoFormDialog
        isOpen={formAberto}
        onClose={resetForm}
        onSubmit={
          modo === "criar" ? handleCriar : handleEditar
        }
        categorias={categorias}
        modo={modo}
        lancamento={editandoLancamento
          ? {
              id: editandoLancamento.id,
              categoriaId: editandoLancamento.id, // placeholder - categorias seriam buscadas separadamente
              tipo: editandoLancamento.tipo,
              valor: editandoLancamento.valor,
              data: editandoLancamento.data,
              descricao: editandoLancamento.descricao,
            }
          : undefined}
      />

      {/* Sheet de lote */}
      <LoteSheet
        onClose={() => {}}
        onSubmit={handleCriarLote}
        categorias={categorias}
      />

      {/* Dialog de exclusão */}
      <DeleteConfirmationDialog
        isOpen={excluirAberto}
        onConfirm={handleExcluirConfirmar}
        onClose={() => {
          setExcluirAberto(false)
          setExcluirId(null)
        }}
        action="excluir"
        target="lançamento"
      />
    </div>
  )
}
