import { expect, test } from "@playwright/test";
import { extrairToken, googleFalha, motivoFalhaApi } from "../lib/auth-bff";
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
