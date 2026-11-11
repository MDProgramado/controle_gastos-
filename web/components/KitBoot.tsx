"use client"

import { useEffect } from "react"

const KIT_SCRIPTS = [
  "/assets/motion/motion.js",
  "/assets/moments/moments.js",
]

/**
 * Carrega os scripts do kit (motion.js e moments.js) DEPOIS da hidratação.
 *
 * Ambos mutam DOM gerenciado pelo React (classes do hero, dígitos de
 * .t-stat, data-moment-mounted) — rodando antes, quebrariam a hidratação
 * (React: "server rendered text didn't match"). PRECISA ficar renderizado
 * pela PÁGINA (não pelo layout): o shell do layout hidrata em um commit e
 * o segmento da página em outro — efeito num componente do layout roda
 * antes da página hidratar. O gate duplo garante import só com a árvore
 * hidratada E o documento carregado, qualquer que seja a ordem. Idempotente
 * (StrictMode).
 */
export function KitBoot() {
  useEffect(() => {
    const start = () => {
      for (const src of KIT_SCRIPTS) {
        if (document.querySelector(`script[data-kit-script="${src}"]`)) continue
        const s = document.createElement("script")
        s.type = "module"
        s.src = src
        s.setAttribute("data-kit-script", src)
        document.head.appendChild(s)
      }
    }

    if (document.readyState === "complete") {
      start()
      return
    }
    window.addEventListener("load", start, { once: true })
    return () => window.removeEventListener("load", start)
  }, [])

  return null
}
