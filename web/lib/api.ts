import { cookies } from "next/headers";

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

  try {
    const res = await fetch(`${process.env.API_URL}/api/auth/me`, {
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
  totalEntradas: string;
  totalSaidas: string;
  saldo: string;
  variacaoEntradas: string;
  variacaoSaidas: string;
  variacaoSaldo: string;
  percentualEntradas: string;
  percentualSaidas: string;
  percentualSaldo: string;
  categorias: Array<{ id: number; nome: string; total: string }>;
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

  try {
    const res = await fetch(
      `${process.env.API_URL}/api/dashboard?ano=${ano}&mes=${mes}`,
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
  valor: string;
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

  try {
    const res = await fetch(
      `${process.env.API_URL}/api/lancamentos?ano=${ano}&mes=${mes}`,
      {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      },
    );
    if (!res.ok) return null;
    const lista = (await res.json()) as LancamentoData[];
    return { lancamentos: lista, total: lista.length };
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

  try {
    const res = await fetch(`${process.env.API_URL}/api/lancamentos`, {
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

  try {
    const res = await fetch(`${process.env.API_URL}/api/lancamentos/${id}`, {
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

/** DELETE /api/lancamentos/{id} — exclui lançamento. Retorna true se 204. */
export async function excluirLancamento(id: number): Promise<boolean> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return false;

  try {
    const res = await fetch(`${process.env.API_URL}/api/lancamentos/${id}`, {
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

  try {
    const res = await fetch(`${process.env.API_URL}/api/categorias`, {
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

  try {
    const res = await fetch(`${process.env.API_URL}/api/categorias`, {
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

  try {
    const res = await fetch(`${process.env.API_URL}/api/categorias/${id}`, {
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

  try {
    const res = await fetch(`${process.env.API_URL}/api/categorias/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    return res.status === 204;
  } catch {
    return false;
  }
}
