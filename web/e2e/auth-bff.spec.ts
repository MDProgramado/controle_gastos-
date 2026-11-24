import { expect, test } from "@playwright/test";
import { extrairToken, googleFalha, motivoFalhaApi, motivoFalhaApi401 } from "../lib/auth-bff";
import { googleClientId, googleOAuthHabilitado } from "../lib/google-oauth-config";
import { limitarPorIp } from "../lib/rate-limit";

function requisicao(headers: Record<string, string> = {}) {
  return { headers: new Headers(headers) } as Parameters<typeof limitarPorIp>[0];
}

function comEnv(valores: Record<string, string | undefined>, callback: () => void) {
  const anteriores = new Map<string, string | undefined>();
  for (const [chave, valor] of Object.entries(valores)) {
    anteriores.set(chave, process.env[chave]);
    if (valor === undefined) delete process.env[chave];
    else process.env[chave] = valor;
  }
  try {
    callback();
  } finally {
    for (const [chave, valor] of anteriores) {
      if (valor === undefined) delete process.env[chave];
      else process.env[chave] = valor;
    }
  }
}

test("limita pelo último hop de x-forwarded-for, não pelo primeiro", () => {
  const chave = "ip-ultimo-hop";
  const primeira = requisicao({ "x-forwarded-for": "203.0.113.1, 198.51.100.7" });
  const segunda = requisicao({ "x-forwarded-for": "10.0.0.99, 198.51.100.7" });

  expect(limitarPorIp(primeira, chave, 1, 60_000)).toBe(false);
  expect(limitarPorIp(segunda, chave, 1, 60_000)).toBe(true);
});

test("descarta registros antigos quando o mapa excede 10000 identificadores", () => {
  const chave = "capacidade-limite";
  const janela = 600_000;
  const original = requisicao({ "x-real-ip": "10.1.0.1" });

  expect(limitarPorIp(original, chave, 1, janela)).toBe(false);

  for (let indice = 0; indice < 10_001; indice += 1) {
    const ip = `10.2.${Math.floor(indice / 256)}.${indice % 256}`;
    limitarPorIp(requisicao({ "x-real-ip": ip }), chave, 1, janela);
  }

  expect(limitarPorIp(original, chave, 1, janela)).toBe(false);
});

test("falha do Google redireciona com 303 e remove o cookie de estado", () => {
  const resposta = googleFalha({ url: "http://localhost:3000/api/auth/google/callback" } as Request);

  expect(resposta.status).toBe(303);
  expect(resposta.headers.get("location")).toBe("http://localhost:3000/login?erro=google");
  expect(resposta.headers.get("set-cookie")).toContain("google_oauth=");
  expect(resposta.headers.get("set-cookie")).toContain("convite_oauth=");
});

test("falha do Google com motivo expõe a etapa na URL sem alterar a mensagem", () => {
  const resposta = googleFalha(
    { url: "http://localhost:3000/api/auth/google/callback" } as Request,
    { motivo: "TOKEN" },
  );

  expect(resposta.status).toBe(303);
  expect(resposta.headers.get("location")).toBe(
    "http://localhost:3000/login?erro=google&motivo=TOKEN",
  );
  expect(resposta.headers.get("set-cookie")).toContain("google_oauth=");
  expect(resposta.headers.get("set-cookie")).toContain("convite_oauth=");
});

test("429 no callback preserva o cookie de estado do OAuth", () => {
  const resposta = googleFalha(
    { url: "http://localhost:3000/api/auth/google/callback" } as Request,
    { erro: "Muitas tentativas. Tente novamente em instantes.", status: 429, manterEstado: true },
  );

  expect(resposta.status).toBe(429);
  expect(resposta.headers.get("set-cookie")).toBeNull();
});

test("motivoFalhaApi separa rede, resposta sem token e status HTTP", () => {
  expect(motivoFalhaApi(null, false)).toBe("API_REDE");
  expect(motivoFalhaApi(200, false)).toBe("API_SEM_TOKEN");
  expect(motivoFalhaApi(401, true)).toBe("API_401");
  expect(motivoFalhaApi(400, true)).toBe("API_400");
});

test("motivoFalhaApi401 separa token inválido de credenciais da conta", () => {
  expect(motivoFalhaApi401("Token Google inválido")).toBe("API_TOKEN_INVALIDO");
  expect(motivoFalhaApi401("E-mail ou senha inválidos")).toBe("API_CREDENCIAIS");
  expect(motivoFalhaApi401("qualquer outro detalhe")).toBe("API_401_OUTRO");
  expect(motivoFalhaApi401(null)).toBe("API_401_OUTRO");
});

