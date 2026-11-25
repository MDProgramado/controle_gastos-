import { encaminharParaApi } from "@/lib/bff";

type Contexto = { params: Promise<{ id: string }> };

export async function POST(_request: Request, { params }: Contexto) {
  const { id } = await params;

  return encaminharParaApi(
    `/api/usuarios/${encodeURIComponent(id)}/desativar`,
    { method: "POST", limparCookieEm401: true },
  );
}
