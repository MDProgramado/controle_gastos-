"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Alert, AlertDescription } from "@/components/ui/alert"
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
import {
  atualizarPropostaPorMensagem,
  criarPropostasEditaveis,
  marcarPropostasConfirmadas,
  propostasDaMensagem,
  valorPropostaParaApi,
  type PropostaEditavel,
  type PropostasPorMensagem,
} from "@/lib/chat"
import { useCategorias } from "./use-categorias"
import { Markdown } from "./markdown"
import { ArrowRight, Loader2, X } from "lucide-react"

const MENSAGENS_EXEMPLO = [
  "Gastei R$ 45 no almoço hoje",
  "Quanto gastei com alimentação este mês?",
]

function formatarMoeda(valor: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(valor)
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

export function ChatPanel({
  onClose,
  className,
}: {
  /** Quando fornecido, mostra um botão de fechar no cabeçalho do painel. */
  onClose?: () => void
  className?: string
}) {
  const [mensagens, setMensagens] = useState<ChatMessage[]>(() => {
  return [
    {
      id: "msg-0",
      role: "assistant",
      content: "Olá, eu sou Pluton! Sou seu assistente financeiro. Como posso ajudar?",
      criadoEm: new Date(),
      opcoes: MENSAGENS_EXEMPLO,
    },
  ]
})
  const [digitando, setDigitando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [mensagemErro, setMensagemErro] = useState<string | null>(null)
  const [valorInput, setValorInput] = useState("")
  const [propostasPorMensagem, setPropostasPorMensagem] = useState<PropostasPorMensagem>({})
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
          const lista = (
            resposta.propostas?.length ? resposta.propostas : [resposta.proposta]
          ).filter((p): p is PropostaLancamento => Boolean(p))
          msgIA.lancamentoProposta = lista[0]
          msgIA.lancamentoPropostas = lista
          setPropostasPorMensagem((atual) => ({
            ...atual,
            ...criarPropostasEditaveis(
              msgIA.id,
              lista,
              (sugestao) => encontrarCategoriaId(sugestao, categorias) ?? undefined,
            ),
          }))
        } else if (resposta.intent === "consulta") {
          msgIA.consultaDados = resposta.dados
        } else if (resposta.intent === "esclarecimento") {
          msgIA.opcoes = resposta.opcoes
        }
        msgIA.citacoes = resposta.citacoes

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

  const atualizarProposta = (
    mensagemId: string,
    id: string,
    mudancas: Partial<PropostaEditavel>,
  ) => {
    setPropostasPorMensagem((atual) =>
      atualizarPropostaPorMensagem(atual, mensagemId, id, mudancas),
    )
  }

  const confirmarPropostas = async (mensagemId: string, ids: string[]) => {
    const alvos = (propostasPorMensagem[mensagemId] ?? []).filter((p) =>
      ids.includes(p.id),
    )
    if (alvos.length === 0) return

    const valores = alvos.map((p) => valorPropostaParaApi(p.valor))
    if (valores.some((valor) => valor === null) || alvos.some((p) => !p.categoriaId)) {
      setErro("Confira valor e categoria de todos os lançamentos antes de confirmar.")
      return
    }

    setDigitando(true)
    try {
      await confirmarLancamentos(
        alvos.map((p, indice) => ({
          tipo: p.tipo,
          valor: valores[indice] as number,
          data: p.data,
          categoriaId: p.categoriaId as string,
          categoriaSugerida: p.categoriaSugerida,
          descricao: p.descricao,
        })),
      )

      setPropostasPorMensagem((atual) =>
        marcarPropostasConfirmadas(atual, mensagemId, ids),
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

  const lidarCancelarLancamento = (mensagemId: string) => {
    setMensagens((atual) => [
      ...atual.slice(-49).map((m) =>
        m.id === mensagemId
          ? { ...m, lancamentoProposta: undefined, lancamentoPropostas: undefined }
          : m,
      ),
      {
        id: `msg-${Date.now()}`,
        role: "assistant",
        content: "Lançamento cancelado",
        criadoEm: new Date(),
      },
    ])
    setPropostasPorMensagem((atual) => {
      const copia = { ...atual }
      delete copia[mensagemId]
      return copia
    })
  }

  return (
    // Altura fixa: no mobile o chat ocupa a largura toda abaixo dos
    // graficos; no lg+ vira coluna lateral ao lado deles.
    <Card className={cn("flex flex-col border border-input bg-card max-h-[90dvh] overflow-hidden", className)}>
      <CardHeader className="flex items-center justify-between border-b px-4 py-3">
        <div className="flex flex-col gap-0.5">
          <CardTitle className="text-sm font-semibold">PLUTON</CardTitle>
          <p className="text-xs text-muted-foreground">Seu assistente financeiro</p>
        </div>
        {onClose && (
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar chat"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-accent/50 hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <X className="h-4 w-4" />
          </button>
        )}
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
            const listaPropostas = propostasDaMensagem(m)
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
                  <div className="w-full max-w-full min-w-0 overflow-hidden rounded-lg bg-muted p-3 text-sm text-foreground">
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
                    {m.citacoes && m.citacoes.length > 0 && (
                      <div className="mt-3 space-y-1 border-t border-border/60 pt-2 text-xs text-muted-foreground">
                        {m.citacoes.map((citacao, indice) => (
                          <div key={`${citacao.sourceId}-${indice}`}>
                            <span className="font-medium text-foreground">
                              {citacao.titulo ?? citacao.sourceId}
                            </span>
                            {citacao.organizacao ? ` — ${citacao.organizacao}` : ""}
                            {citacao.vigencia ? `, ${citacao.vigencia}` : ""}
                            {citacao.dispositivo ? `, ${citacao.dispositivo}` : ""}
                            {` (${citacao.sourceId})`}
                          </div>
                        ))}
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
                            className="h-auto w-full max-w-full whitespace-normal break-words px-3 py-2 text-left leading-snug text-xs"
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
            Object.keys(propostasPorMensagem).length === 0 && (
              <div className="flex w-full max-w-full min-w-0 flex-col gap-2 text-sm text-muted-foreground">
                <p>Diga algo como:</p>
                <div className="flex flex-wrap gap-2">
                  {MENSAGENS_EXEMPLO.map((exemplo) => (
                    <button
                      key={exemplo}
                      onClick={() => {
                        setValorInput(exemplo)
                        enviarMensagemHandler(exemplo)
                      }}
                      className="h-auto w-full max-w-full whitespace-normal break-words px-3 py-2 text-left leading-snug text-sm text-primary underline underline-offset-4 hover:text-primary/80"
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
    const estados = propostasPorMensagem[mensagem.id] ?? []
    const editaveis = estados.filter((p) => !p.confirmada)
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
            // Cada mensagem mantém o estado das próprias propostas, na mesma ordem.
            const estado = estados[indice]
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
                    {formatarMoeda(valorPropostaParaApi(estado.valor) ?? 0)} ·{" "}
                    {estado.data} · {nomeCategoria} — registrado
                  </span>
                </div>
              )
            }

            const ehEntrada = estado.tipo === "ENTRADA"
            const corValor = ehEntrada ? "text-positive" : "text-destructive"
            const valorNumerico = valorPropostaParaApi(estado.valor) ?? 0
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
                        atualizarProposta(mensagem.id, estado.id, { tipo: "ENTRADA" })
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
                        atualizarProposta(mensagem.id, estado.id, { tipo: "SAIDA" })
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
                          atualizarProposta(mensagem.id, estado.id, {
                            valor: e.target.value,
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
                        atualizarProposta(mensagem.id, estado.id, { data: e.target.value })
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
                      atualizarProposta(mensagem.id, estado.id, {
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
                      onClick={() => confirmarPropostas(mensagem.id, [estado.id])}
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
            onClick={() => lidarCancelarLancamento(mensagem.id)}
            className="text-xs"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="default"
            size="sm"
            onClick={() =>
              confirmarPropostas(
                mensagem.id,
                editaveis.map((e) => e.id),
              )
            }
            disabled={
              digitando ||
              editaveis.length === 0 ||
              editaveis.some(
                (e) =>
                  valorPropostaParaApi(e.valor) === null || !e.categoriaId,
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
