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
  confirmarLancamento,
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

function formatarMoeda(valor: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(valor)
}

function agoraISO(): string {
  return new Date().toISOString().slice(0, 10)
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
  const [valorProposta, setValorProposta] = useState("")
  const [tipoProposta, setTipoProposta] = useState<"ENTRADA" | "SAIDA">("SAIDA")
  const [dataProposta, setDataProposta] = useState(agoraISO())
  const [propostaSelecionada, setPropostaSelecionada] = useState<
    PropostaLancamento | null
  >(null)
  const [categoriaSelecionada, setCategoriaSelecionada] = useState<
    string | null
  >(null)
  const { categorias, carregar: carregarCategorias } = useCategorias()

  const mensagensEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    mensagensEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [mensagens])

  useEffect(() => {
    carregarCategorias()
  }, [carregarCategorias])

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
      setPropostaSelecionada(null)
      setCategoriaSelecionada(null)

      try {
        const resposta = await enviarMensagem({
          mensagem: texto,
          historico: mensagens,
        })

        const msgIA: ChatMessage = {
          id: `msg-${Date.now() + 1}`,
          role: "assistant",
          content: resposta.resposta,
          criadoEm: new Date(),
        }

        if (resposta.intent === "lancamento") {
          msgIA.lancamentoProposta = resposta.proposta
          setPropostaSelecionada(resposta.proposta)
          setValorProposta(resposta.proposta.valor.toString())
          setTipoProposta(resposta.proposta.tipo)
          setDataProposta(resposta.proposta.data)
          setCategoriaSelecionada(
            resposta.proposta.categoriaId ?? null,
          )
        } else if (resposta.intent === "consulta") {
          msgIA.consultaDados = resposta.dados
        } else if (resposta.intent === "esclarecimento") {
          msgIA.opcoes = resposta.opcoes
        }

        setMensagens((atual) => [...atual.slice(-49), msgIA])
      } catch {
        setErro("Não foi possível enviar a mensagem. Tente novamente.")
        setMensagemErro(texto)
      } finally {
        setDigitando(false)
      }
    },
    [digitando, mensagens]
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

  const lidarConfirmarLancamento = async () => {
    if (!propostaSelecionada) return
    const valor = parseFloat(valorProposta.replace(",", "."))
    if (!valor || valor <= 0) return
    if (!categoriaSelecionada) return

    setDigitando(true)
    try {
      await confirmarLancamento({
        ...propostaSelecionada,
        tipo: tipoProposta,
        valor,
        data: dataProposta,
        categoriaId: categoriaSelecionada,
      })
      setMensagens((atual) => [
        ...atual.slice(-49),
        {
          id: `msg-${Date.now()}`,
          role: "assistant",
          content: "Lançamento registrado",
          criadoEm: new Date(),
        },
      ])
      setPropostaSelecionada(null)
      setCategoriaSelecionada(null)
    } catch {
      setErro("Não foi possível registrar o lançamento. Tente novamente.")
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
    setPropostaSelecionada(null)
    setCategoriaSelecionada(null)
  }

  return (
    // Altura fixa: no mobile o chat ocupa a largura toda abaixo dos
    // graficos; no lg+ vira coluna lateral ao lado deles.
    <Card className="flex h-[480px] flex-col border border-input bg-card lg:h-full">
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
            const ehLancamento = !!m.lancamentoProposta

            return (
              <div
                key={m.id}
                className={cn(
                  "flex",
                  isUser ? "justify-end" : "justify-start",
                  "max-w-[85%]"
                )}
              >
                {isUser ? (
                  <div className="bg-secondary rounded-lg p-3 text-secondary-foreground text-sm">
                    {m.content}
                  </div>
                ) : ehLancamento && propostaSelecionada ? (
                  renderCardConfirmacao(m, propostaSelecionada)
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
            !propostaSelecionada && (
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
    proposta: PropostaLancamento,
  ) {
    const ehEntrada = proposta.tipo === "ENTRADA"
    const corValor = ehEntrada ? "text-positive" : "text-destructive"
    const categoriaIdAtual =
      categoriaSelecionada ?? proposta.categoriaId ?? null
    const podeConfirmarLocal =
      parseFloat(valorProposta.replace(",", ".")) > 0 &&
      categoriaIdAtual !== null

    return (
      <div className="bg-card rounded-lg p-4 border border-border shadow-sm max-w-[80%]">
        <h3 className="text-sm font-semibold text-foreground mb-2">
          Entendi este lançamento
        </h3>
        <div className="flex items-baseline gap-2 mb-4">
          <span className={`text-xl font-bold ${corValor}`}>
            {ehEntrada ? "+" : "-"} {formatarMoeda(proposta.valor)}
          </span>
          <span className="text-xs text-muted-foreground">{proposta.data}</span>
        </div>

        <div className="space-y-3 mb-4">
          <div>
            <Label className="text-xs text-muted-foreground">Tipo</Label>
            <div className="flex items-center gap-2 mt-1">
              <button
                onClick={() => setTipoProposta("ENTRADA")}
                className={cn(
                  "flex-1 rounded-lg border px-2 py-1.5 text-xs font-medium transition-colors",
                  tipoProposta === "ENTRADA"
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-input bg-transparent text-muted-foreground"
                )}
              >
                Entrada
              </button>
              <button
                onClick={() => setTipoProposta("SAIDA")}
                className={cn(
                  "flex-1 rounded-lg border px-2 py-1.5 text-xs font-medium transition-colors",
                  tipoProposta === "SAIDA"
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-input bg-transparent text-muted-foreground"
                )}
              >
                Saída
              </button>
            </div>
          </div>

          <div>
            <Label className="text-xs text-muted-foreground">Valor</Label>
            <Input
              type="text"
              value={valorProposta}
              onChange={(e) => {
                const v = e.target.value.replace(",", ".")
                setValorProposta(v)
              }}
              className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          <div>
            <Label className="text-xs text-muted-foreground">Data</Label>
            <Input
              type="date"
              value={dataProposta}
              onChange={(e) => setDataProposta(e.target.value)}
              className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          <div>
            <Label className="text-xs text-muted-foreground">Categoria</Label>
            <Select
              value={categoriaIdAtual ?? ""}
              onValueChange={(valor) =>
                setCategoriaSelecionada(valor || null)
              }
            >
              <SelectTrigger
                className="h-9 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm focus-visible:border-ring focus-visible:ring-1 focus-visible:ring-ring w-full"
              >
                <SelectValue
                  placeholder={
                    categoriaIdAtual ? "" : "Escolha uma categoria"
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
            {categoriaIdAtual === null && proposta.categoriaSugerida && (
              <p className="text-xs text-muted-foreground mt-1">
                Sugestão: {proposta.categoriaSugerida}
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center justify-end gap-2">
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
            onClick={lidarConfirmarLancamento}
            disabled={!podeConfirmarLocal || digitando}
            className="text-xs"
          >
            {digitando ? (
              <span className="flex items-center gap-1">
                <Spinner className="mr-1 h-3 w-3" />
                Confirmando…
              </span>
            ) : (
              "Confirmar"
            )}
          </Button>
        </div>
      </div>
    )
  }
}
