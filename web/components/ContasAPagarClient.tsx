"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { CheckCircle2, Clock, AlertTriangle, Search } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export interface ContaAPagarItem {
  id: string;
  descricao: string;
  valor: number;
  dataVencimento: string;
  status: "PENDENTE" | "PAGO" | "VENCIDO";
  categoriaNome?: string;
}

interface ContasAPagarClientProps {
  contasIniciais: ContaAPagarItem[];
}

export function ContasAPagarClient({ contasIniciais }: ContasAPagarClientProps) {
  const router = useRouter();
  const [statusFiltro, setStatusFiltro] = useState<string>("TODOS");
  const [busca, setBusca] = useState("");
  const [pagandoId, setPagandoId] = useState<string | null>(null);

  const contasFiltradas = useMemo(() => {
    return contasIniciais.filter((conta) => {
      if (statusFiltro !== "TODOS" && conta.status !== statusFiltro) {
        return false;
      }
      if (busca.trim()) {
        const termo = busca.toLowerCase();
        const bateDesc = conta.descricao.toLowerCase().includes(termo);
        const bateCat = (conta.categoriaNome || "").toLowerCase().includes(termo);
        if (!bateDesc && !bateCat) return false;
      }
      return true;
    });
  }, [contasIniciais, statusFiltro, busca]);

  const totais = useMemo(() => {
    return contasIniciais.reduce(
      (acc, item) => {
        if (item.status === "PAGO") acc.pago += item.valor;
        else if (item.status === "VENCIDO") acc.vencido += item.valor;
        else acc.pendente += item.valor;
        return acc;
      },
      { pendente: 0, vencido: 0, pago: 0 }
    );
  }, [contasIniciais]);

  const formatarMoeda = (valor: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(valor);

  const handleDarBaixa = async (id: string) => {
    try {
      setPagandoId(id);
      const res = await fetch(`/api/contas-a-pagar/${id}/pagar`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          dataPagamento: new Date().toISOString().slice(0, 10),
        }),
      });

      if (!res.ok) {
        throw new Error("Não foi possível dar baixa na conta.");
      }

      router.refresh();
    } catch (err) {
      alert(err instanceof Error ? err.message : "Erro ao dar baixa.");
    } finally {
      setPagandoId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Cards de Resumo */}
      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-xl border border-border bg-card p-5">
          <div className="flex items-center justify-between text-xs font-medium text-muted-foreground">
            <span>A Vencer (Pendente)</span>
            <Clock className="h-4 w-4 text-amber-500" />
          </div>
          <p className="mt-2 text-2xl font-bold text-foreground">
            {formatarMoeda(totais.pendente)}
          </p>
        </div>

        <div className="rounded-xl border border-border bg-card p-5">
          <div className="flex items-center justify-between text-xs font-medium text-muted-foreground">
            <span>Vencidas</span>
            <AlertTriangle className="h-4 w-4 text-destructive" />
          </div>
          <p className="mt-2 text-2xl font-bold text-destructive">
            {formatarMoeda(totais.vencido)}
          </p>
        </div>

        <div className="rounded-xl border border-border bg-card p-5">
          <div className="flex items-center justify-between text-xs font-medium text-muted-foreground">
            <span>Pagas</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-500" />
          </div>
          <p className="mt-2 text-2xl font-bold text-emerald-500">
            {formatarMoeda(totais.pago)}
          </p>
        </div>
      </div>

      {/* Barra de Filtros */}
      <div className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar por descrição ou categoria…"
            className="h-9 w-full rounded-lg border border-input bg-background pl-9 pr-3 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        <select
          value={statusFiltro}
          onChange={(e) => setStatusFiltro(e.target.value)}
          className="h-9 rounded-lg border border-input bg-background px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="TODOS">Todos os status</option>
          <option value="PENDENTE">Pendentes</option>
          <option value="VENCIDO">Vencidas</option>
          <option value="PAGO">Pagas</option>
        </select>
      </div>

      {/* Tabela */}
      <div className="w-full overflow-x-auto">
        <div className="rounded-xl border border-border bg-card min-w-0">
          <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Descrição</TableHead>
              <TableHead>Categoria</TableHead>
              <TableHead>Vencimento</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Valor</TableHead>
              <TableHead className="text-right">Ação</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {contasFiltradas.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="py-10 text-center text-sm text-muted-foreground"
                >
                  Nenhuma conta encontrada para os filtros selecionados.
                </TableCell>
              </TableRow>
            ) : (
              contasFiltradas.map((conta) => (
                <TableRow key={conta.id}>
                  <TableCell className="font-medium px-4">{conta.descricao}</TableCell>
                  <TableCell className="text-muted-foreground px-4">
                    {conta.categoriaNome || "—"}
                  </TableCell>
                  <TableCell className="px-4">
                    {new Date(`${conta.dataVencimento.slice(0, 10)}T12:00:00`).toLocaleDateString(
                      "pt-BR"
                    )}
                  </TableCell>
                  <TableCell className="px-4">
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        conta.status === "PAGO"
                          ? "bg-emerald-500/10 text-emerald-500"
                          : conta.status === "VENCIDO"
                          ? "bg-destructive/10 text-destructive"
                          : "bg-amber-500/10 text-amber-500"
                      }`}
                    >
                      {conta.status === "PAGO"
                        ? "Pago"
                        : conta.status === "VENCIDO"
                        ? "Vencido"
                        : "Pendente"}
                    </span>
                  </TableCell>
                  <TableCell className="text-right font-semibold px-4">
                    {formatarMoeda(conta.valor)}
                  </TableCell>
                  <TableCell className="text-right px-4">
                    {conta.status !== "PAGO" ? (
                      <button
                        type="button"
                        disabled={pagandoId === conta.id}
                        onClick={() => handleDarBaixa(conta.id)}
                        className="rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
                      >
                        {pagandoId === conta.id ? "Baixando…" : "Dar baixa"}
                      </button>
                    ) : (
                      <span className="text-xs text-muted-foreground">Quitada</span>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  </div>
  );
}
