"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { Chart } from "@/components/ui/chart";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const MONETARY = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const PERCENTUAL = new Intl.NumberFormat("pt-BR", {
  style: "percent",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

type CategoriaGasto = {
  id: number;
  nome: string;
  total: string;
};

type CategoryBreakdownProps = {
  categorias: CategoriaGasto[];
  totalSaidas: string;
};

export function CategoryBreakdown({
  categorias,
  totalSaidas,
}: CategoryBreakdownProps) {
  const ordenadas = [...categorias].sort((a, b) => Number(b.total) - Number(a.total));

  const totalSaidasNum = Number(totalSaidas) || 0;

  const percentual = (categoria: CategoriaGasto): string => {
    if (!totalSaidasNum) {
      return "—";
    }
    return PERCENTUAL.format(Number(categoria.total) / totalSaidasNum);
  };

  if (categorias.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Gasto por categoria</CardTitle>
        </CardHeader>
        <CardContent className="text-muted-foreground text-sm py-4">
          Nenhum lançamento neste mês
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>Gasto por categoria</CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left pb-2 text-muted-foreground">
                  Categoria
                </th>
                <th className="text-right pb-2 text-muted-foreground">
                  Valor
                </th>
                <th className="text-right pb-2 text-muted-foreground">
                  Percentual
                </th>
              </tr>
            </thead>
            <tbody>
              {ordenadas.map((categoria) => (
                <tr key={categoria.id} className="border-b border-border/50">
                  <td className="py-4">{categoria.nome}</td>
                  <td className="py-3 text-right">
                    {MONETARY.format(Number(categoria.total))}
                  </td>
                  <td className="py-3 text-right">
                    {percentual(categoria)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <Chart className="mt-6">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={ordenadas.map(c => ({ ...c, total: Number(c.total) }))}
              layout="horizontal"
              margin={{ left: 120, right: 20, top: 20, bottom: 20 }}
            >
              <CartesianGrid horizontal />
              <YAxis dataKey="nome" width={120} />
              <XAxis type="number" />
              <Tooltip
                formatter={(value) => MONETARY.format(value as number)}
              />
              <Bar dataKey="total" fill="var(--chart-1)" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Chart>
      </CardContent>
    </Card>
  );
}
