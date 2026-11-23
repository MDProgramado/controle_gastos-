import { test, expect } from "@playwright/test";
import { abrirPagina, login, MES_ATUAL } from "./helpers";

const VIEWPORTS = [
  { name: "360px (mobile)", width: 360, height: 800 },
  { name: "768px (tablet)", width: 768, height: 1024 },
  { name: "1440px (desktop)", width: 1440, height: 900 },
];

for (const vp of VIEWPORTS) {
  test.describe(`Layout — ${vp.name}`, () => {
    test("sem sobreposição de conteúdo e sidebar não esconde conteúdo", async ({
      page,
    }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await login(page);

      await abrirPagina(page, "/");
      await expect(
        page.locator("main").getByRole("heading", { name: "Visão geral" }),
      ).toBeVisible();
      expect(
        await page.getByRole("button", { name: "Sair" }).count(),
      ).toBeLessThanOrEqual(1);

      await abrirPagina(page, `/lancamentos?mes=${MES_ATUAL}`);
      await expect(page.getByRole("button", { name: /\+ Novo lançamento/ })).toBeVisible();
      await expect(page.getByRole("button", { name: "Lançar em lote" })).toBeVisible();
      expect(
        await page.getByRole("button", { name: "Sair" }).count(),
      ).toBeLessThanOrEqual(1);

      await abrirPagina(page, "/categorias");
      await expect(
        page.getByRole("heading", { name: "Categorias", level: 1 }),
      ).toBeVisible();
      expect(
        await page.getByRole("button", { name: "Sair" }).count(),
      ).toBeLessThanOrEqual(1);
    });

    test("sidebar mobile abre como Sheet (não ocupa largura do conteúdo)", async ({
      page,
    }) => {
      if (vp.name.includes("360px")) {
        await page.setViewportSize({ width: vp.width, height: vp.height });
        await login(page);
        await abrirPagina(page, "/");

        const btnMenu = page.getByRole("button", { name: "Abrir menu" });
        await expect(btnMenu).toBeVisible();
        await btnMenu.click();
        await expect(page.getByRole("dialog", { name: "PLUTON" })).toBeVisible();
      }
    });
  });
}

test.describe("Console sem erros em todas as rotas", () => {
  test("nenhum erro de console ao navegar pelas 3 páginas", async ({ page }) => {
    const errors: Array<string> = [];
    const pageErrors: Array<string> = [];

    page.on("console", (msg) => {
      if (msg.type() === "error") {
        errors.push(msg.text());
      }
    });
    page.on("pageerror", (err) => {
      pageErrors.push(err.message);
    });

    await login(page);
    await abrirPagina(page, "/");
    await abrirPagina(page, `/lancamentos?mes=${MES_ATUAL}`);
    await abrirPagina(page, "/categorias");

    await page.goto("/login");
    await expect(page).toHaveURL(/\/login/);

    expect(errors).toEqual([]);
    expect(pageErrors).toEqual([]);
  });
});
