package com.controlefinanceiro.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class LancamentoLoteValidationTest {

  private LancamentoService service;
  private Validator validator;

  @BeforeEach
  void setup() {
    LocalValidatorFactoryBean fabrica = new LocalValidatorFactoryBean();
    fabrica.afterPropertiesSet();
    validator = fabrica;

    LancamentoRepository lancamentos = mock(LancamentoRepository.class);
    CategoriaRepository categorias = mock(CategoriaRepository.class);
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    EntityManager entityManager = mock(EntityManager.class);

    Categoria categoria = new Categoria();
    categoria.setId(9L);
    categoria.setNome("Alimentação");
    categoria.setTipo("SAIDA");
    when(categorias.findByIdAndEmpresaId(9L, 1L)).thenReturn(Optional.of(categoria));

    Usuario usuario = new Usuario();
    usuario.setId(7L);
    when(usuarios.findByIdAndEmpresaId(7L, 1L)).thenReturn(Optional.of(usuario));
    when(entityManager.getReference(any(), any())).thenReturn(new Empresa());
    when(lancamentos.save(any(Lancamento.class))).thenAnswer(inv -> inv.getArgument(0));

    service = new LancamentoService(lancamentos, categorias, usuarios, entityManager);
  }

  @Test
  @DisplayName("linha do lote replica as constraints do LancamentoRequest e cascateia")
  void constraintsDaLinha() {
    LancamentoLoteRequest.LancamentoLinha linha =
        new LancamentoLoteRequest.LancamentoLinha(null, null, null, null, null);

    assertThat(validator.validate(linha)).hasSize(4);
    assertThat(validator.validate(new LancamentoLoteRequest(List.of(linha)))).hasSize(4);
  }

  @Test
  @DisplayName("valor nulo na linha 2 gera mensagem estável com índice")
  void valorNuloComIndice() {
    LancamentoLoteRequest lote =
        new LancamentoLoteRequest(
            List.of(
                linha(new BigDecimal("10.00"), "SAIDA", LocalDate.of(2026, 11, 1)),
                linha(null, "SAIDA", LocalDate.of(2026, 11, 2))));

    LinhaInvalidaException erro =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () -> service.processarLote(1L, 7L, lote), LinhaInvalidaException.class);
    assertThat(erro.getIndiceLinha()).isEqualTo(1);
    assertThat(erro).hasMessageContaining("valor é obrigatório");
  }

  @Test
  @DisplayName("tipo nulo na linha gera mensagem estável com índice")
  void tipoNuloComIndice() {
    LancamentoLoteRequest lote =
        new LancamentoLoteRequest(
            List.of(linha(new BigDecimal("10.00"), null, LocalDate.of(2026, 11, 1))));

    LinhaInvalidaException erro =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () -> service.processarLote(1L, 7L, lote), LinhaInvalidaException.class);
    assertThat(erro.getIndiceLinha()).isEqualTo(0);
    assertThat(erro).hasMessageContaining("tipo é obrigatório");
  }

  @Test
  @DisplayName("data nula na linha gera mensagem estável com índice")
  void dataNulaComIndice() {
    LancamentoLoteRequest lote =
        new LancamentoLoteRequest(List.of(linha(new BigDecimal("10.00"), "SAIDA", null)));

    LinhaInvalidaException erro =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () -> service.processarLote(1L, 7L, lote), LinhaInvalidaException.class);
    assertThat(erro.getIndiceLinha()).isEqualTo(0);
    assertThat(erro).hasMessageContaining("data é obrigatória");
  }

  private static LancamentoLoteRequest.LancamentoLinha linha(
      BigDecimal valor, String tipo, LocalDate data) {
    return new LancamentoLoteRequest.LancamentoLinha(9L, tipo, valor, data, "teste");
  }
}
