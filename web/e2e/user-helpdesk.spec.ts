import { expect, test } from "@playwright/test";
import { itensDeNavegacao } from "../lib/navegacao";
import {
  classesStatus,
  rotuloStatus,
  validarTicket,
} from "../lib/helpdesk";
import { podeEditarUsuario } from "../lib/usuarios-admin";

test("rejeita assunto vazio", () => {
  expect(validarTicket("", "Descrição válida")).toBe("Informe o assunto.");
});

test("rejeita assunto contendo apenas espaços", () => {
  expect(validarTicket("   ", "Descrição válida")).toBe(
    "Informe o assunto.",
  );
});

test("rejeita descrição vazia", () => {
  expect(validarTicket("Assunto válido", "")).toBe("Informe a descrição.");
});

test("rejeita descrição contendo apenas espaços", () => {
  expect(validarTicket("Assunto válido", "   ")).toBe(
    "Informe a descrição.",
  );
});

test("rejeita assunto com 121 caracteres", () => {
  expect(validarTicket("a".repeat(121), "Descrição válida")).toBe(
    "O assunto deve ter no máximo 120 caracteres.",
  );
});

test("rejeita descrição com 5001 caracteres", () => {
  expect(validarTicket("Assunto válido", "d".repeat(5001))).toBe(
    "A descrição deve ter no máximo 5000 caracteres.",
  );
});

test("aceita assunto e descrição nos limites", () => {
  expect(validarTicket("a".repeat(120), "d".repeat(5000))).toBeNull();
});

test("ADMIN vê itens comuns e itens administrativos", () => {
  const itens = itensDeNavegacao("ADMIN");
  const labels = itens.map((item) => item.label);

  const esperados = [
    "Dashboard",
    "Lançamentos",
    "Categorias",
    "Importar PDF",
    "Contas a Pagar",
    "Usuários",
    "Helpdesk",
  ];
  expect(labels).toEqual(expect.arrayContaining(esperados));
  expect(labels.filter((label) => esperados.includes(label))).toEqual(esperados);
  expect(itens.find((item) => item.label === "Usuários")?.href).toBe("/admin/usuarios");
  expect(itens.find((item) => item.label === "Helpdesk")?.href).toBe("/admin/suporte");
  expect(labels).not.toContain("Meus problemas");
});

test("USER vê itens comuns e Meus problemas", () => {
  const itens = itensDeNavegacao("USER");
  const labels = itens.map((item) => item.label);

  expect(labels).toEqual([
    "Dashboard",
    "Lançamentos",
    "Categorias",
    "Importar PDF",
    "Contas a Pagar",
    "Meus problemas",
  ]);
  expect(itens.find((item) => item.label === "Meus problemas")?.href).toBe("/suporte");
  expect(labels).not.toContain("Usuários");
  expect(labels).not.toContain("Helpdesk");
});

test("role null ou desconhecido vê apenas itens comuns", () => {
  const labelsNull = itensDeNavegacao(null).map((item) => item.label);
  const labelsDesconhecido = itensDeNavegacao("DESCONHECIDO" as unknown as "USER").map(
    (item) => item.label,
  );

  for (const labels of [labelsNull, labelsDesconhecido]) {
    expect(labels).toEqual([
      "Dashboard",
      "Lançamentos",
      "Categorias",
      "Importar PDF",
      "Contas a Pagar",
    ]);
    expect(labels).not.toContain("Usuários");
    expect(labels).not.toContain("Helpdesk");
    expect(labels).not.toContain("Meus problemas");
  }
});

test("rotuloStatus traduz os status conhecidos", () => {
  const casos = [
    ["ABERTO", "Aberto"],
    ["EM_ANDAMENTO", "Em andamento"],
    ["RESOLVIDO", "Resolvido"],
    ["FECHADO", "Fechado"],
  ];

  for (const [status, esperado] of casos) {
    expect(rotuloStatus(status)).toBe(esperado);
  }
});

