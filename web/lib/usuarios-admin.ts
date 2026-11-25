export type RoleUsuario = "ADMIN" | "USER";

export interface UsuarioAdmin {
  id: number;
  nome: string;
  email: string;
  role: RoleUsuario;
  ativo: boolean;
}

export type UsuarioParaEdicao = UsuarioAdmin & {
  usuarios?: UsuarioAdmin[];
};

export function podeEditarUsuario(
  roleAutor: RoleUsuario | null,
  idAutor: number | null,
  usuario: UsuarioParaEdicao,
): boolean {
  if (roleAutor !== "ADMIN" || idAutor == null) return false;
  if (usuario.id === idAutor) return false;

  const usuarios = usuario.usuarios ?? [usuario];
  const adminsAtivos = usuarios.filter(
    (item) => item.role === "ADMIN" && item.ativo,
  ).length;

  if (usuario.role === "ADMIN" && usuario.ativo && adminsAtivos <= 1) {
    return false;
  }

  return true;
}
