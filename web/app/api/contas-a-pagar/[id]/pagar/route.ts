import { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

interface RouteContext {
  params: Promise<{ id: string }>;
}

export async function PATCH(request: NextRequest, context: RouteContext) {
  const { id } = await context.params;
  const body = await request.json().catch(() => ({}));

  return encaminharParaApi(`/api/contas-a-pagar/${id}/pagar`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}
