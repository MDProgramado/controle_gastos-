"use client";

import * as React from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
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
  const urlMes = searchParams.get("mes") ?? new Date().toISOString().slice(0, 7);

  function mudarMes(delta: number) {
    const [ano, mesAtual] = urlMes.split("-").map(Number);
    const data = new Date(ano, mesAtual - 1 + delta, 1);
    const proximo = data.toISOString().slice(0, 7);
    router.push(`/?mes=${proximo}`, { scroll: false });
  }

  function onChangeMes(event: React.ChangeEvent<HTMLInputElement>) {
    router.push(`/?mes=${event.target.value}`, { scroll: false });
  }

  return (
    <div className="flex items-center gap-4" key={urlMes}>
      <Label htmlFor="mes-selector" className="font-semibold">
        Mês
      </Label>
      <Input
        id="mes-selector"
        name="mes-selector"
        type="month"
        value={urlMes}
        onChange={onChangeMes}
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
