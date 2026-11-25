"use client";

import { useCallback, useEffect, useState } from "react";
import { Database, RefreshCw } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Spinner } from "@/components/Spinner";
import {
  iniciarIngestaoRag,
  obterStatusRag,
  type RagSituacao,
  type RagStatus,
} from "@/lib/rag-admin";

const ROTULOS: Record<RagSituacao, string> = {
  PARADO: "Parado",
  EM_ANDAMENTO: "Em andamento",
  SUCESSO: "Atualizado",
  ERRO: "Falhou",
};

function variaveisSituacao(situacao: RagSituacao) {
  if (situacao === "SUCESSO") return "default" as const;
  if (situacao === "ERRO") return "destructive" as const;
  if (situacao === "EM_ANDAMENTO") return "secondary" as const;
  return "outline" as const;
}

function formatarData(valor: string | null): string {
  if (!valor) return "—";
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? valor : data.toLocaleString("pt-BR");
}

function mensagemErro(erro: unknown): string {
  return erro instanceof Error
    ? erro.message
    : "Não foi possível consultar o serviço de reindexação.";
}

export function RagAdminClient() {
  const [status, setStatus] = useState<RagStatus | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [executando, setExecutando] = useState(false);
  const [confirmarAberto, setConfirmarAberto] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    try {
      const atual = await obterStatusRag();
      setStatus(atual);
      setErro(null);
    } catch (e) {
      setErro(mensagemErro(e));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => void carregar());
  }, [carregar]);

  useEffect(() => {
    if (status?.situacao !== "EM_ANDAMENTO") return;
    const temporizador = window.setInterval(() => void carregar(), 5000);
    return () => window.clearInterval(temporizador);
  }, [status?.situacao, carregar]);

  async function executar() {
    setExecutando(true);
    setErro(null);
    try {
      await iniciarIngestaoRag();
      setConfirmarAberto(false);
      await carregar();
    } catch (e) {
      setErro(mensagemErro(e));
    } finally {
      setExecutando(false);
    }
  }

  const situacao = status?.situacao ?? "PARADO";
  const total = status?.total ?? 0;
  const processados = status?.processados ?? 0;
  const percentual =
    situacao === "SUCESSO"
      ? 100
      : total > 0
        ? Math.min(100, Math.round((processados / total) * 100))
        : 0;
  const ocupado = carregando || executando || situacao === "EM_ANDAMENTO";

  return (
    <Card className="rounded-2xl border-border/70 bg-card">
      <CardHeader className="gap-4 border-b border-border/60 pb-5 sm:flex-row sm:items-start sm:justify-between">
        <div className="space-y-1.5">
          <CardTitle className="text-lg font-semibold tracking-tight">
            Reindexação da base
          </CardTitle>
          <p className="max-w-[60ch] text-sm leading-relaxed text-muted-foreground">
            A execução recria os vetores de todos os chunks do corpus
            versionado e substitui a base usada nas respostas da IA.
          </p>
        </div>
        <Badge variant={variaveisSituacao(situacao)} className="shrink-0">
          {carregando ? "Carregando" : ROTULOS[situacao]}
        </Badge>
      </CardHeader>

      <CardContent className="space-y-5">
        {erro && (
          <Alert variant="destructive">
            <AlertDescription>{erro}</AlertDescription>
          </Alert>
        )}

        {status?.erro && (
          <Alert variant="destructive">
            <AlertDescription>
              A última execução falhou: {status.erro}
            </AlertDescription>
          </Alert>
        )}

        <div className="grid gap-4 sm:grid-cols-3">
          <div className="space-y-1 rounded-xl border border-border/60 bg-background/50 p-4">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Chunks indexados
            </span>
            <p className="text-xl font-semibold tabular-nums text-foreground">
              {status?.chunks ?? "—"}
            </p>
          </div>
          <div className="space-y-1 rounded-xl border border-border/60 bg-background/50 p-4">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Progresso
            </span>
            <p className="text-xl font-semibold tabular-nums text-foreground">
              {processados}/{total}
            </p>
          </div>
          <div className="space-y-1 rounded-xl border border-border/60 bg-background/50 p-4">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              Última conclusão
            </span>
            <p className="text-sm font-medium tabular-nums text-foreground">
              {formatarData(status?.finalizadoEm ?? null)}
            </p>
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="inline-flex items-center gap-1.5">
              <Database className="h-3.5 w-3.5" />
              {status?.caminho ?? "Carregando caminho da base..."}
            </span>
            <span className="tabular-nums">{percentual}%</span>
          </div>
          <Progress value={percentual} aria-label="Progresso da reindexação" />
        </div>

        <div className="flex flex-col gap-3 border-t border-border/50 pt-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="max-w-[58ch] text-xs leading-relaxed text-muted-foreground">
            Cada execução consome créditos da API de IA. Confirme apenas quando
            precisar atualizar a base versionada.
          </p>
          <Button
            type="button"
            onClick={() => setConfirmarAberto(true)}
            disabled={ocupado}
            className="shrink-0"
          >
            {executando ? (
              <Spinner className="h-4 w-4" />
            ) : (
              <RefreshCw className="h-4 w-4" />
            )}
            {executando ? "Executando..." : "Reindexar base"}
          </Button>
        </div>
      </CardContent>

      <AlertDialog open={confirmarAberto} onOpenChange={setConfirmarAberto}>
        <AlertDialogContent>
          <AlertDialogTitle>Reindexar base de conhecimento</AlertDialogTitle>
          <AlertDialogDescription>
            Isso substitui os vetores atuais pelo corpus versionado. A operação
            consome créditos da API de IA e pode levar alguns minutos.
          </AlertDialogDescription>
          <div className="flex flex-wrap justify-end gap-2">
            <AlertDialogCancel onClick={() => setConfirmarAberto(false)}>
              Cancelar
            </AlertDialogCancel>
            <AlertDialogAction
              disabled={executando}
              onClick={() => void executar()}
              className="bg-primary text-primary-foreground hover:bg-primary/90"
            >
              Confirmar reindexação
            </AlertDialogAction>
          </div>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
