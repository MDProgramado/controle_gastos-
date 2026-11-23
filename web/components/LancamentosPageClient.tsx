"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { LancamentosTable } from "@/components/LancamentosTable";
import { LancamentoFormDialog } from "@/components/LancamentoFormDialog";
import { LoteSheet } from "@/components/LoteSheet";
import { ChatPanel } from "@/components/ai/chat-panel";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/Spinner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { DeleteConfirmationDialog } from "@/components/DeleteConfirmationDialog";
import { formatarBRL } from "@/lib/format";
import { centavosDeString, normalizarValorParaApi } from "@/lib/money";
import type { ResultadoCriacaoLote } from "@/lib/lancamentos";

const ZERO = BigInt(0);

type LancamentoData = {
  id: number;
  tipo: string;
  valor: string;
  data: string;
  descricao: string;
  categoriaNome: string;
  categoriaTipo: string;
};

type CategoriaOption = {
  id: number;
  nome: string;
  tipo: string;
  pessoal: boolean;
  ativo: boolean;
};

type LancamentosPageClientProps = {
  lancamentos: LancamentoData[];
  total: number;
  categorias: CategoriaOption[];
  onCriar: (
    request: {
      categoriaId: number;
      tipo: string;
      valor: string;
      data: string;
      descricao?: string;
    },
  ) => Promise<{ id: number } | null>;
  onEditar: (
    id: number,
    request: {
      categoriaId: number;
      tipo: string;
      valor: string;
      data: string;
      descricao?: string;
    },
  ) => Promise<{ id: number } | null>;
  onExcluir: (id: number) => Promise<boolean>;
  onCriarLote: (
    linhas: Array<{
      categoriaId: number;
      tipo: string;
      valor: string;
      data: string;
      descricao?: string;
    }>,
  ) => Promise<ResultadoCriacaoLote>;
};

