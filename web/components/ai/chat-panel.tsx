"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { SelectValue } from "@/components/ui/select"
import { SelectContent } from "@/components/ui/select"
import { SelectItem } from "@/components/ui/select"
import { Spinner } from "@/components/Spinner"
import {
  enviarMensagem,
  confirmarLancamentos,
} from "@/lib/ai/chat-service"
import type {
  ChatMessage,
  PropostaLancamento,
} from "@/lib/ai/types"
import { useCategorias } from "./use-categorias"
import { Markdown } from "./markdown"
import { ArrowRight, Loader2 } from "lucide-react"

const MENSAGENS_EXEMPLO = [
  "Gastei R$ 45 no almoço hoje",
  "Quanto gastei com alimentação este mês?",
]

/** Proposta em edição (valores como string para o input controlado). */
type PropostaEditavel = {
  id: string;
  tipo: "ENTRADA" | "SAIDA";
  valor: string;
  data: string;
  categoriaId: string | null;
  categoriaSugerida?: string;
  descricao?: string;
  confirmada: boolean;
};

function formatarMoeda(valor: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(valor)
}

function agoraISO(): string {
  return new Date().toISOString().slice(0, 10)
}

/** Normaliza texto para comparar nomes ignorando acentos e caixa. */
function normalizarTexto(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
}

/**
 * Procura a categoria do banco que corresponde ao nome sugerido pela IA (ignora acentos e
 * caixa). Devolve null quando não há correspondência — nesse caso o usuário escolhe.
 */
function encontrarCategoriaId(
  nome: string | undefined,
  categorias: Array<{ id: number; nome: string }>,
): string | null {
  if (!nome) return null
  const alvo = normalizarTexto(nome)
  const achada = categorias.find((c) => normalizarTexto(c.nome) === alvo)
  return achada ? String(achada.id) : null
}

