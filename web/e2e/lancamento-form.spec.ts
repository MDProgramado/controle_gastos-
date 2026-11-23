import { test, expect, type Page } from "@playwright/test";
import { abrirPagina, login, MES_ATUAL } from "./helpers";

function dialogsVisiveis(page: Page) {
  return page.locator('[role="dialog"]').filter({ visible: true });
}

async function clicarPorCoordenadas(page: Page, seletor: string) {
  const el = page.locator(seletor).first();
  await el.scrollIntoViewIfNeeded();
  const box = await el.boundingBox();
  if (box) {
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  } else {
    await el.click();
  }
}

async function clicarNovoLancamento(page: Page) {
  const btn = page.getByRole("button", { name: /\+ Novo lançamento/ });
  await expect(btn).toBeVisible();
  await btn.scrollIntoViewIfNeeded();
  await btn.click();
}

async function abrirLancamentos(page: Page) {
  await login(page);
  await abrirPagina(page, `/lancamentos?mes=${MES_ATUAL}`);
  await expect(page.getByRole("button", { name: "Lançar em lote" })).toBeVisible();
}

test.describe("Novo lançamento — Dialog", () => {
  test("clicar '+ Novo lançamento' abre Dialog visível", async ({ page }) => {
    await abrirLancamentos(page);
    await clicarNovoLancamento(page);

    const dialog = dialogsVisiveis(page).first();
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText("Novo Lançamento");
  });

  test("Dialog fecha ao clicar Cancelar", async ({ page }) => {
    await abrirLancamentos(page);
    await clicarNovoLancamento(page);

    const dialog = dialogsVisiveis(page).first();
    await expect(dialog).toBeVisible();

    await dialog.getByRole("button", { name: "Cancelar" }).click();
    await expect(dialogsVisiveis(page)).toHaveCount(0);
  });

  test("validação: salvar sem categoria exibe erro", async ({ page }) => {
    await abrirLancamentos(page);
    await clicarNovoLancamento(page);

    const dialog = dialogsVisiveis(page).first();
    await expect(dialog).toBeVisible();

    await dialog.getByRole("button", { name: /Criar lançamento/ }).click();
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText("Selecione uma categoria.")).toBeVisible();
  });

  test("validação: valor deve ser maior que zero", async ({ page }) => {
    await abrirLancamentos(page);
    await clicarNovoLancamento(page);

    const dialog = dialogsVisiveis(page).first();
    await expect(dialog).toBeVisible();

    await dialog.getByRole("button", { name: /Criar lançamento/ }).click();
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText("Informe o valor.")).toBeVisible();
  });
});

test.describe("Lançar em lote — Sheet", () => {
  test("clicar 'Lançar em lote' abre Sheet", async ({ page }) => {
    await abrirLancamentos(page);
    await clicarPorCoordenadas(page, 'button:has-text("Lançar em lote")');

    const sheet = dialogsVisiveis(page).last();
    await expect(sheet).toBeVisible();
    await expect(sheet).toContainText("Lançar em lote");
  });

  test("adicionar linha no lote funciona sem erro", async ({ page }) => {
    await abrirLancamentos(page);
    await clicarPorCoordenadas(page, 'button:has-text("Lançar em lote")');

    const sheet = dialogsVisiveis(page).last();
    await expect(sheet).toBeVisible();

    const btnAdicionar = sheet.getByRole("button", { name: "Adicionar linha" });
    await expect(btnAdicionar).toBeVisible();
    await expect(btnAdicionar).not.toBeDisabled();
  });

  test("Sheet fecha ao clicar Fechar", async ({ page }) => {
    await abrirLancamentos(page);
    await clicarPorCoordenadas(page, 'button:has-text("Lançar em lote")');

    const sheet = dialogsVisiveis(page).last();
    await expect(sheet).toBeVisible();

    await clicarPorCoordenadas(page, 'button[aria-label="Fechar"]');
    await expect(dialogsVisiveis(page)).toHaveCount(0);
  });
});
