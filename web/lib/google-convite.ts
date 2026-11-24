export const COOKIE_CONVITE_OAUTH = "convite_oauth";

const TOKEN_CONVITE = /^[A-Za-z0-9_-]{20,512}$/;

export function validarTokenConvite(valor: string | null | undefined): string | null {
  if (!valor) return null;
  const token = valor.trim();
  return TOKEN_CONVITE.test(token) ? token : null;
}

export function opcoesCookieConvite() {
  return {
    httpOnly: true,
    sameSite: "lax" as const,
    path: "/",
    secure: process.env.NODE_ENV === "production",
    maxAge: 600,
  };
}
