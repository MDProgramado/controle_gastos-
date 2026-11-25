package com.controlefinanceiro.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FerramentaChamadaParserTest {

  @Test
  @DisplayName("mantém pipes dentro do JSON de argumentos")
  void mantemPipesDentroDoJson() {
    String linha =
        "FERRAMENTA|consultar_base_conhecimento|{\"query\":\"rotativo|cartão\",\"topK\":3}";

    FerramentaChamada chamada = FerramentaChamadaParser.parse(linha);

    assertThat(chamada).isNotNull();
    assertThat(chamada.nome()).isEqualTo("consultar_base_conhecimento");
    assertThat(chamada.argsJson()).isEqualTo("{\"query\":\"rotativo|cartão\",\"topK\":3}");
  }

  @Test
  @DisplayName("aceita args com vírgulas e espaços sem quebrar")
  void aceitaArgsComVirgulasEspacos() {
    String linha =
        "FERRAMENTA|simular_quitacao_ou_juros|{\"valor\": 1000, \"taxa_mensal\": 2.5,"
            + " \"parcelas\": 12}";

    FerramentaChamada chamada = FerramentaChamadaParser.parse(linha);

    assertThat(chamada).isNotNull();
    assertThat(chamada.nome()).isEqualTo("simular_quitacao_ou_juros");
    assertThat(chamada.argsJson()).contains("\"taxa_mensal\": 2.5");
  }

  @Test
  @DisplayName("linha que não é chamada de ferramenta retorna nulo")
  void linhaQueNaoEChamadaRetornaNulo() {
    assertThat(FerramentaChamadaParser.parse("CONSULTA|R$ 10,00")).isNull();
  }
}
