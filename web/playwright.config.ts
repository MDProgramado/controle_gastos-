import { defineConfig } from "@playwright/test";

/**
 * Playwright E2E (fase 1 — Task 3 do plano 01-03): valida o fluxo de login
 * ponta a ponta (BFF → API → cookie httpOnly → shell → logout).
 *
 * Pré-requisitos: API (:8080) e web (:3000) rodando localmente.
 * Run: `npx playwright test` a partir de `web/`.
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  retries: 0,
  workers: 1, // sequencial — mesmo servidor, evitar corrida de sessão
  reporter: [["list"]],
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
});
