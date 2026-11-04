import { cookies } from "next/headers";

/**
 * Sessão atual: lê o cookie "sess" e repassa à API como Bearer.
 * Em 401 da API o cookie é limpo antes de responder — prepara o fluxo de
 * sessão expirada (?expirada=1 no /login).
 */
export async function GET() {
  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

  let res: Response;
  try {
    res = await fetch(`${process.env.API_URL}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { erro: "Não foi possível conectar. Tente novamente em instantes." },
      { status: 502 },
    );
  }

  if (res.status === 401) {
    cookieStore.delete("sess"); // sessão expirada/inválida na API
  }

  const body = await res.text();
  return new Response(body, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("content-type") ?? "application/json" },
  });
}
