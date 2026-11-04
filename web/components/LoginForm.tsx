"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/Spinner";

type FieldErrors = { email?: string; senha?: string };
type Falha = "credencial" | "rede" | null;

// Copy EXATA do contrato UI-SPEC (D-05: mensagens genéricas anti-enumeração)
const MSG_CREDENCIAIS = "E-mail ou senha inválidos. Verifique os dados e tente novamente.";
const MSG_REDE = "Não foi possível conectar. Tente novamente em instantes.";
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Formulário de login controlado (sem RHF/zod — 2 campos, D-05/UI-SPEC).
 * Estados: default · invalid · submitting · erro-credencial · erro-rede.
 */
export function LoginForm({ sessaoExpirada = false }: { sessaoExpirada?: boolean }) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erros, setErros] = useState<FieldErrors>({});
  const [falha, setFalha] = useState<Falha>(null);
  const [submitting, setSubmitting] = useState(false);
  const emailRef = useRef<HTMLInputElement>(null);
  const senhaRef = useRef<HTMLInputElement>(null);

  // Sessão expirada (`/login?expirada=1`): Server Component não pode mutar cookie,
  // então a validação/limpeza acontece aqui via rota BFF — `GET /api/me` devolve
  // 401 e a própria rota apaga o cookie "sess". Se a sessão ainda for válida
  // (URL ?expirada=1 acessada "à mão"), volta para o shell sem deslogar.
  useEffect(() => {
    if (!sessaoExpirada) return;
    let cancelado = false;
    (async () => {
      try {
        const res = await fetch("/api/me", { cache: "no-store" });
        if (!cancelado && res.ok) router.replace("/");
      } catch {
        // API inacessível: mantém o banner; o cookie é revalidado no próximo acesso
      }
    })();
    return () => {
      cancelado = true;
    };
  }, [sessaoExpirada, router]);

  function validar(): FieldErrors {
    const novos: FieldErrors = {};
    if (!email.trim()) novos.email = "Informe seu e-mail.";
    else if (!EMAIL_RE.test(email.trim())) novos.email = "Informe um e-mail válido.";
    if (!senha) novos.senha = "Informe sua senha.";
    return novos;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    setFalha(null);
    const novos = validar();
    setErros(novos);
    if (novos.email) {
      emailRef.current?.focus();
      return;
    }
    if (novos.senha) {
      senhaRef.current?.focus();
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), senha }),
      });

      if (res.ok) {
        router.push("/"); // BFF setou o cookie httpOnly → shell protegido
        return;
      }

      if (res.status === 401 || res.status === 400) {
        // 401 = credencial inválida; 400 = API rejeitou os dados (D-05: mesma
        // mensagem genérica, nunca revelar qual campo falhou)
        setSenha("");
        setFalha("credencial");
        senhaRef.current?.focus();
      } else {
        setFalha("rede");
      }
    } catch {
      setFalha("rede");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-2">
      <div className="flex flex-col gap-1">
        <Label htmlFor="email" className="font-semibold">
          E-mail
        </Label>
        <Input
          ref={emailRef}
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          placeholder="seu@email.com"
          value={email}
          onChange={(event) => {
            setEmail(event.target.value);
            if (erros.email) setErros((atual) => ({ ...atual, email: undefined }));
          }}
          aria-invalid={erros.email ? true : undefined}
          aria-describedby={erros.email ? "email-erro" : undefined}
          className="h-11 md:text-base"
        />
        {erros.email && (
          <p id="email-erro" className="text-sm leading-snug text-destructive">
            {erros.email}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-1">
        <Label htmlFor="senha" className="font-semibold">
          Senha
        </Label>
        <Input
          ref={senhaRef}
          id="senha"
          name="senha"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(event) => {
            setSenha(event.target.value);
            if (erros.senha) setErros((atual) => ({ ...atual, senha: undefined }));
          }}
          aria-invalid={erros.senha ? true : undefined}
          aria-describedby={erros.senha ? "senha-erro" : undefined}
          className="h-11 md:text-base"
        />
        {erros.senha && (
          <p id="senha-erro" className="text-sm leading-snug text-destructive">
            {erros.senha}
          </p>
        )}
      </div>

      <Button
        type="submit"
        disabled={submitting}
        aria-busy={submitting}
        className="mt-2 h-11 w-full font-semibold"
      >
        {submitting ? (
          <>
            <Spinner />
            Entrando…
          </>
        ) : (
          "Entrar"
        )}
      </Button>

      {falha && (
        <Alert variant="destructive" role="alert" className="mt-1">
          <AlertDescription>{falha === "credencial" ? MSG_CREDENCIAIS : MSG_REDE}</AlertDescription>
        </Alert>
      )}
    </form>
  );
}
