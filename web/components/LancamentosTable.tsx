"use client";

import { Pencil, Trash2, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export interface LancamentoItem {
  id: string;
  descricao: string;
  valor: number;
  tipo: "ENTRADA" | "SAIDA";
  data: string;
  categoriaId?: string;
  categoriaNome: string;
  categoriaTipo?: "ENTRADA" | "SAIDA";
}

interface LancamentosTableProps {
  lancamentos: LancamentoItem[];
  paginaAtual: number;
  pageSize?: number;
  onPageChange: (novaPagina: number) => void;
  onEdit: (lancamento: LancamentoItem) => void;
  onDelete: (lancamento: LancamentoItem) => void;
}

const formatadorMoeda = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

function formatarDataBr(dataIso: string): string {
  const apenasData = dataIso.slice(0, 10);
  const [ano, mes, dia] = apenasData.split("-");
  if (!ano || !mes || !dia) return dataIso;
  return `${dia}/${mes}/${ano}`;
}

export function LancamentosTable({
  lancamentos,
  paginaAtual,
  pageSize = 10,
  onPageChange,
  onEdit,
  onDelete,
}: LancamentosTableProps) {
  const totalItens = lancamentos.length;
  const totalPaginas = Math.max(1, Math.ceil(totalItens / pageSize));
  const paginaSegura = Math.min(Math.max(1, paginaAtual), totalPaginas);

  const inicio = (paginaSegura - 1) * pageSize;
  const paginados = lancamentos.slice(inicio, inicio + pageSize);

  return (
    <div className="space-y-4">
      <div className="w-full overflow-x-auto">
        <div className="rounded-md border min-w-0">
          <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Data</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Categoria</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead className="text-right">Valor</TableHead>
              <TableHead className="w-[100px] text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {paginados.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="h-24 text-center text-muted-foreground"
                >
                  Nenhum lançamento encontrado para os filtros selecionados.
                </TableCell>
              </TableRow>
            ) : (
              paginados.map((item) => {
                const isEntrada = item.tipo === "ENTRADA";
                return (
                  <TableRow key={item.id}>
                    <TableCell className="whitespace-nowrap px-4">
                      {formatarDataBr(item.data)}
                    </TableCell>
                    <TableCell className="font-medium px-4">
                      {item.descricao}
                    </TableCell>
                    <TableCell className="px-4">{item.categoriaNome}</TableCell>
                    <TableCell className="px-4">
                      <Badge
                        variant={isEntrada ? "default" : "secondary"}
                        className={
                          isEntrada
                            ? "bg-emerald-600/15 text-emerald-700 hover:bg-emerald-600/20 dark:text-emerald-400"
                            : "bg-rose-600/15 text-rose-700 hover:bg-rose-600/20 dark:text-rose-400"
                        }
                      >
                        {isEntrada ? "Entrada" : "Saída"}
                      </Badge>
                    </TableCell>
                    <TableCell
                      className={`text-right font-semibold tabular-nums px-4 ${
                        isEntrada
                          ? "text-emerald-600 dark:text-emerald-400"
                          : "text-rose-600 dark:text-rose-400"
                      }`}
                    >
                      {isEntrada ? "+ " : "- "}
                      {formatadorMoeda.format(item.valor)}
                    </TableCell>
                    <TableCell className="text-right px-4">
                      <div className="flex justify-end gap-1">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={() => onEdit(item)}
                          aria-label={`Editar ${item.descricao}`}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={() => onDelete(item)}
                          aria-label={`Excluir ${item.descricao}`}
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex flex-col items-center justify-between gap-2 text-sm text-muted-foreground sm:flex-row">
        <div>
          Exibindo{" "}
          <span className="font-medium text-foreground">
            {totalItens === 0 ? 0 : inicio + 1}
          </span>{" "}
          a{" "}
          <span className="font-medium text-foreground">
            {Math.min(inicio + pageSize, totalItens)}
          </span>{" "}
          de{" "}
          <span className="font-medium text-foreground">{totalItens}</span>{" "}
          lançamentos
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onPageChange(paginaSegura - 1)}
            disabled={paginaSegura <= 1}
          >
            <ChevronLeft className="mr-1 h-4 w-4" />
            Anterior
          </Button>
          <span className="px-2 font-medium text-foreground">
            Página {paginaSegura} de {totalPaginas}
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onPageChange(paginaSegura + 1)}
            disabled={paginaSegura >= totalPaginas}
          >
            Próxima
            <ChevronRight className="ml-1 h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  </div>
  );
}
