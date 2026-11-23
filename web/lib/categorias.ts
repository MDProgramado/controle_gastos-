export function podeAlternarStatus(categoria: { ativo: boolean }): boolean {
  return categoria.ativo;
}

export function mensagemStatusCategoria(categoria: { ativo: boolean }): string {
  return categoria.ativo
    ? "Desativar categoria"
    : "Reativação indisponível: a API não oferece este recurso.";
}