test("extrairToken aceita token e accessToken da API", () => {
  expect(extrairToken({ token: "jwt-senha" })).toBe("jwt-senha");
  expect(extrairToken({ accessToken: "jwt-google" })).toBe("jwt-google");
  expect(extrairToken({ token: "", accessToken: "jwt-google" })).toBe("jwt-google");
  expect(extrairToken({ token: 123, accessToken: null })).toBeNull();
  expect(extrairToken(null)).toBeNull();
});

test("googleClientId prefere NEXT_PUBLIC preenchido e faz fallback quando vazio", () => {
  comEnv(
    { NEXT_PUBLIC_GOOGLE_CLIENT_ID: "  publico  ", GOOGLE_CLIENT_ID: "servidor" },
    () => expect(googleClientId()).toBe("publico"),
  );
  comEnv(
    { NEXT_PUBLIC_GOOGLE_CLIENT_ID: "   ", GOOGLE_CLIENT_ID: " servidor " },
    () => expect(googleClientId()).toBe("servidor"),
  );
});

test("googleOAuthHabilitado exige flag, clientId e segredo", () => {
  const base = {
    NEXT_PUBLIC_GOOGLE_OAUTH_ENABLED: "1",
    NEXT_PUBLIC_GOOGLE_CLIENT_ID: "cliente",
    GOOGLE_CLIENT_ID: "servidor",
  } satisfies Record<string, string | undefined>;

  comEnv(base, () => {
    expect(googleOAuthHabilitado("segredo")).toBe(true);
    expect(googleOAuthHabilitado("   ")).toBe(false);
  });
  comEnv({ ...base, NEXT_PUBLIC_GOOGLE_OAUTH_ENABLED: "0" }, () => {
    expect(googleOAuthHabilitado("segredo")).toBe(false);
  });
  comEnv(
    { ...base, NEXT_PUBLIC_GOOGLE_CLIENT_ID: "", GOOGLE_CLIENT_ID: "" },
    () => expect(googleOAuthHabilitado("segredo")).toBe(false),
  );
});

test("limite determinístico devolve excedeu e reiniciaEm sem depender do relógio", async () => {
  const { verificarLimite } = await import("../lib/rate-limit");
  const chave = "janela-deterministica";
  const agora = 1_700_000_000_000;
  const req = requisicao({ "x-real-ip": "10.9.0.1" });

  expect(verificarLimite(req, chave, 2, 60_000, agora)).toEqual({
    excedeu: false,
    reiniciaEm: agora + 60_000,
  });
  expect(verificarLimite(req, chave, 2, 60_000, agora + 1)).toEqual({
    excedeu: false,
    reiniciaEm: agora + 60_000,
  });
  expect(verificarLimite(req, chave, 2, 60_000, agora + 2)).toEqual({
    excedeu: true,
    reiniciaEm: agora + 60_000,
  });
});

test("limite determinístico renova a janela depois do vencimento", async () => {
  const { verificarLimite } = await import("../lib/rate-limit");
  const chave = "janela-renovada";
  const agora = 1_700_000_100_000;
  const req = requisicao({ "x-real-ip": "10.9.0.2" });

  expect(verificarLimite(req, chave, 1, 1_000, agora).excedeu).toBe(false);
  expect(verificarLimite(req, chave, 1, 1_000, agora + 999).excedeu).toBe(true);
  expect(verificarLimite(req, chave, 1, 1_000, agora + 1_000)).toEqual({
    excedeu: false,
    reiniciaEm: agora + 2_000,
  });
});

test("mapa do rate limit vive no globalThis e pode ser reiniciado por ele", async () => {
  const { CHAVE_REGISTROS_RATE_LIMIT } = await import("../lib/rate-limit");
  const chave = "singleton-hmr";
  const req = requisicao({ "x-real-ip": "10.8.0.1" });

  expect(limitarPorIp(req, chave, 1, 60_000)).toBe(false);

  const global = globalThis as Record<string, unknown>;
  const mapa = global[CHAVE_REGISTROS_RATE_LIMIT];
  expect(mapa).toBeInstanceOf(Map);
  expect((mapa as Map<string, unknown>).has(`${chave}:10.8.0.1`)).toBe(true);

  delete global[CHAVE_REGISTROS_RATE_LIMIT];
  expect(limitarPorIp(req, chave, 1, 60_000)).toBe(false);
});

