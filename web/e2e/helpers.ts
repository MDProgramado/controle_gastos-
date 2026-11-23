import { expect, type Page } from "@playwright/test";

function variavelObrigatoria(nome: string): string {
  const valor = process.env[nome];
  if (!valor) {
    throw new Error(`Defina ${nome} antes de executar os testes E2E.`);
  }
  return valor;
}

export const EMAIL = variavelObrigatoria("E2E_EMAIL");
export const SENHA = variavelObrigatoria("E2E_SENHA");
export const MES_ATUAL =
  process.env.E2E_MES ?? new Date().toISOString().slice(0, 7);
export const HOJE = new Date().toISOString().slice(0, 10);

export async function login(page: Page): Promise<void> {
  await page.goto("/login");
  await page.getByLabel("E-mail").fill(EMAIL);
  await page.getByLabel("Senha").fill(SENHA);
  await page.getByRole("button", { name: "Entrar" }).click();
  await page.waitForURL((url) => url.pathname !== "/login", { timeout: 15000 });
}

export async function abrirPagina(page: Page, url: string): Promise<void> {
  await page.goto(url);
  await expect(page.locator("main#main-content")).toBeVisible();
}
