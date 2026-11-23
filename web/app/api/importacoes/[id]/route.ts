import { encaminharParaApi } from "@/lib/bff";

type Contexto = { params: Promise<{ id: string }> };

export async function GET(_request: Request, { params }: Contexto) {
  const idValido = validarId((await params).id);
  if (idValido === null) {
    return Response.json({ erro: "id inválido" }, { status: 400 });
  }

  return encaminharParaApi(`/api/importacoes/${idValido}`);
}

export async function DELETE(_request: Request, { params }: Contexto) {
  const idValido = validarId((await params).id);
  if (idValido === null) {
    return Response.json({ erro: "id inválido" }, { status: 400 });
  }

  return encaminharParaApi(`/api/importacoes/${idValido}`, { method: "DELETE" });
}

function validarId(id: string): number | null {
  const numero = Number(id);
  if (!Number.isInteger(numero) || numero <= 0) {
    return null;
  }
  return numero;
}
