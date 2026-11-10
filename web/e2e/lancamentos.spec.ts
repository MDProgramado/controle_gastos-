import { test, expect } from "@playwright/test";

const EMAIL = "admin@controle.com";
const SENHA = "admin123";

async function login(page: import("@playwright/test").Page) {
  await page.goto("/login");
  await page.getByLabel("E-mail").fill(EMAIL);
  await page.getByLabel("Senha").fill(SENHA);
  await page.getByRole("button", { name: "Entrar" }).click();
  await page.waitForURL((url) => url.pathname !== "/login", { timeout: 15000 });
}

test.describe("Lançamentos — listagem e filtros", () => {
  test("página carrega sem erro 500 e exibe botões de ação", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(2000);

    // Página deve carregar sem erro de servidor
    await expect(page).toHaveURL(/lancamentos/);

    // Botões principais visíveis
    await expect(page.getByRole("button", { name: /\+ Novo lançamento/ })).toBeVisible();
    await expect(page.getByRole("button", { name: "Lançar em lote" })).toBeVisible();
  });

  test("tabela exibe lançamentos ou estado vazio", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(2000);

    const tabela = page.locator("table");
    const estadoVazio = page.getByText("Nenhum lançamento neste período");
    await expect(
      tabela.or(estadoVazio).first(),
    ).toBeVisible();
  });

  test("mes selecionado é mantido na URL", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(2000);
    expect(page.url()).toContain("mes=2026-11");
  });
});
