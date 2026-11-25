export type PapelAssistente = "ADMIN" | "USER" | null;

export type AcaoAssistente = {
  id: string;
  label: string;
  href?: string;
  abreChat?: boolean;
};

const acoesComuns: AcaoAssistente[] = [
  { id: "chat", label: "Falar com a IA", abreChat: true },
  { id: "lancamentos", label: "Lançamentos", href: "/lancamentos" },
];

export function acoesDoAssistente(role: PapelAssistente): AcaoAssistente[] {
  switch (role) {
    case "ADMIN":
      return [
        ...acoesComuns,
        { id: "usuarios", label: "Usuários", href: "/admin/usuarios" },
        { id: "helpdesk", label: "Helpdesk", href: "/admin/suporte" },
      ];
    case "USER":
      return [
        ...acoesComuns,
        { id: "suporte", label: "Meus problemas", href: "/suporte" },
      ];
    default:
      return [...acoesComuns];
  }
}
