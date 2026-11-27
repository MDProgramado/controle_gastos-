package com.controlefinanceiro.config;

import com.controlefinanceiro.auth.GoogleTokenInvalidoException;
import com.controlefinanceiro.auth.GoogleVerificacaoIndisponivelException;
import com.controlefinanceiro.common.AcessoNegadoException;
import com.controlefinanceiro.common.CadastroInvalidoException;
import com.controlefinanceiro.common.CategoriaDuplicadaException;
import com.controlefinanceiro.common.CategoriaNaoEncontradaException;
import com.controlefinanceiro.common.CategoriaTipoIncompativelException;
import com.controlefinanceiro.common.CodigoVerificacaoCooldownException;
import com.controlefinanceiro.common.CodigoVerificacaoInvalidoException;
import com.controlefinanceiro.common.ContaAPagarJaPagaException;
import com.controlefinanceiro.common.ContaAPagarNaoEncontradoException;
import com.controlefinanceiro.common.ConviteInvalidoException;
import com.controlefinanceiro.common.CredenciaisInvalidasException;
import com.controlefinanceiro.common.LancamentoNaoEncontradoException;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.common.LoteDemasiadoGrandeException;
import com.controlefinanceiro.common.MonetarioException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.RoleInvalidoException;
import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.controlefinanceiro.common.TelefoneJaVinculadoException;
import com.controlefinanceiro.common.TelefoneNaoPertenceAoUsuarioException;
import com.controlefinanceiro.common.TicketNaoEncontradoException;
import com.controlefinanceiro.common.TicketStatusInvalidoException;
import com.controlefinanceiro.common.UltimoAdminAtivoException;
import com.controlefinanceiro.common.UsuarioNaoEncontradoException;
import com.controlefinanceiro.importacao.ArquivoInvalidoException;
import com.controlefinanceiro.importacao.ImportacaoEstadoInvalidoException;
import com.controlefinanceiro.importacao.ImportacaoNaoEncontradaException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

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

  /** ID token Google inválido ou não confiável → 401, sem detalhar qual claim falhou. */
  @ExceptionHandler(GoogleTokenInvalidoException.class)
  public ProblemDetail googleTokenInvalido(GoogleTokenInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  /** Falha de I/O ao verificar o ID token Google (JWKS) → 503, sem expor o token. */
  @ExceptionHandler(GoogleVerificacaoIndisponivelException.class)
  public ProblemDetail googleVerificacaoIndisponivel(GoogleVerificacaoIndisponivelException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }

  /** Cadastro/convite recusado com mensagem genérica → 400 (anti-enumeração). */
  @ExceptionHandler(CadastroInvalidoException.class)
  public ProblemDetail cadastroInvalido(CadastroInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Convite inexistente, expirado, usado ou com e-mail divergente → 400. */
  @ExceptionHandler(ConviteInvalidoException.class)
  public ProblemDetail conviteInvalido(ConviteInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Usuário fora da empresa do token → 404 (sem vazar existência entre tenants). */
  @ExceptionHandler(UsuarioNaoEncontradoException.class)
  public ProblemDetail usuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  /** Lançamento fora do tenant/usuário ou inexistente → 404. */
  @ExceptionHandler(LancamentoNaoEncontradoException.class)
  public ProblemDetail lancamentoNaoEncontrado(LancamentoNaoEncontradoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(TicketNaoEncontradoException.class)
  public ProblemDetail ticketNaoEncontrado(TicketNaoEncontradoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ContaAPagarNaoEncontradoException.class)
  public ProblemDetail contaAPagarNaoEncontrada(ContaAPagarNaoEncontradoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ContaAPagarJaPagaException.class)
  public ProblemDetail contaAPagarJaPaga(ContaAPagarJaPagaException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(TicketStatusInvalidoException.class)
  public ProblemDetail ticketStatusInvalido(TicketStatusInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Operação deixaria a empresa sem ADMIN ativo → 409. */
  @ExceptionHandler(UltimoAdminAtivoException.class)
  public ProblemDetail ultimoAdminAtivo(UltimoAdminAtivoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  /** Rota autenticada sem token/token inválido/expirado (via SecurityContext) → 401. */
  @ExceptionHandler(NaoAutorizadoException.class)
  public ProblemDetail naoAutorizado(NaoAutorizadoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  /** Autenticado, mas sem permissão para a operação (isolamento multi-tenant/autorização) → 403. */
  @ExceptionHandler(AcessoNegadoException.class)
  public ProblemDetail acessoNegado(AcessoNegadoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  /** Role fora do enum ADMIN/USER → 400 ProblemDetail. */
  @ExceptionHandler(RoleInvalidoException.class)
  public ProblemDetail roleInvalido(RoleInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
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

  /**
   * Serviço de IA (OpenRouter) indisponível ou sobrecarregado → 503 com mensagem que o usuário pode
   * entender (fase 4).
   */
  @ExceptionHandler(ServicoIaIndisponivelException.class)
  public ProblemDetail servicoIaIndisponivel(ServicoIaIndisponivelException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }

  /** Timeout/conexão com serviço externo (IA) → 503 amigável, sem vazar stack/URL. */
  @ExceptionHandler(ResourceAccessException.class)
  public ProblemDetail recursoInacessivel(ResourceAccessException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE,
        "O serviço de IA demorou para responder. Tente novamente em instantes.");
  }

  /** Arquivo rejeitado na importação (não é PDF, vazio, ou acima de 8 MB) → 400 (fase 5). */
  @ExceptionHandler(ArquivoInvalidoException.class)
  public ProblemDetail arquivoInvalido(ArquivoInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /**
   * Lote de upload acima do limite do servidor (spring.servlet.multipart.max-request-size) → 413.
   * Erros por arquivo (≤ 8 MB) viram 400 com status ERRO na resposta, não chegam aqui.
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ProblemDetail uploadDemasiadoGrande(MaxUploadSizeExceededException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "Lote de arquivos excede o limite de 40 MB. Envie menos arquivos de uma vez.");
  }

  /**
   * Campo obrigatório ausente no multipart (ex.: arquivo não enviado) → 400 sem vazar detalhe
   * interno do Spring. Não engole MaxUploadSizeExceededException (413), que tem handler próprio
   * acima.
   */
  @ExceptionHandler(MultipartException.class)
  public ProblemDetail parteObrigatoriaAusente(MultipartException ex) {
    if (ex instanceof MaxUploadSizeExceededException) {
      throw ex; // deixa o handler específico de 413 tratar
    }
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Nenhum arquivo enviado.");
  }

  /** Importação não encontrada para a empresa do token → 404 (fase 5, R3). */
  @ExceptionHandler(ImportacaoNaoEncontradaException.class)
  public ProblemDetail importacaoNaoEncontrada(ImportacaoNaoEncontradaException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  /** Operação inválida para o estado da importação (ex.: descartar já confirmada) → 409. */
  @ExceptionHandler(ImportacaoEstadoInvalidoException.class)
  public ProblemDetail importacaoEstadoInvalido(ImportacaoEstadoInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  /** Número de telefone não encontrado ou não pertence ao usuário autenticado → 400. */
  @ExceptionHandler(TelefoneNaoPertenceAoUsuarioException.class)
  public ProblemDetail telefoneNaoPertenceAoUsuario(TelefoneNaoPertenceAoUsuarioException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Código de verificação inválido/expirado/esgotado → 400 com a mensagem amigável. */
  @ExceptionHandler(CodigoVerificacaoInvalidoException.class)
  public ProblemDetail codigoVerificacaoInvalido(CodigoVerificacaoInvalidoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Solicitação de código dentro do cooldown de envio → 429. */
  @ExceptionHandler(CodigoVerificacaoCooldownException.class)
  public ProblemDetail codigoVerificacaoCooldown(CodigoVerificacaoCooldownException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
  }

  /** Número já vinculado a outra empresa → 409. */
  @ExceptionHandler(TelefoneJaVinculadoException.class)
  public ProblemDetail telefoneJaVinculado(TelefoneJaVinculadoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  /** Exceção de argumento inválido (cadastro/verificação WhatsApp) → 400 ProblemDetail. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Estado ilegal de configuração da IA → 503 sem vazar detalhe interno. */
  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail estadoIlegal(IllegalStateException ex) {
    String mensagem = ex.getMessage();
    if (mensagem != null && mensagem.contains("ai.openrouter.api-key")) {
      return ProblemDetail.forStatusAndDetail(
          HttpStatus.SERVICE_UNAVAILABLE, "Serviço de IA não configurado. Contate o suporte.");
    }
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.");
  }
}
