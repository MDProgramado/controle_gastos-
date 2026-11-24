import { expect, test } from "@playwright/test";
import { NextRequest } from "next/server";
import {
  authorizeUrl,
  derivarChallenge,
  gerarState,
  gerarVerifier,
  googleStartHref,
} from "../lib/google-oauth";

async function comEnv(valores: Record<string, string | undefined>, callback: () => void | Promise<void>) {
  const anteriores = new Map<string, string | undefined>();
  for (const [chave, valor] of Object.entries(valores)) {
    anteriores.set(chave, process.env[chave]);
    if (valor === undefined) delete process.env[chave];
    else process.env[chave] = valor;
  }
  try {
    await callback();
  } finally {
    for (const [chave, valor] of anteriores) {
      if (valor === undefined) delete process.env[chave];
      else process.env[chave] = valor;
    }
  }
}

const oauthHabilitado = {
  NEXT_PUBLIC_GOOGLE_OAUTH_ENABLED: "1",
  NEXT_PUBLIC_GOOGLE_CLIENT_ID: "cliente-teste",
  GOOGLE_CLIENT_ID: "cliente-teste",
  GOOGLE_CLIENT_SECRET: "segredo-teste",
} satisfies Record<string, string>;

test("gera state e verifier aleatórios em base64url", () => {
  const state = gerarState();
  const verifier = gerarVerifier();

  expect(state).toMatch(/^[A-Za-z0-9_-]{40,}$/);
  expect(verifier).toMatch(/^[A-Za-z0-9_-]{40,}$/);
  expect(state).not.toBe(gerarState());
});

test("deriva challenge S256 segundo RFC 7636", async () => {
  const verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

  await expect(derivarChallenge(verifier)).resolves.toBe(
    "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
  );
});

test("googleStartHref monta URLs de login e cadastro sem segredos", () => {
  expect(googleStartHref("LOGIN")).toBe("/api/auth/google/start?intencao=LOGIN");
  expect(googleStartHref("CADASTRO")).toBe("/api/auth/google/start?intencao=CADASTRO");
});

test("authorizeUrl monta Authorization Code + PKCE", () => {
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID = "  cliente-teste  ";
  try {
    const url = new URL(
      authorizeUrl("estado-123", "challenge-abc", "http://localhost:3000/api/auth/google/callback"),
    );

    expect(`${url.origin}${url.pathname}`).toBe(
      "https://accounts.google.com/o/oauth2/v2/auth",
    );
    expect(url.toString()).not.toContain("client_secret");
    expect(url.searchParams.has("client_secret")).toBe(false);
    expect(url.searchParams.get("client_id")).toBe("cliente-teste");
    expect(url.searchParams.get("redirect_uri")).toBe(
      "http://localhost:3000/api/auth/google/callback",
    );
    expect(url.searchParams.get("response_type")).toBe("code");
    expect(url.searchParams.get("scope")).toBe("openid email profile");
    expect(url.searchParams.get("state")).toBe("estado-123");
    expect(url.searchParams.get("code_challenge")).toBe("challenge-abc");
    expect(url.searchParams.get("code_challenge_method")).toBe("S256");
    expect(url.searchParams.get("access_type")).toBe("online");
    expect(url.searchParams.get("prompt")).toBe("select_account");
    expect(url.searchParams.get("intencao")).toBeNull();
  } finally {
    if (clientId === undefined) delete process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
    else process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID = clientId;
  }
});

test("valida token de convite apenas no formato base64url esperado", async () => {
  const { validarTokenConvite } = await import("../lib/google-convite");

  expect(validarTokenConvite("convite_valido_1234567890")).toBe("convite_valido_1234567890");
  expect(validarTokenConvite("  convite_valido_1234567890  ")).toBe(
    "convite_valido_1234567890",
  );
  expect(validarTokenConvite("curto")).toBeNull();
  expect(validarTokenConvite("convite com espaços 123456")).toBeNull();
  expect(validarTokenConvite("convite/barra_1234567890")).toBeNull();
  expect(validarTokenConvite(null)).toBeNull();
});

test("cookie de convite é httpOnly, lax e expira em 10 minutos", async () => {
  const { opcoesCookieConvite } = await import("../lib/google-convite");

  await comEnv({ NODE_ENV: "development" }, () => {
    expect(opcoesCookieConvite()).toEqual({
      httpOnly: true,
      sameSite: "lax",
      path: "/",
      secure: false,
      maxAge: 600,
    });
  });
  await comEnv({ NODE_ENV: "production" }, () => {
    expect(opcoesCookieConvite().secure).toBe(true);
  });
});

test("rota de convite grava cookie e redireciona sem expor o token na URL", async () => {
  const token = "convite_valido_1234567890";
  const { GET } = await import("../app/convite/[token]/route");

  const resposta = await GET(new NextRequest(`http://localhost:3000/convite/${token}`), {
    params: Promise.resolve({ token }),
  });

  expect(resposta.status).toBe(303);
  const destino = new URL(resposta.headers.get("location") ?? "");
  expect(destino.pathname).toBe("/api/auth/google/start");
  expect(destino.search).toBe("?intencao=CONVITE");
  expect(destino.toString()).not.toContain(token);
  const cookies = resposta.headers.get("set-cookie") ?? "";
  expect(cookies).toContain(`convite_oauth=${token}`);
  expect(cookies).toContain("HttpOnly");
  expect(cookies).toContain("SameSite=lax");
  expect(cookies).toContain("Max-Age=600");
});

test("rota de convite rejeita token inválido sem iniciar OAuth", async () => {
  const { GET } = await import("../app/convite/[token]/route");

  const resposta = await GET(new NextRequest("http://localhost:3000/convite/curto"), {
    params: Promise.resolve({ token: "curto" }),
  });

  expect(resposta.status).toBe(400);
  expect(resposta.headers.get("location")).toBeNull();
});

test("start do Google ignora conviteToken na query e usa somente o cookie", async () => {
  const tokenQuery = "query_token_valido_1234567890";
  const tokenCookie = "cookie_token_valido_1234567890";
  const { GET } = await import("../app/api/auth/google/start/route");

  await comEnv(oauthHabilitado, async () => {
    const semCookie = new NextRequest(
      `http://localhost:3000/api/auth/google/start?intencao=CONVITE&conviteToken=${tokenQuery}`,
      { headers: { "x-real-ip": "10.20.0.1" } },
    );
    const respostaSemCookie = await GET(semCookie);
    expect(respostaSemCookie.status).toBe(400);

    const comCookie = new NextRequest(
      `http://localhost:3000/api/auth/google/start?intencao=CONVITE&conviteToken=${tokenQuery}`,
      {
        headers: {
          cookie: `convite_oauth=${tokenCookie}`,
          "x-real-ip": "10.20.0.2",
        },
      },
    );
    const resposta = await GET(comCookie);
    expect(resposta.status).toBe(303);
    expect(resposta.headers.get("location")).toContain("accounts.google.com");
    expect(resposta.headers.get("location")).not.toContain(tokenQuery);
    const cookies = resposta.headers.get("set-cookie") ?? "";
    expect(cookies).toContain(tokenCookie);
    expect(cookies).not.toContain(tokenQuery);
    expect(cookies).toContain("convite_oauth=;");
    expect(cookies).toContain("Expires=Thu, 01 Jan 1970");
  });
});