export function LancamentosPageClient({
  lancamentos: lancamentosIniciais,
  categorias,
  onCriar,
  onEditar,
  onExcluir,
  onCriarLote,
}: LancamentosPageClientProps) {
  const router = useRouter();

  // 1. Imports e estados de filtro/paginação
  const [paginaAtual, setPaginaAtual] = React.useState(1);
  const [tipo, setTipo] = React.useState<string>("TODOS");
  const [categoriaId, setCategoriaId] = React.useState<string>("TODAS");
  const [competencia, setCompetencia] = React.useState<string>("");
  const [busca, setBusca] = React.useState<string>("");

  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  // Dialog de criar/editar
  const [formAberto, setFormAberto] = React.useState(false);
  const [modo, setModo] = React.useState<"criar" | "editar">("criar");
  const [editandoLancamento, setEditandoLancamento] = React.useState<
    LancamentoData | null
  >(null);

  // Dialog de exclusão
  const [excluirId, setExcluirId] = React.useState<number | null>(null);
  const [excluirAberto, setExcluirAberto] = React.useState(false);

  // Sheet de lote
  const [loteAberto, setLoteAberto] = React.useState(false);

  const abrirLote = React.useCallback(() => {
    setLoteAberto(true);
  }, []);

  const resetForm = React.useCallback(() => {
    setModo("criar");
    setEditandoLancamento(null);
    setFormAberto(false);
  }, []);

  const abrirCriar = React.useCallback(() => {
    setModo("criar");
    setEditandoLancamento(null);
    setFormAberto(true);
  }, []);

  const abrirEditar = React.useCallback((l: LancamentoData) => {
    setModo("editar");
    setEditandoLancamento(l);
    setFormAberto(true);
  }, []);

  // Mapeia categorias para o formato esperado pelo diálogo (id como string)
  const categoriasParaDialog = React.useMemo(() => {
    return categorias.map((c) => ({
      id: String(c.id),
      nome: c.nome,
      tipo: c.tipo as "ENTRADA" | "SAIDA",
      ativo: c.ativo,
    }));
  }, [categorias]);

  // 2. Filtragem real com useMemo e reset de página
  const lancamentosFiltrados = React.useMemo(() => {
    return lancamentosIniciais.filter((item) => {
      if (tipo !== "TODOS" && item.tipo !== tipo) {
        return false;
      }

      if (categoriaId !== "TODAS") {
        const catId = Number(categoriaId);
        const cat = categorias.find((c) => c.id === catId);
        const matchNome =
          cat &&
          item.categoriaNome === cat.nome &&
          (!item.categoriaTipo || item.categoriaTipo === cat.tipo);
        if (!matchNome) return false;
      }

      if (competencia && !item.data.startsWith(competencia)) {
        return false;
      }

      if (busca.trim()) {
        const termo = busca.toLowerCase();
        const matchDescricao = item.descricao.toLowerCase().includes(termo);
        const matchCategoria = item.categoriaNome.toLowerCase().includes(termo);
        if (!matchDescricao && !matchCategoria) return false;
      }

      return true;
    });
  }, [lancamentosIniciais, categorias, tipo, categoriaId, competencia, busca]);

  // Mapeia lançamentos filtrados para o formato da tabela
  const lancamentosParaTabela = React.useMemo(() => {
    return lancamentosFiltrados.map((item) => {
      const cat = categorias.find(
        (c) =>
          c.nome === item.categoriaNome && c.tipo === item.categoriaTipo,
      );
      return {
        id: String(item.id),
        descricao: item.descricao,
        valor: item.valor,
        tipo: item.tipo as "ENTRADA" | "SAIDA",
        data: item.data,
        categoriaId: cat ? String(cat.id) : undefined,
        categoriaNome: item.categoriaNome,
        categoriaTipo: item.tipo as "ENTRADA" | "SAIDA",
      };
    });
  }, [lancamentosFiltrados, categorias]);

  // 3. Mapeamento correto do lançamento para o modal de edição
  const lancamentoParaForm = React.useMemo(() => {
    if (!editandoLancamento) return null;

    const categoriaEncontrada = categorias.find(
      (c) =>
        c.nome === editandoLancamento.categoriaNome &&
        c.tipo === editandoLancamento.tipo,
    );

    return {
      id: String(editandoLancamento.id),
      descricao: editandoLancamento.descricao,
      valor: editandoLancamento.valor,
      tipo: editandoLancamento.tipo as "ENTRADA" | "SAIDA",
      categoriaId: categoriaEncontrada ? String(categoriaEncontrada.id) : "",
      data: editandoLancamento.data,
    };
  }, [editandoLancamento, categorias]);

  const handleCriar = React.useCallback(
    async (dados: {
      descricao: string;
      valor: string;
      tipo: "ENTRADA" | "SAIDA";
      categoriaId: string;
      data: string;
    }): Promise<void> => {
      const valor = normalizarValorParaApi(dados.valor);
      if (valor === null) {
        setError("Informe um valor maior que zero, com até 2 casas decimais.");
        return;
      }
      setLoading(true);
      setError(null);
      try {
        const resultado = await onCriar({
          categoriaId: Number(dados.categoriaId),
          tipo: dados.tipo,
          valor,
          data: dados.data,
          descricao: dados.descricao,
        });
        if (resultado) {
          resetForm();
          router.refresh();
        } else {
          setError("Erro ao criar lançamento. Tente novamente.");
        }
      } finally {
        setLoading(false);
      }
    },
    [onCriar, resetForm, router],
  );

  const handleEditar = React.useCallback(
    async (dados: {
      descricao: string;
      valor: string;
      tipo: "ENTRADA" | "SAIDA";
      categoriaId: string;
      data: string;
    }): Promise<void> => {
      if (!editandoLancamento) return;
      const valor = normalizarValorParaApi(dados.valor);
      if (valor === null) {
        setError("Informe um valor maior que zero, com até 2 casas decimais.");
        return;
      }
      setLoading(true);
      setError(null);
      try {
        const resultado = await onEditar(editandoLancamento.id, {
          categoriaId: Number(dados.categoriaId),
          tipo: dados.tipo,
          valor,
          data: dados.data,
          descricao: dados.descricao,
        });
        if (resultado) {
          resetForm();
          router.refresh();
        } else {
          setError("Erro ao editar lançamento. Tente novamente.");
        }
      } finally {
        setLoading(false);
      }
    },
    [onEditar, editandoLancamento, resetForm, router],
  );

  // 4. handleExcluirConfirmar com router.refresh()
  const handleExcluirConfirmar = React.useCallback(async () => {
    if (!excluirId) return;
    setLoading(true);
    setError(null);
    try {
      const resultado = await onExcluir(excluirId);
      if (resultado) {
        setExcluirAberto(false);
        setExcluirId(null);
        router.refresh();
      } else {
        setError("Erro ao excluir lançamento. Tente novamente.");
      }
    } finally {
      setLoading(false);
    }
  }, [onExcluir, excluirId, router]);

  const handleCriarLote = React.useCallback(
    async (
      linhas: Array<{
        categoriaId: number;
        tipo: string;
        valor: string;
        data: string;
        descricao?: string;
      }>,
    ): Promise<ResultadoCriacaoLote> => {
      setLoading(true);
      try {
        const resultado = await onCriarLote(linhas);
        if (resultado.ok) {
          router.refresh();
        }
        return resultado;
      } finally {
        setLoading(false);
      }
    },
    [onCriarLote, router],
  );

  const resumo = React.useMemo(() => {
    let entradas = ZERO;
    let saidas = ZERO;
    for (const l of lancamentosIniciais) {
      const centavos = centavosDeString(l.valor) ?? ZERO;
      if (l.tipo === "ENTRADA") entradas += centavos;
      else saidas += centavos < ZERO ? -centavos : centavos;
    }
    return { entradas, saidas, saldo: entradas - saidas };
  }, [lancamentosIniciais]);

  return (
    <div className="space-y-6">
      {/* Botões de ação principal */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <Button
            type="button"
            variant="default"
            onClick={abrirCriar}
            disabled={loading}
            className="h-11"
          >
            <span className="flex items-center gap-1">
              {loading ? (
                <span className="flex items-center gap-1">
                  <Spinner className="mr-1" />
                  Criando…
                </span>
              ) : (
                <>
                  <span className="mr-1">+</span> Novo lançamento
                </>
              )}
            </span>
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={abrirLote}
            disabled={loading}
            className="h-11"
          >
            <span className="flex items-center gap-1">
              Lançar em lote
            </span>
          </Button>
        </div>
        {error && (
          <Alert variant="destructive" role="alert" className="mb-4">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
      </div>

      {/* Resumo do mês — counter-rolls */}
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
              {formatarBRL(resumo.entradas)}
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
              {formatarBRL(resumo.saidas)}
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
                resumo.saldo >= ZERO ? "text-positive" : "text-destructive"
              }`}
            >
              {formatarBRL(resumo.saldo)}
            </p>
            <button
              type="button"
              className="shrink-0 text-xs text-primary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              onClick={() => setTipo("TODOS")}
              aria-label="Mostrar todas"
            >
              Todas
            </button>
          </div>
        </div>
      </div>

      {/* Filtros + Tabela + Chat */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <div className="lg:col-span-8">
          {/* Filtros */}
          <div className="space-y-5">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">
                  Tipo
                </label>
                <select
                  value={tipo}
                  onChange={(e) => setTipo(e.target.value)}
                  className="w-full rounded-md border bg-background px-3 py-1.5 text-sm"
                >
                  <option value="TODOS">Todos os tipos</option>
                  <option value="ENTRADA">Entradas</option>
                  <option value="SAIDA">Saídas</option>
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">
                  Categoria
                </label>
                <select
                  value={categoriaId}
                  onChange={(e) => setCategoriaId(e.target.value)}
                  className="w-full rounded-md border bg-background px-3 py-1.5 text-sm"
                >
                  <option value="TODAS">Todas as categorias</option>
                  {categorias.map((cat) => (
                    <option key={cat.id} value={String(cat.id)}>
                      {cat.nome}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">
                  Competência
                </label>
                <input
                  type="month"
                  value={competencia}
                  onChange={(e) => setCompetencia(e.target.value)}
                  className="w-full rounded-md border bg-background px-3 py-1.5 text-sm"
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">
                  Busca
                </label>
                <input
                  type="text"
                  value={busca}
                  onChange={(e) => setBusca(e.target.value)}
                  placeholder="Descrição ou categoria"
                  className="w-full rounded-md border bg-background px-3 py-1.5 text-sm"
                />
              </div>
            </div>

            <LancamentosTable
              lancamentos={lancamentosParaTabela}
              paginaAtual={paginaAtual}
              pageSize={10}
              onPageChange={setPaginaAtual}
              onEdit={(item) => {
                const original = lancamentosIniciais.find(
                  (l) => String(l.id) === item.id,
                );
                if (original) abrirEditar(original);
              }}
              onDelete={(item) => {
                setExcluirId(Number(item.id));
                setExcluirAberto(true);
              }}
            />
          </div>
        </div>

        <div className="lg:col-span-4">
          <ChatPanel />
        </div>
      </div>

      {/* Dialog de criar/editar */}
      <LancamentoFormDialog
        isOpen={formAberto}
        onClose={resetForm}
        onSubmit={modo === "criar" ? handleCriar : handleEditar}
        categorias={categoriasParaDialog}
        lancamento={lancamentoParaForm}
      />

      <LoteSheet
        open={loteAberto}
        onOpenChange={setLoteAberto}
        onSubmit={handleCriarLote}
        categorias={categorias}
      />

      {/* Dialog de exclusão */}
      <DeleteConfirmationDialog
        isOpen={excluirAberto}
        onConfirm={handleExcluirConfirmar}
        onClose={() => {
          setExcluirAberto(false);
          setExcluirId(null);
        }}
        action="excluir"
        target="lançamento"
      />
    </div>
  );
}
