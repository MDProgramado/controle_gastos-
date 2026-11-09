"use client";

import {
  useCallback,
  useState,
  type FormEvent,
} from "react";
import { cn } from "cn";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/Spinner";
import { DeleteConfirmationDialog } from "@/components/DeleteConfirmationDialog";

interface CategoriaData {
  id: number;
  nome: string;
  tipo: string;
  pessoal: boolean;
  ativo: boolean;
}

interface CategoriasClientProps {
  categorias: CategoriaData[];
  userRole: "ADMIN" | "USER";
  onCriar: (request: { nome: string; tipo: string; pessoal: boolean }) => Promise<CategoriaData | null>;
  onEditar: (id: number, request: { nome: string; tipo: string; pessoal: boolean }) => Promise<CategoriaData | null>;
  onDesativar: (id: number) => Promise<boolean>;
}

export function CategoriasClient({
  categorias: categoriasIniciais,
  userRole,
  onCriar,
  onEditar,
  onDesativar,
}: CategoriasClientProps) {
  const [categorias, setCategorias] = useState<CategoriaData[]>(categoriasIniciais);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  // Estado do formulário (criar/editar)
  const [formAberto, setFormAberto] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [nome, setNome] = useState("");
  const [tipo, setTipo] = useState("ENTRADA");
  const [pessoal, setPessoal] = useState(false);
  const [errosForm, setErrosForm] = useState<{ nome?: string; tipo?: string }>({});

  // Dialog de desativação
  const [desativarId, setDesativarId] = useState<number | null>(null);
  const [desativarAberto, setDesativarAberto] = useState(false);

  const podeEscrever = userRole === "ADMIN";

  const resetForm = useCallback(() => {
    setEditandoId(null);
    setNome("");
    setTipo("ENTRADA");
    setPessoal(false);
    setErrosForm({});
  }, []);

  const abrirCriar = useCallback(() => {
    if (!podeEscrever) return;
    resetForm();
    setFormAberto(true);
  }, [podeEscrever, resetForm]);

  const abrirEditar = useCallback(
    (c: CategoriaData) => {
      if (!podeEscrever) return;
      setEditandoId(c.id);
      setNome(c.nome);
      setTipo(c.tipo as "ENTRADA" | "SAÍDA");
      setPessoal(c.pessoal);
      setFormAberto(true);
    },
    [podeEscrever],
  );

  function validarForm(): boolean {
    const novos: { nome?: string; tipo?: string } = {};
    if (!nome.trim()) novos.nome = "Informe o nome da categoria.";
    if (!tipo) novos.tipo = "Informe o tipo.";
    setErrosForm(novos);
    return Object.keys(novos).length === 0;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!podeEscrever) return;
    if (!validarForm()) return;

    setLoading(true);
    setErro(null);
    setSucesso(null);

    const request = {
      nome: nome.trim(),
      tipo,
      pessoal,
    };

    try {
      if (editandoId !== null) {
        const resultado = await onEditar(editandoId, request);
        if (resultado) {
          setCategorias((atual) =>
            atual.map((c) => (c.id === editandoId ? { ...c, ...resultado } : c)),
          );
          setSucesso("Categoria atualizada com sucesso.");
          setFormAberto(false);
        } else {
          setErro("Não foi possível atualizar a categoria.");
        }
      } else {
        const resultado = await onCriar(request);
        if (resultado) {
          setCategorias((atual) => [resultado, ...atual]);
          setSucesso("Categoria criada com sucesso.");
          setFormAberto(false);
        } else {
          setErro("Não foi possível criar a categoria.");
        }
      }
    } catch {
      setErro("Não foi possível conectar. Tente novamente em instantes.");
    } finally {
      setLoading(false);
    }
  }

  function abrirDesativar(id: number) {
    if (!podeEscrever) return;
    setDesativarId(id);
    setDesativarAberto(true);
  }

  async function confirmarDesativar() {
    if (desativarId === null) return;
    setLoading(true);
    try {
      const ok = await onDesativar(desativarId);
      if (ok) {
        setCategorias((atual) =>
          atual.map((c) =>
            c.id === desativarId ? { ...c, ativo: false } : c,
          ),
        );
        setSucesso("Categoria desativada com sucesso.");
      } else {
        setErro("Não foi possível desativar a categoria.");
      }
    } catch {
      setErro("Não foi possível conectar. Tente novamente em instantes.");
    } finally {
      setDesativarAberto(false);
      setDesativarId(null);
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Cabeçalho + ações */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-[20px] leading-[1.2] font-semibold text-foreground">
            Categorias
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Gerencie as categorias de entrada e saída da empresa.
          </p>
        </div>

        {podeEscrever && (
          <Button
            type="button"
            variant="default"
            onClick={abrirCriar}
            disabled={loading}
            className="h-11 md:text-base"
          >
            {loading ? "Criando…" : "Adicionar categoria"}
          </Button>
        )}
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

      {/* Tabela / Empty state */}
      <div className="rounded-lg border border-input bg-card shadow-sm">
        {categorias.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-sm text-muted-foreground">
              Nenhuma categoria cadastrada
            </p>
          </div>
        ) : (
          <table className="w-full text-sm text-left">
            <thead>
              <tr className="border-b bg-muted text-muted-foreground">
                <th className="p-3">Nome</th>
                <th className="p-3">Tipo</th>
                <th className="p-3">Pessoal</th>
                <th className="p-3">Ativo</th>
                <th className="p-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {categorias.map((c) => (
                <tr
                  key={c.id}
                  className={cn(
                    "hover:bg-muted/50 transition-colors",
                    !c.ativo && "opacity-60",
                  )}
                >
                  <td className="p-3">
                    <span className={cn("font-medium", !c.ativo && "text-muted-foreground")}>
                      {c.nome}
                    </span>
                  </td>
                  <td className="p-3">
                    <span
                      className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-semibold ${
                        c.tipo === "ENTRADA"
                          ? "border-[#16A34A] text-[#16A34A] bg-[#16A34A]/10"
                          : "border-[#DC2626] text-[#DC2626] bg-[#DC2626]/10"
                      }`}
                    >
                      {c.tipo}
                    </span>
                  </td>
                  <td className="p-3">
                    <span className={cn("text-xs", !c.ativo && "text-muted-foreground")}>
                      {c.pessoal ? "Sim" : "Não"}
                    </span>
                  </td>
                  <td className="p-3">
                    <span
                      className={cn(
                        "inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-semibold",
                        c.ativo
                          ? "border-[#16A34A] text-[#16A34A] bg-[#16A34A]/10"
                          : "border-muted-foreground text-muted-foreground bg-muted",
                      )}
                    >
                      {c.ativo ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td className="p-3 text-right">
                    <div className="flex items-center justify-end gap-2">
                      {podeEscrever && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => abrirEditar(c)}
                          disabled={loading}
                          className="h-8 px-2 text-xs"
                        >
                          Editar
                        </Button>
                      )}
                      {podeEscrever && c.ativo && (
                        <Button
                          type="button"
                          variant="destructive"
                          size="sm"
                          onClick={() => abrirDesativar(c.id)}
                          disabled={loading}
                          className="h-8 px-2 text-xs"
                        >
                          Desativar
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Formulário criar/editar (apenas ADMIN) */}
      {podeEscrever && formAberto && (
        <CardForm
          podeEditar={editandoId !== null}
          nome={nome}
          setNome={setNome}
          tipo={tipo}
          setTipo={setTipo}
          pessoal={pessoal}
          setPessoal={setPessoal}
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

      {/* Dialog de desativação */}
      <DeleteConfirmationDialog
        isOpen={desativarAberto}
        onConfirm={confirmarDesativar}
        onClose={() => setDesativarAberto(false)}
        action="desativar"
        target="categoria"
      />
    </div>
  );
}

interface CardFormProps {
  podeEditar: boolean;
  nome: string;
  setNome: (v: string) => void;
  tipo: string;
  setTipo: (t: string) => void;
  pessoal: boolean;
  setPessoal: (p: boolean) => void;
  errosForm: { nome?: string; tipo?: string };
  setErrosForm: (val: { nome?: string; tipo?: string } | ((prev: { nome?: string; tipo?: string }) => { nome?: string; tipo?: string })) => void;
  loading: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancel: () => void;
}

function CardForm({
  podeEditar,
  nome,
  setNome,
  tipo,
  setTipo,
  pessoal,
  setPessoal,
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
          {podeEditar ? "Editar categoria" : "Nova categoria"}
        </h2>
        <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
          Cancelar
        </Button>
      </div>

      <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2 sm:gap-4" noValidate>
        <div className="flex flex-col gap-1 sm:col-span-2">
          <Label htmlFor="nome">Nome</Label>
          <Input
            id="nome"
            name="nome"
            type="text"
            placeholder="Nome da categoria"
            value={nome}
            onChange={(e) => {
              setNome(e.target.value);
              if (errosForm.nome) setErrosForm((atual) => ({ ...atual, nome: undefined }));
            }}
            disabled={loading}
            className="h-11"
            aria-invalid={errosForm.nome ? true : undefined}
          />
          {errosForm.nome && <p className="text-sm text-destructive">{errosForm.nome}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <Label htmlFor="tipo">Tipo</Label>
          <select
            id="tipo"
            name="tipo"
            value={tipo}
            onChange={(e) => setTipo(e.target.value)}
            disabled={loading}
            className="h-11 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
            aria-invalid={errosForm.tipo ? true : undefined}
          >
            <option value="ENTRADA">Entrada</option>
            <option value="SAÍDA">Saída</option>
          </select>
          {errosForm.tipo && <p className="text-sm text-destructive">{errosForm.tipo}</p>}
        </div>

        <div className="flex items-center gap-2 sm:col-span-2">
          <input
            id="pessoal"
            name="pessoal"
            type="checkbox"
            checked={pessoal}
            onChange={(e) => setPessoal(e.target.checked)}
            disabled={loading}
            className="h-4 w-4 rounded border-input text-primary focus:ring-primary"
          />
          <Label htmlFor="pessoal" className="text-sm text-foreground">
            Categoria pessoal
          </Label>
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
            ) : podeEditar ? "Atualizar" : "Criar"
            }
          </Button>
        </div>
      </form>
    </div>
  );
}
