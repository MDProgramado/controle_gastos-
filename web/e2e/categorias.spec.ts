import { test, expect } from "@playwright/test";
import { abrirPagina, login } from "./helpers";

test.describe("Categorias", () => {
  test("página de categorias carrega sem erros", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/categorias");
    await expect(page).toHaveURL(/categorias/);
  });

  test("lista categorias existentes", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/categorias");
    await expect(
      page.getByRole("heading", { name: "Categorias", level: 1 }),
    ).toBeVisible();
  });

  test("criar nova categoria", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/categorias");

    const btnCriar = page.getByRole("button", { name: /Nova categoria/i });
    await expect(btnCriar).toBeVisible();
    await btnCriar.click();

    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    await dialog.getByLabel("Nome").fill("Categoria Teste E2E");
    await dialog.getByLabel("Tipo").click();
    await page.getByRole("option", { name: /entrada/i }).click();
    await dialog.getByRole("button", { name: /salvar/i }).click();
    await expect(page.getByText("Categoria Teste E2E")).toBeVisible();
  });
});
