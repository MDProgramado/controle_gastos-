import { encaminharParaApi } from "@/lib/bff";

type Contexto = { params: Promise<{ id: string }> };

export async function POST(request: Request, { params }: Contexto) {
  const { id } = await params;
  const idValido = Number(id);
  if (!Number.isInteger(idValido) || idValido <= 0) {
    return Response.json({ erro: "id inválido" }, { status: 400 });
  }

  let corpo: string;
  try {
    corpo = await request.text();
  } catch {
    return Response.json({ erro: "corpo inválido" }, { status: 400 });
  }

  return encaminharParaApi(`/api/importacoes/${idValido}/confirmar`, {
    method: "POST",
    body: corpo,
  });
}
