"use client";

import {
  useCallback,
  useMemo,
  useState,
  type Dispatch,
  type FormEvent,
  type SetStateAction,
} from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/Spinner";
import { DeleteConfirmationDialog } from "@/components/DeleteConfirmationDialog";

type TipoLancamento = "ENTRADA" | "SAIDA";

interface LancamentoData {
  id: number;
  tipo: string;
  valor: number;
  data: string;
  descricao: string;
  categoriaNome: string;
  categoriaTipo: string;
}

interface CategoriaOption {
  id: number;
  nome: string;
  tipo: string;
  pessoal: boolean;
  ativo: boolean;
}

interface LancamentosClientProps {
  lancamentos: LancamentoData[];
  total: number;
  categorias: CategoriaOption[];
  onCriar: (request: { categoriaId: number; tipo: string; valor: number; data: string; descricao?: string }) => Promise<LancamentoData | null>;
  onEditar: (id: number, request: { categoriaId: number; tipo: string; valor: number; data: string; descricao?: string }) => Promise<LancamentoData | null>;
  onExcluir: (id: number) => Promise<boolean>;
}

const VALOR_RE = /^-?\d+(\.\d{1,2})?$/;

export function LancamentosClient({
  lancamentos: lancamentosIniciais,
  total,
  categorias,
  onCriar,
  onEditar,
  onExcluir,
}: LancamentosClientProps) {
  const [lancamentos, setLancamentos] = useState<LancamentoData[]>(lancamentosIniciais);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  // Estado do formulário (criar/editar)
  const [formAberto, setFormAberto] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [categoriaId, setCategoriaId] = useState<number | null>(null);
  const [tipo, setTipo] = useState<TipoLancamento>("ENTRADA");
  const [valor, setValor] = useState("");
  const [data, setData] = useState("");
  const [descricao, setDescricao] = useState("");
  const [errosForm, setErrosForm] = useState<{ categoriaId?: string; tipo?: string; valor?: string; data?: string }>({});

  // Dialog de exclusão
  const [excluirId, setExcluirId] = useState<number | null>(null);
  const [excluirAberto, setExcluirAberto] = useState(false);

  const categoriaMap = useMemo(
    () => new Map(categorias.map((c) => [c.id, c.nome])),
    [categorias],
  );

  const podeEditar = editandoId !== null;

  const resetForm = useCallback(() => {
    setEditandoId(null);
    setCategoriaId(null);
    setTipo("ENTRADA");
    setValor("");
    setData("");
    setDescricao("");
    setErrosForm({});
  }, []);

  const abrirCriar = useCallback(() => {
    resetForm();
    setFormAberto(true);
  }, [resetForm]);

  const abrirEditar = useCallback(
    (l: LancamentoData) => {
      setEditandoId(l.id);
      setCategoriaId(l.categoriaNome ? categorias.find((c) => c.nome === l.categoriaNome)?.id ?? null : null);
      setTipo(l.tipo as TipoLancamento);
      setValor(String(l.valor));
      setData(l.data);
      setDescricao(l.descricao);
      setFormAberto(true);
    },
    [categorias],
  );

  function validarForm(): boolean {
    const novos: { categoriaId?: string; tipo?: string; valor?: string; data?: string } = {};
    if (!categoriaId) novos.categoriaId = "Selecione uma categoria.";
    if (!tipo) novos.tipo = "Selecione o tipo (entrada ou saída).";
    if (!data) novos.data = "Informe a data.";
    else if (isNaN(new Date(data).getTime())) novos.data = "Informe uma data válida.";
    if (!valor) {
      novos.valor = "Informe o valor.";
    } else {
      const v = parseFloat(valor);
      if (!VALOR_RE.test(valor)) {
        novos.valor = "O valor aceita no máximo 2 casas decimais.";
      } else if (v <= 0) {
        novos.valor = "O valor deve ser maior que zero.";
      }
    }
    setErrosForm(novos);
    return Object.keys(novos).length === 0;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!validarForm()) return;

    setLoading(true);
    setErro(null);
    setSucesso(null);

    const request = {
      categoriaId: categoriaId!,
      tipo,
      valor: parseFloat(valor),
      data,
      descricao: descricao || undefined,
    };

    try {
      if (podeEditar && editandoId !== null) {
        const resultado = await onEditar(editandoId, request);
        if (resultado) {
          setLancamentos((atual) =>
            atual.map((l) => (l.id === editandoId ? { ...l, ...resultado } : l)),
          );
          setSucesso("Lançamento atualizado com sucesso.");
          setFormAberto(false);
        } else {
          setErro("Não foi possível atualizar o lançamento.");
        }
      } else {
        const resultado = await onCriar(request);
        if (resultado) {
          setLancamentos((atual) => [resultado, ...atual]);
          setSucesso("Lançamento criado com sucesso.");
          setFormAberto(false);
        } else {
          setErro("Não foi possível criar o lançamento.");
        }
      }
    } catch {
      setErro("Não foi possível carregar os dados. Tente novamente em instantes.");
    } finally {
      setLoading(false);
    }
  }

  function abrirExcluir(id: number) {
    setExcluirId(id);
    setExcluirAberto(true);
  }

  async function confirmarExcluir() {
    if (excluirId === null) return;
    setLoading(true);
    try {
      const ok = await onExcluir(excluirId);
      if (ok) {
        setLancamentos((atual) => atual.filter((l) => l.id !== excluirId));
        setSucesso("Lançamento excluído com sucesso.");
      } else {
        setErro("Não foi possível excluir o lançamento.");
      }
    } catch {
      setErro("Não foi possível carregar os dados. Tente novamente em instantes.");
    } finally {
      setExcluirAberto(false);
      setExcluirId(null);
      setLoading(false);
    }
  }

  const truncado = total > 50;

  return (
    <div className="space-y-6">
      {/* Cabeçalho + ações */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-[28px] leading-[1.2] font-semibold text-foreground">
            Lançamentos
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Listagem filtrada por mês. Valor positivo = entrada, negativo = saída.
          </p>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Button
            type="button"
            variant="default"
            onClick={abrirCriar}
            disabled={loading}
            className="h-11 md:text-base"
          >
            {loading ? "Criando…" : "Adicionar lançamento"}
          </Button>
        </div>
      </div>

      {/* Alert de erro */}
      {erro && (
        <Alert variant="destructive" role="alert" className="mb-4">
          <AlertDescription>{erro}</AlertDescription>
        </Alert>
      )}

      {/* Alerta de sucesso */}
      {sucesso && (
        <Alert role="alert" className="mb-4">
          <AlertDescription>{sucesso}</AlertDescription>
        </Alert>
      )}

      {/* Aviso de truncamento */}
      {truncado && (
        <Alert role="alert" className="mb-4">
          <AlertDescription>
            Mostrando 50 de {total} lançamentos
          </AlertDescription>
        </Alert>
      )}

      {/* Tabela / Empty state */}
      <div className="rounded-lg border border-input bg-card shadow-sm">
        {lancamentos.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-sm text-muted-foreground">
              Nenhum lançamento neste mês
            </p>
          </div>
        ) : (
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
              {lancamentos.map((l) => {
                const ehEntrada = l.tipo === "ENTRADA";
                const categoriaNome =
                  l.categoriaNome || categoriaMap.get(l.id) || "—";
                return (
                  <tr key={l.id} className="hover:bg-muted/50 transition-colors">
                    <td className="p-4">{l.data}</td>
                    <td className="p-4">{l.descricao || "—"}</td>
                    <td className="p-4">{categoriaNome}</td>
                    <td className="p-4">
                      <span className={`inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs font-semibold ${ehEntrada ? "border-positive text-positive bg-positive/10" : "border-destructive text-destructive bg-destructive/10"}`}>
                        {l.tipo}
                      </span>
                    </td>
                    <td className="p-4 text-right font-semibold">
                      <span className={ehEntrada ? "text-positive" : "text-destructive"}>
                        {ehEntrada ? "+" : "-"} R$ {Math.abs(l.valor).toLocaleString(
                          "pt-BR",
                          { minimumFractionDigits: 2 },
                        )}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => abrirEditar(l)}
                          disabled={loading}
                          className="h-8 px-2 text-xs"
                        >
                          Editar
                        </Button>
                        <Button
                          type="button"
                          variant="destructive"
                          size="sm"
                          onClick={() => abrirExcluir(l.id)}
                          disabled={loading}
                          className="h-8 px-2 text-xs"
                        >
                          Excluir
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Formulário criar/editar */}
      {formAberto && (
        <CardForm
          podeEditar={podeEditar}
          categorias={categorias}
          categoriaId={categoriaId}
          setCategoriaId={setCategoriaId}
          tipo={tipo}
          setTipo={setTipo}
          valor={valor}
          setValor={setValor}
          data={data}
          setData={setData}
          descricao={descricao}
          setDescricao={setDescricao}
          errosForm={errosForm}
          setErrosForm={setErrosForm}
          loading={loading}
          onSubmit={onSubmit}
          onCancel={() => {
            setFormAberto(false);
            resetForm();
          }}
        />
      )}

      {/* Dialog de exclusão */}
      <DeleteConfirmationDialog
        isOpen={excluirAberto}
        onConfirm={confirmarExcluir}
        onClose={() => setExcluirAberto(false)}
        action="excluir"
        target="lançamento"
      />
    </div>
  );
}

interface CardFormProps {
  podeEditar: boolean;
  categorias: CategoriaOption[];
  categoriaId: number | null;
  setCategoriaId: (id: number | null) => void;
  tipo: TipoLancamento;
  setTipo: (t: TipoLancamento) => void;
  valor: string;
  setValor: (v: string) => void;
  data: string;
  setData: (d: string) => void;
  descricao: string;
  setDescricao: (d: string) => void;
  errosForm: { categoriaId?: string; tipo?: string; valor?: string; data?: string };
  setErrosForm: Dispatch<SetStateAction<{ categoriaId?: string; tipo?: string; valor?: string; data?: string }>>;
  loading: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancel: () => void;
}

function CardForm({
  podeEditar,
  categorias,
  categoriaId,
  setCategoriaId,
  tipo,
  setTipo,
  valor,
  setValor,
  data,
  setData,
  descricao,
  setDescricao,
  errosForm,
  setErrosForm,
  loading,
  onSubmit,
  onCancel,
}: CardFormProps) {
  return (
    <div className="rounded-lg border border-input bg-card shadow-sm p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="font-semibold text-foreground">
          {podeEditar ? "Editar lançamento" : "Novo lançamento"}
        </h2>
        <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
          Cancelar
        </Button>
      </div>

      <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2 sm:gap-4" noValidate>
        <div className="flex flex-col gap-1">
          <Label htmlFor="categoria">Categoria</Label>
          <select
            id="categoria"
            name="categoria"
            value={categoriaId ?? ""}
            onChange={(e) => setCategoriaId(e.target.value ? Number(e.target.value) : null)}
            disabled={loading}
            className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
            aria-invalid={errosForm.categoriaId ? true : undefined}
          >
            <option value="">Selecione uma categoria</option>
            {categorias.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome} ({c.tipo})
              </option>
            ))}
          </select>
          {errosForm.categoriaId && (
            <p className="text-sm text-destructive">{errosForm.categoriaId}</p>
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
          {errosForm.tipo && <p className="text-sm text-destructive">{errosForm.tipo}</p>}
        </div>

        <div className="flex flex-col gap-1 sm:col-span-2">
          <Label htmlFor="valor">Valor</Label>
          <Input
            id="valor"
            name="valor"
            type="text"
            placeholder="0,00"
            value={valor}
            onChange={(e) => {
              const v = e.target.value.replace(",", ".");
              setValor(v);
              if (errosForm.valor) setErrosForm((atual) => ({ ...atual, valor: undefined }));
            }}
            disabled={loading}
            className="h-11"
            aria-invalid={errosForm.valor ? true : undefined}
          />
          {errosForm.valor && <p className="text-sm text-destructive">{errosForm.valor}</p>}
        </div>

        <div className="flex flex-col gap-1 sm:col-span-2">
          <Label htmlFor="data">Data</Label>
          <Input
            id="data"
            name="data"
            type="date"
            value={data}
            onChange={(e) => {
              setData(e.target.value);
              if (errosForm.data) setErrosForm((atual) => ({ ...atual, data: undefined }));
            }}
            disabled={loading}
            className="h-11"
            aria-invalid={errosForm.data ? true : undefined}
          />
          {errosForm.data && <p className="text-sm text-destructive">{errosForm.data}</p>}
        </div>

        <div className="flex flex-col gap-1 sm:col-span-2">
          <Label htmlFor="descricao">Descrição</Label>
          <Input
            id="descricao"
            name="descricao"
            type="text"
            placeholder="Descrição do lançamento"
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            disabled={loading}
            className="h-11"
          />
        </div>

        <div className="flex items-center gap-2 sm:col-span-2">
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
            ) : podeEditar ? "Salvar alterações" : "Salvar"
            }
          </Button>
        </div>
      </form>
    </div>
  );
}
