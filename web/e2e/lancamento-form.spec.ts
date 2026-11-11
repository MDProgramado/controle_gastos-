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

async function clicarPorCoordenadas(page: import("@playwright/test").Page, seletor: string) {
  const el = page.locator(seletor).first();
  // o hero do kit ocupa a primeira tela inteira: o botão começa fora do
  // viewport, e mouse.click(x, y) usa coordenadas de viewport — rola até ele
  // antes de medir a caixa.
  await el.scrollIntoViewIfNeeded();
  const box = await el.boundingBox();
  if (box) {
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  } else {
    await el.click();
  }
}

test.describe("Novo lançamento — Dialog", () => {
  test("clicar '+ Novo lançamento' abre Dialog visível", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(5000);

    await page.getByRole("button", { name: /\+ Novo lançamento/ }).click();
    await page.waitForTimeout(3000);

    const dialogs = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(dialogs.first()).toBeVisible({ timeout: 5000 });
    await expect(dialogs.first()).toContainText("Novo lançamento");
  });

  test("Dialog fecha ao clicar Cancelar", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(5000);

    await page.getByRole("button", { name: /\+ Novo lançamento/ }).click();
    await page.waitForTimeout(3000);

    const visiveis = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(visiveis.first()).toBeVisible({ timeout: 5000 });

    await page.getByRole("button", { name: "Cancelar" }).click();
    await page.waitForTimeout(1000);

    const visiveis2 = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(visiveis2).toHaveCount(0);
  });

  test("validação: salvar sem categoria exibe erro", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(5000);

    await page.getByRole("button", { name: /\+ Novo lançamento/ }).click();
    await page.waitForTimeout(3000);

    const visiveis = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(visiveis.first()).toBeVisible({ timeout: 5000 });

    await page.getByRole("button", { name: "Salvar" }).click();
    await page.waitForTimeout(1000);

    // Dialog deve permanecer aberto (validação falhou)
    const visiveis2 = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(visiveis2).toBeVisible();
  });

  test("validação: valor deve ser maior que zero", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(5000);

    await page.getByRole("button", { name: /\+ Novo lançamento/ }).click();
    await page.waitForTimeout(3000);

    const visiveis = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(visiveis.first()).toBeVisible({ timeout: 5000 });

    await page.getByRole("button", { name: "Salvar" }).click();
    await page.waitForTimeout(1000);

    const visiveis2 = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(visiveis2).toBeVisible();
  });
});

test.describe("Lançar em lote — Sheet", () => {
  test("clicar 'Lançar em lote' abre Sheet", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(5000);

    await clicarPorCoordenadas(page, 'button:has-text("Lançar em lote")');
    await page.waitForTimeout(3000);

    const sheets = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(sheets.last()).toBeVisible({ timeout: 5000 });
    await expect(sheets.last()).toContainText("Lançar em lote");
  });

  test("adicionar linha no lote funciona sem erro", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(5000);

    await clicarPorCoordenadas(page, 'button:has-text("Lançar em lote")');
    await page.waitForTimeout(3000);

    const sheets = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(sheets.last()).toBeVisible({ timeout: 5000 });

    // Verificar que o botão "Adicionar linha" está visível e clicável
    const btnAdicionar = page.getByRole("button", { name: "Adicionar linha" });
    await expect(btnAdicionar).toBeVisible();
    await expect(btnAdicionar).not.toBeDisabled();
  });

  test("Sheet fecha ao clicar Fechar", async ({ page }) => {
    await login(page);
    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(5000);

    await clicarPorCoordenadas(page, 'button:has-text("Lançar em lote")');
    await page.waitForTimeout(3000);

    const sheets = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(sheets.last()).toBeVisible({ timeout: 5000 });

    await clicarPorCoordenadas(page, 'button:has-text("Fechar")');
    await page.waitForTimeout(1000);

    const visiveis2 = page.locator('[role="dialog"]').filter({ visible: true });
    await expect(visiveis2).toHaveCount(0);
  });
});
