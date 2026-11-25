package com.controlefinanceiro.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.conta.ContaAPagarRepository;
import com.controlefinanceiro.conta.FaturaCartaoRepository;
import com.controlefinanceiro.dashboard.DashboardRepository;
import com.controlefinanceiro.helpdesk.HelpdeskMensagemRepository;
import com.controlefinanceiro.helpdesk.HelpdeskTicketRepository;
import com.controlefinanceiro.importacao.ImportacaoDocumentoRepository;
import com.controlefinanceiro.importacao.ImportacaoFaturaRepository;
import com.controlefinanceiro.importacao.ImportacaoItemRepository;
import com.controlefinanceiro.lancamento.LancamentoRepository;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/** Garante que nenhuma query financeira redefine o isolamento por empresa/usuário. */
class IsolamentoUsuarioRepositoryTest {

  private static final List<Class<?>> REPOSITORIOS =
      List.of(
          LancamentoRepository.class,
          DashboardRepository.class,
          ImportacaoDocumentoRepository.class,
          ImportacaoItemRepository.class,
          ImportacaoFaturaRepository.class,
          ContaAPagarRepository.class,
          FaturaCartaoRepository.class,
          HelpdeskTicketRepository.class,
          HelpdeskMensagemRepository.class);

  @Test
  @DisplayName("toda query financeira declarada filtra usuario_id")
  void todaQueryFinanceiraFiltraUsuario() {
    for (Class<?> repositorio : REPOSITORIOS) {
      for (Method metodo : repositorio.getDeclaredMethods()) {
        Query query = metodo.getAnnotation(Query.class);
        if (ehFilaAdminHelpdesk(repositorio, metodo)) {
          assertThat(query)
              .as("fila admin %s.%s usa @Query", repositorio.getSimpleName(), metodo.getName())
              .isNotNull();
          assertThat(query.value())
              .as(
                  "fila admin %s.%s deve filtrar empresa_id",
                  repositorio.getSimpleName(), metodo.getName())
              .contains("empresa.id = :empresaId")
              .doesNotContain("usuario.id");
          continue;
        }
        if (repositorio == HelpdeskMensagemRepository.class) {
          assertThat(query).as("query de mensagem %s", metodo.getName()).isNotNull();
          assertThat(query.value())
              .as("query de mensagem %s deve filtrar ticket e empresa", metodo.getName())
              .contains("m.ticket.id = :ticketId")
              .contains("m.empresa.id = :empresaId");
          continue;
        }
        if (query != null) {
          assertThat(query.value())
              .as(
                  "query %s.%s deve filtrar usuario_id",
                  repositorio.getSimpleName(), metodo.getName())
              .contains("usuario.id");
        } else if (metodo.getName().startsWith("findBy")) {
          assertThat(metodo.getName())
              .as(
                  "query derivada %s.%s deve filtrar usuario",
                  repositorio.getSimpleName(), metodo.getName())
              .contains("UsuarioId");
        }
      }
    }
  }

  private boolean ehFilaAdminHelpdesk(Class<?> repositorio, Method metodo) {
    return repositorio == HelpdeskTicketRepository.class
        && ("findByEmpresaIdAndStatus".equals(metodo.getName())
            || "findByIdAndEmpresaId".equals(metodo.getName()));
  }

  @Test
  @DisplayName("repositorios financeiros expoem busca por id escalonada")
  void buscaPorIdEscalonadaExiste() throws Exception {
    assertThat(
            LancamentoRepository.class.getMethod(
                "findByIdAndEmpresaIdAndUsuarioId", Long.class, Long.class, Long.class))
        .isNotNull();
    assertThat(
            ImportacaoDocumentoRepository.class.getMethod(
                "findByIdAndEmpresaIdAndUsuarioId", Long.class, Long.class, Long.class))
        .isNotNull();
    assertThat(
            FaturaCartaoRepository.class.getMethod(
                "findByIdAndEmpresaIdAndUsuarioId", Long.class, Long.class, Long.class))
        .isNotNull();
  }

  @Test
  @DisplayName("repositorios do helpdesk expoem buscas escalonadas")
  void buscasDoHelpdeskEscalonadasExistem() throws Exception {
    assertThat(
            HelpdeskTicketRepository.class.getMethod(
                "findByIdAndEmpresaIdAndUsuarioId", Long.class, Long.class, Long.class))
        .isNotNull();
    assertThat(
            HelpdeskTicketRepository.class.getMethod(
                "findByIdAndEmpresaId", Long.class, Long.class))
        .isNotNull();
    assertThat(
            HelpdeskMensagemRepository.class.getMethod(
                "findByTicketIdAndEmpresaIdOrderByCriadoEmAsc", Long.class, Long.class))
        .isNotNull();
  }
}
