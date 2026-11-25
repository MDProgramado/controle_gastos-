"use server";

import type { SessionUser } from "@/lib/api";
import { encaminharParaApi } from "@/lib/bff";
import {
  podeEditarUsuario,
  type RoleUsuario,
  type UsuarioAdmin,
} from "@/lib/usuarios-admin";

export type ResultadoAcaoUsuario =
  | { ok: true; mensagem: string; token?: string }
  | { ok: false; mensagem: string };

const ERRO_GENERICO = "Não foi possível concluir a ação. Tente novamente.";

function falha(mensagem: string): ResultadoAcaoUsuario {
  return { ok: false, mensagem };
}

function roleValida(role: string): role is RoleUsuario {
  return role === "ADMIN" || role === "USER";
}

function idValido(id: number): boolean {
  return Number.isSafeInteger(id) && id > 0;
}

async function mensagemErro(resposta: Response): Promise<string> {
  const corpo = (await resposta.json().catch(() => null)) as {
    detail?: string;
    erro?: string;
  } | null;
  return corpo?.detail ?? corpo?.erro ?? ERRO_GENERICO;
}

async function autorizarAdmin(): Promise<
  { email: string } | { erro: string }
> {
  const resposta = await encaminharParaApi("/api/auth/me", {
    limparCookieEm401: true,
  });
  if (resposta.status === 401) {
    return { erro: "Sessão expirada. Entre novamente." };
  }
  if (!resposta.ok) return { erro: await mensagemErro(resposta) };

  const eu = (await resposta.json().catch(() => null)) as SessionUser | null;
  if (!eu || eu.role !== "ADMIN" || !eu.email) {
    return { erro: "Acesso restrito a administradores." };
  }
  return { email: eu.email };
}

async function carregarContexto(id: number): Promise<
  | { idAutor: number; usuarios: UsuarioAdmin[] }
  | { erro: string }
> {
  const autor = await autorizarAdmin();
  if ("erro" in autor) return autor;
  if (!idValido(id)) return { erro: "Usuário inválido." };

  const resposta = await encaminharParaApi("/api/usuarios", {
    limparCookieEm401: true,
  });
  if (!resposta.ok) return { erro: await mensagemErro(resposta) };

  const usuarios = (await resposta.json().catch(() => null)) as
    | UsuarioAdmin[]
    | null;
  if (!Array.isArray(usuarios)) return { erro: ERRO_GENERICO };

  const emailAutor = autor.email.trim().toLowerCase();
  const idAutor =
    usuarios.find(
      (usuario) => usuario.email?.trim().toLowerCase() === emailAutor,
    )?.id ?? null;
  if (idAutor == null) return { erro: "Autor da ação não encontrado." };

  return { idAutor, usuarios };
}

export async function actionAtualizarUsuario(
  id: number,
  nome: string,
  role: string,
): Promise<ResultadoAcaoUsuario> {
  const contexto = await carregarContexto(id);
  if ("erro" in contexto) return falha(contexto.erro);

  const nomeLimpo = nome.trim();
  if (!nomeLimpo || nomeLimpo.length > 120) {
    return falha("Informe um nome de 1 a 120 caracteres.");
  }
  if (!roleValida(role)) return falha("Perfil inválido.");

  const alvo = contexto.usuarios.find((usuario) => usuario.id === id);
  if (!alvo) return falha("Usuário não encontrado.");
  if (
    !podeEditarUsuario("ADMIN", contexto.idAutor, {
      ...alvo,
      usuarios: contexto.usuarios,
    })
  ) {
    return falha("Este usuário não pode ser alterado.");
  }

  const resposta = await encaminharParaApi(
    `/api/usuarios/${encodeURIComponent(id)}`,
    {
      method: "PATCH",
      body: JSON.stringify({ nome: nomeLimpo, role }),
      limparCookieEm401: true,
    },
  );
  if (!resposta.ok) return falha(await mensagemErro(resposta));

  return { ok: true, mensagem: "Dados do usuário atualizados." };
}

export async function actionDesativarUsuario(
  id: number,
): Promise<ResultadoAcaoUsuario> {
  const contexto = await carregarContexto(id);
  if ("erro" in contexto) return falha(contexto.erro);

  const alvo = contexto.usuarios.find((usuario) => usuario.id === id);
  if (!alvo) return falha("Usuário não encontrado.");
  if (!alvo.ativo) return falha("Este usuário já está inativo.");
  if (
    !podeEditarUsuario("ADMIN", contexto.idAutor, {
      ...alvo,
      usuarios: contexto.usuarios,
    })
  ) {
    return falha("Este usuário não pode ser desativado.");
  }

  const resposta = await encaminharParaApi(
    `/api/usuarios/${encodeURIComponent(id)}/desativar`,
    { method: "POST", limparCookieEm401: true },
  );
  if (!resposta.ok) return falha(await mensagemErro(resposta));

  return { ok: true, mensagem: "Acesso desativado." };
}

export async function actionExcluirUsuario(
  id: number,
): Promise<ResultadoAcaoUsuario> {
  const contexto = await carregarContexto(id);
  if ("erro" in contexto) return falha(contexto.erro);

  const alvo = contexto.usuarios.find((usuario) => usuario.id === id);
  if (!alvo) return falha("Usuário não encontrado.");
  if (!alvo.ativo) return falha("Este usuário já está inativo.");
  if (
    !podeEditarUsuario("ADMIN", contexto.idAutor, {
      ...alvo,
      usuarios: contexto.usuarios,
    })
  ) {
    return falha("Este usuário não pode ser excluído.");
  }

  const resposta = await encaminharParaApi(
    `/api/usuarios/${encodeURIComponent(id)}`,
    { method: "DELETE", limparCookieEm401: true },
  );
  if (!resposta.ok) return falha(await mensagemErro(resposta));

  return {
    ok: true,
    mensagem: "Conta excluída. O acesso foi desativado e os dados permanecem na empresa.",
  };
}

export async function actionConvidarUsuario(
  email: string,
  role: string,
): Promise<ResultadoAcaoUsuario> {
  const autor = await autorizarAdmin();
  if ("erro" in autor) return falha(autor.erro);

  const emailLimpo = email.trim().toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailLimpo) || emailLimpo.length > 320) {
    return falha("Informe um e-mail válido.");
  }
  if (!roleValida(role)) return falha("Perfil inválido.");

  const resposta = await encaminharParaApi("/api/usuarios/convites", {
    method: "POST",
    body: JSON.stringify({ email: emailLimpo, role }),
    limparCookieEm401: true,
  });
  if (!resposta.ok) return falha(await mensagemErro(resposta));

  const convite = (await resposta.json().catch(() => null)) as {
    token?: string;
  } | null;
  if (!convite?.token) return falha("O convite foi criado, mas o link não pôde ser exibido.");

  return {
    ok: true,
    mensagem: "Convite criado. Envie o link ao convidado.",
    token: convite.token,
  };
}
