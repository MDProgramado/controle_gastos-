import { expect, test } from "@playwright/test";
import { itensDeNavegacao } from "../lib/navegacao";

test("ADMIN vê itens comuns e itens administrativos", () => {
  const itens = itensDeNavegacao("ADMIN");
  const labels = itens.map((item) => item.label);

  expect(labels).toEqual([
    "Dashboard",
    "Lançamentos",
    "Categorias",
    "Importar PDF",
    "Contas a Pagar",
    "Usuários",
    "Helpdesk",
  ]);
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
