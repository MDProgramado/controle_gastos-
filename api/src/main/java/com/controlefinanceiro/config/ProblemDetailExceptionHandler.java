package com.controlefinanceiro.config;

import com.controlefinanceiro.common.CredenciaisInvalidasException;
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
}
