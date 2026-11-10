"use client"

import * as React from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
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

const lancamentoSchema = z.object({
  categoriaId: z.string().refine(
    (val) => val !== "" && !isNaN(parseInt(val)),
    { message: "Selecione uma categoria." }
  ),
  tipo: z.enum(["ENTRADA", "SAIDA"]).refine(
    (val) => val === "ENTRADA" || val === "SAIDA",
    { message: "Selecione o tipo (entrada ou saída)." },
  ),
  valor: z
    .string()
    .refine((val) => {
      const v = parseFloat(val.replace(",", "."))
      return !isNaN(v) && v > 0
    }, {
      message: "O valor deve ser maior que zero.",
    })
    .refine((val) => {
      const v = parseFloat(val.replace(",", "."))
      return !isNaN(v) || /^\d+(\.\d{1,2})?$/.test(val)
    }, {
      message: "O valor aceita no máximo 2 casas decimais.",
    }),
  data: z.string().refine(
    (val) => !isNaN(new Date(val).getTime()),
    { message: "Informe uma data válida." }
  ),
  descricao: z.string().optional(),
})

type LancamentoFormValues = z.infer<typeof lancamentoSchema>

export function LancamentoFormDialog({
  isOpen,
  onClose,
  onSubmit,
  categorias,
  modo,
  lancamento,
}: LancamentoFormDialogProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LancamentoFormValues>({
    resolver: zodResolver(lancamentoSchema),
    defaultValues: {
      categoriaId: lancamento?.categoriaId?.toString() ?? undefined,
      tipo: (lancamento?.tipo || "ENTRADA") as TipoLancamento,
      valor: lancamento?.valor.toString() ?? "",
      data: lancamento?.data ?? "",
      descricao: lancamento?.descricao ?? "",
    },
  })

  const chaveDialog = lancamento ? `editar-${lancamento.id}` : "novo"

  const categoriasFiltradas = categorias.filter(
    (c) => c.tipo === (lancamento?.tipo || "ENTRADA") && c.ativo,
  )

  async function onFormSubmit(data: LancamentoFormValues) {
    const resultado = await onSubmit({
      categoriaId: parseInt(data.categoriaId),
      tipo: data.tipo,
      valor: data.valor,
      data: data.data,
      descricao: data.descricao || undefined,
    })
    if (resultado) {
      onClose()
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
          <form
            onSubmit={handleSubmit(onFormSubmit)}
            className="flex flex-col gap-4"
            noValidate
          >
            <div className="flex flex-col gap-1">
              <Label htmlFor="categoria">Categoria</Label>
              <Select
                {...register("categoriaId")}
                disabled={isSubmitting}
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
              {errors.categoriaId && (
                <p className="text-sm text-destructive mt-1">
                  {errors.categoriaId.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="tipo">Tipo</Label>
              <select
                id="tipo"
                {...register("tipo")}
                disabled={isSubmitting}
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
              >
                <option value="ENTRADA">Entrada</option>
                <option value="SAIDA">Saída</option>
              </select>
              {errors.tipo && (
                <p className="text-sm text-destructive mt-1">
                  {errors.tipo.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="valor">Valor</Label>
              <Input
                id="valor"
                {...register("valor")}
                type="text"
                placeholder="0,00"
                disabled={isSubmitting}
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
              />
              {errors.valor && (
                <p className="text-sm text-destructive mt-1">
                  {errors.valor.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="data">Data</Label>
              <Input
                id="data"
                {...register("data")}
                type="date"
                disabled={isSubmitting}
                className="h-11 w-full rounded-lg border border-input bg-transparent px-4 py-1 text-base focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50"
              />
              {errors.data && (
                <p className="text-sm text-destructive mt-1">
                  {errors.data.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <Label htmlFor="descricao">Descrição</Label>
              <Input
                id="descricao"
                {...register("descricao")}
                type="text"
                placeholder="Descrição do lançamento"
                disabled={isSubmitting}
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
                disabled={isSubmitting}
                className="h-11 w-auto font-semibold"
              >
                {isSubmitting ? (
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
