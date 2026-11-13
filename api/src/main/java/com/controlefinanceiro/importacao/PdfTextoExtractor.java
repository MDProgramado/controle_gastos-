package com.controlefinanceiro.importacao;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Extração de texto de PDF no backend (sem OCR — custo zero).
 *
 * <p>Usa Apache PDFBox {@link PDFTextStripper}. O PDF bruto nunca é persistido (LGPD); apenas o
 * texto extraído é devolvido para sanitização e envio à IA.
 */
@Component
public class PdfTextoExtractor {

  private static final Logger LOG = LoggerFactory.getLogger(PdfTextoExtractor.class);
  private static final int LIMITE_MINIMO_CARACTERES = 5;

  private final int maxPaginas;
  private final int maxCaracteres;

  public PdfTextoExtractor(
      @Value("${importacao.max-paginas:200}") int maxPaginas,
      @Value("${importacao.max-caracteres:200000}") int maxCaracteres) {
    this.maxPaginas = maxPaginas;
    this.maxCaracteres = maxCaracteres;
  }

  /**
   * Extrai o texto bruto do PDF.
   *
   * @param pdfBytes bytes do arquivo PDF
   * @return texto extraído (nunca nulo)
   * @throws PdfDemasiadoGrandeException se exceder páginas ou caracteres configurados
   * @throws SemTextoExtraivelException se não houver texto extraível
   * @throws IOException se o arquivo não for um PDF válido
   */
  public String extrair(byte[] pdfBytes) throws IOException {
    try (PDDocument documento = Loader.loadPDF(pdfBytes)) {
      int paginas = documento.getNumberOfPages();
      if (paginas > maxPaginas) {
        throw new PdfDemasiadoGrandeException(
            "PDF excede o limite de %d páginas (informado: %d)".formatted(maxPaginas, paginas));
      }

      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      String texto = stripper.getText(documento);

      if (texto == null || texto.trim().length() < LIMITE_MINIMO_CARACTERES) {
        throw new SemTextoExtraivelException(
            "PDF sem texto extraível (possivelmente digitalizado ou imagem).");
      }

      if (texto.length() > maxCaracteres) {
        texto = texto.substring(0, maxCaracteres);
        LOG.warn("Texto do PDF truncado a {} caracteres", maxCaracteres);
      }

      return texto;
    }
  }
}
