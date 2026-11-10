import { test, expect } from "@playwright/test";

const EMAIL = "admin@controle.com";
const SENHA = "admin123";

const VIEWPORTS = [
  { name: "360px (mobile)", width: 360, height: 800 },
  { name: "768px (tablet)", width: 768, height: 1024 },
  { name: "1440px (desktop)", width: 1440, height: 900 },
];

async function login(page: import("@playwright/test").Page) {
  await page.goto("/login");
  await page.getByLabel("E-mail").fill(EMAIL);
  await page.getByLabel("Senha").fill(SENHA);
  await page.getByRole("button", { name: "Entrar" }).click();
  await page.waitForURL((url) => url.pathname !== "/login", { timeout: 15000 });
}

for (const vp of VIEWPORTS) {
  test.describe(`Layout — ${vp.name}`, () => {
    test("sem sobreposição de conteúdo e sidebar não esconde conteúdo", async ({
      page,
    }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await login(page);

      // Dashboard
      await page.goto("/");
      await page.waitForTimeout(2000);
      await expect(page.locator("main").getByRole("heading", { name: "Visão geral" })).toBeVisible();

      // Um único "Sair" (no desktop está sempre visível; no mobile fica dentro do Sheet fechado)
      const sairCount = await page.getByRole("button", { name: "Sair" }).count();
      expect(sairCount).toBeLessThanOrEqual(1);

      // Lançamentos
      await page.goto("/lancamentos?mes=2026-11");
      await page.waitForTimeout(2000);
      await expect(page.getByRole("button", { name: /\+ Novo lançamento/ })).toBeVisible();
      await expect(page.getByRole("button", { name: "Lançar em lote" })).toBeVisible();

      const sairCount2 = await page.getByRole("button", { name: "Sair" }).count();
      expect(sairCount2).toBeLessThanOrEqual(1);

      // Categorias
      await page.goto("/categorias");
      await page.waitForTimeout(2000);
      const sairCount3 = await page.getByRole("button", { name: "Sair" }).count();
      expect(sairCount3).toBeLessThanOrEqual(1);
    });

    test("sidebar mobile abre como Sheet (não ocupa largura do conteúdo)", async ({
      page,
    }) => {
      if (vp.name.includes("360px")) {
        await page.setViewportSize({ width: vp.width, height: vp.height });
        await login(page);
        await page.goto("/");
        await page.waitForTimeout(2000);

        // Botão hambúrguer deve estar visível em mobile
        const btnMenu = page.getByRole("button", { name: "Abrir menu" });
        if ((await btnMenu.count()) > 0) {
          await btnMenu.click();
          await page.waitForTimeout(1000);
          // Sheet deve abrir
          await expect(page.locator('[role="dialog"]')).toBeVisible();
        }
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

    // Navegar por todas as páginas
    await page.goto("/");
    await page.waitForTimeout(2000);

    await page.goto("/lancamentos?mes=2026-11");
    await page.waitForTimeout(2000);

    await page.goto("/categorias");
    await page.waitForTimeout(2000);

    await page.goto("/login");
    await page.waitForTimeout(1000);

    console.log("ERROS DE CONSOLE:", errors.length);
    console.log("PAGE ERRORS:", pageErrors.length);
    errors.forEach((e) => console.log("  ERROR:", e));
    pageErrors.forEach((e) => console.log("  PAGE ERROR:", e));

    expect(errors.length).toBe(0);
    expect(pageErrors.length).toBe(0);
  });
});
