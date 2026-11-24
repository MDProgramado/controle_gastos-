"use client";

import { useMemo, useState, useTransition } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Spinner } from "@/components/Spinner";
import type { CategoriaGrupo503020Data } from "@/lib/api";
import {
  GRUPOS_503020,
  grupoLabel,
  grupoPercentual,
  isGrupo503020,
  type Grupo503020,
} from "@/lib/grupos-503020";

type Props = {
  itens: CategoriaGrupo503020Data[];
  podeEditar: boolean;
  onSalvar: (
    categoriaId: number,
    grupo: Grupo503020,
  ) => Promise<CategoriaGrupo503020Data | null>;
};

export function Grupos503020Client({ itens, podeEditar, onSalvar }: Props) {
  const [selecoes, setSelecoes] = useState<Record<number, Grupo503020>>(() =>
    Object.fromEntries(itens.map((item) => [item.categoriaId, item.grupo])),
  );
  const [mensagem, setMensagem] = useState<
    { tipo: "sucesso" | "erro"; texto: string } | null
  >(null);
  const [pendente, startTransition] = useTransition();

  const alterados = useMemo(
    () => itens.filter((item) => selecoes[item.categoriaId] !== item.grupo),
    [itens, selecoes],
  );

  function salvar() {
    if (alterados.length === 0) return;
    setMensagem(null);
    startTransition(async () => {
      for (const item of alterados) {
        const salvo = await onSalvar(item.categoriaId, selecoes[item.categoriaId]);
        if (!salvo) {
          setMensagem({
            tipo: "erro",
            texto: `Não foi possível salvar “${item.nome}”. Tente novamente.`,
          });
          return;
        }
      }
      setMensagem({
        tipo: "sucesso",
        texto: "Mapeamento 50/30/20 salvo com sucesso.",
      });
    });
  }

  return (
    <Card className="rounded-2xl border-border/70 bg-card">
      <CardHeader className="gap-4 border-b border-border/60 pb-5 sm:flex-row sm:items-start sm:justify-between">
        <div className="space-y-1.5">
          <CardTitle className="text-lg font-semibold tracking-tight">
            Regras 50/30/20
          </CardTitle>
          <p className="max-w-[60ch] text-sm leading-relaxed text-muted-foreground">
            Enquadre cada categoria de saída em necessidades, desejos ou futuro.
            Categorias sem mapeamento aparecem como desejos.
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2">
          {GRUPOS_503020.map((grupo) => (
            <Badge key={grupo} variant="secondary" className="tabular-nums">
              {grupoLabel(grupo)} {grupoPercentual(grupo)}%
            </Badge>
          ))}
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {mensagem && (
          <Alert variant={mensagem.tipo === "erro" ? "destructive" : "default"}>
            <AlertDescription>{mensagem.texto}</AlertDescription>
          </Alert>
        )}

        {itens.length === 0 ? (
          <p className="rounded-xl border border-dashed border-border/70 p-6 text-center text-sm text-muted-foreground">
            Cadastre uma categoria de saída para definir suas regras 50/30/20.
          </p>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-border/60">
            <table className="w-full min-w-[34rem] text-sm">
              <thead className="bg-muted/40 text-left text-xs uppercase tracking-wider text-muted-foreground">
                <tr>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Categoria
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Origem
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Grupo
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    Meta
                  </th>
                </tr>
              </thead>
              <tbody>
                {itens.map((item) => {
                  const grupo = selecoes[item.categoriaId] ?? item.grupo;
                  return (
                    <tr key={item.categoriaId} className="border-t border-border/50">
                      <td className="px-4 py-3 font-medium text-foreground">
                        {item.nome}
                      </td>
                      <td className="px-4 py-3">
                        {item.padrao ? (
                          <Badge variant="outline">Padrão</Badge>
                        ) : (
                          <Badge variant="secondary">Definido</Badge>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {podeEditar ? (
                          <Select
                            value={grupo}
                            onValueChange={(valor) => {
                              if (!isGrupo503020(valor)) return;
                              setSelecoes((anteriores) => ({
                                ...anteriores,
                                [item.categoriaId]: valor,
                              }));
                            }}
                          >
                            <SelectTrigger
                              className="h-9 w-full min-w-[10rem]"
                              aria-label={`Grupo 50/30/20 de ${item.nome}`}
                            >
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {GRUPOS_503020.map((opcao) => (
                                <SelectItem key={opcao} value={opcao}>
                                  {grupoLabel(opcao)}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        ) : (
                          <span className="text-muted-foreground">
                            {grupoLabel(grupo)}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right tabular-nums text-muted-foreground">
                        {grupoPercentual(grupo)}%
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {podeEditar && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/50 pt-4">
            <span className="text-xs text-muted-foreground">
              {alterados.length === 0
                ? "Nenhuma alteração pendente."
                : `${alterados.length} ${
                    alterados.length === 1
                      ? "alteração pendente"
                      : "alterações pendentes"
                  }`}
            </span>
            <Button onClick={salvar} disabled={pendente || alterados.length === 0}>
              {pendente ? (
                <>
                  <Spinner className="mr-2 h-4 w-4" />
                  Salvando
                </>
              ) : (
                "Salvar mapeamento"
              )}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
