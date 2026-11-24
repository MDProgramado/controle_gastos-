import { googleClientId } from "./google-oauth-config";

export type IntencaoGoogle = "CADASTRO" | "LOGIN" | "CONVITE";

const AUTORIZACAO = "https://accounts.google.com/o/oauth2/v2/auth";
const ESCOPO = "openid email profile";
const BASE64URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

function emBase64Url(bytes: Uint8Array): string {
  let resultado = "";
  for (let indice = 0; indice < bytes.length; indice += 3) {
    const a = bytes[indice];
    const b = indice + 1 < bytes.length ? bytes[indice + 1] : 0;
    const c = indice + 2 < bytes.length ? bytes[indice + 2] : 0;
    resultado += BASE64URL[a >> 2];
    resultado += BASE64URL[((a & 3) << 4) | (b >> 4)];
    resultado += indice + 1 < bytes.length ? BASE64URL[((b & 15) << 2) | (c >> 6)] : "";
    resultado += indice + 2 < bytes.length ? BASE64URL[c & 63] : "";
  }
  return resultado;
}

function aleatorio(bytes: number): string {
  const valores = new Uint8Array(bytes);
  crypto.getRandomValues(valores);
  return emBase64Url(valores);
}

export function googleStartHref(intencao: Exclude<IntencaoGoogle, "CONVITE">): string {
  return `/api/auth/google/start?intencao=${intencao}`;
}

export function gerarState(): string {
  return aleatorio(32);
}

export function gerarVerifier(): string {
  return aleatorio(32);
}

export async function derivarChallenge(verifier: string): Promise<string> {
  const resumo = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  return emBase64Url(new Uint8Array(resumo));
}

export function authorizeUrl(state: string, challenge: string, redirectUri?: string): string {
  const url = new URL(AUTORIZACAO);
  url.search = new URLSearchParams({
    client_id: googleClientId(),
    redirect_uri: redirectUri ?? "",
    response_type: "code",
    scope: ESCOPO,
    state,
    code_challenge: challenge,
    code_challenge_method: "S256",
    access_type: "online",
    prompt: "select_account",
  }).toString();
  return url.toString();
}
