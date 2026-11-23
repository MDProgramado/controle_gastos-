package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;

class ImportacaoPaginacaoTest {

  private ImportacaoService service;

  @BeforeEach
  void setup() {
    service = mock(ImportacaoService.class);
    when(service.listar(any(), any(), any())).thenReturn(Page.empty());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("usuario@teste.com", 1L, 7L, 0, "USER"), List.of()));
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("tamanho é limitado a 100 e página negativa é rejeitada")
  void paginacaoLimitada() {
    ImportacaoController controller = new ImportacaoController(service);

    controller.listar(0, 20);
    controller.listar(3, 500);

    ArgumentCaptor<PageRequest> paginas = ArgumentCaptor.forClass(PageRequest.class);
    verify(service, times(2)).listar(any(), any(), paginas.capture());
    assertThat(paginas.getAllValues())
        .containsExactly(PageRequest.of(0, 20), PageRequest.of(3, 100));

    assertThatThrownBy(() -> controller.listar(-1, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pagina");
    verify(service, times(2)).listar(any(), any(), any());
  }
}
