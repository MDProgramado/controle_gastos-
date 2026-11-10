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

test.describe("Autenticação", () => {
  test("carrega a tela de login com copy correta", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { level: 1 })).toHaveText("Controle Financeiro");
    await expect(page.getByText("Entre com seu e-mail e senha.")).toBeVisible();
    await expect(page.getByLabel("E-mail")).toBeVisible();
    await expect(page.getByLabel("Senha")).toBeVisible();
    await expect(page.getByRole("button", { name: "Entrar" })).toBeVisible();
  });

  test("validação client-side: campos vazios", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(page.getByText("Informe seu e-mail.")).toBeVisible();
  });

  test("validação client-side: e-mail inválido", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("E-mail").fill("nao-e-email");
    await page.getByLabel("Senha").fill("qualquer");
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(page.getByText("Informe um e-mail válido.")).toBeVisible();
  });

  test("credencial errada → mensagem genérica (anti-enumeração)", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByLabel("E-mail").fill(EMAIL);
    await page.getByLabel("Senha").fill("senha-errada");
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(
      page.getByRole("alert").getByText(
        "E-mail ou senha inválidos. Verifique os dados e tente novamente.",
      ),
    ).toBeVisible();
    expect(page.url()).toContain("/login");
  });

  test("login certo → shell autenticado", async ({ page }) => {
    await login(page);
    await expect(page.locator("main").getByRole("heading", { name: "Visão geral" })).toBeVisible();
  });

  test("cookie de sessão é httpOnly (inacessível via JS)", async ({ page, context }) => {
    await login(page);
    const docCookie = await page.evaluate(() => document.cookie);
    expect(docCookie).not.toContain("sess");
    expect(docCookie).not.toContain("eyJ");
    const cookies = await context.cookies();
    const sess = cookies.find((c) => c.name === "sess");
    expect(sess).toBeDefined();
    expect(sess!.httpOnly).toBe(true);
  });

  test("sem cookie → redireciona para /login", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveURL(/\/login/);
  });

  test("usuário logado não fica em /login", async ({ page }) => {
    await login(page);
    await page.goto("/login");
    await expect(page).toHaveURL(/\/$/);
  });

  test("logout → volta ao login e bloqueia o shell", async ({ page }) => {
    await login(page);
    await page.getByRole("button", { name: "Sair" }).click();
    await page.waitForURL((url) => url.pathname === "/login", { timeout: 15000 });
    await page.goto("/");
    await expect(page).toHaveURL(/\/login/);
  });
});
