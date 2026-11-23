import "server-only";
import { googleClientId, googleOAuthHabilitado } from "./google-oauth-config";

const TOKEN = "https://oauth2.googleapis.com/token";

export function googleClientSecret(): string {
  return (process.env.GOOGLE_CLIENT_SECRET || "").trim();
}

export function googleOAuthPronto(): boolean {
  return googleOAuthHabilitado(googleClientSecret());
}

export function tokenRequest(code: string, verifier: string, redirectUri?: string): {
  url: string;
  body: URLSearchParams;
} {
  return {
    url: TOKEN,
    body: new URLSearchParams({
      code,
      code_verifier: verifier,
      client_id: googleClientId(),
      client_secret: googleClientSecret(),
      redirect_uri: redirectUri ?? "",
      grant_type: "authorization_code",
    }),
  };
}
