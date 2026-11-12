"use client"

import {
  useCallback,
  useMemo,
  useState,
  type FormEvent,
} from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Spinner } from "@/components/Spinner"
import { Switch } from "@/components/ui/switch"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { Dialog } from "@/components/ui/dialog"
import { DialogTrigger } from "@/components/ui/dialog"
import { DialogContent } from "@/components/ui/dialog"
import { DialogHeader } from "@/components/ui/dialog"
import { DialogTitle } from "@/components/ui/dialog"
import { DialogDescription } from "@/components/ui/dialog"
import { DropdownMenu } from "@/components/ui/dropdown-menu"
import { DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { DropdownMenuContent } from "@/components/ui/dropdown-menu"
import { DropdownMenuItem } from "@/components/ui/dropdown-menu"
import { AlertDialog } from "@/components/ui/alert-dialog"
import { AlertDialogTrigger as AlertDialogTriggerPrimitive } from "@/components/ui/alert-dialog"
import { AlertDialogContent } from "@/components/ui/alert-dialog"
import { AlertDialogCancel } from "@/components/ui/alert-dialog"
import { AlertDialogAction } from "@/components/ui/alert-dialog"
import { AlertDialogTitle } from "@/components/ui/alert-dialog"
import { AlertDialogDescription } from "@/components/ui/alert-dialog"
import { Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip"
import { Edit, Trash2, Check, X } from "lucide-react"

interface CategoriaData {
  id: number
  nome: string
  tipo: string
  pessoal: boolean
  ativo: boolean
}

interface CategoriasClientProps {
  categorias: CategoriaData[]
  userRole: "ADMIN" | "USER"
  onCriar: (
    request: { nome: string; tipo: string; pessoal: boolean },
  ) => Promise<CategoriaData | null>
  onEditar: (
    id: number,
    request: { nome: string; tipo: string; pessoal: boolean },
  ) => Promise<CategoriaData | null>
  onDesativar: (id: number) => Promise<boolean>
}

const SAIDAS_LABEL = "Saídas"
const ENTRADAS_LABEL = "Entradas"

export function CategoriasClient({
  categorias: categoriasIniciais,
  userRole,
  onCriar,
  onEditar,
  onDesativar,
}: CategoriasClientProps) {
  const [categorias, setCategorias] = useState<CategoriaData[]>(categoriasIniciais)
  const [loading, setLoading] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)

  // Estado do formulário (criar/editar)
  const [formAberto, setFormAberto] = useState(false)
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [nome, setNome] = useState("")
  const [tipo, setTipo] = useState("ENTRADA")
  const [pessoal, setPessoal] = useState(false)
  const [errosForm, setErrosForm] = useState<{ nome?: string; tipo?: string }>({})

  // AlertDialog de desativação
  const [excluirId, setExcluirId] = useState<number | null>(null)
  const [excluirAberto, setExcluirAberto] = useState(false)

  // Nome da categoria em foco no AlertDialog ("desativar a categoria X?")
  const categoriaParaDesativar = useMemo(
    () => categorias.find((c) => c.id === excluirId) ?? null,
    [categorias, excluirId],
  )

  const podeEscrever = userRole === "ADMIN"

  const saidas = useMemo(
    () => categorias.filter((c) => c.tipo === "SAIDA"),
    [categorias],
  )
  const entradas = useMemo(
    () => categorias.filter((c) => c.tipo === "ENTRADA"),
    [categorias],
  )

  const resetForm = useCallback(() => {
    setEditandoId(null)
    setNome("")
    setTipo("ENTRADA")
    setPessoal(false)
    setErrosForm({})
  }, [])

  const abrirCriar = useCallback(() => {
    if (!podeEscrever) return
    resetForm()
    setFormAberto(true)
  }, [podeEscrever, resetForm])

  const abrirEditar = useCallback(
    (c: CategoriaData) => {
      if (!podeEscrever) return
      setEditandoId(c.id)
      setNome(c.nome)
      setTipo(c.tipo as "ENTRADA" | "SAIDA")
      setPessoal(c.pessoal)
      setFormAberto(true)
    },
    [podeEscrever],
  )

  function validarForm(): boolean {
    const novos: { nome?: string; tipo?: string } = {}
    if (!nome.trim()) novos.nome = "Informe o nome da categoria."
    if (!tipo) novos.tipo = "Selecione o tipo (entrada ou saída)."
    setErrosForm(novos)
    return Object.keys(novos).length === 0
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!podeEscrever) return
    if (!validarForm()) return

    setLoading(true)
    setErro(null)
    setSucesso(null)

    const request = {
      nome: nome.trim(),
      tipo,
      pessoal,
    }

    try {
      if (editandoId !== null) {
        const resultado = await onEditar(editandoId, request)
        if (resultado) {
          setCategorias((atual) =>
            atual.map((c) => (c.id === editandoId ? { ...c, ...resultado } : c)),
          )
          setSucesso("Categoria atualizada com sucesso.")
          setFormAberto(false)
        } else {
          setErro("Não foi possível atualizar a categoria.")
        }
      } else {
        const resultado = await onCriar(request)
        if (resultado) {
          setCategorias((atual) => [resultado, ...atual])
          setSucesso("Categoria criada com sucesso.")
          setFormAberto(false)
        } else {
          setErro("Não foi possível criar a categoria.")
        }
      }
    } catch {
      setErro("Não foi possível conectar. Tente novamente em instantes.")
    } finally {
      setLoading(false)
    }
  }

  async function toggleStatus(c: CategoriaData) {
    if (!podeEscrever) return
    setLoading(true)
    setErro(null)
    try {
      // Desativar é a única operação disponível via DELETE (soft delete).
      // Quando o switch é desligado, desativa a categoria.
      // Quando ligado, apenas atualiza o estado local (sem endpoint de reativação no backend).
      if (c.ativo) {
        const ok = await onDesativar(c.id)
        if (ok) {
          setCategorias((atual) =>
            atual.map((cat) =>
              cat.id === c.id ? { ...cat, ativo: false } : cat,
            ),
          )
          setSucesso("Categoria desativada com sucesso.")
        } else {
          setErro("Não foi possível alterar o status da categoria.")
        }
      } else {
        // Sem endpoint de reativação; apenas atualiza o estado local otimisticamente.
        setCategorias((atual) =>
          atual.map((cat) => (cat.id === c.id ? { ...cat, ativo: true } : cat)),
        )
        setSucesso("Categoria reativada (apenas local).")
      }
    } catch {
      setErro("Não foi possível conectar. Tente novamente em instantes.")
    } finally {
      setLoading(false)
    }
  }

  async function confirmarExcluir() {
    if (excluirId === null) return
    setLoading(true)
    try {
      const ok = await onDesativar(excluirId)
      if (ok) {
        setCategorias((atual) =>
          atual.map((c) => (c.id === excluirId ? { ...c, ativo: false } : c)),
        )
        setSucesso("Categoria excluída com sucesso.")
      } else {
        setErro("Não foi possível excluir a categoria.")
      }
    } catch {
      setErro("Não foi possível conectar. Tente novamente em instantes.")
    } finally {
      setExcluirAberto(false)
      setExcluirId(null)
      setLoading(false)
    }
  }

  const badgeVariants = {
    ENTRADA:
      "border-positive/40 text-positive bg-positive/10",
    SAIDA:
      "border-destructive/40 text-destructive bg-destructive/10",
  }

  return (
    <div className="space-y-6">
      {/* Cabeçalho + ações — o <h1> da página vive no hero (page.tsx) */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        {/* Momento counter-rolls: as contagens reais rolam uma vez ao entrar */}
        <div
          className="grid w-full max-w-xl grid-cols-3 gap-x-6"
          data-moment="counter-rolls"
        >
          <div className="flex flex-col border-t border-border pt-3">
            <span className="text-xs font-medium text-muted-foreground">
              Total
            </span>
            <p className="t-stat mt-1.5 text-2xl font-semibold tabular-nums text-foreground">
              {categorias.length}
            </p>
          </div>
          <div className="flex flex-col border-t border-border pt-3">
            <span className="text-xs font-medium text-muted-foreground">
              Entradas
            </span>
            <p className="t-stat mt-1.5 text-2xl font-semibold tabular-nums text-positive">
              {entradas.length}
            </p>
          </div>
          <div className="flex flex-col border-t border-border pt-3">
            <span className="text-xs font-medium text-muted-foreground">
              Saídas
            </span>
            <p className="t-stat mt-1.5 text-2xl font-semibold tabular-nums text-destructive">
              {saidas.length}
            </p>
          </div>
        </div>

        {podeEscrever && (
          <Button
            type="button"
            variant="default"
            onClick={abrirCriar}
            disabled={loading}
            className="h-11 shrink-0"
          >
            {loading ? (
              <span className="flex items-center gap-1">
                <Spinner className="mr-1" />
                Criando…
              </span>
            ) : (
              <>
                <span className="mr-2">+</span> Nova categoria
              </>
            )}
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

      {/* Seção Saídas */}
      {saidas.length > 0 && (
        <section className="rounded-lg border border-input bg-card shadow-sm">
          <div className="border-b border-border px-4 py-3">
            <h2 className="text-sm font-semibold text-foreground">
              {SAIDAS_LABEL} ({saidas.length})
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead>
                <tr className="border-b bg-muted text-muted-foreground">
                  <th className="p-4">Nome</th>
                  <th className="p-4">Tipo</th>
                  {/* Abaixo de md a coluna "Pessoal" some: Nome, Tipo, Status e
                      Ações cabem na tela, e a tabela ainda rola se precisar. */}
                  <th className="hidden p-4 md:table-cell">Pessoal</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {saidas.map((c) => renderCategoriaRow(c))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Seção Entradas */}
      {entradas.length > 0 && (
        <section className="rounded-lg border border-input bg-card shadow-sm">
          <div className="border-b border-border px-4 py-3">
            <h2 className="text-sm font-semibold text-foreground">
              {ENTRADAS_LABEL} ({entradas.length})
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead>
                <tr className="border-b bg-muted text-muted-foreground">
                  <th className="p-4">Nome</th>
                  <th className="p-4">Tipo</th>
                  {/* Abaixo de md a coluna "Pessoal" some: Nome, Tipo, Status e
                      Ações cabem na tela, e a tabela ainda rola se precisar. */}
                  <th className="hidden p-4 md:table-cell">Pessoal</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {entradas.map((c) => renderCategoriaRow(c))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Empty state */}
      {categorias.length === 0 && (
        <div className="rounded-lg border border-input bg-card shadow-sm p-12 text-center">
          <p className="text-sm text-muted-foreground">
            Nenhuma categoria cadastrada
          </p>
          {podeEscrever && (
            <div className="mt-4">
              <Button type="button" variant="outline" onClick={abrirCriar}>
                Adicionar categoria
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Dialog criar/editar */}
      {podeEscrever && formAberto && (
        <Dialog open={formAberto} onOpenChange={setFormAberto}>
          <DialogTrigger asChild>
            <button style={{ display: "none" }} aria-hidden="true" />
          </DialogTrigger>
          <DialogContent className="w-full max-w-[480px]">
            <DialogHeader>
              <DialogTitle>
                {editandoId ? "Editar categoria" : "Nova categoria"}
              </DialogTitle>
              <DialogDescription>
                {editandoId
                  ? "Altere o nome ou o tipo da categoria."
                  : "Crie uma categoria de entrada ou de saída."}
              </DialogDescription>
            </DialogHeader>
            {/* O <form> fica FORA do DialogDescription: o Radix renderiza a
                descrição como <p> e <form>/<div> dentro de <p> quebra a
                hidratação ("<div> cannot be a descendant of <p>"). */}
            <form
              onSubmit={onSubmit}
              className="grid gap-4 sm:grid-cols-2 sm:gap-4"
              noValidate
            >
              <div className="flex flex-col gap-1 sm:col-span-2">
                <Label htmlFor="nome">Nome</Label>
                <Input
                  id="nome"
                  name="nome"
                  type="text"
                  placeholder="Nome da categoria"
                  value={nome}
                  onChange={(e) => {
                    setNome(e.target.value)
                    if (errosForm.nome)
                      setErrosForm((atual) => ({ ...atual, nome: undefined }))
                  }}
                  disabled={loading}
                  className="h-11"
                  aria-invalid={errosForm.nome ? true : undefined}
                />
                {errosForm.nome && (
                  <p className="text-sm text-destructive">{errosForm.nome}</p>
                )}
              </div>

              <div className="flex flex-col gap-1">
                <Label htmlFor="tipo">Tipo</Label>
                <Select
                  value={tipo}
                  onValueChange={setTipo}
                  disabled={loading}
                >
                  <SelectTrigger
                    id="tipo"
                    className="h-11 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
                    aria-invalid={errosForm.tipo ? true : undefined}
                  >
                    <SelectValue placeholder="Selecione o tipo" />
                  </SelectTrigger>
                  <SelectContent position="popper">
                    <SelectItem value="ENTRADA">Entrada</SelectItem>
                    <SelectItem value="SAIDA">Saída</SelectItem>
                  </SelectContent>
                </Select>
                {errosForm.tipo && (
                  <p className="text-sm text-destructive">{errosForm.tipo}</p>
                )}
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
                  Pessoal
                </Label>
              </div>

              <div className="flex items-center justify-end gap-2 sm:col-span-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setFormAberto(false)
                    resetForm()
                  }}
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
                  ) : editandoId ? "Salvar alterações" : "Salvar"}
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      )}

      {/* AlertDialog de desativação — a API faz soft delete (DELETE /api/categorias/{id}
          marca ativo=false), então a ação é DESATIVAR, não excluir. */}
      <AlertDialog open={excluirAberto} onOpenChange={setExcluirAberto}>
        <AlertDialogTriggerPrimitive asChild>
          <button style={{ display: "none" }} aria-hidden="true" />
        </AlertDialogTriggerPrimitive>
        <AlertDialogContent>
          <AlertDialogTitle>Desativar categoria</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja desativar a categoria{" "}
            {categoriaParaDesativar?.nome ?? ""}? Ela deixa de aparecer nas
            telas de lançamento, mas continua no cadastro.
          </AlertDialogDescription>
          <AlertDialogCancel onClick={() => setExcluirAberto(false)}>
            Cancelar
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={confirmarExcluir}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            Desativar
          </AlertDialogAction>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )

  function renderCategoriaRow(c: CategoriaData) {
    const tipoBadge = badgeVariants[c.tipo as keyof typeof badgeVariants]
    const podeEscreverLocal = podeEscrever

    return (
      <tr
        key={c.id}
        className={cn(
          "hover:bg-muted/50 transition-colors",
          !c.ativo && "opacity-50",
        )}
      >
        <td className="p-4">
          <span className={cn("font-medium", !c.ativo && "text-muted-foreground")}>
            {c.nome}
          </span>
        </td>
        <td className="p-4">
          <span
            className={`inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs font-semibold ${tipoBadge}`}
          >
            {c.tipo === "ENTRADA" ? "Entrada" : "Saída"}
          </span>
        </td>
        <td className="hidden p-3 md:table-cell">
          <span className={cn("text-xs", !c.ativo && "text-muted-foreground")}>
            {c.pessoal ? <span className="flex items-center gap-1"><Check className="h-3 w-3" /> Sim</span> : <span className="flex items-center gap-1"><X className="h-3 w-3" /> Não</span>}
          </span>
        </td>
        <td className="p-4">
          <Switch
            checked={c.ativo}
            onCheckedChange={() => toggleStatus(c)}
            disabled={loading || !podeEscreverLocal}
            className="h-4 w-8"
            aria-label={`Alternar status da categoria ${c.nome}`}
          />
        </td>
        <td className="p-4 text-right">
          <div className="flex items-center justify-end gap-2">
            {podeEscreverLocal && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => abrirEditar(c)}
                    disabled={loading}
                    className="h-8 w-8 p-0"
                    aria-label="Editar categoria"
                  >
                    <Edit className="h-3 w-3" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Editar</TooltipContent>
              </Tooltip>
            )}
            {podeEscreverLocal && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-8 w-8 p-0"
                    aria-label="Mais ações"
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem
                    onClick={() => {
                      setExcluirId(c.id)
                      setExcluirAberto(true)
                    }}
                    disabled={loading}
                  >
                    Desativar
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
        </td>
      </tr>
    )
  }
}
