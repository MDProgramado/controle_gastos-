/**
 * Tipos compartilhados entre o Sheet de lote (LoteSheet) e a linha do
 * lote (LoteLinha). Ficam num arquivo proprio para os dois nao se
 * importarem mutuamente.
 */

/** Campo de uma linha do lote que pode falhar na validacao. */
export type CampoLote = "categoria" | "tipo" | "valor" | "data"

/**
 * Erro de uma linha do lote, ligado ao campo que falhou — a mensagem
 * aparece no campo, nao num aviso generico.
 */
export type ErroLinha = {
  campo: CampoLote
  mensagem: string
}

export type TipoLancamento = "ENTRADA" | "SAIDA"

export type CategoriaOption = {
  id: number
  nome: string
  tipo: string
  pessoal: boolean
  ativo: boolean
}

export type LinhaLote = {
  id: string
  categoriaId: number | null
  tipo: TipoLancamento
  valor: string
  data: string
  descricao: string
  erro?: string
}
