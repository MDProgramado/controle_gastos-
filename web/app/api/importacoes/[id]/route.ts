import { cookies } from "next/headers";

/**
 * BFF do detalhe e descarte de uma importação (fase 5).
 *
 * GET — detalhe completo (itens + fatura + resumo) de um documento do tenant.
 * DELETE — descarta uma importação pendente (status → DESCARTADO); a API
 * devolve 409 quando o documento já foi confirmado.
 *
 * Padrão de `web/app/api/ai/chat/route.ts`: lê o cookie "sess", repassa
 * `Authorization: Bearer` e devolve o status/corpo da API; erro de rede → 502.
 */

type Contexto = { params: Promise<{ id: string }> };

export async function GET(_request: Request, { params }: Contexto) {
  const { id } = await params;
  const idValido = validarId(id);
  if (idValido === null) {
    return Response.json({ erro: "id inválido" }, { status: 400 });
  }

  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

  let res: Response;
  try {
    res = await fetch(`${process.env.API_URL}/api/importacoes/${idValido}`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { erro: "Não foi possível conectar. Tente novamente em instantes." },
      { status: 502 },
    );
  }

  const body = await res.text();
  return new Response(body, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("content-type") ?? "application/json" },
  });
}

export async function DELETE(_request: Request, { params }: Contexto) {
  const { id } = await params;
  const idValido = validarId(id);
  if (idValido === null) {
    return Response.json({ erro: "id inválido" }, { status: 400 });
  }

  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

  let res: Response;
  try {
    res = await fetch(`${process.env.API_URL}/api/importacoes/${idValido}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { erro: "Não foi possível conectar. Tente novamente em instantes." },
      { status: 502 },
    );
  }

  // 204 não tem corpo; devolve vazio com o status da API.
  if (res.status === 204) {
    return new Response(null, { status: 204 });
  }

  const body = await res.text();
  return new Response(body, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("content-type") ?? "application/json" },
  });
}

/** Só aceita ids inteiros positivos — protege a URL da API de qualquer injection. */
function validarId(id: string): number | null {
  const numero = Number(id);
  if (!Number.isInteger(numero) || numero <= 0) {
    return null;
  }
  return numero;
}
