const ASSUNTO_MAXIMO = 120;
const DESCRICAO_MAXIMA = 5000;

export function validarTicket(assunto: string, descricao: string): string | null {
  const assuntoLimpo = assunto.trim();
  const descricaoLimpa = descricao.trim();

  if (!assuntoLimpo) return "Informe o assunto.";
  if (!descricaoLimpa) return "Informe a descrição.";
  if (assuntoLimpo.length > ASSUNTO_MAXIMO) {
    return `O assunto deve ter no máximo ${ASSUNTO_MAXIMO} caracteres.`;
  }
  if (descricaoLimpa.length > DESCRICAO_MAXIMA) {
    return `A descrição deve ter no máximo ${DESCRICAO_MAXIMA} caracteres.`;
  }
  return null;
}

export type StatusTicket =
  | "ABERTO"
  | "EM_ANDAMENTO"
  | "RESOLVIDO"
  | "FECHADO";

export type OrigemMensagem = "USER" | "ADMIN";

export interface TicketResumo {
  id: number;
  usuarioId?: number | null;
  assunto: string;
  descricao: string;
  status: StatusTicket;
  criadoEm: string;
  atualizadoEm: string;
}

export interface TicketMensagem {
  id: number;
  ticketId: number;
  usuarioId?: number | null;
  conteudo: string;
  origem: OrigemMensagem;
  criadoEm: string;
}

export interface TicketDetalhe extends TicketResumo {
  mensagens: TicketMensagem[];
}

export interface PaginaTickets {
  tickets: TicketResumo[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
}

export const ROTULOS_STATUS: Record<StatusTicket, string> = {
  ABERTO: "Aberto",
  EM_ANDAMENTO: "Em andamento",
  RESOLVIDO: "Resolvido",
  FECHADO: "Fechado",
};

export const CLASSES_STATUS: Record<StatusTicket, string> = {
  ABERTO: "border-secondary/50 bg-secondary/20 text-foreground",
  EM_ANDAMENTO: "border-primary/50 bg-primary/20 text-foreground",
  RESOLVIDO: "border-positive/40 bg-positive/10 text-positive",
  FECHADO: "border-border bg-muted text-muted-foreground",
};

export function rotuloStatus(status: string): string {
  return ROTULOS_STATUS[status as StatusTicket] ?? "Status desconhecido";
}

export function classesStatus(status: string): string {
  return CLASSES_STATUS[status as StatusTicket] ?? CLASSES_STATUS.FECHADO;
}

export const LIMITE_ASSUNTO = ASSUNTO_MAXIMO;
export const LIMITE_DESCRICAO = DESCRICAO_MAXIMA;
