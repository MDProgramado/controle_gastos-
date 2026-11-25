"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { useRouter } from "next/navigation";
import {
  ChevronRight,
  MessageSquareWarning,
  RefreshCw,
  Send,
} from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/Spinner";
import {
  CLASSES_STATUS,
  LIMITE_ASSUNTO,
  LIMITE_DESCRICAO,
  ROTULOS_STATUS,
  validarTicket,
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

type Estado = "carregando" | "pronto" | "erro";

export function SuporteClient() {
  const router = useRouter();
  const [tickets, setTickets] = useState<TicketResumo[]>([]);
  const [estadoLista, setEstadoLista] = useState<Estado>("carregando");
  const [erroLista, setErroLista] = useState<string | null>(null);

  const [selecionadoId, setSelecionadoId] = useState<number | null>(null);
  const [detalhe, setDetalhe] = useState<TicketDetalhe | null>(null);
  const [estadoDetalhe, setEstadoDetalhe] = useState<Estado>("pronto");
  const [erroDetalhe, setErroDetalhe] = useState<string | null>(null);
  const selecaoAtiva = useRef(0);

  const [assunto, setAssunto] = useState("");
  const [descricao, setDescricao] = useState("");
  const [erroValidacao, setErroValidacao] = useState<string | null>(null);
  const [erroEnvio, setErroEnvio] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const [mensagem, setMensagem] = useState("");
  const [erroMensagem, setErroMensagem] = useState<string | null>(null);
  const [enviandoMensagem, setEnviandoMensagem] = useState(false);

  const carregarTickets = useCallback(async () => {
    try {
      const res = await fetch("/api/helpdesk/tickets?page=0&size=50", {
        cache: "no-store",
      });
      if (res.status === 401) {
        router.push("/login?expirada=1");
        return;
      }
      if (!res.ok) {
        const corpo = (await res.json().catch(() => ({}))) as {
          detail?: string;
          erro?: string;
        };
        throw new Error(corpo.detail ?? corpo.erro ?? ERRO_CONEXAO);
      }

      const dados = (await res.json()) as PaginaTickets;
      setTickets(Array.isArray(dados.tickets) ? dados.tickets : []);
      setEstadoLista("pronto");
    } catch (erro) {
      setErroLista(erro instanceof Error ? erro.message : ERRO_CONEXAO);
      setEstadoLista("erro");
    }
  }, [router]);

  const abrirTicket = useCallback(async (id: number) => {
    const seq = ++selecaoAtiva.current;
    setSelecionadoId(id);
    setDetalhe(null);
    setEstadoDetalhe("carregando");
    setErroDetalhe(null);
    setMensagem("");
    setErroMensagem(null);

    try {
      const res = await fetch(`/api/helpdesk/tickets/${id}`, {
        cache: "no-store",
      });
      if (res.status === 401) {
        router.push("/login?expirada=1");
        return;
      }
      if (!res.ok) {
        const corpo = (await res.json().catch(() => ({}))) as {
          detail?: string;
          erro?: string;
        };
        throw new Error(corpo.detail ?? corpo.erro ?? ERRO_CONEXAO);
      }
      if (seq !== selecaoAtiva.current) return;

      const dados = (await res.json()) as TicketDetalhe;
      setDetalhe(dados);
      setEstadoDetalhe("pronto");
    } catch (erro) {
      if (seq !== selecaoAtiva.current) return;
      setErroDetalhe(erro instanceof Error ? erro.message : ERRO_CONEXAO);
      setEstadoDetalhe("erro");
    }
  }, [router]);

  useEffect(() => {
    let ativo = true;

    fetch("/api/helpdesk/tickets?page=0&size=50", { cache: "no-store" })
      .then(async (res) => {
        if (res.status === 401) {
          router.push("/login?expirada=1");
          return;
        }
        if (!res.ok) {
          const corpo = (await res.json().catch(() => ({}))) as {
            detail?: string;
            erro?: string;
          };
          throw new Error(corpo.detail ?? corpo.erro ?? ERRO_CONEXAO);
        }

        const dados = (await res.json()) as PaginaTickets;
        if (!ativo) return;
        setTickets(Array.isArray(dados.tickets) ? dados.tickets : []);
        setEstadoLista("pronto");
      })
      .catch((erro: unknown) => {
        if (!ativo) return;
        setErroLista(erro instanceof Error ? erro.message : ERRO_CONEXAO);
        setEstadoLista("erro");
      });

    return () => {
      ativo = false;
    };
  }, [router]);

  const erroAssunto = erroValidacao?.toLowerCase().includes("assunto")
    ? erroValidacao
    : null;
  const erroDescricao = erroValidacao?.toLowerCase().includes("descrição")
    ? erroValidacao
    : null;

  async function enviarTicket(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const erro = validarTicket(assunto, descricao);
    setErroValidacao(erro);
    setErroEnvio(null);
    setSucesso(null);
    if (erro) return;

    setEnviando(true);
    try {
      const res = await fetch("/api/helpdesk/tickets", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          assunto: assunto.trim(),
          descricao: descricao.trim(),
        }),
      });
      if (res.status === 401) {
        router.push("/login?expirada=1");
        return;
      }
      if (!res.ok) {
        const corpo = (await res.json().catch(() => ({}))) as {
          detail?: string;
          erro?: string;
        };
        throw new Error(corpo.detail ?? corpo.erro ?? ERRO_CONEXAO);
      }

      const criado = (await res.json()) as TicketResumo;
      setAssunto("");
      setDescricao("");
      setSucesso("Chamado enviado. Acompanhe a conversa ao lado.");
      setEstadoLista("carregando");
      setErroLista(null);
      await carregarTickets();
      if (criado?.id) void abrirTicket(criado.id);
    } catch (erro) {
      setErroEnvio(erro instanceof Error ? erro.message : ERRO_CONEXAO);
    } finally {
      setEnviando(false);
    }
  }

  async function enviarMensagem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (selecionadoId === null) return;

    const conteudo = mensagem.trim();
    if (!conteudo) {
      setErroMensagem("Informe sua mensagem.");
      return;
    }
    if (conteudo.length > LIMITE_DESCRICAO) {
      setErroMensagem(`A mensagem deve ter no máximo ${LIMITE_DESCRICAO} caracteres.`);
      return;
    }

    const id = selecionadoId;
    setErroMensagem(null);
    setEnviandoMensagem(true);
    try {
      const res = await fetch(`/api/helpdesk/tickets/${id}/mensagens`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ conteudo }),
      });
      if (res.status === 401) {
        router.push("/login?expirada=1");
        return;
      }
      if (!res.ok) {
        const corpo = (await res.json().catch(() => ({}))) as {
          detail?: string;
          erro?: string;
        };
        throw new Error(corpo.detail ?? corpo.erro ?? ERRO_CONEXAO);
      }

      const enviada = (await res.json()) as TicketMensagem;
      setDetalhe((atual) =>
        atual?.id === id
          ? { ...atual, mensagens: [...atual.mensagens, enviada] }
          : atual,
      );
      setMensagem("");
    } catch (erro) {
      setErroMensagem(erro instanceof Error ? erro.message : ERRO_CONEXAO);
    } finally {
      setEnviandoMensagem(false);
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,24rem)_minmax(0,1fr)]">
      <section
        aria-labelledby="novo-problema-h"
        className="h-fit rounded-xl border border-border bg-card p-5"
      >
        <div className="space-y-1.5">
          <h2
            id="novo-problema-h"
            className="text-base font-semibold text-foreground"
          >
            Novo problema
          </h2>
          <p className="text-sm leading-relaxed text-muted-foreground">
            Descreva o que aconteceu para o suporte entender e responder.
          </p>
        </div>

        <form onSubmit={enviarTicket} className="mt-5 space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="assunto">Assunto</Label>
            <Input
              id="assunto"
              value={assunto}
              maxLength={LIMITE_ASSUNTO}
              placeholder="Ex.: não consigo importar o extrato"
              disabled={enviando}
              onChange={(event) => {
                setAssunto(event.target.value);
                setErroValidacao(null);
                setSucesso(null);
              }}
              aria-invalid={erroAssunto ? true : undefined}
              aria-describedby={erroAssunto ? "erro-assunto" : undefined}
              className="h-11"
            />
            <div className="flex items-start justify-between gap-3 text-xs">
              <span id="erro-assunto" className="text-destructive">
                {erroAssunto}
              </span>
              <span className="ml-auto shrink-0 tabular-nums text-muted-foreground">
                {assunto.length}/{LIMITE_ASSUNTO}
              </span>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="descricao">Descrição</Label>
            <textarea
              id="descricao"
              value={descricao}
              rows={7}
              maxLength={LIMITE_DESCRICAO}
              placeholder="Informe o que você esperava e o que aconteceu."
              disabled={enviando}
              onChange={(event) => {
                setDescricao(event.target.value);
                setErroValidacao(null);
                setSucesso(null);
              }}
              aria-invalid={erroDescricao ? true : undefined}
              aria-describedby={erroDescricao ? "erro-descricao" : undefined}
              className="min-h-32 w-full resize-y rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20"
            />
            <div className="flex items-start justify-between gap-3 text-xs">
              <span id="erro-descricao" className="text-destructive">
                {erroDescricao}
              </span>
              <span className="ml-auto shrink-0 tabular-nums text-muted-foreground">
                {descricao.length}/{LIMITE_DESCRICAO}
              </span>
            </div>
          </div>

          {erroEnvio && (
            <Alert variant="destructive">
              <AlertDescription>{erroEnvio}</AlertDescription>
            </Alert>
          )}

          {sucesso && (
            <p
              role="status"
              className="rounded-lg border border-border bg-muted/40 px-3 py-2 text-sm text-foreground"
            >
              {sucesso}
            </p>
          )}

          <Button type="submit" disabled={enviando} className="h-11 w-full">
            {enviando ? (
              <>
                <Spinner />
                Enviando…
              </>
            ) : (
              "Enviar chamado"
            )}
          </Button>
        </form>
      </section>

      <div className="min-w-0">
        <section aria-labelledby="meus-chamados-h" className="rounded-xl border border-border bg-card">
          <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4">
            <h2
              id="meus-chamados-h"
              className="text-base font-semibold text-foreground"
            >
              Meus chamados
            </h2>
            <span className="text-xs tabular-nums text-muted-foreground">
              {estadoLista === "pronto" ? `${tickets.length} no total` : ""}
            </span>
          </div>

          {estadoLista === "carregando" && (
            <div className="flex min-h-32 items-center justify-center gap-2 px-5 py-10 text-sm text-muted-foreground">
              <Spinner />
              Carregando seus chamados…
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
                onClick={() => {
                  setEstadoLista("carregando");
                  setErroLista(null);
                  void carregarTickets();
                }}
              >
                <RefreshCw />
                Tentar novamente
              </Button>
            </div>
          )}

          {estadoLista === "pronto" && tickets.length === 0 && (
            <div className="flex flex-col items-center gap-3 px-5 py-12 text-center">
              <MessageSquareWarning
                aria-hidden="true"
                className="size-7 text-muted-foreground"
              />
              <div className="space-y-1">
                <p className="text-sm font-medium text-foreground">
                  Nenhum chamado registrado
                </p>
                <p className="text-sm text-muted-foreground">
                  Preencha o formulário para relatar o primeiro problema.
                </p>
              </div>
            </div>
          )}

          {estadoLista === "pronto" && tickets.length > 0 && (
            <ul className="divide-y divide-border">
              {tickets.map((ticket) => {
                const status = ticket.status as StatusTicket;
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
                          <span>
                            {new Date(ticket.criadoEm).toLocaleString(
                              "pt-BR",
                              FORMATO_DATA,
                            )}
                          </span>
                        </span>
                      </span>
                      <span
                        className={cn(
                          "shrink-0 rounded-full border px-2.5 py-1 text-xs font-medium",
                          CLASSES_STATUS[status] ?? CLASSES_STATUS.ABERTO,
                        )}
                      >
                        {ROTULOS_STATUS[status] ?? ticket.status}
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
            aria-labelledby="conversa-h"
            aria-live="polite"
            className="mt-6 overflow-hidden rounded-xl border border-border bg-card"
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
                      <h3
                        id="conversa-h"
                        className="break-words text-base font-semibold text-foreground"
                      >
                        {detalhe.assunto}
                      </h3>
                      <p className="mt-1 text-xs tabular-nums text-muted-foreground">
                        Chamado #{detalhe.id} · aberto em{" "}
                        {new Date(detalhe.criadoEm).toLocaleString(
                          "pt-BR",
                          FORMATO_DATA,
                        )}
                      </p>
                    </div>
                    <span
                      className={cn(
                        "shrink-0 rounded-full border px-2.5 py-1 text-xs font-medium",
                        CLASSES_STATUS[detalhe.status] ?? CLASSES_STATUS.ABERTO,
                      )}
                    >
                      {ROTULOS_STATUS[detalhe.status] ?? detalhe.status}
                    </span>
                  </div>
                  <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-relaxed text-muted-foreground">
                    {detalhe.descricao}
                  </p>
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
                            {item.origem === "ADMIN" ? "Suporte" : "Você"}
                          </span>
                          <span className="text-xs tabular-nums text-muted-foreground">
                            {new Date(item.criadoEm).toLocaleString(
                              "pt-BR",
                              FORMATO_DATA,
                            )}
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
                  <Label htmlFor="mensagem" className="sr-only">
                    Sua mensagem
                  </Label>
                  <textarea
                    id="mensagem"
                    value={mensagem}
                    rows={3}
                    maxLength={LIMITE_DESCRICAO}
                    placeholder="Escreva uma resposta ou detalhe adicional"
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
    </div>
  );
}
