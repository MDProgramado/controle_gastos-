import { expect, test } from "@playwright/test";
import {
  authorizeUrl,
  derivarChallenge,
  gerarState,
  gerarVerifier,
  googleStartHref,
} from "../lib/google-oauth";

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
