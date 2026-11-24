import { cookies } from "next/headers";
import { urlApiServidor } from "./api-url";
import { API_TIMEOUT_MS, respostaErroBff, urlApiSegura } from "./bff-helpers";

type EncaminharOptions = {
  method?: string;
  body?: BodyInit | null;
  contentType?: string | null;
  headers?: Record<string, string>;
  query?: Record<string, string | string[] | undefined> | URLSearchParams;
  limparCookieEm401?: boolean;
};

export async function encaminharParaApi(
  caminho: string,
  options: EncaminharOptions = {},
): Promise<Response> {
  const caminhoSeguro = urlApiSegura(caminho);
  if (!caminhoSeguro) {
    return respostaErroBff(403, "Rota não permitida");
  }

  const apiUrl = urlApiServidor();
  if (!apiUrl) {
    return respostaErroBff(503, "API não configurada");
  }

  const token = (await cookies()).get("sess")?.value;
  if (!token) {
    return respostaErroBff(401, "Sessão expirada");
  }

  const url = new URL(caminhoSeguro, apiUrl);
  const query = options.query;
  if (query instanceof URLSearchParams) {
    query.forEach((valor, chave) => url.searchParams.append(chave, valor));
  } else {
    for (const [chave, valor] of Object.entries(query ?? {})) {
      if (Array.isArray(valor)) {
        valor.forEach((item) => url.searchParams.append(chave, item));
      } else if (valor !== undefined) {
        url.searchParams.set(chave, valor);
      }
    }
  }

  try {
    const headers: Record<string, string> = { Authorization: `Bearer ${token}` };
    if (options.contentType) {
      headers["Content-Type"] = options.contentType;
    } else if (options.contentType !== null && typeof options.body === "string") {
      headers["Content-Type"] = "application/json";
    }

    const resposta = await fetch(url, {
      method: options.method ?? "GET",
      headers: { ...headers, ...options.headers },
      body: options.body,
      signal: AbortSignal.timeout(API_TIMEOUT_MS),
      cache: "no-store",
    });

    if (resposta.status === 401 && options.limparCookieEm401) {
      (await cookies()).delete("sess");
    }

    const respostaHeaders = {
      "content-type": resposta.headers.get("content-type") ?? "application/json",
      "cache-control": "no-store",
    };

    if (resposta.status === 204) {
      return new Response(null, { status: 204, headers: respostaHeaders });
    }

    return new Response(resposta.body, {
      status: resposta.status,
      headers: respostaHeaders,
    });
  } catch (erro) {
    if (erro instanceof Error && erro.name === "TimeoutError") {
      return respostaErroBff(504, "Tempo limite ao consultar a API");
    }
    return respostaErroBff(502, "Não foi possível conectar à API");
  }
}
