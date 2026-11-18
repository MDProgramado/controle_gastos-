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

/** PDF mínimo (a validação client-side só olha tipo/extensão e tamanho). */
const PDF_PEQUENO = Buffer.from("%PDF-1.4\n%%EOF\n", "utf-8");
/** 1 byte acima do limite de 4 MB por arquivo (D-29). */
const PDF_GRANDE = Buffer.alloc(4 * 1024 * 1024 + 1, 0x25); // % repetido
const PDF_NAO_PDF = Buffer.from("isto não é um pdf", "utf-8");

test.describe("Importação de PDFs", () => {
  test("página carrega com título, lead e seções do contrato", async ({ page }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    await expect(page).toHaveURL(/importacoes/);
    // h1 visível no md+ (sr-only no mobile)
    await expect(page.getByRole("heading", { name: "Importar PDFs", level: 1 })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Enviar PDFs", level: 2 })).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Importações pendentes", level: 2 }),
    ).toBeVisible();
    // Cabeçalho e área de upload
    await expect(
      page.getByRole("heading", { name: /Importar/i }),
    ).toBeVisible();
    await expect(page.locator('input[type="file"]')).toBeAttached();
  });

  test("item 'Importar PDF' na sidebar navega para a página", async ({ page }) => {
    await login(page);
    await page.goto("/");
    await page.waitForTimeout(2000);
    const item = page.getByRole("link", { name: "Importar PDF" }).first();
    await item.click();
    await page.waitForURL((url) => url.pathname.includes("/importacoes"), { timeout: 15000 });
  });

  test("dropzone: CTA desabilitado sem arquivos e helper visível", async ({ page }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    await expect(page.getByText("Arraste os PDFs aqui")).toBeVisible();
    await expect(page.getByText("Até 5 PDFs por importação, 4 MB por arquivo.")).toBeVisible();

    const cta = page.getByRole("button", { name: "Analisar PDFs" });
    await expect(cta).toBeDisabled();
    await expect(
      page.getByText("Selecione ao menos um PDF para analisar."),
    ).toBeVisible();
    await expect(
      page.getByText("Nenhum lançamento é gravado antes da sua confirmação."),
    ).toBeVisible();
  });

  test("arquivo que não é PDF é rejeitado com a copy do contrato", async ({ page }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    await page.locator('input[type="file"]').setInputFiles({
      name: "documento.txt",
      mimeType: "text/plain",
      buffer: PDF_NAO_PDF,
    });

    await expect(
      page.getByText(
        "Este arquivo não é um PDF. Envie faturas de cartão ou extratos bancários em PDF.",
      ),
    ).toBeVisible();
    // Nenhum arquivo entra na lista: CTA continua desabilitado.
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeDisabled();
  });

  test("PDF válido entra na lista com estado 'Aguardando' e pode ser removido", async ({
    page,
  }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    await page.locator('input[type="file"]').setInputFiles({
      name: "fatura-cartao-jan.pdf",
      mimeType: "application/pdf",
      buffer: PDF_PEQUENO,
    });

    const linha = page.getByText("fatura-cartao-jan.pdf");
    await expect(linha).toBeVisible();
    await expect(page.getByText("Aguardando").first()).toBeVisible();
    // Heurística de origem pelo nome do arquivo
    await expect(page.getByText("Fatura de cartão").first()).toBeVisible();

    // CTA habilitado com 1 arquivo
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeEnabled();

    // Remover pela ação X (aria-label do contrato)
    await page.getByRole("button", { name: "Remover fatura-cartao-jan.pdf" }).click();
    await expect(page.getByText("fatura-cartao-jan.pdf")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeDisabled();
  });

  test("PDF acima de 4 MB é rejeitado com a copy do contrato", async ({ page }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    await page.locator('input[type="file"]').setInputFiles({
      name: "extrato-grande.pdf",
      mimeType: "application/pdf",
      buffer: PDF_GRANDE,
    });

    await expect(
      page.getByText(
        /O arquivo extrato-grande\.pdf tem .+ e o limite é 4 MB por arquivo\. Envie um PDF menor\./,
      ),
    ).toBeVisible();
  });

  test("mais de 5 PDFs excede o limite do lote", async ({ page }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    const arquivos = Array.from({ length: 6 }, (_, i) => ({
      name: `fatura-${i + 1}.pdf`,
      mimeType: "application/pdf",
      buffer: PDF_PEQUENO,
    }));
    await page.locator('input[type="file"]').setInputFiles(arquivos);

    // Prova real: o limite de 5 é respeitado e o erro é exibido
    await expect(
      page.getByText("Você pode enviar até 5 PDFs por vez. Remova um arquivo para continuar."),
    ).toBeVisible();
  });

  test("'Limpar lista' esvazia o envio", async ({ page }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    await page.locator('input[type="file"]').setInputFiles({
      name: "extrato-jan.pdf",
      mimeType: "application/pdf",
      buffer: PDF_PEQUENO,
    });
    await expect(page.getByText("extrato-jan.pdf")).toBeVisible();

    await page.getByRole("button", { name: "Limpar lista" }).click();
    await expect(page.getByText("extrato-jan.pdf")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeDisabled();
  });

  test("histórico: estado vazio ou lista de importações visíveis", async ({ page }) => {
    await login(page);
    await page.goto("/importacoes");
    await page.waitForTimeout(2000);

    const vazio = page.getByText("Nenhuma importação por aqui ainda.");
    const colunaArquivo = page.getByRole("columnheader", { name: "Arquivo" });
    await expect(vazio.or(colunaArquivo).first()).toBeVisible();
  });
});
