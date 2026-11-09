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
  totalEntradas: number;
  totalSaidas: number;
  saldo: number;
  variacaoEntradas: number;
  variacaoSaidas: number;
  variacaoSaldo: number;
  percentualEntradas: string;
  percentualSaidas: string;
  percentualSaldo: string;
  categorias: Array<{ id: number; nome: string; total: number }>;
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
