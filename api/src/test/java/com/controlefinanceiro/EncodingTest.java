package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guarda contra corrupção de encoding (acentos virando "?", mojibake ou BOM).
 *
 * <p>Percorre os fontes da API e, se presentes, os do web. Falha listando arquivo:linha.
 */
class EncodingTest {

  private static final Set<String> EXTENSOES =
      Set.of(".java", ".ts", ".tsx", ".js", ".mjs", ".css", ".properties", ".yml", ".yaml", ".sql");

  /** Migrações já aplicadas com BOM: alterar os bytes poderia mudar o checksum do Flyway. */
  private static final Set<String> BOM_PERMITIDO = Set.of("V16__ai_conhecimento.sql");

  /** Mojibake típico: U+FFFD, "Ã" + Latin-1 (ex.: "Ã£"), e os glifos vistos em incidentes. */
  private static final Pattern MOJIBAKE =
      Pattern.compile("[\\uFFFD\\u01DC\\u01ED]|Ã[\\u00A0-\\u00FF]");

  /** Acento trocado por "?" no meio de palavra: "n?o", "Par?metro", "Sa?da". */
  private static final Pattern INTERROGACAO_NA_PALAVRA = Pattern.compile("\\p{L}\\?\\p{L}");

  /** Linhas com URL/query string legítima. */
  private static final Pattern QUERY_STRING = Pattern.compile("https?://|\\?[A-Za-z_]+=");

  @Test
  void fontesEstaoEmUtf8SemCorrupcao() throws IOException {
    List<Path> raizes = new ArrayList<>(List.of(Path.of("src/main"), Path.of("src/test")));
    for (String dir : List.of("app", "lib", "components", "contexts", "e2e")) {
      raizes.add(Path.of("../web", dir));
    }

    List<String> problemas = new ArrayList<>();
    // Arquivos da raiz do web (proxy.ts, next.config.ts...), sem descer em node_modules/.next.
    Path raizWeb = Path.of("../web");
    if (Files.isDirectory(raizWeb)) {
      try (Stream<Path> arquivos = Files.list(raizWeb)) {
        for (Path arquivo :
            arquivos
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".mjs"))
                .toList()) {
          verificar(arquivo, problemas);
        }
      }
    }
    for (Path raiz : raizes) {
      if (!Files.isDirectory(raiz)) {
        continue;
      }
      try (Stream<Path> arquivos = Files.walk(raiz)) {
        for (Path arquivo :
            arquivos.filter(Files::isRegularFile).filter(this::monitorado).toList()) {
          verificar(arquivo, problemas);
        }
      }
    }

    assertThat(problemas).as("Problemas de encoding encontrados").isEmpty();
  }

  private boolean monitorado(Path p) {
    String nome = p.getFileName().toString();
    return !p.toString().contains("node_modules") && EXTENSOES.stream().anyMatch(nome::endsWith);
  }

  private void verificar(Path arquivo, List<String> problemas) throws IOException {
    byte[] bytes = Files.readAllBytes(arquivo);
    String nome = arquivo.getFileName().toString();
    String texto;
    try {
      texto =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(bytes))
              .toString();
    } catch (CharacterCodingException e) {
      problemas.add(arquivo + ": não é UTF-8 válido");
      return;
    }

    if (texto.startsWith("﻿")) {
      if (!BOM_PERMITIDO.contains(nome)) {
        problemas.add(arquivo + ": começa com BOM");
      }
      texto = texto.substring(1);
    }

    if (nome.equals("EncodingTest.java")) {
      return; // contém os próprios padrões de detecção
    }

    String[] linhas = texto.split("\n", -1);
    for (int i = 0; i < linhas.length; i++) {
      String linha = linhas[i];
      if (MOJIBAKE.matcher(linha).find()) {
        problemas.add(arquivo + ":" + (i + 1) + ": mojibake -> " + linha.strip());
      } else if (INTERROGACAO_NA_PALAVRA.matcher(linha).find()
          && !QUERY_STRING.matcher(linha).find()) {
        problemas.add(arquivo + ":" + (i + 1) + ": acento trocado por '?' -> " + linha.strip());
      }
    }
  }
}
