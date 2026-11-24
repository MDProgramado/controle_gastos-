import { cookies } from "next/headers";
import { urlApiServidor } from "./api-url";
import { API_TIMEOUT_MS } from "./bff-helpers";
import type { ValorMonetario } from "./money";
import { resultadoErroLote, type ResultadoCriacaoLote } from "./lancamentos";
import type { Grupo503020 } from "./grupos-503020";

function fetchApi(url: string, init: RequestInit = {}): Promise<Response> {
  return fetch(url, {
    ...init,
    cache: "no-store",
    signal: AbortSignal.timeout(API_TIMEOUT_MS),
  });
}

/** Shape de `GET /api/auth/me` (contrato da API — plano 02). */
export type SessionUser = {
  email: string;
  empresa_id: number;
  role: "ADMIN" | "USER";
};

/**
 * Server-side (Server Component): lê o cookie "sess" e valida a sessão chamando
 * a API diretamente com `Authorization: Bearer` — mesma autenticação das rotas
 * BFF (D-02/P4). `API_URL` é server-only: este módulo nunca é importado pelo
 * browser.
 *
 * Escolha documentada no SUMMARY: chamada direta à API em vez de self-fetch da
 * rota `/api/me` (evita roundtrip HTTP interno durante o render; o contrato é
 * idêntico).
 *
 * Retorna `null` quando não há cookie, a API responde 401 (sessão expirada) ou
 * está inacessível — o chamador decide o redirect.
 */
export async function getMe(): Promise<SessionUser | null> {
  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as SessionUser;
  } catch {
    return null;
  }
}

/** Shape de `GET /api/dashboard?ano=&mes=` (contrato da API — plano 03-01). */
export type DashboardData = {
  totalEntradas: ValorMonetario;
  totalSaidas: ValorMonetario;
  saldo: ValorMonetario;
  variacaoEntradas: ValorMonetario;
  variacaoSaidas: ValorMonetario;
  variacaoSaldo: ValorMonetario;
  percentualEntradas: string;
  percentualSaidas: string;
  percentualSaldo: string;
  categorias: Array<{ id: number; nome: string; total: ValorMonetario }>;
};

/**
 * Server-side (Server Component): busca os dados agregados do dashboard para
 * um mês específico. Segue o mesmo padrão de `getMe()`: lê o cookie "sess",
 * envia `Authorization: Bearer <token>` e usa `cache: "no-store"`.
 *
 * Retorna `null` quando não há cookie de sessão ou a API não responde com OK.
 * O chamador decide o redirect para /login quando necessário.
 */
export async function getDashboard(
  ano: number,
  mes: number,
): Promise<DashboardData | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(
      `${apiUrl}/api/dashboard?ano=${ano}&mes=${mes}`,
      {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      },
    );
    if (!res.ok) return null;
    return (await res.json()) as DashboardData;
  } catch {
    return null;
  }
}

/** Shape de `GET /api/lancamentos?ano=&mes=` (contrato da API — plano 03-04). */
export type LancamentoData = {
  id: number;
  tipo: string;
  valor: ValorMonetario;
  data: string;
  descricao: string;
  categoriaNome: string;
  categoriaTipo: string;
};

/** Shape de entrada para criar/editar lançamento (plano 03-04). */
export type LancamentoRequest = {
  categoriaId: number;
  tipo: string;
  valor: string;
  data: string;
  descricao?: string;
};

/**
 * Server-side: lista lançamentos de um mês (GET /api/lancamentos?ano=&mes=).
 * Segue o padrão de `getMe()`: lê cookie "sess", envia Bearer, cache no-store.
 * Retorna null quando não há cookie ou a API não responde OK.
 */
export async function getLancamentos(
  ano: number,
  mes: number,
): Promise<{ lancamentos: LancamentoData[]; total: number } | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(
      `${apiUrl}/api/lancamentos?ano=${ano}&mes=${mes}`,
      {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      },
    );
    if (!res.ok) return null;
    const resJson = await res.json() as {
      lancamentos: LancamentoData[];
      total: number;
    };
    return { lancamentos: resJson.lancamentos, total: resJson.total };
  } catch {
    return null;
  }
}

