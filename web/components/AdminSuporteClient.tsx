"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { useRouter } from "next/navigation";
import { ChevronRight, Inbox, RefreshCw, Send } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/Spinner";
import {
  classesStatus,
  LIMITE_DESCRICAO,
  rotuloStatus,
  type PaginaTickets,
  type StatusTicket,
  type TicketDetalhe,
  type TicketMensagem,
  type TicketResumo,
} from "@/lib/helpdesk";
import { cn } from "@/lib/utils";

const FORMATO_DATA: Intl.DateTimeFormatOptions = {
  day: "2-digit",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
};

const ERRO_CONEXAO = "Não foi possível conectar. Tente novamente em instantes.";

const STATUS_OPCOES: StatusTicket[] = [
  "ABERTO",
  "EM_ANDAMENTO",
  "RESOLVIDO",
  "FECHADO",
];

const FILTROS = [
  { valor: "", rotulo: "Todos os status" },
  ...STATUS_OPCOES.map((status) => ({
    valor: status,
    rotulo: rotuloStatus(status),
  })),
];

const CLASSE_CAMPO =
  "h-10 w-full rounded-lg border border-input bg-background px-3 text-sm text-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50";

type Estado = "carregando" | "pronto" | "erro";

async function lerErro(resposta: Response): Promise<string> {
  const corpo = (await resposta.json().catch(() => ({}))) as {
    detail?: string;
    erro?: string;
  };
  return corpo.detail ?? corpo.erro ?? ERRO_CONEXAO;
}

function formatarData(valor: string): string {
  return new Date(valor).toLocaleString("pt-BR", FORMATO_DATA);
}

