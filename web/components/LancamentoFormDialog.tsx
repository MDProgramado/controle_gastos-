"use client";

import { useEffect, useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { validarValorLote } from "@/lib/money";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export interface CategoriaOption {
  id: string;
  nome: string;
  tipo: "ENTRADA" | "SAIDA";
  ativo?: boolean;
}

export interface LancamentoFormValues {
  descricao: string;
  valor: string;
  tipo: "ENTRADA" | "SAIDA";
  categoriaId: string;
  data: string;
}

export interface LancamentoParaEdicao {
  id: string;
  descricao: string;
  valor: string;
  tipo: "ENTRADA" | "SAIDA";
  categoriaId: string;
  data: string;
}

const lancamentoSchema = z.object({
  descricao: z.string().min(2, "Informe uma descrição com pelo menos 2 caracteres."),
  valor: z
    .string()
    .min(1, "Informe o valor.")
    .refine((val) => validarValorLote(val) === null, "Informe um valor numérico maior que zero."),
  tipo: z.enum(["ENTRADA", "SAIDA"]),
  categoriaId: z.string().min(1, "Selecione uma categoria."),
  data: z.string().min(1, "Informe a data."),
});

interface LancamentoFormDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (dados: LancamentoFormValues, idEdicao?: string) => Promise<void> | void;
  categorias: CategoriaOption[];
  lancamento?: LancamentoParaEdicao | null;
  isSubmitting?: boolean;
}

export function LancamentoFormDialog({
  isOpen,
  onClose,
  onSubmit,
  categorias,
  lancamento = null,
  isSubmitting = false,
}: LancamentoFormDialogProps) {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<LancamentoFormValues>({
    resolver: zodResolver(lancamentoSchema),
    defaultValues: {
      descricao: "",
      valor: "",
      tipo: "SAIDA",
      categoriaId: "",
      data: new Date().toISOString().slice(0, 10),
    },
  });

  const tipoSelecionado = watch("tipo");
  const categoriaSelecionada = watch("categoriaId");

  const categoriasFiltradas = useMemo(() => {
    return categorias.filter(
      (cat) => cat.tipo === tipoSelecionado && cat.ativo !== false
    );
  }, [categorias, tipoSelecionado]);

  useEffect(() => {
    if (!isOpen) return;

    if (lancamento) {
      reset({
        descricao: lancamento.descricao,
        valor: lancamento.valor,
        tipo: lancamento.tipo,
        categoriaId: lancamento.categoriaId,
        data: lancamento.data.slice(0, 10),
      });
    } else {
      reset({
        descricao: "",
        valor: "",
        tipo: "SAIDA",
        categoriaId: "",
        data: new Date().toISOString().slice(0, 10),
      });
    }
  }, [isOpen, lancamento, reset]);

  const handleTipoChange = (novoTipo: "ENTRADA" | "SAIDA") => {
    setValue("tipo", novoTipo, { shouldValidate: true });
    setValue("categoriaId", "", { shouldValidate: false });
  };

  const handleFormSubmit = async (dados: LancamentoFormValues) => {
    await onSubmit(dados, lancamento?.id);
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto w-[calc(100vw-2rem)] sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>
            {lancamento ? "Editar Lançamento" : "Novo Lançamento"}
          </DialogTitle>
          <DialogDescription>
            {lancamento
              ? "Atualize os dados do lançamento financeiro abaixo."
              : "Preencha os campos para registrar uma nova movimentação."}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="tipo">Tipo</Label>
              <Select
                value={tipoSelecionado}
                onValueChange={(val) => handleTipoChange(val as "ENTRADA" | "SAIDA")}
              >
                <SelectTrigger id="tipo">
                  <SelectValue placeholder="Selecione o tipo" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ENTRADA">Entrada</SelectItem>
                  <SelectItem value="SAIDA">Saída</SelectItem>
                </SelectContent>
              </Select>
              {errors.tipo && (
                <p className="text-xs text-destructive">{errors.tipo.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="data">Data</Label>
              <Input id="data" type="date" {...register("data")} />
              {errors.data && (
                <p className="text-xs text-destructive">{errors.data.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="descricao">Descrição</Label>
            <Input
              id="descricao"
              placeholder="Ex.: Supermercado, Salário, Aluguel..."
              {...register("descricao")}
            />
            {errors.descricao && (
              <p className="text-xs text-destructive">{errors.descricao.message}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="valor">Valor (R$)</Label>
              <Input
                id="valor"
                inputMode="decimal"
                placeholder="0,00"
                {...register("valor")}
              />
              {errors.valor && (
                <p className="text-xs text-destructive">{errors.valor.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="categoriaId">Categoria</Label>
              <Select
                value={categoriaSelecionada}
                onValueChange={(val) =>
                  setValue("categoriaId", val, { shouldValidate: true })
                }
              >
                <SelectTrigger id="categoriaId">
                  <SelectValue placeholder="Selecione..." />
                </SelectTrigger>
                <SelectContent>
                  {categoriasFiltradas.length === 0 ? (
                    <div className="px-2 py-1.5 text-xs text-muted-foreground">
                      Nenhuma categoria ativa para este tipo
                    </div>
                  ) : (
                    categoriasFiltradas.map((cat) => (
                      <SelectItem key={cat.id} value={cat.id}>
                        {cat.nome}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
              {errors.categoriaId && (
                <p className="text-xs text-destructive">
                  {errors.categoriaId.message}
                </p>
              )}
            </div>
          </div>

          <DialogFooter className="pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting
                ? "Salvando..."
                : lancamento
                ? "Salvar alterações"
                : "Criar lançamento"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