/** POST /api/lancamentos — cria lançamento. Retorna o lançamento criado ou null. */
export async function criarLancamento(
  request: LancamentoRequest,
): Promise<LancamentoData | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/lancamentos`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as LancamentoData;
  } catch {
    return null;
  }
}

/** PUT /api/lancamentos/{id} — edita lançamento. Retorna o lançamento editado ou null. */
export async function editarLancamento(
  id: number,
  request: LancamentoRequest,
): Promise<LancamentoData | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/lancamentos/${id}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as LancamentoData;
  } catch {
    return null;
  }
}

/** Shape de entrada para lote de lançamentos (POST /api/lancamentos/lote). */
export type LancamentoLinha = {
  categoriaId: number;
  tipo: string;
  valor: string;
  data: string;
  descricao?: string;
};

export type LoteRequest = {
  linhas: LancamentoLinha[];
};

export async function criarLote(lote: LoteRequest): Promise<ResultadoCriacaoLote> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return { ok: false, mensagem: "Sessão expirada" };
  const apiUrl = urlApiServidor();
  if (!apiUrl) return { ok: false, mensagem: "API não configurada" };

  try {
    const res = await fetchApi(`${apiUrl}/api/lancamentos/lote`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(lote),
      cache: "no-store",
    });

    if (!res.ok) {
      const corpo = await res.json().catch(() => ({}));
      return resultadoErroLote(corpo, res.status);
    }

    const criados = (await res.json()) as LancamentoData[];
    return { ok: true, criados: Array.isArray(criados) ? criados.length : 0 };
  } catch {
    return { ok: false, mensagem: "Não foi possível salvar o lote." };
  }
}

/** Shape de entrada para chat com IA (POST /api/ai/chat). */
export type AiChatRequest = {
  mensagem: string;
  historico: Array<{ role: string; content: string }>;
  sessionId?: string;
};

/** Shape de resposta do chat com IA (POST /api/ai/chat). */
export type AiChatResponse = {
  intent: "consulta" | "lancamento" | "esclarecimento";
  resposta: string;
  dados?: Array<{ label: string; valor: number }>;
  proposta?: {
    tipo: "ENTRADA" | "SAIDA";
    valor: number;
    data: string;
    categoriaId: string | null;
    categoriaSugerida?: string;
    descricao?: string;
  };
  opcoes?: string[];
};

/** POST /api/ai/chat — envia mensagem para a IA e obtém resposta estruturada. */
export async function chatComIA(
  request: AiChatRequest,
): Promise<AiChatResponse | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/ai/chat`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as AiChatResponse;
  } catch {
    return null;
  }
}

/** DELETE /api/lancamentos/{id} — exclui lançamento. Retorna true se 204. */
export async function excluirLancamento(id: number): Promise<boolean> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return false;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return false;

  try {
    const res = await fetchApi(`${apiUrl}/api/lancamentos/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    return res.status === 204;
  } catch {
    return false;
  }
}

/** Shape de `GET /api/categorias` (contrato da API — plano 03-04). */
export type CategoriaData = {
  id: number;
  nome: string;
  tipo: string;
  pessoal: boolean;
  ativo: boolean;
};

/** Shape de entrada para criar/editar categoria (plano 03-04). */
export type CategoriaRequest = {
  nome: string;
  tipo: string;
  pessoal: boolean;
};

/**
 * Server-side: lista categorias do tenant (GET /api/categorias).
 * Segue o padrão de `getMe()`: lê cookie "sess", envia Bearer, cache no-store.
 * Retorna null quando não há cookie ou a API não responde OK.
 */
export async function getCategorias(): Promise<CategoriaData[] | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/categorias`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as CategoriaData[];
  } catch {
    return null;
  }
}

/** POST /api/categorias — cria categoria (ADMIN-only no backend). Retorna a categoria criada ou null. */
export async function criarCategoria(
  request: CategoriaRequest,
): Promise<CategoriaData | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/categorias`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as CategoriaData;
  } catch {
    return null;
  }
}

/** PUT /api/categorias/{id} — edita categoria (ADMIN-only no backend). Retorna a categoria editada ou null. */
export async function editarCategoria(
  id: number,
  request: CategoriaRequest,
): Promise<CategoriaData | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/categorias/${id}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as CategoriaData;
  } catch {
    return null;
  }
}

/** DELETE /api/categorias/{id} — desativa categoria (soft delete, ADMIN-only no backend). Retorna true se 204. */
export async function desativarCategoria(id: number): Promise<boolean> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return false;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return false;

  try {
    const res = await fetchApi(`${apiUrl}/api/categorias/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    return res.status === 204;
  } catch {
    return false;
  }
}

export type CategoriaGrupo503020Data = {
  categoriaId: number;
  nome: string;
  tipo: string;
  grupo: Grupo503020;
  padrao: boolean;
};

export async function getGrupos503020(): Promise<CategoriaGrupo503020Data[] | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/categorias/grupos-503020`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as CategoriaGrupo503020Data[];
  } catch {
    return null;
  }
}

export async function salvarGrupo503020(
  categoriaId: number,
  grupo: Grupo503020,
): Promise<CategoriaGrupo503020Data | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(`${apiUrl}/api/categorias/grupos-503020/${categoriaId}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ grupo }),
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as CategoriaGrupo503020Data;
  } catch {
    return null;
  }
}

/* ============================ Importação de PDFs (fase 5) ============================ */

/**
 * Alerta do resumo analítico (D-26). `tipo` é estável para o frontend; valores
 * conhecidos hoje: DUPLICIDADE, DIVERGENCIA_FATURA, FATURA_NO_EXTRATO,
 * CATEGORIA_REVISAR, GASTO_ATIPICO.
 */
export type AlertaImportacao = {
  tipo: string;
  severidade: string; // ALTA | MEDIA
  mensagem: string;
};

/** Gasto bem acima da mediana da própria categoria (D-27). */
export type GastoAtipicoImportacao = {
  categoria: string;
  valor: number;
  media: number;
  descricao: string;
  /** ISO yyyy-MM-dd. */
  data: string;
};