export function AdminSuporteClient() {
  const router = useRouter();

  const [filtro, setFiltro] = useState("");
  const [tickets, setTickets] = useState<TicketResumo[]>([]);
  const [total, setTotal] = useState(0);
  const [estadoLista, setEstadoLista] = useState<Estado>("carregando");
  const [erroLista, setErroLista] = useState<string | null>(null);

  const [selecionadoId, setSelecionadoId] = useState<number | null>(null);
  const [detalhe, setDetalhe] = useState<TicketDetalhe | null>(null);
  const [estadoDetalhe, setEstadoDetalhe] = useState<Estado>("pronto");
  const [erroDetalhe, setErroDetalhe] = useState<string | null>(null);
  const [novoStatus, setNovoStatus] = useState("");
  const [alterandoStatus, setAlterandoStatus] = useState(false);
  const [erroStatus, setErroStatus] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const [mensagem, setMensagem] = useState("");
  const [erroMensagem, setErroMensagem] = useState<string | null>(null);
  const [enviandoMensagem, setEnviandoMensagem] = useState(false);

  const selecaoAtiva = useRef(0);
  const carregamentoAtivo = useRef(0);

  const buscarTickets = useCallback(
    async (status: string): Promise<PaginaTickets | null> => {
      const parametros = new URLSearchParams({ page: "0", size: "50" });
      if (status) parametros.set("status", status);

      const res = await fetch(
        `/api/admin/helpdesk/tickets?${parametros.toString()}`,
        { cache: "no-store" },
      );
      if (res.status === 401) {
        router.push("/login?expirada=1");
        return null;
      }
      if (!res.ok) throw new Error(await lerErro(res));
      return (await res.json()) as PaginaTickets;
    },
    [router],
  );

  const carregarTickets = useCallback(
    async (status: string) => {
      const seq = ++carregamentoAtivo.current;
      setEstadoLista("carregando");
      setErroLista(null);

      try {
        const dados = await buscarTickets(status);
        if (seq !== carregamentoAtivo.current || !dados) return;
        setTickets(Array.isArray(dados.tickets) ? dados.tickets : []);
        setTotal(
          typeof dados.totalElementos === "number" ? dados.totalElementos : 0,
        );
        setEstadoLista("pronto");
      } catch (erro) {
        if (seq !== carregamentoAtivo.current) return;
        setErroLista(erro instanceof Error ? erro.message : ERRO_CONEXAO);
        setEstadoLista("erro");
      }
    },
    [buscarTickets],
  );

  useEffect(() => {
    const seq = ++carregamentoAtivo.current;
    void buscarTickets(filtro)
      .then((dados) => {
        if (seq !== carregamentoAtivo.current || !dados) return;
        setTickets(Array.isArray(dados.tickets) ? dados.tickets : []);
        setTotal(
          typeof dados.totalElementos === "number" ? dados.totalElementos : 0,
        );
        setEstadoLista("pronto");
      })
      .catch((erro: unknown) => {
        if (seq !== carregamentoAtivo.current) return;
        setErroLista(erro instanceof Error ? erro.message : ERRO_CONEXAO);
        setEstadoLista("erro");
      });
  }, [buscarTickets, filtro]);

  const abrirTicket = useCallback(
    async (id: number) => {
      const seq = ++selecaoAtiva.current;
      setSelecionadoId(id);
      setDetalhe(null);
      setEstadoDetalhe("carregando");
      setErroDetalhe(null);
      setMensagem("");
      setErroMensagem(null);
      setNovoStatus("");
      setErroStatus(null);
      setAviso(null);

      try {
        const res = await fetch(`/api/admin/helpdesk/tickets/${id}`, {
          cache: "no-store",
        });
        if (res.status === 401) {
          router.push("/login?expirada=1");
          return;
        }
        if (!res.ok) throw new Error(await lerErro(res));
        if (seq !== selecaoAtiva.current) return;

        const dados = (await res.json()) as TicketDetalhe;
        setDetalhe(dados);
        setNovoStatus(
          STATUS_OPCOES.includes(dados.status as StatusTicket)
            ? dados.status
            : "",
        );
        setEstadoDetalhe("pronto");
      } catch (erro) {
        if (seq !== selecaoAtiva.current) return;
        setErroDetalhe(erro instanceof Error ? erro.message : ERRO_CONEXAO);
        setEstadoDetalhe("erro");
      }
    },
    [router],
  );

  function aoFiltrar(valor: string) {
    selecaoAtiva.current += 1;
    setFiltro(valor);
    setEstadoLista("carregando");
    setErroLista(null);
    setSelecionadoId(null);
    setDetalhe(null);
    setEstadoDetalhe("pronto");
    setErroDetalhe(null);
    setAviso(null);
  }

  async function enviarMensagem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!detalhe) return;

    const conteudo = mensagem.trim();
    if (!conteudo) {
      setErroMensagem("Informe sua mensagem.");
      return;
    }
    if (conteudo.length > LIMITE_DESCRICAO) {
      setErroMensagem(
        `A mensagem deve ter no máximo ${LIMITE_DESCRICAO} caracteres.`,
      );
      return;
    }

    const id = detalhe.id;
    setErroMensagem(null);
    setAviso(null);
    setEnviandoMensagem(true);
    try {
      const res = await fetch(
        `/api/admin/helpdesk/tickets/${id}/mensagens`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ conteudo }),
        },
      );
      if (res.status === 401) {
        router.push("/login?expirada=1");
        return;
      }
      if (!res.ok) throw new Error(await lerErro(res));

      const enviada = (await res.json()) as TicketMensagem;
      setDetalhe((atual) =>
        atual?.id === id
          ? {
              ...atual,
              mensagens: [
                ...atual.mensagens.filter((item) => item.id !== enviada.id),
                enviada,
              ],
            }
          : atual,
      );
      setMensagem("");
      setAviso("Resposta enviada.");
    } catch (erro) {
      setErroMensagem(erro instanceof Error ? erro.message : ERRO_CONEXAO);
    } finally {
      setEnviandoMensagem(false);
    }
  }

  async function alterarStatus() {
    if (!detalhe || !novoStatus || novoStatus === detalhe.status) return;

    const id = detalhe.id;
    setAlterandoStatus(true);
    setErroStatus(null);
    setAviso(null);
    try {
      const res = await fetch(
        `/api/admin/helpdesk/tickets/${id}/status`,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ status: novoStatus }),
        },
      );
      if (res.status === 401) {
        router.push("/login?expirada=1");
        return;
      }
      if (!res.ok) throw new Error(await lerErro(res));

      const atualizado = (await res.json()) as TicketResumo;
      setDetalhe((atual) =>
        atual?.id === id
          ? { ...atual, ...atualizado, mensagens: atual.mensagens }
          : atual,
      );
      setTickets((atuais) =>
        atuais.map((ticket) =>
          ticket.id === id ? { ...ticket, ...atualizado } : ticket,
        ),
      );
      setNovoStatus(atualizado.status);
      setAviso("Status atualizado.");
      if (filtro && filtro !== atualizado.status) {
        void carregarTickets(filtro);
      }
    } catch (erro) {
      setErroStatus(erro instanceof Error ? erro.message : ERRO_CONEXAO);
    } finally {
      setAlterandoStatus(false);
    }
  }

  return (
    <div className="space-y-6">
      <section
        aria-labelledby="fila-h"
        className="overflow-hidden rounded-xl border border-border bg-card"
      >
        <div className="flex flex-col gap-4 border-b border-border px-5 py-4 md:flex-row md:items-end md:justify-between">
          <div className="space-y-1">
            <h2
              id="fila-h"
              className="text-base font-semibold text-foreground"
            >
              Fila de chamados
            </h2>
            <p className="text-xs text-muted-foreground">
              Chamados da empresa, do mais recente para o mais antigo.
            </p>
          </div>

          <div className="flex flex-wrap items-end gap-3">
            <div className="min-w-44 flex-1 space-y-1.5 md:flex-none">
              <Label htmlFor="filtro-status">Filtrar por status</Label>
              <select
                id="filtro-status"
                value={filtro}
                onChange={(event) => aoFiltrar(event.target.value)}
                className={CLASSE_CAMPO}
              >
                {FILTROS.map((opcao) => (
                  <option key={opcao.valor} value={opcao.valor}>
                    {opcao.rotulo}
                  </option>
                ))}
              </select>
            </div>
            <Button
              type="button"
              variant="outline"
              className="h-10"
              onClick={() => void carregarTickets(filtro)}
              disabled={estadoLista === "carregando"}
            >
              <RefreshCw />
              Atualizar
            </Button>
          </div>
        </div>

        {estadoLista === "pronto" && (
          <div className="border-b border-border px-5 py-2 text-xs tabular-nums text-muted-foreground">
            {total} {total === 1 ? "chamado" : "chamados"} no total
          </div>
        )}

        {estadoLista === "carregando" && (
          <div className="flex min-h-32 items-center justify-center gap-2 px-5 py-10 text-sm text-muted-foreground">
            <Spinner />
            Carregando chamados…
          </div>
        )}

        {estadoLista === "erro" && (
          <div className="space-y-3 p-5">
            <Alert variant="destructive">
              <AlertDescription>{erroLista}</AlertDescription>
            </Alert>
            <Button
              type="button"
              variant="outline"
              onClick={() => void carregarTickets(filtro)}
            >
              <RefreshCw />
              Tentar novamente
            </Button>
          </div>
        )}

        {estadoLista === "pronto" && tickets.length === 0 && (
          <div className="flex flex-col items-center gap-3 px-5 py-12 text-center">
            <Inbox aria-hidden="true" className="size-7 text-muted-foreground" />
            <div className="space-y-1">
              <p className="text-sm font-medium text-foreground">
                Nenhum chamado neste filtro
              </p>
              <p className="text-sm text-muted-foreground">
                Os chamados abertos pelos usuários aparecem aqui.
              </p>
            </div>
          </div>
        )}

        {estadoLista === "pronto" && tickets.length > 0 && (
          <ul className="divide-y divide-border">
            {tickets.map((ticket) => {
              const selecionado = ticket.id === selecionadoId;
              return (
                <li key={ticket.id}>
                  <button
                    type="button"
                    onClick={() => void abrirTicket(ticket.id)}
                    aria-current={selecionado ? "true" : undefined}
                    className={cn(
                      "flex w-full min-h-16 items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                      selecionado && "bg-accent/40",
                    )}
                  >
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-foreground">
                        {ticket.assunto}
                      </span>
                      <span className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                        <span className="tabular-nums">#{ticket.id}</span>
                        {ticket.usuarioId != null && (
                          <span className="tabular-nums">
                            solicitante #{ticket.usuarioId}
                          </span>
                        )}
                        <span className="tabular-nums">
                          {formatarData(ticket.criadoEm)}
                        </span>
                      </span>
                    </span>
                    <span
                      className={cn(
                        "shrink-0 rounded-full border px-2.5 py-1 text-xs font-medium",
                        classesStatus(ticket.status),
                      )}
                    >
                      {rotuloStatus(ticket.status)}
                    </span>
                    <ChevronRight
                      aria-hidden="true"
                      className="size-4 shrink-0 text-muted-foreground"
                    />
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {selecionadoId !== null && (
        <section
          aria-label="Detalhe do chamado"
          className="overflow-hidden rounded-xl border border-border bg-card"
        >
          {estadoDetalhe === "carregando" && (
            <div className="flex min-h-32 items-center justify-center gap-2 px-5 py-10 text-sm text-muted-foreground">
              <Spinner />
              Carregando a conversa…
            </div>
          )}

          {estadoDetalhe === "erro" && (
            <div className="space-y-3 p-5">
              <Alert variant="destructive">
                <AlertDescription>{erroDetalhe}</AlertDescription>
              </Alert>
              <Button
                type="button"
                variant="outline"
                onClick={() => void abrirTicket(selecionadoId)}
              >
                <RefreshCw />
                Tentar novamente
              </Button>
            </div>
          )}

          {estadoDetalhe === "pronto" && detalhe && (
            <>
              <div className="border-b border-border px-5 py-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h3 className="break-words text-base font-semibold text-foreground">
                      {detalhe.assunto}
                    </h3>
                    <p className="mt-1 text-xs tabular-nums text-muted-foreground">
                      Chamado #{detalhe.id}
                      {detalhe.usuarioId != null &&
                        ` · solicitante #${detalhe.usuarioId}`}
                      {" · aberto em "}
                      {formatarData(detalhe.criadoEm)}
                    </p>
                  </div>
                  <span
                    className={cn(
                      "shrink-0 rounded-full border px-2.5 py-1 text-xs font-medium",
                      classesStatus(detalhe.status),
                    )}
                  >
                    {rotuloStatus(detalhe.status)}
                  </span>
                </div>

                <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-relaxed text-muted-foreground">
                  {detalhe.descricao}
                </p>

                <div className="mt-4 flex flex-wrap items-end gap-3 border-t border-border pt-4">
                  <div className="min-w-44 flex-1 space-y-1.5 md:flex-none">
                    <Label htmlFor="status-chamado">Alterar status</Label>
                    <select
                      id="status-chamado"
                      value={novoStatus}
                      onChange={(event) => {
                        setNovoStatus(event.target.value);
                        setErroStatus(null);
                      }}
                      disabled={alterandoStatus}
                      className={CLASSE_CAMPO}
                    >
                      <option value="" disabled>
                        Selecione o status
                      </option>
                      {STATUS_OPCOES.map((status) => (
                        <option key={status} value={status}>
                          {rotuloStatus(status)}
                        </option>
                      ))}
                    </select>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    className="h-10"
                    onClick={() => void alterarStatus()}
                    disabled={
                      alterandoStatus || !novoStatus || novoStatus === detalhe.status
                    }
                  >
                    {alterandoStatus ? (
                      <>
                        <Spinner />
                        Salvando…
                      </>
                    ) : (
                      "Salvar status"
                    )}
                  </Button>
                </div>

                {erroStatus && (
                  <p role="alert" className="mt-2 text-sm text-destructive">
                    {erroStatus}
                  </p>
                )}
                {aviso && (
                  <p
                    role="status"
                    className="mt-2 rounded-lg border border-border bg-muted/40 px-3 py-2 text-sm text-foreground"
                  >
                    {aviso}
                  </p>
                )}
              </div>

              <div className="max-h-96 space-y-3 overflow-y-auto px-5 py-4">
                {detalhe.mensagens.length === 0 ? (
                  <p className="py-4 text-center text-sm text-muted-foreground">
                    Nenhuma mensagem além da descrição inicial.
                  </p>
                ) : (
                  detalhe.mensagens.map((item) => (
                    <article
                      key={item.id}
                      className="rounded-lg border border-border bg-background/50 p-3"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-xs font-semibold text-foreground">
                          {item.origem === "ADMIN"
                            ? "Suporte"
                            : `Solicitante${item.usuarioId != null ? ` #${item.usuarioId}` : ""}`}
                        </span>
                        <span className="text-xs tabular-nums text-muted-foreground">
                          {formatarData(item.criadoEm)}
                        </span>
                      </div>
                      <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-foreground">
                        {item.conteudo}
                      </p>
                    </article>
                  ))
                )}
              </div>

              <form
                onSubmit={enviarMensagem}
                className="space-y-2 border-t border-border p-4"
              >
                <Label htmlFor="resposta-admin" className="sr-only">
                  Resposta do suporte
                </Label>
                <textarea
                  id="resposta-admin"
                  value={mensagem}
                  rows={3}
                  maxLength={LIMITE_DESCRICAO}
                  placeholder="Escreva a resposta para o solicitante"
                  disabled={enviandoMensagem}
                  onChange={(event) => {
                    setMensagem(event.target.value);
                    setErroMensagem(null);
                  }}
                  aria-invalid={erroMensagem ? true : undefined}
                  className="min-h-20 w-full resize-y rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20"
                />
                {erroMensagem && (
                  <p role="alert" className="text-sm text-destructive">
                    {erroMensagem}
                  </p>
                )}
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span className="text-xs tabular-nums text-muted-foreground">
                    {mensagem.length}/{LIMITE_DESCRICAO}
                  </span>
                  <Button type="submit" disabled={enviandoMensagem}>
                    {enviandoMensagem ? (
                      <>
                        <Spinner />
                        Enviando…
                      </>
                    ) : (
                      <>
                        <Send />
                        Responder
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </>
          )}
        </section>
      )}
    </div>
  );
}
