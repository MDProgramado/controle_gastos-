"use client";


import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ChevronLeft, ChevronRight, Calendar } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

/**
 * Seletor de mês: input type="month" + setas ‹ › para navegar mês anterior/
 * próximo. Sincroniza a seleção com o parâmetro de URL ?mes=YYYY-MM via
 * router.push (scroll desativado).
 *
 * Sem atributos min/max no input — meses futuros e passados são permitidos
 * (regras D-14/D-15).
 */

interface MonthSelectorProps {
  mesAtual: string; // Formato esperado: "YYYY-MM"
}

function adicionarMeses(mesIso: string, delta: number): string {
  const [anoStr, mesStr] = mesIso.split("-");
  const ano = Number(anoStr);
  const mes = Number(mesStr);

  if (!ano || !mes) {
    const hoje = new Date();
    return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, "0")}`;
  }

  const data = new Date(ano, mes - 1 + delta, 1);
  const novoAno = data.getFullYear();
  const novoMes = String(data.getMonth() + 1).padStart(2, "0");
  return `${novoAno}-${novoMes}`;
}

function formatarMesExtenso(mesIso: string): string {
  const [anoStr, mesStr] = mesIso.split("-");
  const ano = Number(anoStr);
  const mes = Number(mesStr);

  if (!ano || !mes) return mesIso;

  const data = new Date(ano, mes - 1, 1);
  const texto = new Intl.DateTimeFormat("pt-BR", {
    month: "long",
    year: "numeric",
  }).format(data);

  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export function MonthSelector({ mesAtual }: MonthSelectorProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const navegarParaMes = (novoMes: string) => {
    if (!novoMes) return;
    const params = new URLSearchParams(searchParams.toString());
    params.set("mes", novoMes);
    router.push(`${pathname}?${params.toString()}`);
  };

  const handleMesAnterior = () => {
    navegarParaMes(adicionarMeses(mesAtual, -1));
  };

  const handleProximoMes = () => {
    navegarParaMes(adicionarMeses(mesAtual, 1));
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Button
        type="button"
        variant="outline"
        size="icon"
        onClick={handleMesAnterior}
        aria-label="Mês anterior"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      <div className="flex items-center gap-2 rounded-md border bg-background px-3 py-1.5 text-sm font-medium">
        <Calendar className="h-4 w-4 text-muted-foreground" />
        <span>{formatarMesExtenso(mesAtual)}</span>
      </div>

      <Button
        type="button"
        variant="outline"
        size="icon"
        onClick={handleProximoMes}
        aria-label="Próximo mês"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>

      <Input
        type="month"
        value={mesAtual}
        onChange={(e) => navegarParaMes(e.target.value)}
        className="w-auto"
        aria-label="Selecionar mês e ano"
      />
    </div>
  );
}