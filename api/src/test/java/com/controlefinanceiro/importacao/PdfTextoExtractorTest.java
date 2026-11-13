package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de unidade de {@link PdfTextoExtractor} (sem banco, sem rede).
 *
 * <p>Usa um PDF mínimo hardcoded (como bytes) para não depender de fontes específicas do Apache
 * PDFBox.
 */
class PdfTextoExtractorTest {

  private PdfTextoExtractor extractor = new PdfTextoExtractor(200, 200_000);

  /** PDF mínimo válido (1 página) com o texto "Fatura do cartao". */
  private static final byte[] PDF_COM_TEXTO =
      ("%PDF-1.4\n"
              + "1 0 obj\n"
              + "<< /Type /Catalog /Pages 2 0 R >>\n"
              + "endobj\n"
              + "2 0 obj\n"
              + "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n"
              + "endobj\n"
              + "3 0 obj\n"
              + "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\n"
              + "endobj\n"
              + "4 0 obj\n"
              + "<< /Length 35 >>\n"
              + "stream\n"
              + "BT /F1 12 Tf 50 700 Td (Fatura do cartao) Tj ET\n"
              + "endstream\n"
              + "endobj\n"
              + "xref\n"
              + "0 5\n"
              + "0000000000 65535 f \n"
              + "0000000010 00000 n \n"
              + "0000000049 00000 n \n"
              + "0000000099 00000 n \n"
              + "0000000164 00000 n \n"
              + "trailer\n"
              + "<< /Size 5 /Root 1 0 R >>\n"
              + "startxref\n"
              + "230\n"
              + "%%EOF\n")
          .getBytes(StandardCharsets.ISO_8859_1);

  /** PDF mínimo válido (1 página em branco, sem texto). */
  private static final byte[] PDF_SEM_TEXTO =
      ("%PDF-1.4\n"
              + "1 0 obj\n"
              + "<< /Type /Catalog /Pages 2 0 R >>\n"
              + "endobj\n"
              + "2 0 obj\n"
              + "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n"
              + "endobj\n"
              + "3 0 obj\n"
              + "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\n"
              + "endobj\n"
              + "4 0 obj\n"
              + "<< /Length 0 >>\n"
              + "stream\n"
              + "endstream\n"
              + "endobj\n"
              + "xref\n"
              + "0 5\n"
              + "0000000000 65535 f \n"
              + "0000000010 00000 n \n"
              + "0000000049 00000 n \n"
              + "0000000099 00000 n \n"
              + "0000000164 00000 n \n"
              + "trailer\n"
              + "<< /Size 5 /Root 1 0 R >>\n"
              + "startxref\n"
              + "230\n"
              + "%%EOF\n")
          .getBytes(StandardCharsets.ISO_8859_1);

  /** PDF com 2 páginas (para testar limite de 1 página). */
  private static final byte[] PDF_DUPAGINAS =
      ("%PDF-1.4\n"
              + "1 0 obj\n"
              + "<< /Type /Catalog /Pages 2 0 R >>\n"
              + "endobj\n"
              + "2 0 obj\n"
              + "<< /Type /Pages /Kids [3 0 R 6 0 R] /Count 2 >>\n"
              + "endobj\n"
              + "3 0 obj\n"
              + "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\n"
              + "endobj\n"
              + "4 0 obj\n"
              + "<< /Length 0 >>\n"
              + "stream\n"
              + "endstream\n"
              + "endobj\n"
              + "6 0 obj\n"
              + "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 7 0 R >>\n"
              + "endobj\n"
              + "7 0 obj\n"
              + "<< /Length 0 >>\n"
              + "stream\n"
              + "endstream\n"
              + "endobj\n"
              + "xref\n"
              + "0 8\n"
              + "0000000000 65535 f \n"
              + "0000000010 00000 n \n"
              + "0000000049 00000 n \n"
              + "0000000099 00000 n \n"
              + "0000000164 00000 n \n"
              + "0000000230 00000 n \n"
              + "0000000298 00000 n \n"
              + "0000000367 00000 n \n"
              + "trailer\n"
              + "<< /Size 8 /Root 1 0 R >>\n"
              + "startxref\n"
              + "437\n"
              + "%%EOF\n")
          .getBytes(StandardCharsets.ISO_8859_1);

  @Test
  @DisplayName("extra texto de PDF com conteúdo")
  void extraTextoDePdfComConteudo() throws Exception {
    String texto = extractor.extrair(PDF_COM_TEXTO);

    assertThat(texto).contains("Fatura do cartao");
  }

  @Test
  @DisplayName("PDF sem texto extraível lança SemTextoExtraivelException")
  void pdfSemTextoLancaExcecaoDedicada() throws Exception {
    assertThatThrownBy(() -> extractor.extrair(PDF_SEM_TEXTO))
        .isInstanceOf(SemTextoExtraivelException.class)
        .hasMessageContaining("sem texto extraível");
  }

  @Test
  @DisplayName("PDF excedendo limite de páginas lança PdfDemasiadoGrandeException")
  void pdfExcedendoLimiteDePaginasLancaExcecao() throws Exception {
    PdfTextoExtractor extratorRestrito = new PdfTextoExtractor(1, 200_000);

    assertThatThrownBy(() -> extratorRestrito.extrair(PDF_DUPAGINAS))
        .isInstanceOf(PdfDemasiadoGrandeException.class);
  }
}
