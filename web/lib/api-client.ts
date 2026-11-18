/**
- API client-side (Client Component).
- Funções que podem ser chamadas diretamente do navegador.
 *
- A API (Spring Boot) exige o header Authorization: Bearer, e o cookie "sess" é httpOnly —
- ou seja, o JavaScript da página não o lê. Por isso as chamadas passam pelas rotas BFF do
- Next (/api/ai/chat, /api/categorias), que repassam o Bearer para a API.
 */

export type AiChatRequest = {
  mensagem: string;
  historico: Array<{ role: string; content: string }>;
  sessionId?: string;
};

export type AiPropostaLancamento = {
  tipo: "ENTRADA" | "SAIDA";
  valor: number;
  data: string;
  categoriaId: string | null;
  categoriaSugerida?: string;
  descricao?: string;
};

export type AiChatResponse = {
  intent: "consulta" | "lancamento" | "esclarecimento";
  resposta: string;
  dados?: Array<{ label: string; valor: number }>;
  proposta?: AiPropostaLancamento;
  /** Todos os lançamentos retornados na mensagem (pode ter mais de um). */
  propostas?: AiPropostaLancamento[];
  opcoes?: string[];
};

/**
- POST /api/ai/chat — envia mensagem para a IA e obtém resposta estruturada.
 *
- Quando a API responde com erro (503 do serviço de IA, por exemplo), devolve a mensagem do
- servidor para o chat mostrar algo útil em vez de um erro genérico.
 */
export async function chatComIA(
  request: AiChatRequest,
): Promise<AiChatResponse | null> {
  try {
    const res = await fetch("/api/ai/chat", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) {
      const detalhe = await extrairDetalhe(res);
      throw new Error(
        detalhe ??
          "Não foi possível falar com o serviço de IA. Tente novamente mais tarde.",
      );
    }
    return (await res.json()) as AiChatResponse;
  } catch (e) {
    if (e instanceof Error) throw e;
    return null;
  }
}

/** Lê o campo "detail" de um ProblemDetail (RFC 7807) devolvido pela API. */
async function extrairDetalhe(res: Response): Promise<string | null> {
  try {
    const corpo = (await res.json()) as { detail?: string };
    return typeof corpo?.detail === "string" ? corpo.detail : null;
  } catch {
    return null;
  }
}

function extrairDetalheTexto(texto: string): string | null {
  try {
    const corpo = JSON.parse(texto) as { detail?: string };
    return typeof corpo?.detail === "string" ? corpo.detail : null;
  } catch {
    return null;
  }
}

export type CategoriaDisponivel = {
  id: number;
  nome: string;
  tipo: string;
  ativo: boolean;
};

/** GET /api/categorias — categorias reais do tenant (via BFF). */
export async function listarCategorias(): Promise<CategoriaDisponivel[]> {
  try {
    const res = await fetch("/api/categorias", { cache: "no-store" });
    if (!res.ok) return [];
    const dados = (await res.json()) as CategoriaDisponivel[];
    return Array.isArray(dados) ? dados : [];
  } catch {
    return [];
  }
}

export type LancamentoParaGravar = {
  categoriaId: number;
  tipo: "ENTRADA" | "SAIDA";
  valor: number;
  data: string;
  descricao?: string;
};

/**
- POST /api/lancamentos/lote — grava os lançamentos confirmados em UMA transação
- (tudo-ou-nada). Devolve os ids criados ou null em caso de falha.
 */
export async function gravarLancamentos(
  linhas: LancamentoParaGravar[],
): Promise<number[] | null> {
  try {
    const res = await fetch("/api/lancamentos/lote", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ linhas }),
      cache: "no-store",
    });
    if (!res.ok) return null;
    const dados = (await res.json()) as Array<{ id: number }>;
    return dados.map((l) => l.id);
  } catch {
    return null;
  }
}

/* ============================ Importação de PDFs (fase 5) ============================ */

/**
- Erro da API/BFF com o status HTTP — permite ao componente reagir a 401
- (sessão expirada) e 503 (IA fora) com copy própria, além de exibir a
- mensagem (pt-BR) vinda do servidor.
 */
export class ApiError extends Error {
  status: number;

  constructor(status: number, mensagem: string) {
    super(mensagem);
    this.name = "ApiError";
    this.status = status;
  }
}

/**
- Tipos do contrato da fase 5 (duplicados aqui, como `AiChatRequest` — este
- módulo roda no browser e não pode importar `lib/api.ts`, que é server-only).
 */
export type AlertaImportacao = {
  tipo: string;
  severidade: string; // ALTA | MEDIA
  mensagem: string;
};

export type GastoAtipicoImportacao = {
  categoria: string;
  valor: number;
  media: number;
  descricao: string;
  /** ISO yyyy-MM-dd. */
  data: string;
};

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

