"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

/**
 * "Sair": POST `/api/auth/logout` (limpa o cookie httpOnly) → volta ao /login,
 * sem confirmação (UI-SPEC — logout não é ação destrutiva de dados).
 */
export function LogoutButton() {
  const router = useRouter();

  async function sair() {
    try {
      const res = await fetch("/api/auth/logout", { method: "POST" });
      if (!res.ok) throw new Error(`logout falhou: ${res.status}`);
      router.replace("/login");
    } catch {
      // resposta inesperada: recarrega o server state (o gate por cookie decide)
      router.refresh();
    }
  }

  return (
    <Button
      type="button"
      variant="outline"
      className="h-11 w-full md:w-auto md:px-8"
      onClick={() => void sair()}
    >
      Sair
    </Button>
  );
}
