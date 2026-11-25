import {
  Database,
  DollarSign,
  FileUp,
  Headset,
  Home,
  Layout,
  MessageSquareWarning,
  Receipt,
  Users,
  type LucideIcon,
} from "lucide-react";

export type RoleUsuario = "ADMIN" | "USER";

export interface ItemNavegacao {
  href: string;
  label: string;
  icon: LucideIcon;
}

const ITENS_COMUNS: ItemNavegacao[] = [
  { href: "/", label: "Dashboard", icon: Home },
  { href: "/lancamentos", label: "Lançamentos", icon: DollarSign },
  { href: "/categorias", label: "Categorias", icon: Layout },
  { href: "/importacoes", label: "Importar PDF", icon: FileUp },
  { href: "/contas-a-pagar", label: "Contas a Pagar", icon: Receipt },
];

export function itensDeNavegacao(role: RoleUsuario | null): ItemNavegacao[] {
  if (role === "ADMIN") {
    return [
      ...ITENS_COMUNS,
      { href: "/admin/usuarios", label: "Usuários", icon: Users },
      { href: "/admin/suporte", label: "Helpdesk", icon: Headset },
      { href: "/admin/rag", label: "Base RAG", icon: Database },
    ];
  }

  if (role === "USER") {
    return [
      ...ITENS_COMUNS,
      { href: "/suporte", label: "Meus problemas", icon: MessageSquareWarning },
    ];
  }

  return [...ITENS_COMUNS];
}
