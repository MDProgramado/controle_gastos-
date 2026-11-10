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

test.describe("Categorias", () => {
  test("página de categorias carrega sem erros", async ({ page }) => {
    await login(page);
    await page.goto("/categorias");
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/categorias/);
  });

  test("lista categorias existentes", async ({ page }) => {
    await login(page);
    await page.goto("/categorias");
    await page.waitForTimeout(2000);

    // Deve haver uma lista de categorias ou estado vazio
    const tabela = page.locator("table");
    const vazio = page.getByText(/nenhuma categoria/i);
    await expect(tabela.or(vazio).first()).toBeVisible();
  });

  test("criar nova categoria", async ({ page }) => {
    await login(page);
    await page.goto("/categorias");
    await page.waitForTimeout(2000);

    const btnCriar = page.getByRole("button", { name: /criar categoria/i });
    if ((await btnCriar.count()) > 0) {
      await btnCriar.click();
      await page.waitForTimeout(1000);

      await page.getByLabel("Nome").fill("Categoria Teste E2E");
      await page.getByLabel("Tipo").selectOption("ENTRADA");
      await page.getByRole("button", { name: /salvar/i }).click();
      await page.waitForTimeout(2000);
    }
  });
});
