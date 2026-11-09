"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";

/**
 * Seletor de mês: input type="month" + setas ‹ › para navegar mês anterior/
 * próximo. Sincroniza a seleção com o parâmetro de URL ?mes=YYYY-MM via
 * router.push (scroll desativado).
 *
 * Sem atributos min/max no input — meses futuros e passados são permitidos
 * (regras D-14/D-15).
 */
export function MonthSelector() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [mes, setMes] = useState(
    searchParams.get("mes") ?? new Date().toISOString().slice(0, 7),
  );

  // Sempre que o parâmetro de URL muda (ex.: setas ‹ ›), atualiza o input.
  useEffect(() => {
    const urlMes = searchParams.get("mes");
    if (urlMes && urlMes !== mes) {
      setMes(urlMes);
    }
  }, [searchParams, mes]);

  function mudarMes(delta: number) {
    const [ano, mesAtual] = mes.split("-").map(Number);
    const data = new Date(ano, mesAtual - 1 + delta, 1);
    const proximo = data.toISOString().slice(0, 7);
    setMes(proximo);
    router.push(`/?mes=${proximo}`, { scroll: false });
  }

  return (
    <div className="flex items-center gap-4">
      <Label htmlFor="mes-selector">Mês</Label>
      <input
        id="mes-selector"
        type="month"
        value={mes}
        onChange={(e) => setMes(e.target.value)}
        className="w-40"
      />
      <Button type="button" variant="outline" size="icon" onClick={() => mudarMes(-1)}>
        ‹
      </Button>
      <Button type="button" variant="outline" size="icon" onClick={() => mudarMes(1)}>
        ›
      </Button>
    </div>
  );
}
