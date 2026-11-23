import { test, expect } from "@playwright/test";
import { abrirPagina, login } from "./helpers";

const PDF_PEQUENO = Buffer.from("%PDF-1.4\n%%EOF\n", "utf-8");
const PDF_GRANDE = Buffer.alloc(4 * 1024 * 1024 + 1, 0x25);
const PDF_NAO_PDF = Buffer.from("isto não é um pdf", "utf-8");

test.describe("Importação de PDFs", () => {
  test("página carrega com título, lead e seções do contrato", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/importacoes");

    await expect(page).toHaveURL(/importacoes/);
    await expect(page.getByRole("heading", { name: "Importar PDFs", level: 1 })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Enviar PDFs", level: 2 })).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Importações pendentes", level: 2 }),
    ).toBeVisible();
    await expect(page.locator('input[type="file"]')).toBeAttached();
  });

  test("item 'Importar PDF' na sidebar navega para a página", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/");
    await page.getByRole("link", { name: "Importar PDF" }).first().click();
    await expect(page).toHaveURL(/\/importacoes/);
  });

  test("dropzone: CTA desabilitado sem arquivos e helper visível", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/importacoes");

    await expect(page.getByText("Arraste os PDFs aqui")).toBeVisible();
    await expect(page.getByText("Até 5 PDFs por importação, 4 MB por arquivo.")).toBeVisible();
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeDisabled();
    await expect(page.getByText("Selecione ao menos um PDF para analisar.")).toBeVisible();
    await expect(page.getByText("Nenhum lançamento é gravado antes da sua confirmação.")).toBeVisible();
  });

  test("arquivo que não é PDF é rejeitado com a copy do contrato", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/importacoes");

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
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeDisabled();
  });

  test("PDF válido entra na lista com estado 'Aguardando' e pode ser removido", async ({
    page,
  }) => {
    await login(page);
    await abrirPagina(page, "/importacoes");

    await page.locator('input[type="file"]').setInputFiles({
      name: "fatura-cartao-jan.pdf",
      mimeType: "application/pdf",
      buffer: PDF_PEQUENO,
    });

    await expect(page.getByText("fatura-cartao-jan.pdf")).toBeVisible();
    await expect(page.getByText("Aguardando").first()).toBeVisible();
    await expect(page.getByText("Fatura de cartão").first()).toBeVisible();
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeEnabled();

    await page.getByRole("button", { name: "Remover fatura-cartao-jan.pdf" }).click();
    await expect(page.getByText("fatura-cartao-jan.pdf")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Analisar PDFs" })).toBeDisabled();
  });

  test("PDF acima de 4 MB é rejeitado com a copy do contrato", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/importacoes");

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
    await abrirPagina(page, "/importacoes");

    await page.locator('input[type="file"]').setInputFiles(
      Array.from({ length: 6 }, (_, i) => ({
        name: `fatura-${i + 1}.pdf`,
        mimeType: "application/pdf",
        buffer: PDF_PEQUENO,
      })),
    );

    await expect(
      page.getByText("Você pode enviar até 5 PDFs por vez. Remova um arquivo para continuar."),
    ).toBeVisible();
  });

  test("'Limpar lista' esvazia o envio", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/importacoes");

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

  test("histórico: seção de pendentes visível", async ({ page }) => {
    await login(page);
    await abrirPagina(page, "/importacoes");
    await expect(
      page.getByRole("heading", { name: "Importações pendentes", level: 2 }),
    ).toBeVisible();
  });
});
