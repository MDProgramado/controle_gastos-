import { cookies } from "next/headers";

/** Limpa a sessão: apaga o cookie httpOnly "sess" (sem confirmação — não é destrutivo). */
export async function POST() {
  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  cookieStore.delete("sess");
  return Response.json({ ok: true });
}