/** Resumo do mês devolvido pela análise e pelo detalhe (D-26). */
export type ResumoAnalitico = {
  totalRecebido: number;
  totalPago: number;
  totalComprometido: number;
  saldoPrevisto: number;
  /** Nulo quando o lote não tem fatura de cartão. */
  totalFatura: number | null;
  /** ISO yyyy-MM-dd — nulo sem fatura. */
  dataVencimento: string | null;
  alertas: AlertaImportacao[];
  gastosAtipicos: GastoAtipicoImportacao[];
};

/**
 * Item de staging devolvido para a tela de revisão (contrato do 05-04).
 * `valor` é número JSON: a API serializa BigDecimal plain
 * (write-bigdecimal-as-plain, scale 2 preservado no texto do JSON).
 */
export type ImportacaoItemRevisao = {
  id: number;
  /** ISO yyyy-MM-dd. */
  data: string | null;
  descricao: string | null;
  valor: number;
  /** RECEITA | DESPESA_PAGA | A_PAGAR */
  tipo: string;
  categoriaNome: string | null;
  categoriaId: number | null;
  /** CARTAO | EXTRATO */
  origem: string;
  parcelaAtual: number | null;
  parcelaTotal: number | null;
  /** Marcações do item (ex.: "REVISAR_CATEGORIA: …"), concatenadas. */
  alerta: string | null;
};

/** Um documento do lote (fatura/extrato) com o resultado da análise. */
export type ImportacaoDocumentoAnalise = {
  nomeArquivo: string;
  /** ANALISADO | DUPLICADO | SEM_TEXTO | ERRO */
  status: string;
  mensagem: string | null;
  documentoId: number | null;
  /** CARTAO | EXTRATO — nulo nos estados de erro. */
  tipoDocumento: string | null;
  modeloUsado: string | null;
  degradado: boolean | null;
  itens: ImportacaoItemRevisao[];
  rejeitados: Array<{ motivo: string; conteudoBruto: string | null }>;
};

/** Shape de `POST /api/importacoes/pdf` (contrato do 05-04). */
export type ImportacaoAnaliseResponse = {
  documentos: ImportacaoDocumentoAnalise[];
  resumo: ResumoAnalitico;
};

/** Item da seleção do usuário (body do confirmar — contrato do 05-05). */
export type ConfirmarItemRequest = {
  itemId: number;
  incluir: boolean;
  categoriaIdOverride?: number | null;
};

/** Body do `POST /api/importacoes/{id}/confirmar`. */
export type ConfirmarRequest = {
  itens: ConfirmarItemRequest[];
};

/** Shape de `POST /api/importacoes/{id}/confirmar` (contrato do 05-05). */
export type ConfirmarResponse = {
  importados: number;
  ignorados: number;
  duplicados: number;
  lancamentoIds: number[];
  contaAPagarIds: number[];
};

/** Linha do histórico de importações (Spring Page de DocumentoResumo). */
export type ImportacaoHistoricoResumo = {
  id: number;
  nomeArquivo: string;
  /** CARTAO | EXTRATO */
  tipoDocumento: string;
  /** ANALISADO | CONFIRMADO | DESCARTADO | DUPLICADO | SEM_TEXTO | ERRO */
  status: string;
  tamanhoBytes: number;
  /** ISO-8601. */
  criadoEm: string;
};

/** Shape paginado devolvido por `GET /api/importacoes` (Spring Page). */
export type ImportacaoHistorico = {
  content: ImportacaoHistoricoResumo[];
  totalElements: number;
};

/** Cabeçalho de fatura devolvido no detalhe de uma importação. */
export type ImportacaoFaturaView = {
  total: number;
  /** ISO yyyy-MM-dd. */
  dataVencimento: string;
  /** ISO yyyy-MM-dd (1º dia do mês de referência). */
  mesReferencia: string;
};

/** Shape de `GET /api/importacoes/{id}` (contrato do 05-04). */
export type ImportacaoDetalhe = {
  id: number;
  nomeArquivo: string;
  tipoDocumento: string;
  status: string;
  criadoEm: string;
  itens: ImportacaoItemRevisao[];
  fatura: ImportacaoFaturaView | null;
  resumo: ResumoAnalitico;
};

/**
 * Server-side: histórico de importações do tenant (GET /api/importacoes?pagina=&tamanho=).
 * Segue o padrão de `getLancamentos()`: lê cookie "sess", envia Bearer, cache
 * no-store. Retorna null quando não há cookie ou a API não responde OK.
 */
export async function getImportacoes(
  pagina: number,
  tamanho: number,
): Promise<ImportacaoHistorico | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const apiUrl = urlApiServidor();
  if (!apiUrl) return null;

  try {
    const res = await fetchApi(
      `${apiUrl}/api/importacoes?pagina=${pagina}&tamanho=${tamanho}`,
      {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      },
    );
    if (!res.ok) return null;
    const resJson = (await res.json()) as ImportacaoHistorico;
    return { content: resJson.content, totalElements: resJson.totalElements };
  } catch {
    return null;
  }
}
