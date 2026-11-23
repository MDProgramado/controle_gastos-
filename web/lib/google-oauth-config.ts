export function googleClientId(): string {
  return (
    (process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || "").trim() ||
    (process.env.GOOGLE_CLIENT_ID || "").trim()
  );
}

export function googleOAuthHabilitado(clientSecret: string): boolean {
  return (
    process.env.NEXT_PUBLIC_GOOGLE_OAUTH_ENABLED === "1" &&
    googleClientId().length > 0 &&
    clientSecret.trim().length > 0
  );
}
