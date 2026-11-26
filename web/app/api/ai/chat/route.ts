import { encaminharParaApi } from "@/lib/bff";
import { AI_TIMEOUT_MS } from "@/lib/bff-helpers";

export const maxDuration = 60;

export async function POST(request: Request) {
  let corpo: string;
  try {
    corpo = await request.text();
  } catch {
    return Response.json({ erro: "corpo inválido" }, { status: 400 });
  }

  return encaminharParaApi("/api/ai/chat", {
    method: "POST",
    body: corpo,
    timeoutMs: AI_TIMEOUT_MS,
  });
}
