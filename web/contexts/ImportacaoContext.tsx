// contexts/ImportacaoContext.tsx
"use client"

import * as React from "react"
import { useRouter } from "next/navigation"
import {
  ApiError,
  analisarPDFs,
  type ImportacaoAnaliseResponse,
} from "@/lib/api-client"

type StatusBackground = "ocioso" | "analisando" | "concluido" | "erro"

type ImportacaoContextValue = {
  statusBg: StatusBackground
  progressoBg: number
  totalArquivosBg: number
  resultadoBg: ImportacaoAnaliseResponse | null
  erroBg: { titulo: string; descricao: string } | null
  iniciarAnaliseBackground: (arquivos: File[]) => Promise<ImportacaoAnaliseResponse | null>
  limparStatusBackground: () => void
}

const ImportacaoContext = React.createContext<ImportacaoContextValue | null>(null)

export function ImportacaoProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const [statusBg, setStatusBg] = React.useState<StatusBackground>("ocioso")
  const [progressoBg, setProgressoBg] = React.useState(0)
  const [totalArquivosBg, setTotalArquivosBg] = React.useState(0)
  const [resultadoBg, setResultadoBg] = React.useState<ImportacaoAnaliseResponse | null>(null)
  const [erroBg, setErroBg] = React.useState<{ titulo: string; descricao: string } | null>(null)
  const timerRef = React.useRef<ReturnType<typeof setInterval> | null>(null)

  const pararTimer = () => {
    if (timerRef.current !== null) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }

  React.useEffect(() => pararTimer, [])

  const iniciarAnaliseBackground = React.useCallback(
    async (arquivos: File[]): Promise<ImportacaoAnaliseResponse | null> => {
      pararTimer()
      setErroBg(null)
      setResultadoBg(null)
      setTotalArquivosBg(arquivos.length)
      setProgressoBg(4)
      setStatusBg("analisando")

      const fatorLote = Math.max(1, arquivos.length * 0.85)
      timerRef.current = setInterval(() => {
        setProgressoBg((p) => {
          if (p >= 95) return 95
          const passo = Math.max(0.4, ((95 - p) * 0.065) / fatorLote)
          return Math.min(95, p + passo)
        })
      }, 250)

      try {
        const resposta = await analisarPDFs(arquivos)
        pararTimer()
        setProgressoBg(100)
        setResultadoBg(resposta)
        setStatusBg("concluido")
        return resposta
      } catch (e) {
        pararTimer()
        setProgressoBg(0)
        if (e instanceof ApiError && e.status === 401) {
          setStatusBg("ocioso")
          router.push("/login?expirada=1")
          return null
        }
        setErroBg({
          titulo:
            e instanceof ApiError && e.status === 503
              ? "A análise por IA está indisponível"
              : "Não foi possível analisar os arquivos",
          descricao:
            e instanceof ApiError && e.message
              ? e.message
              : "Nenhum dado foi gravado. Tente novamente em instantes.",
        })
        setStatusBg("erro")
        return null
      }
    },
    [router],
  )

  const limparStatusBackground = React.useCallback(() => {
    pararTimer()
    setStatusBg("ocioso")
    setProgressoBg(0)
    setTotalArquivosBg(0)
    setResultadoBg(null)
    setErroBg(null)
  }, [])

  return (
    <ImportacaoContext.Provider
      value={{
        statusBg,
        progressoBg,
        totalArquivosBg,
        resultadoBg,
        erroBg,
        iniciarAnaliseBackground,
        limparStatusBackground,
      }}
    >
      {children}
    </ImportacaoContext.Provider>
  )
}

export function useImportacaoBackground() {
  const ctx = React.useContext(ImportacaoContext)
  if (!ctx) {
    throw new Error("useImportacaoBackground deve ser usado dentro de <ImportacaoProvider>")
  }
  return ctx
}