export function ChatPanel() {
  const [mensagens, setMensagens] = useState<ChatMessage[]>(() => {
  return [
    {
      id: "msg-0",
      role: "assistant",
      content: "Olá! Sou seu assistente financeiro. Como posso ajudar?",
      criadoEm: new Date(),
      opcoes: MENSAGENS_EXEMPLO,
    },
  ]
})
  const [digitando, setDigitando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [mensagemErro, setMensagemErro] = useState<string | null>(null)
  const [valorInput, setValorInput] = useState("")
  const [propostas, setPropostas] = useState<PropostaEditavel[]>([])
  const { categorias } = useCategorias()

  // Sessão estável do chat: o backend guarda a memória da conversa por sessionId. Sem ele, cada
  // mensagem abriria uma sessão nova e a IA perderia o contexto.
  const [sessionId] = useState(() =>
    typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : `sessao-${Date.now()}-${Math.random().toString(36).slice(2)}`,
  )

  const mensagensEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    mensagensEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [mensagens])

  useEffect(() => {
    if (mensagemErro) {
      const t = setTimeout(() => setMensagemErro(null), 5000)
      return () => clearTimeout(t)
    }
  }, [mensagemErro])

  const enviarMensagemHandler = useCallback(
    async (texto: string) => {
      if (!texto.trim() || digitando) return

      const msgUsuario: ChatMessage = {
        id: `msg-${Date.now()}`,
        role: "user",
        content: texto,
        criadoEm: new Date(),
      }

      setMensagens((atual) => [...atual.slice(-49), msgUsuario])
      setDigitando(true)
      setErro(null)
      setPropostas([])

      try {
        const resposta = await enviarMensagem({
          mensagem: texto,
          historico: mensagens,
          sessionId,
        })

        const msgIA: ChatMessage = {
          id: `msg-${Date.now() + 1}`,
          role: "assistant",
          content: resposta.resposta,
          criadoEm: new Date(),
        }

        if (resposta.intent === "lancamento") {
          const lista = resposta.propostas?.length
            ? resposta.propostas
            : [resposta.proposta]
          msgIA.lancamentoProposta = lista[0]
          msgIA.lancamentoPropostas = lista
          setPropostas(
            lista.map((p, indice) => ({
              id: `prop-${Date.now()}-${indice}`,
              tipo: p.tipo,
              valor: String(p.valor),
              data: p.data || agoraISO(),
              categoriaId:
                p.categoriaId ?? encontrarCategoriaId(p.categoriaSugerida, categorias),
              categoriaSugerida: p.categoriaSugerida,
              descricao: p.descricao,
              confirmada: false,
            })),
          )
        } else if (resposta.intent === "consulta") {
          msgIA.consultaDados = resposta.dados
        } else if (resposta.intent === "esclarecimento") {
          msgIA.opcoes = resposta.opcoes
        }

        setMensagens((atual) => [...atual.slice(-49), msgIA])
      } catch (e) {
        setErro(
          e instanceof Error
            ? e.message
            : "Não foi possível enviar a mensagem. Tente novamente.",
        )
        setMensagemErro(texto)
      } finally {
        setDigitando(false)
      }
    },
    [digitando, mensagens, categorias, sessionId]
  )

  const tentarNovamente = useCallback(() => {
    if (mensagemErro) {
      enviarMensagemHandler(mensagemErro)
      setMensagemErro(null)
    }
  }, [mensagemErro, enviarMensagemHandler])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const texto = valorInput.trim()
    if (!texto) return
    setValorInput("")
    enviarMensagemHandler(texto)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      const texto = valorInput.trim()
      if (!texto) return
      setValorInput("")
      enviarMensagemHandler(texto)
    }
  }

  // --- Confirmação de lançamento ---

  const atualizarProposta = (id: string, mudancas: Partial<PropostaEditavel>) => {
    setPropostas((atual) =>
      atual.map((p) => (p.id === id ? { ...p, ...mudancas } : p)),
    )
  }

  const confirmarPropostas = async (ids: string[]) => {
    const alvos = propostas.filter((p) => ids.includes(p.id))
    if (alvos.length === 0) return

    const invalidas = alvos.filter(
      (p) => !parseFloat(p.valor.replace(",", ".")) || !p.categoriaId,
    )
    if (invalidas.length > 0) return

    setDigitando(true)
    try {
      // Uma única transação (tudo-ou-nada): ou entra tudo, ou nada é gravado.
      await confirmarLancamentos(
        alvos.map((p) => ({
          tipo: p.tipo,
          valor: parseFloat(p.valor.replace(",", ".")),
          data: p.data,
          categoriaId: p.categoriaId,
          categoriaSugerida: p.categoriaSugerida,
          descricao: p.descricao,
        })),
      )

      setPropostas((atual) =>
        atual.map((p) => (ids.includes(p.id) ? { ...p, confirmada: true } : p)),
      )

      setMensagens((atual) => [
        ...atual.slice(-49),
        {
          id: `msg-${Date.now()}`,
          role: "assistant",
          content:
            alvos.length === 1
              ? "Lançamento registrado"
              : `${alvos.length} lançamentos registrados`,
          criadoEm: new Date(),
        },
      ])
    } catch (e) {
      setErro(
        e instanceof Error
          ? e.message
          : "Não foi possível registrar o lançamento. Tente novamente.",
      )
    } finally {
      setDigitando(false)
    }
  }

  const lidarCancelarLancamento = () => {
    setMensagens((atual) => [
      ...atual.slice(-49),
      {
        id: `msg-${Date.now()}`,
        role: "assistant",
        content: "Lançamento cancelado",
        criadoEm: new Date(),
      },
    ])
    setPropostas([])
  }

  return (
    // Altura fixa: no mobile o chat ocupa a largura toda abaixo dos
    // graficos; no lg+ vira coluna lateral ao lado deles.
    <Card className="flex flex-col border border-input bg-card max-h-[90dvh] overflow-hidden">
      <CardHeader className="px-4 py-3 flex items-center justify-between border-b">
        <div className="flex items-center gap-2">
          <CardTitle className="text-sm font-semibold">Assistente</CardTitle>
          <Badge variant="outline" className="text-xs">beta</Badge>
        </div>
      </CardHeader>

      <CardContent className="flex-1 min-h-0 p-4 overflow-y-auto relative">
        {erro && (
          <Alert variant="destructive" className="mb-3 rounded-lg border border-destructive/50">
            <AlertDescription>{erro}</AlertDescription>
            <div className="mt-2 flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={tentarNovamente}
                className="text-xs"
              >
                Tentar de novo
              </Button>
            </div>
          </Alert>
        )}

        <div className="flex flex-col gap-3 mb-4">
          {mensagens.map((m) => {
            const isUser = m.role === "user"
            const listaPropostas =
              m.lancamentoPropostas ??
              (m.lancamentoProposta ? [m.lancamentoProposta] : [])
            const ehLancamento = listaPropostas.length > 0

            return (
              <div
                key={m.id}
                className={cn(
                  "flex",
                  isUser
                    ? "justify-end max-w-[85%]"
                    : "justify-start w-full",
                )}
              >
                {isUser ? (
                  <div className="bg-secondary rounded-lg p-3 text-secondary-foreground text-sm">
                    {m.content}
                  </div>
                ) : ehLancamento ? (
                  renderCardConfirmacao(m, listaPropostas)
                ) : (
                  <div className="max-w-[80%] rounded-lg bg-muted p-3 text-sm text-foreground">
                    {/* A resposta da IA vem em Markdown (tabelas, listas,
                        negrito) — por isso o Markdown. */}
                    <Markdown>{m.content}</Markdown>
                    {m.consultaDados && m.consultaDados.length > 0 && (
                      <div className="mt-3 p-3 rounded-md bg-card/50 border border-border/50">
                        <div className="grid gap-2 text-sm">
                          {m.consultaDados.map((d: { label: string; valor: number }) => (
                            <div key={d.label} className="flex justify-between">
                              <span className="text-muted-foreground">
                                {d.label}
                              </span>
                              <span className="font-medium">
                                {formatarMoeda(d.valor)}
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                    {m.opcoes && m.opcoes.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-2">
                        {m.opcoes.map((opcao) => (
                          <Button
                            key={opcao}
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setValorInput(opcao)
                              enviarMensagemHandler(opcao)
                            }}
                            className="text-xs"
                          >
                            {opcao}
                          </Button>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )
          })}

          {digitando && (
            <div className="flex justify-start max-w-[85%]">
              <div className="bg-muted rounded-lg p-3 text-sm text-foreground animate-pulse">
                <span className="flex items-center gap-1 text-muted-foreground">
                  <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground animate-bounce" />
                  <span
                    className="h-1.5 w-1.5 rounded-full bg-muted-foreground animate-bounce"
                    style={{ animationDelay: "0.2s" }}
                  />
                  <span
                    className="h-1.5 w-1.5 rounded-full bg-muted-foreground animate-bounce"
                    style={{ animationDelay: "0.4s" }}
                  />
                </span>
              </div>
            </div>
          )}

          {mensagens.length === 1 &&
            mensagens[0].role === "assistant" &&
            !digitando &&
            !erro &&
            propostas.length === 0 && (
              <div className="flex flex-col gap-2 max-w-[80%] text-sm text-muted-foreground">
                <p>Diga algo como:</p>
                <div className="flex flex-wrap gap-2">
                  {MENSAGENS_EXEMPLO.map((exemplo) => (
                    <button
                      key={exemplo}
                      onClick={() => {
                        setValorInput(exemplo)
                        enviarMensagemHandler(exemplo)
                      }}
                      className="text-sm text-primary underline underline-offset-4 hover:text-primary/80"
                    >
                      {exemplo}
                    </button>
                  ))}
                </div>
              </div>
            )}
        </div>

        <div ref={mensagensEndRef} />
      </CardContent>

      <div className="border-t border-input p-3 bg-card">
        <form onSubmit={handleSubmit} className="flex gap-2">
          <textarea
            value={valorInput}
            onChange={(e) => setValorInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Digite sua mensagem…"
            rows={1}
            className={cn(
              "flex-1 min-h-[44px] max-h-[88px] rounded-lg border border-input bg-transparent px-3 py-2 text-sm focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50",
              digitando ? "disabled" : ""
            )}
            disabled={digitando}
          />
          <Button
            type="submit"
            variant="default"
            disabled={digitando || !valorInput.trim()}
            className="h-[44px] w-[44px] rounded-lg p-0"
            aria-label="Enviar mensagem"
          >
            {digitando ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <ArrowRight className="h-4 w-4" />
            )}
          </Button>
        </form>
      </div>
    </Card>
  )

  function renderCardConfirmacao(
    mensagem: ChatMessage,
    lista: PropostaLancamento[],
  ) {
    const editaveis = propostas.filter((p) => !p.confirmada)
    const total = lista.length
    return (
      <div className="bg-card rounded-lg p-4 border border-border shadow-sm w-full">
        <h3 className="text-sm font-semibold text-foreground mb-1">
          {total === 1
            ? "Entendi este lançamento"
            : `Entendi ${total} lançamentos`}
        </h3>
        <p className="text-xs text-muted-foreground mb-3">
          Confira os valores e escolha a categoria de cada item antes de confirmar.
        </p>

        <div className="flex flex-col gap-4">
          {lista.map((proposta, indice) => {
            // O estado "propostas" guarda exatamente as propostas desta mensagem, na mesma ordem.
            const estado = propostas[indice]
            if (!estado) {
              return (
                <div
                  key={`${proposta.tipo}-${indice}`}
                  className="rounded-lg border border-border bg-muted/40 p-3 text-sm"
                >
                  <span className="text-muted-foreground">
                    {proposta.tipo === "ENTRADA" ? "+" : "-"} {formatarMoeda(proposta.valor)} ·{" "}
                    {proposta.data} ·{" "}
                    {proposta.categoriaSugerida ?? "sem categoria"}
                  </span>
                </div>
              )
            }

            if (estado.confirmada) {
              const nomeCategoria =
                categorias.find((c) => String(c.id) === estado.categoriaId)?.nome ??
                estado.categoriaSugerida ??
                "sem categoria"
              return (
                <div
                  key={estado.id}
                  className="rounded-lg border border-border bg-muted/40 p-3 text-sm"
                >
                  <span className="text-muted-foreground">
                    {estado.tipo === "ENTRADA" ? "+" : "-"}{" "}
                    {formatarMoeda(parseFloat(estado.valor.replace(",", ".")) || 0)} ·{" "}
                    {estado.data} · {nomeCategoria} — registrado
                  </span>
                </div>
              )
            }

            const ehEntrada = estado.tipo === "ENTRADA"
            const corValor = ehEntrada ? "text-positive" : "text-destructive"
            const valorNumerico = parseFloat(estado.valor.replace(",", "."))
            const podeConfirmar =
              valorNumerico > 0 && estado.categoriaId !== null

            return (
              <div
                key={estado.id}
                className="rounded-lg border border-border p-3 space-y-3"
              >
                <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                  <span className={`text-lg font-bold ${corValor}`}>
                    {ehEntrada ? "+" : "-"} {formatarMoeda(valorNumerico || 0)}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {estado.data}
                  </span>
                  {estado.descricao && (
                    <span className="text-xs text-muted-foreground">
                      · {estado.descricao}
                    </span>
                  )}
                </div>

                <div>
                  <Label className="text-xs text-muted-foreground">Tipo</Label>
                  <div className="flex items-center gap-2 mt-1">
                    <button
                      onClick={() =>
                        atualizarProposta(estado.id, { tipo: "ENTRADA" })
                      }
                      className={cn(
                        "flex-1 rounded-lg border px-2 py-1.5 text-xs font-medium transition-colors",
                        estado.tipo === "ENTRADA"
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-input bg-transparent text-muted-foreground",
                      )}
                    >
                      Entrada
                    </button>
                    <button
                      onClick={() =>
                        atualizarProposta(estado.id, { tipo: "SAIDA" })
                      }
                      className={cn(
                        "flex-1 rounded-lg border px-2 py-1.5 text-xs font-medium transition-colors",
                        estado.tipo === "SAIDA"
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-input bg-transparent text-muted-foreground",
                      )}
                    >
                      Saída
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  <div>
                    <Label className="text-xs text-muted-foreground">Valor</Label>
                    <Input
                      type="text"
                      value={estado.valor}
                      onChange={(e) =>
                        atualizarProposta(estado.id, {
                          valor: e.target.value.replace(",", "."),
                        })
                      }
                      className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring"
                    />
                  </div>
                  <div>
                    <Label className="text-xs text-muted-foreground">Data</Label>
                    <Input
                      type="date"
                      value={estado.data}
                      onChange={(e) =>
                        atualizarProposta(estado.id, { data: e.target.value })
                      }
                      className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring"
                    />
                  </div>
                </div>

                <div>
                  <Label className="text-xs text-muted-foreground">
                    Categoria
                  </Label>
                  <Select
                    value={estado.categoriaId ?? ""}
                    onValueChange={(valor) =>
                      atualizarProposta(estado.id, {
                        categoriaId: valor || null,
                      })
                    }
                  >
                    <SelectTrigger className="h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring w-full">
                      <SelectValue
                        placeholder={
                          estado.categoriaId ? "" : "Escolha uma categoria"
                        }
                      />
                    </SelectTrigger>
                    <SelectContent>
                      {categorias.map((c: { id: number; nome: string }) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.nome}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {estado.categoriaId === null && estado.categoriaSugerida && (
                    <p className="text-xs text-muted-foreground mt-1">
                      Sugestão: {estado.categoriaSugerida}
                    </p>
                  )}
                </div>

                {total > 1 && (
                  <div className="flex justify-end">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => confirmarPropostas([estado.id])}
                      disabled={!podeConfirmar || digitando}
                      className="text-xs"
                    >
                      Confirmar este
                    </Button>
                  </div>
                )}
              </div>
            )
          })}
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2 mt-4">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={lidarCancelarLancamento}
            className="text-xs"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="default"
            size="sm"
            onClick={() =>
              confirmarPropostas(editaveis.map((e) => e.id))
            }
            disabled={
              digitando ||
              editaveis.length === 0 ||
              editaveis.some(
                (e) =>
                  !parseFloat(e.valor.replace(",", ".")) || !e.categoriaId,
              )
            }
            className="text-xs"
          >
            {digitando ? (
              <span className="flex items-center gap-1">
                <Spinner className="mr-1 h-3 w-3" />
                Confirmando…
              </span>
            ) : total > 1 ? (
              `Confirmar todos (${editaveis.length})`
            ) : (
              "Confirmar"
            )}
          </Button>
        </div>
      </div>
    )
  }
}
