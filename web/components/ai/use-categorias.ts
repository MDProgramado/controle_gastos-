import { useCallback } from "react"
import { CategoriaData } from "@/lib/api"

/**
 * Hook que fornece a lista de categorias do sistema para uso no chat.
 *
 * Nesta fase, usa dados mock tipados. Quando o backend expor o endpoint
 * de categorias, basta trocar o corpo de `categoriasMock` por uma chamada
 * ao `getCategorias()` de `@/lib/api`.
 *
 * TODO: TROCAR PELA CHAMADA REAL À API quando o endpoint existir.
 */
export function useCategorias() {
  const categoriasMock: CategoriaData[] = [
    { id: 1, nome: "Alimentação", tipo: "SAIDA", pessoal: false, ativo: true },
    { id: 2, nome: "Transporte", tipo: "SAIDA", pessoal: false, ativo: true },
    { id: 3, nome: "Supermercado", tipo: "SAIDA", pessoal: false, ativo: true },
    { id: 4, nome: "Saúde", tipo: "SAIDA", pessoal: false, ativo: true },
    { id: 5, nome: "Assinaturas", tipo: "SAIDA", pessoal: false, ativo: true },
    { id: 6, nome: "Salário", tipo: "ENTRADA", pessoal: false, ativo: true },
    { id: 7, nome: "Receita Extra", tipo: "ENTRADA", pessoal: false, ativo: true },
    { id: 8, nome: "Pessoal", tipo: "SAIDA", pessoal: true, ativo: true },
  ]

  const carregar = useCallback(() => {
    // TODO: substituir por getCategorias() quando a API estiver pronta
  }, [])

  return { categorias: categoriasMock, carregar, recarregar: carregar }
}