export type ImportacaoItemRevisao = {
  id: number;
  /** ISO yyyy-MM-dd. */
  data: string | null;
  descricao: string | null;
  /** Número JSON (BigDecimal plain da API, scale 2). */
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

export type ImportacaoDocumentoAnalise = {
  nomeArquivo: string;
  /** ANALISADO | DUPLICADO | SEM_TEXTO | ERRO */
  status: string;
  mensagem: string | null;
  documentoId: number | null;
  tipoDocumento: string | null; // CARTAO | EXTRATO
  modeloUsado: string | null;
  degradado: boolean | null;
  itens: ImportacaoItemRevisao[];
  rejeitados: Array<{ motivo: string; conteudoBruto: string | null }>;
};

export type ImportacaoAnaliseResponse = {
  documentos: ImportacaoDocumentoAnalise[];
  resumo: ResumoAnalitico;
};

export type ConfirmarItemRequest = {
  itemId: number;
  incluir: boolean;
  categoriaIdOverride?: number | null;
};

export type ConfirmarRequest = {
  itens: ConfirmarItemRequest[];
};

export type ConfirmarResponse = {
  importados: number;
  ignorados: number;
  duplicados: number;
  lancamentoIds: number[];
  contaAPagarIds: number[];
};

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

export type ImportacaoHistorico = {
  content: ImportacaoHistoricoResumo[];
  totalElements: number;
};

function mapearErroUpload(status: number, detalhe: string | null): ApiError {
  switch (status) {
    case 401:
      return new ApiError(401, "Sua sessão expirou. Entre novamente.");
    case 413:
      return new ApiError(
        413,
        detalhe ??
          "O envio excede o limite de 4 MB por arquivo ou 4,5 MB por requisição. Envie menos arquivos.",
      );
    case 503:
      return new ApiError(
        503,
        detalhe ??
          "A análise por IA está indisponível. Nenhum dado foi gravado — tente novamente em alguns minutos.",
      );
    default:
      return new ApiError(
        status,
        detalhe ?? "Não foi possível enviar os arquivos. Tente novamente em instantes.",
      );
  }
}

/**
- POST /api/importacoes — envia 1..5 PDFs para a análise (multipart). Os
- limites já foram validados no cliente (D-29); o BFF valida de novo antes de
- repassar. Lança {@link ApiError} com mensagem em pt-BR em qualquer falha.
 *
- O callback opcional `onUploadProgress` recebe o percentual real (0 a 100) da
- transferência dos bytes do navegador até o BFF.
 */
export async function analisarPDFs(
  arquivos: File[],
  onUploadProgress?: (percentual: number) => void,
): Promise<ImportacaoAnaliseResponse> {
  const form = new FormData();
  for (const arquivo of arquivos) {
    form.append("arquivos", arquivo, arquivo.name);
  }

  if (onUploadProgress) {
    return new Promise<ImportacaoAnaliseResponse>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", "/api/importacoes");
      xhr.withCredentials = true;

      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable && event.total > 0) {
          onUploadProgress(Math.round((event.loaded / event.total) * 100));
        }
      };

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            resolve(JSON.parse(xhr.responseText) as ImportacaoAnaliseResponse);
          } catch {
            reject(
              new ApiError(
                502,
                "Resposta inválida do servidor ao analisar os arquivos.",
              ),
            );
          }
          return;
        }
        const detalhe = extrairDetalheTexto(xhr.responseText);
        reject(mapearErroUpload(xhr.status, detalhe));
      };

      xhr.onerror = () => {
        reject(
          new ApiError(
            502,
            "Não foi possível enviar os arquivos. Verifique sua conexão e tente novamente.",
          ),
        );
      };

      xhr.send(form);
    });
  }

  try {
    const res = await fetch("/api/importacoes", {
      method: "POST",
      body: form, // o browser gera o multipart com boundary — não setar Content-Type
      cache: "no-store",
    });
    if (!res.ok) {
      const detalhe = await extrairDetalhe(res);
      throw mapearErroUpload(res.status, detalhe);
    }
    return (await res.json()) as ImportacaoAnaliseResponse;
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError(
      502,
      "Não foi possível enviar os arquivos. Verifique sua conexão e tente novamente.",
    );
  }
}

/**
 * Normaliza a resposta de GET /api/importacoes/{id} caso a API devolva o
 * documento único em vez do envelope completo `ImportacaoAnaliseResponse`.
 */
