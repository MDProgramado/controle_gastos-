package com.controlefinanceiro.categoria.grupo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.UsuarioAuth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CategoriaGrupo503020ControllerTest {

  private CategoriaGrupo503020Service service;
  private CategoriaGrupo503020Controller controller;

  @BeforeEach
  void setup() {
    service = mock(CategoriaGrupo503020Service.class);
    controller = new CategoriaGrupo503020Controller(service);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UsuarioAuth("admin@teste.com", 1L, 10L, 1, "ADMIN"), null, List.of()));
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listarUsaTenantDoToken() {
    List<CategoriaGrupo503020Response> resposta =
        List.of(new CategoriaGrupo503020Response(5L, "Lazer", "SAIDA", "DESEJO", true));
    when(service.listar(1L)).thenReturn(resposta);

    ResponseEntity<List<CategoriaGrupo503020Response>> resultado = controller.listar();

    assertThat(resultado.getBody()).isEqualTo(resposta);
    verify(service).listar(1L);
  }

  @Test
  void salvarUsaTenantDoToken() {
    CategoriaGrupo503020Request request = new CategoriaGrupo503020Request("NECESSIDADE");
    CategoriaGrupo503020Response resposta =
        new CategoriaGrupo503020Response(5L, "Alimentação", "SAIDA", "NECESSIDADE", false);
    when(service.salvar(1L, 5L, "NECESSIDADE")).thenReturn(resposta);

    ResponseEntity<CategoriaGrupo503020Response> resultado = controller.salvar(5L, request);

    assertThat(resultado.getBody()).isEqualTo(resposta);
    verify(service).salvar(1L, 5L, "NECESSIDADE");
  }
}