test("429 do Google informa Retry-After em segundos quando há reiniciaEm", () => {
  const resposta = googleFalha(
    { url: "http://localhost:3000/api/auth/google/callback" } as Request,
    { erro: "Muitas tentativas.", status: 429, reiniciaEm: 30_000, agora: 0 },
  );

  expect(resposta.status).toBe(429);
  expect(resposta.headers.get("retry-after")).toBe("30");
  expect(resposta.headers.get("set-cookie")).toContain("convite_oauth=");
});

test("falha sem janela não informa Retry-After", () => {
  const resposta = googleFalha(
    { url: "http://localhost:3000/api/auth/google/callback" } as Request,
    { erro: "Falha", status: 400 },
  );

  expect(resposta.headers.get("retry-after")).toBeNull();
});

test("urlApiServidor mantém API_URL em desenvolvimento e produção", async () => {
  const { urlApiServidor } = await import("../lib/api-url");

  comEnv(
    { VERCEL_ENV: undefined, API_URL: "http://localhost:8080" },
    () => expect(urlApiServidor()).toBe("http://localhost:8080"),
  );
  comEnv(
    {
      VERCEL_ENV: "production",
      API_URL: "https://api.producao.example/",
      API_URL_PREVIEW: "https://api.preview.example",
      API_URL_PRODUCAO: "https://api.producao.example",
    },
    () => expect(urlApiServidor()).toBe("https://api.producao.example/"),
  );
});

test("preview exige API_URL_PREVIEW configurada", async () => {
  const { urlApiServidor } = await import("../lib/api-url");

  comEnv(
    {
      VERCEL_ENV: "preview",
      API_URL: "https://api.producao.example",
      API_URL_PREVIEW: undefined,
      API_URL_PRODUCAO: "https://api.producao.example",
    },
    () => expect(urlApiServidor()).toBeNull(),
  );
});

test("preview bloqueia alvo igual à produção mesmo com barra final", async () => {
  const { urlApiServidor } = await import("../lib/api-url");

  comEnv(
    {
      VERCEL_ENV: "preview",
      API_URL: "https://api.producao.example",
      API_URL_PREVIEW: "https://api.producao.example/",
      API_URL_PRODUCAO: "https://api.producao.example",
    },
    () => expect(urlApiServidor()).toBeNull(),
  );
});

test("preview rejeita API_URL_PREVIEW inválida", async () => {
  const { urlApiServidor } = await import("../lib/api-url");

  comEnv(
    {
      VERCEL_ENV: "preview",
      API_URL: "https://api.producao.example",
      API_URL_PREVIEW: "nao-e-url",
      API_URL_PRODUCAO: "https://api.producao.example",
    },
    () => expect(urlApiServidor()).toBeNull(),
  );
});

test("preview exige API_URL_PRODUCAO para comparar origens", async () => {
  const { urlApiServidor } = await import("../lib/api-url");

  comEnv(
    {
      VERCEL_ENV: "preview",
      API_URL: "https://api.producao.example",
      API_URL_PREVIEW: "https://api.preview.example",
      API_URL_PRODUCAO: undefined,
    },
    () => expect(urlApiServidor()).toBeNull(),
  );
});

test("preview rejeita API_URL_PRODUCAO inválida", async () => {
  const { urlApiServidor } = await import("../lib/api-url");

  comEnv(
    {
      VERCEL_ENV: "preview",
      API_URL: "https://api.producao.example",
      API_URL_PREVIEW: "https://api.preview.example",
      API_URL_PRODUCAO: "nao-e-url",
    },
    () => expect(urlApiServidor()).toBeNull(),
  );
});

test("preview aceita alvo distinto da produção", async () => {
  const { urlApiServidor } = await import("../lib/api-url");

  comEnv(
    {
      VERCEL_ENV: "preview",
      API_URL: "https://api.producao.example",
      API_URL_PREVIEW: "https://api.preview.example",
      API_URL_PRODUCAO: "https://api.producao.example",
    },
    () => expect(urlApiServidor()).toBe("https://api.preview.example"),
  );
});

test("next.config expõe cabeçalhos de segurança sem iniciar servidor", async () => {
  const { cabecalhosSeguranca } = await import("../next.config");
  const valores = new Map<string, string>();
  for (const cabecalho of cabecalhosSeguranca) {
    valores.set(cabecalho.key.toLowerCase(), cabecalho.value);
  }

  expect(valores.get("x-content-type-options")).toBe("nosniff");
  expect(valores.get("x-frame-options")).toBe("DENY");
  expect(valores.get("referrer-policy")).toBe("strict-origin-when-cross-origin");
  expect(valores.get("permissions-policy")).toBe(
    "camera=(), microphone=(), geolocation=()",
  );
  expect(valores.get("strict-transport-security")).toContain("max-age=");
  expect(valores.has("content-security-policy")).toBe(false);
});