function normalizarRespostaAnalise(dados: unknown): ImportacaoAnaliseResponse {
  const raiz = (dados ?? {}) as Partial<ImportacaoAnaliseResponse> & {
    documento?: unknown;
  };

  if (Array.isArray(raiz.documentos) && raiz.resumo) {
    return raiz as ImportacaoAnaliseResponse;
  }

  const alvo = (
    raiz.documento && typeof raiz.documento === "object" ? raiz.documento : raiz
  ) as Partial<ImportacaoDocumentoAnalise> & {
    id?: number;
    totalFatura?: number | null;
    dataVencimento?: string | null;
    alertas?: AlertaImportacao[];
    gastosAtipicos?: GastoAtipicoImportacao[];
  };

  const doc: ImportacaoDocumentoAnalise = {
    nomeArquivo: alvo.nomeArquivo ?? "Documento importado",
    status: alvo.status ?? "ANALISADO",
    mensagem: alvo.mensagem ?? null,
    documentoId: alvo.documentoId ?? (typeof alvo.id === "number" ? alvo.id : null),
    tipoDocumento: alvo.tipoDocumento ?? null,
    modeloUsado: alvo.modeloUsado ?? null,
    degradado: alvo.degradado ?? null,
    itens: Array.isArray(alvo.itens) ? alvo.itens : [],
    rejeitados: Array.isArray(alvo.rejeitados) ? alvo.rejeitados : [],
  };

  let totalRecebido = 0;
  let totalPago = 0;
  let totalComprometido = 0;
  for (const item of doc.itens) {
    if (item.tipo === "RECEITA") totalRecebido += item.valor;
    else if (item.tipo === "DESPESA_PAGA") totalPago += item.valor;
    else if (item.tipo === "A_PAGAR") totalComprometido += item.valor;
  }

  return {
    documentos: [doc],
    resumo: raiz.resumo ?? {
      totalRecebido,
      totalPago,
      totalComprometido,
      saldoPrevisto: totalRecebido - totalPago - totalComprometido,
      totalFatura: alvo.totalFatura ?? null,
      dataVencimento: alvo.dataVencimento ?? null,
      alertas: Array.isArray(alvo.alertas) ? alvo.alertas : [],
      gastosAtipicos: Array.isArray(alvo.gastosAtipicos) ? alvo.gastosAtipicos : [],
    },
  };
}

/**
 * GET /api/importacoes/{id} — recupera uma importação pendente já analisada
 * para reabrir a tela de revisão e confirmação (inclusive após F5 ou troca de página).
 */
export async function obterImportacaoPendente(
  id: number,
): Promise<ImportacaoAnaliseResponse> {
  try {
    const res = await fetch(`/api/importacoes/${id}`, { cache: "no-store" });
    const dados: unknown = await res.json().catch(() => null);

    if (!res.ok) {
      const corpo = dados as { detail?: string; erro?: string } | null;
      const detalhe =
        typeof corpo?.detail === "string"
          ? corpo.detail
          : typeof corpo?.erro === "string"
            ? corpo.erro
            : null;

      if (res.status === 401) {
        throw new ApiError(401, "Sua sessão expirou. Entre novamente.");
      }
      if (res.status === 404) {
        throw new ApiError(
          404,
          detalhe ?? "Esta importação não está mais disponível ou já foi descartada.",
        );
      }
      throw new ApiError(
        res.status,
        detalhe ?? "Não foi possível carregar os itens desta importação.",
      );
    }

    return normalizarRespostaAnalise(dados);
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError(
      502,
      "Não foi possível carregar a importação. Verifique sua conexão e tente novamente.",
    );
  }
}

/**
- POST /api/importacoes/{id}/confirmar — grava os itens marcados em transação
- tudo-ou-nada. Lança {@link ApiError} com a mensagem do ProblemDetail (ex.:
- "Linha 3 inválida: …") quando a confirmação falha.
 */
export async function confirmarImportacao(
  id: number,
  request: ConfirmarRequest,
): Promise<ConfirmarResponse> {
  try {
    const res = await fetch(`/api/importacoes/${id}/confirmar`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) {
      const detalhe = await extrairDetalhe(res);
      if (res.status === 401) {
        throw new ApiError(401, "Sua sessão expirou. Entre novamente.");
      }
      throw new ApiError(
        res.status,
        detalhe ?? "Não foi possível confirmar. Nenhum lançamento foi gravado — tente novamente.",
      );
    }
    return (await res.json()) as ConfirmarResponse;
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError(
      502,
      "Não foi possível confirmar. Verifique sua conexão e tente novamente.",
    );
  }
}

/** GET /api/importacoes — histórico de importações do tenant (paginado). */
export async function listarImportacoes(
  pagina = 0,
  tamanho = 20,
): Promise<ImportacaoHistorico | null> {
  try {
    const res = await fetch(`/api/importacoes?pagina=${pagina}&tamanho=${tamanho}`, {
      cache: "no-store",
    });
    if (!res.ok) return null;
    const dados = (await res.json()) as ImportacaoHistorico;
    return { content: dados.content, totalElements: dados.totalElements };
  } catch {
    return null;
  }
}

/**
- DELETE /api/importacoes/{id} — descarta uma importação pendente. Devolve
- true em 204; em falha lança {@link ApiError} com a mensagem do servidor
- (ex.: 409 "Importação já confirmada — …").
 */
export async function descartarImportacao(id: number): Promise<boolean> {
  try {
    const res = await fetch(`/api/importacoes/${id}`, {
      method: "DELETE",
      cache: "no-store",
    });
    if (res.status === 204) return true;
    const detalhe = await extrairDetalhe(res);
    if (res.status === 401) {
      throw new ApiError(401, "Sua sessão expirou. Entre novamente.");
    }
    throw new ApiError(
      res.status,
      detalhe ?? "Não foi possível descartar a importação. Tente novamente em instantes.",
    );
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError(
      502,
      "Não foi possível descartar a importação. Verifique sua conexão e tente novamente.",
    );
  }
}