test("rotuloStatus usa rótulo genérico para status desconhecido", () => {
  expect(rotuloStatus("STATUS_INESPERADO")).toBe("Status desconhecido");
  expect(rotuloStatus("")).toBe("Status desconhecido");
});

test("classesStatus usa apenas classes dos tokens do tema", () => {
  const casos = [
    ["ABERTO", "border-secondary/50 bg-secondary/20 text-foreground"],
    ["EM_ANDAMENTO", "border-primary/50 bg-primary/20 text-foreground"],
    ["RESOLVIDO", "border-positive/40 bg-positive/10 text-positive"],
    ["FECHADO", "border-border bg-muted text-muted-foreground"],
  ];

  for (const [status, esperado] of casos) {
    expect(classesStatus(status)).toBe(esperado);
  }
});

test("classesStatus usa classes neutras para status desconhecido", () => {
  expect(classesStatus("STATUS_INESPERADO")).toBe(
    "border-border bg-muted text-muted-foreground",
  );
  expect(classesStatus("")).toBe("border-border bg-muted text-muted-foreground");
});

test("rotuloStatus trata chaves herdadas como status desconhecido", () => {
  expect(rotuloStatus("toString")).toBe(rotuloStatus("STATUS_INESPERADO"));
  expect(rotuloStatus("toString")).toBe("Status desconhecido");
});

test("classesStatus trata chaves herdadas como status desconhecido", () => {
  expect(classesStatus("constructor")).toBe(
    classesStatus("STATUS_INESPERADO"),
  );
  expect(classesStatus("constructor")).toBe(
    "border-border bg-muted text-muted-foreground",
  );
});

test("USER não pode editar outro usuário", () => {
  const alvo = {
    id: 2,
    nome: "Ana",
    email: "ana@ogum.example",
    role: "ADMIN" as const,
    ativo: true,
  };

  expect(
    podeEditarUsuario("USER", 1, { ...alvo, usuarios: [alvo] }),
  ).toBe(false);
});

test("ADMIN não pode editar a própria conta", () => {
  const autor = {
    id: 7,
    nome: "Bruno",
    email: "bruno@ogum.example",
    role: "ADMIN" as const,
    ativo: true,
  };
  const outroAdmin = {
    id: 8,
    nome: "Carla",
    email: "carla@ogum.example",
    role: "ADMIN" as const,
    ativo: true,
  };

  expect(
    podeEditarUsuario("ADMIN", 7, {
      ...autor,
      usuarios: [autor, outroAdmin],
    }),
  ).toBe(false);
});

test("ADMIN não pode editar o último ADMIN ativo", () => {
  const ultimoAdmin = {
    id: 9,
    nome: "Diego",
    email: "diego@ogum.example",
    role: "ADMIN" as const,
    ativo: true,
  };
  const inativo = {
    id: 10,
    nome: "Elisa",
    email: "elisa@ogum.example",
    role: "ADMIN" as const,
    ativo: false,
  };

  expect(
    podeEditarUsuario("ADMIN", 7, {
      ...ultimoAdmin,
      usuarios: [ultimoAdmin, inativo],
    }),
  ).toBe(false);
});

test("ADMIN pode editar outro usuário com mais de um ADMIN ativo", () => {
  const autor = {
    id: 7,
    nome: "Bruno",
    email: "bruno@ogum.example",
    role: "ADMIN" as const,
    ativo: true,
  };
  const outroAdmin = {
    id: 8,
    nome: "Carla",
    email: "carla@ogum.example",
    role: "ADMIN" as const,
    ativo: true,
  };
  const usuario = {
    id: 11,
    nome: "Fábio",
    email: "fabio@ogum.example",
    role: "USER" as const,
    ativo: true,
  };

  expect(
    podeEditarUsuario("ADMIN", 7, {
      ...usuario,
      usuarios: [autor, outroAdmin, usuario],
    }),
  ).toBe(true);
});
