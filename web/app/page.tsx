import { redirect } from "next/navigation";
import { getMe } from "@/lib/api";
import { LogoutButton } from "@/components/LogoutButton";

// Cache Components (Next 16): a sessão é lida por request (cookies) — a rota
// é dinâmica por natureza e bloqueia no servidor (docs: "Allow blocking route").
export const instant = false;

/**
 * Shell autenticado (Server Component): valida a sessão via `getMe()`.
 * Sem sessão → redirect ao login com o flag de expiração; o cookie obsoleto é
 * limpo pela rota `GET /api/me` acionada na tela de login (RSC não pode mutar
 * cookie — regra do Next 16).
 */
export default async function Home() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }

  return (
    <main className="flex min-h-dvh w-full flex-col items-center justify-center px-4 py-12">
      <div className="w-full max-w-[400px] rounded-xl border border-border bg-card p-6 text-center shadow-none md:p-8 md:shadow-md">
        <h1 className="text-[20px] leading-[1.2] font-semibold text-foreground">
          Você está autenticado.
        </h1>
        <p className="mt-2 text-base text-muted-foreground">
          O dashboard chega na Fase 3. Use o botão Sair para encerrar a sessão.
        </p>
        <div className="mt-6">
          <LogoutButton />
        </div>
      </div>
    </main>
  );
}
