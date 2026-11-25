package com.controlefinanceiro.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RagIngestaoEstadoTest {

  @Test
  @DisplayName("estado inicial é parado sem progresso")
  void estadoInicialEParado() {
    RagIngestaoEstado estado = new RagIngestaoEstado();

    var status = estado.status();

    assertThat(status.situacao()).isEqualTo("PARADO");
    assertThat(status.total()).isZero();
    assertThat(status.processados()).isZero();
    assertThat(status.erro()).isNull();
    assertThat(status.iniciadoEm()).isNull();
    assertThat(status.finalizadoEm()).isNull();
  }

  @Test
  @DisplayName("registra progresso e conclusão")
  void registraProgressoEConclusao() {
    RagIngestaoEstado estado = new RagIngestaoEstado();

    estado.iniciar(10);
    Instant iniciado = estado.status().iniciadoEm();
    estado.registrarProgresso(4);
    estado.concluir();

    var status = estado.status();
    assertThat(status.situacao()).isEqualTo("SUCESSO");
    assertThat(status.total()).isEqualTo(10);
    assertThat(status.processados()).isEqualTo(10);
    assertThat(status.iniciadoEm()).isEqualTo(iniciado);
    assertThat(status.finalizadoEm()).isNotNull();
    assertThat(status.erro()).isNull();
  }

  @Test
  @DisplayName("registra falha com mensagem sanitizada")
  void registraFalhaComMensagemSanitizada() {
    RagIngestaoEstado estado = new RagIngestaoEstado();
    String mensagem = "erro\nlinha " + "x".repeat(400);

    estado.iniciar(10);
    estado.registrarProgresso(3);
    estado.falhar(mensagem);

    var status = estado.status();
    assertThat(status.situacao()).isEqualTo("ERRO");
    assertThat(status.processados()).isEqualTo(3);
    assertThat(status.erro()).doesNotContain("\n");
    assertThat(status.erro().length()).isLessThanOrEqualTo(300);
    assertThat(status.finalizadoEm()).isNotNull();
  }
}
