package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** Item 1.3: os padrões do application.yml usam só modelos pagos (com política de dados). */
class ModelosPadraoTest {

  private static final Pattern PADRAO_ENV = Pattern.compile("^\\$\\{[A-Z0-9_]+:(.*)}$");

  @Test
  @DisplayName("nenhum modelo :free ou openrouter/free nos padrões de modelo do chat e da extração")
  @SuppressWarnings("unchecked")
  void padroesSemModelosGratuitos() throws IOException {
    Map<String, Object> raiz;
    try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
      raiz = new Yaml().load(in);
    }
    Map<String, Object> openrouter =
        (Map<String, Object>) ((Map<String, Object>) raiz.get("ai")).get("openrouter");

    for (String chave : new String[] {"modelo", "modelos", "modelos-extracao", "modelos-reserva"}) {
      String padrao = padrao(String.valueOf(openrouter.get(chave)));
      assertThat(padrao).as("padrão de ai.openrouter.%s", chave).doesNotContain(":free");
      assertThat(padrao).as("padrão de ai.openrouter.%s", chave).doesNotContain("openrouter/free");
    }
    assertThat(padrao(String.valueOf(openrouter.get("modelos"))))
        .startsWith("google/gemma-4-31b-it");
    assertThat(padrao(String.valueOf(openrouter.get("data-collection")))).isEqualTo("deny");
    assertThat(padrao(String.valueOf(openrouter.get("zdr")))).isEqualTo("true");
  }

  /** Extrai o valor padrão de {@code ${ENV:padrao}} (ou o próprio valor, se literal). */
  private static String padrao(String valor) {
    Matcher m = PADRAO_ENV.matcher(valor.trim());
    return m.matches() ? m.group(1) : valor;
  }
}
