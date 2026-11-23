import { Card } from "@/components/ui/card";
import { CardContent } from "@/components/ui/card";
import { CardHeader } from "@/components/ui/card";
import { CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";
import { DonutChart } from "@/components/DonutChart";
import {
  agruparCategorias,
  formatarBRL,
  formatarPercentual,
  progressoPercentual,
  type CategoriaTotal,
} from "@/lib/format";
import { centavosAssinadosDeString } from "@/lib/money";

const ZERO = BigInt(0);

type CategoriaGasto = CategoriaTotal;

type CategoryBreakdownProps = {
  categorias: CategoriaGasto[];
  totalSaidas: string;
};

export function CategoryBreakdown({
  categorias,
  totalSaidas,
}: CategoryBreakdownProps) {
  const totalSaidasCentavos =
    centavosAssinadosDeString(totalSaidas) ?? ZERO;
  const agrupadas = agruparCategorias(categorias);

  const hasData = totalSaidasCentavos > ZERO && categorias.length > 0;

  if (!hasData) {
    return (
      <Card className="w-full min-w-0">
        <CardHeader>
          <CardTitle>Gastos por categoria</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col items-center justify-center py-12 text-center">
          <p className="text-sm text-muted-foreground mb-4">
            Nenhuma saída neste mês
          </p>
          <Button variant="outline" asChild>
            <a href="/lancamentos">Adicionar lançamento</a>
          </Button>
        </CardContent>
      </Card>
    );
  }

  const rows = agrupadas.map((categoria) => ({
    id: categoria.id,
    nome: categoria.nome,
    total: formatarBRL(categoria.centavos),
    percentual: formatarPercentual(categoria.centavos, totalSaidasCentavos),
    progressValue: progressoPercentual(categoria.centavos, totalSaidasCentavos),
  }));

  return (
    <Card className="@container w-full min-w-0">
      <CardHeader>
        <CardTitle>Gastos por categoria</CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="flex w-full min-w-0 flex-col gap-4 @min-[640px]:flex-row @min-[640px]:items-start @min-[640px]:gap-7">
          <div className="w-full min-w-0">
            <DonutChart categorias={categorias} totalSaidas={totalSaidas} />
          </div>

          <div className="w-full min-w-0 @min-[640px]:flex-1">
            <Card className="h-full">
              <CardHeader>
                <CardTitle className="text-sm font-semibold">
                  Categorias
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-0">
                <div className="w-full min-w-0 overflow-hidden">
                  <table className="w-full min-w-0 table-fixed text-sm">
                    <thead>
                      <tr className="border-b border-border">
                        <th className="w-[36%] px-2 pb-2 text-left text-muted-foreground @min-[640px]:w-[34%] @min-[640px]:px-4">
                          Categoria
                        </th>
                        <th className="w-[28%] whitespace-nowrap px-2 pb-2 text-right text-muted-foreground @min-[640px]:w-[24%] @min-[640px]:px-4">
                          Valor
                        </th>
                        <th className="w-[36%] whitespace-nowrap px-2 pb-2 text-right text-muted-foreground @min-[640px]:w-[42%] @min-[640px]:px-4">
                          Percentual
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((row) => (
                        <tr key={row.id} className="border-b border-border/50">
                          <td className="truncate px-2 py-3 @min-[640px]:px-4">
                            {row.nome}
                          </td>
                          <td className="whitespace-nowrap px-2 py-3 text-right @min-[640px]:px-4">
                            {row.total}
                          </td>
                          <td className="px-2 py-3 text-right @min-[640px]:px-4">
                            <div className="flex items-center gap-2">
                              <span className="inline-flex shrink-0 items-center justify-center whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium bg-destructive/10 text-destructive">
                                {row.percentual}
                              </span>
                              <Progress
                                value={row.progressValue}
                                className="hidden h-1 min-w-[24px] flex-1 @min-[640px]:block"
                                style={{ transform: "none" }}
                              />
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
