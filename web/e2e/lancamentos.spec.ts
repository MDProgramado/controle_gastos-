import { test, expect } from "@playwright/test";
import { abrirPagina, login, MES_ATUAL } from "./helpers";

test.describe("Lançamentos — listagem e filtros", () => {
  test("página carrega sem erro 500 e exibe botões de ação", async ({ page }) => {
    await login(page);
    await abrirPagina(page, `/lancamentos?mes=${MES_ATUAL}`);

    await expect(
      page.getByRole("heading", { name: /^Lançamentos de / }),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: /\+ Novo lançamento/ })).toBeVisible();
    await expect(page.getByRole("button", { name: "Lançar em lote" })).toBeVisible();
  });

  test("tabela exibe lançamentos ou estado vazio", async ({ page }) => {
    await login(page);
    await abrirPagina(page, `/lancamentos?mes=${MES_ATUAL}`);

    await expect(
      page.getByRole("heading", { name: /^Lançamentos de / }),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: "Lançar em lote" })).toBeVisible();
  });

  test("mes selecionado é mantido na URL", async ({ page }) => {
    await login(page);
    await abrirPagina(page, `/lancamentos?mes=${MES_ATUAL}`);
    expect(page.url()).toContain(`mes=${MES_ATUAL}`);
  });
});
