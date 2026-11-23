import { LoginForm } from "@/components/LoginForm";
import { SkipLink } from "@/components/SkipLink";
import { Card } from "@/components/ui/card";
import { googleOAuthPronto } from "@/lib/google-oauth-server";

// Cache Components (Next 16): o banner depende de searchParams (request time) —
// a rota bloqueia no servidor em vez de entrar no shell estático (docs oficiais).
export const instant = false;

/**
 * Tela de login (D-05 / UI-SPEC): ponto focal = card de 400px sob o h1 da
 * marca; único acento teal na tela = botão "Entrar". Único `h1` da página.
 */
export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ expirada?: string; erro?: string }>;
}) {
  const { expirada, erro } = await searchParams;
  const sessaoExpirada = expirada === "1";
  const googleOauthEnabled = googleOAuthPronto();

  return (
    <>
      <SkipLink />
      <main
        id="main-content"
        className="flex min-h-dvh w-full flex-col items-center justify-center px-4 py-12"
      >
      <div className="w-full max-w-100">
        <header className="mb-6 text-center">
          <h1 className="text-[28px] leading-[1.2] font-semibold text-foreground">
            Pluton - Seu assistente pessoal
          </h1>
          <p className="mt-1 text-base text-muted-foreground">Entre com seu e-mail e senha.</p>
        </header>

        <Card className="p-6 ring-border md:p-8 md:shadow-md">
          {sessaoExpirada && (
            <div
              role="status"
              className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm leading-snug text-destructive"
            >
              Sua sessão expirou. Entre novamente.
            </div>
          )}
          {erro === "google" && (
            <div
              role="alert"
              className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm leading-snug text-destructive"
            >
              Não foi possível entrar com Google. Tente novamente.
            </div>
          )}
          <LoginForm
            sessaoExpirada={sessaoExpirada}
            googleOauthEnabled={googleOauthEnabled}
          />
        </Card>
      </div>
      </main>
    </>
  );
}
