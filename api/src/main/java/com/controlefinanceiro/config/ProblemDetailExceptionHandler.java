package com.controlefinanceiro.config;

import com.controlefinanceiro.common.CategoriaDuplicadaException;
import com.controlefinanceiro.common.CategoriaNaoEncontradaException;
import com.controlefinanceiro.common.CategoriaTipoIncompativelException;
import com.controlefinanceiro.common.CredenciaisInvalidasException;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.common.LoteDemasiadoGrandeException;
import com.controlefinanceiro.common.MonetarioException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Padrão P7 — RFC 7807 ProblemDetail (PLAN §3.3). Nunca logar header {@code Authorization}, cookie
 * ou body de login (PLAN §4.5).
 */
@RestControllerAdvice
public class ProblemDetailExceptionHandler {

  /** D-05/T-2-01: 401 com a mensagem genérica única — nunca dizer qual campo falhou. */
  @ExceptionHandler(CredenciaisInvalidasException.class)
  public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  /** Rota autenticada sem token/token inválido/expirado (via SecurityContext) → 401. */
  @ExceptionHandler(NaoAutorizadoException.class)
  public ProblemDetail naoAutorizado(NaoAutorizadoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  /** Validação do DTO (@Valid) → 400 listando os campos com problema. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail argumentosInvalidos(MethodArgumentNotValidException ex) {
    String campos =
        ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, campos);
  }

  /**
   * Corpo malformado ou com campo desconhecido (fail-on-unknown-properties) → 400 ProblemDetail.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail corpoInvalido(HttpMessageNotReadableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
  }

  /** Tipo de categoria inválido ou incompatível → 400 ProblemDetail (CAT-04). */
  @ExceptionHandler(CategoriaTipoIncompativelException.class)
  public ProblemDetail categoriaTipoIncompativel(CategoriaTipoIncompativelException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Valor monetário inválido (>2 casas decimais ou <= 0) → 400 ProblemDetail (LAN-04, LAN-07). */
  @ExceptionHandler(MonetarioException.class)
  public ProblemDetail monetarioInvalido(MonetarioException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Categoria duplicada (mesmo nome+tipo+empresa) → 400 ProblemDetail. */
  @ExceptionHandler(CategoriaDuplicadaException.class)
  public ProblemDetail categoriaDuplicada(CategoriaDuplicadaException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Categoria não encontrada para a empresa autenticada → 404 ProblemDetail. */
  @ExceptionHandler(CategoriaNaoEncontradaException.class)
  public ProblemDetail categoriaNaoEncontrada(CategoriaNaoEncontradaException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  /** Linha inválida em lote de lançamentos → 400 ProblemDetail com o índice da linha (LOT-02). */
  @ExceptionHandler(LinhaInvalidaException.class)
  public ProblemDetail linhaInvalida(LinhaInvalidaException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setProperty("indiceLinha", ex.getIndiceLinha());
    return problemDetail;
  }

  /** Lote acima do limite de 500 linhas → 400 ProblemDetail (LOT-01, T-02-09). */
  @ExceptionHandler(LoteDemasiadoGrandeException.class)
  public ProblemDetail loteDemasiadoGrande(LoteDemasiadoGrandeException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }
}
