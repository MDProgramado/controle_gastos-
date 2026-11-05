import { test, expect } from "@playwright/test";

/**
 * E2E de autenticação (01-03 Task 3 — checklist de 10 passos).
 * Pré-requisitos: API + web rodando; seed admin@controle.com / admin123.
 */

const EMAIL = "admin@controle.com";
const SENHA = "admin123";

test.describe("Login", () => {
  test("carrega a tela com copy correta (D-05)", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { level: 1 })).toHaveText(
      "Controle Financeiro",
    );
    await expect(
      page.getByText("Entre com seu e-mail e senha."),
    ).toBeVisible();
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
    // permanece no login
    expect(page.url()).toContain("/login");
  });

  test("login certo → shell autenticado", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("E-mail").fill(EMAIL);
    await page.getByLabel("Senha").fill(SENHA);
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(
      page.getByRole("heading", { level: 1, name: "Você está autenticado." }),
    ).toBeVisible();
  });

  test("cookie de sessão é httpOnly (inacessível via JS)", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByLabel("E-mail").fill(EMAIL);
    await page.getByLabel("Senha").fill(SENHA);
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(page.getByRole("heading", { name: "Você está autenticado." })).toBeVisible();

    // document.cookie NÃO pode expor o JWT
    const docCookie = await page.evaluate(() => document.cookie);
    expect(docCookie).not.toContain("sess");
    expect(docCookie).not.toContain("eyJ"); // prefixo JWT

    // mas o cookie existe e é httpOnly (enviado ao servidor)
    const cookies = await contextCookies(page);
    const sess = cookies.find((c) => c.name === "sess");
    expect(sess, "cookie 'sess' deve existir").toBeDefined();
    expect(sess!.httpOnly, "cookie deve ser httpOnly").toBe(true);
  });
});

test.describe("Sessão", () => {
  test("sem cookie → redireciona para /login", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveURL(/\/login/);
  });

  test("com cookie → acessa o shell", async ({ page }) => {
    await login(page);
    await page.goto("/");
    await expect(
      page.getByRole("heading", { name: "Você está autenticado." }),
    ).toBeVisible();
  });

  test("logout → volta ao login e bloqueia o shell", async ({ page }) => {
    await login(page);
    await page.getByRole("button", { name: "Sair" }).click();
    await expect(page).toHaveURL(/\/login/);

    // shell agora está bloqueado
    await page.goto("/");
    await expect(page).toHaveURL(/\/login/);
  });

  test("usuário logado não fica em /login", async ({ page }) => {
    await login(page);
    await page.goto("/login");
    // proxy.ts redireciona autenticado para /
    await expect(page).toHaveURL(/\/$/);
  });
});

/** Helper: login completo e espera o shell. */
async function login(page: import("@playwright/test").Page) {
  await page.goto("/login");
  await page.getByLabel("E-mail").fill(EMAIL);
  await page.getByLabel("Senha").fill(SENHA);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(
    page.getByRole("heading", { name: "Você está autenticado." }),
  ).toBeVisible();
}

/** Helper: lê cookies do contexto. */
async function contextCookies(page: import("@playwright/test").Page) {
  return page.context().cookies();
}
