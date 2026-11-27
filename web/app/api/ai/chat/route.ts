import { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";
import { AI_TIMEOUT_MS } from "@/lib/bff-helpers";
import { limitarPorIp } from "@/lib/rate-limit";

export const maxDuration = 60;

export async function POST(request: NextRequest) {
  if (limitarPorIp(request, "ai-chat", 20, 60_000)) {
    return Response.json(
      { detail: "Muitas mensagens em pouco tempo. Aguarde um instante e tente novamente." },
      { status: 429 },
    );
  }

  let corpo: string;
  try {
    corpo = await request.text();
  } catch {
    return Response.json({ erro: "corpo invÃ¡lido" }, { status: 400 });
  }

  return encaminharParaApi("/api/ai/chat", {
    method: "POST",
    body: corpo,
    timeoutMs: AI_TIMEOUT_MS,
  });
}
