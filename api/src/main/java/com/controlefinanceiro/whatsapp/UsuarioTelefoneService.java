package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.common.AcessoNegadoException;
import com.controlefinanceiro.common.CodigoVerificacaoInvalidoException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.TelefoneJaVinculadoException;
import com.controlefinanceiro.common.TelefoneNaoPertenceAoUsuarioException;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import com.controlefinanceiro.whatsapp.dto.TelefoneResponse;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de vínculo e verificação de números de WhatsApp.
 *
 * <p>Regras: 1. ADMIN pode vincular/trocar telefone de qualquer usuário da mesma empresa. 2.
 * Somente o dono do telefone pode confirmar o código. 3. Código de 6 dígitos, hash BCrypt, válido
 * 10 min. 4. Limite de 3 tentativas por número por hora. 5. Antes de verificar, o número não
 * autoriza nenhuma ação.
 */
@Service
public class UsuarioTelefoneService {

  private static final int CODIGO_DIGITOS = 6;
  private static final int CODIGO_VALIDADE_MINUTOS = 10;
  private static final int MAX_TENTATIVAS_POR_HORA = 3;

  private final SecureRandom random = new SecureRandom();
  private final UsuarioTelefoneRepository telefoneRepository;
  private final WhatsappCodigoVerificacaoRepository codigoRepository;
  private final AuditoriaTelefoneRepository auditoriaRepository;
  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final WhatsappEnvioService envioService;

  public UsuarioTelefoneService(
      UsuarioTelefoneRepository telefoneRepository,
      WhatsappCodigoVerificacaoRepository codigoRepository,
      AuditoriaTelefoneRepository auditoriaRepository,
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      WhatsappEnvioService envioService) {
    this.telefoneRepository = telefoneRepository;
    this.codigoRepository = codigoRepository;
    this.auditoriaRepository = auditoriaRepository;
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.envioService = envioService;
  }

  /**
   * Vincula (ou troca) um número de WhatsApp a um usuário.
   *
   * <p>Exige que o usuário autenticado seja ADMIN e que o usuário alvo pertença à mesma empresa.
   * Desativa o telefone anterior do usuário (se houver) e cria o novo, gerando código de
   * verificação.
   *
   * @param adminId id do ADMIN que está fazendo a operação (do token JWT)
   * @param empresaId tenant do token (verificado, nunca do body)
   * @param usuarioId id do usuário a quem o telefone será vinculado
   * @param numeroE164 número no formato E.164
   * @return resposta com dados do telefone vinculado
   */
  @Transactional
  public TelefoneResponse vincularTelefone(
      Long adminId, Long empresaId, Long usuarioId, String numeroE164) {

    // Verifica se o admin existe e pertence à empresa
    Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new NaoAutorizadoException("Administrador não encontrado"));
    if (!"ADMIN".equals(admin.getRole())) {
      throw new NaoAutorizadoException("Apenas administradores podem vincular telefones");
    }
    if (!admin.getEmpresa().getId().equals(empresaId)) {
      throw new NaoAutorizadoException("Administrador não pertence a esta empresa");
    }

    // Verifica se o usuário alvo existe e pertence à mesma empresa
    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    if (!usuario.getEmpresa().getId().equals(empresaId)) {
      throw new AcessoNegadoException("Usuário não pertence a esta empresa");
    }

    // Se o número já estiver vinculado a outra empresa, rejeita
    Optional<UsuarioTelefone> telefoneExistente = telefoneRepository.findByNumeroE164(numeroE164);
    if (telefoneExistente.isPresent()
        && !telefoneExistente.get().getEmpresa().getId().equals(empresaId)) {
      throw new TelefoneJaVinculadoException(
          "Número de telefone já está vinculado a outra empresa");
    }

    // Determina a ação: TROCAR se já há telefone ativo para este usuário, senão VINCULAR
    Optional<UsuarioTelefone> telefoneAtivo = telefoneRepository.findAtivoByUsuarioId(usuarioId);
    String acao = telefoneAtivo.isPresent() ? "TROCAR" : "VINCULAR";

    // Desativa o telefone anterior (se houver)
    telefoneAtivo.ifPresent(
        t -> {
          t.setAtivo(false);
          telefoneRepository.save(t);
        });

    // Cria o novo telefone (não verificado ainda)
    UsuarioTelefone telefone = new UsuarioTelefone();
    telefone.setUsuario(usuario);
    telefone.setEmpresa(usuario.getEmpresa());
    telefone.setNumeroE164(numeroE164);
    telefone.setAtivo(true);
    telefone.setVerificadoEm(null);
    telefone = telefoneRepository.save(telefone);

    // Gera código de 6 dígitos e salva com hash
    String codigo = gerarCodigo();
    String codigoHash = passwordEncoder.encode(codigo);
    OffsetDateTime expiraEm =
        OffsetDateTime.now().plus(CODIGO_VALIDADE_MINUTOS, ChronoUnit.MINUTES);

    WhatsappCodigoVerificacao verificacao = new WhatsappCodigoVerificacao();
    verificacao.setTelefone(telefone);
    verificacao.setCodigoHash(codigoHash);
    verificacao.setExpiraEm(expiraEm);
    verificacao.setTentativas(0);
    codigoRepository.save(verificacao);

    // Envia o código via Meta Cloud API (template aprovado)
    envioService.enviarCodigo(numeroE164, codigo);

    // Registra auditoria
    AuditoriaTelefone auditoria = new AuditoriaTelefone();
    auditoria.setAdmin(admin);
    auditoria.setUsuario(usuario);
    auditoria.setNumeroMascarado(mascararNumero(numeroE164));
    auditoria.setAcao(acao);
    auditoriaRepository.save(auditoria);

    return new TelefoneResponse(
        telefone.getId(),
        telefone.getNumeroE164(),
        mascararNumero(numeroE164),
        telefone.isVerificado(),
        telefone.isAtivo());
  }

  /**
   * Verifica o código de 6 dígitos informado pelo usuário.
   *
   * <p>Somente o próprio usuário dono do número pode confirmar (checado pelo usuário autenticado,
   * não pelo número informado).
   *
   * @param usuarioId id do usuário autenticado (do token JWT)
   * @param numeroE164 número do telefone
   * @param codigo código de 6 dígitos
   * @return resposta com status de verificação
   */
  @Transactional(noRollbackFor = CodigoVerificacaoInvalidoException.class)
  public TelefoneResponse verificarCodigo(Long usuarioId, String numeroE164, String codigo) {

    UsuarioTelefone telefone =
        telefoneRepository
            .findByNumeroE164(numeroE164)
            .orElseThrow(
                () ->
                    new TelefoneNaoPertenceAoUsuarioException("Número de telefone não encontrado"));

    // Só o dono do telefone pode confirmar
    if (!telefone.getUsuario().getId().equals(usuarioId)) {
      throw new TelefoneNaoPertenceAoUsuarioException(
          "Este número não pertence ao usuário autenticado");
    }

    if (!telefone.isAtivo()) {
      throw new CodigoVerificacaoInvalidoException("Número de telefone inativo");
    }

    // Busca código válido (não usado, não expirado) — filtro de expiração feito em Java pois
    // now() no HQL não é comparável com OffsetDateTime
    List<WhatsappCodigoVerificacao> todosNaoUsados =
        codigoRepository.findByTelefoneIdAndUsadoEmIsNullOrderByCriadoEmDesc(telefone.getId());
    List<WhatsappCodigoVerificacao> validos =
        todosNaoUsados.stream().filter(c -> !c.isExpirado()).toList();
    if (validos.isEmpty()) {
      throw new CodigoVerificacaoInvalidoException(
          "Código expirado ou não encontrado. Solicite um novo.");
    }

    WhatsappCodigoVerificacao codigoAtual = validos.get(0);

    // Verifica se o código já foi usado
    if (codigoAtual.isUsado()) {
      throw new CodigoVerificacaoInvalidoException("Código já foi utilizado");
    }

    // Verifica expiração
    if (codigoAtual.isExpirado()) {
      throw new CodigoVerificacaoInvalidoException("Código expirado. Solicite um novo.");
    }

    // Verifica limite de tentativas (3 por hora)
    if (codigoAtual.getTentativas() >= MAX_TENTATIVAS_POR_HORA) {
      throw new CodigoVerificacaoInvalidoException(
          "Limite de tentativas excedido. Solicite um novo código.");
    }

    // Compara o código com o hash
    if (!passwordEncoder.matches(codigo, codigoAtual.getCodigoHash())) {
      codigoAtual.incrementarTentativa();
      codigoRepository.save(codigoAtual);

      if (codigoAtual.getTentativas() >= MAX_TENTATIVAS_POR_HORA) {
        throw new CodigoVerificacaoInvalidoException(
            "Código inválido. Limite de tentativas excedido.");
      }
      throw new CodigoVerificacaoInvalidoException(
          "Código inválido. Tentativa "
              + codigoAtual.getTentativas()
              + " de "
              + MAX_TENTATIVAS_POR_HORA);
    }

    // Código correto: marca como verificado e usado
    telefone.setVerificadoEm(OffsetDateTime.now());
    telefoneRepository.save(telefone);

    codigoAtual.setUsadoEm(OffsetDateTime.now());
    codigoRepository.save(codigoAtual);

    return new TelefoneResponse(
        telefone.getId(),
        telefone.getNumeroE164(),
        mascararNumero(numeroE164),
        telefone.isVerificado(),
        telefone.isAtivo());
  }

  /** Busca o telefone ativo de um usuário (para uso em outros módulos). */
  public Optional<UsuarioTelefone> buscarTelefoneAtivoPorUsuario(Long usuarioId) {
    return telefoneRepository.findAtivoByUsuarioId(usuarioId);
  }

  /** Gera código aleatório de 6 dígitos. */
  private String gerarCodigo() {
    BigInteger num = new BigInteger(CODIGO_DIGITOS * 4, random);
    int valor = num.intValue() % (int) Math.pow(10, CODIGO_DIGITOS);
    return String.format("%0" + CODIGO_DIGITOS + "d", valor);
  }

  /** Máscara do número para logs e exibição: +55 71 ****-**99. Nunca exibe o número completo. */
  public String mascararNumero(String numeroE164) {
    if (numeroE164 == null || numeroE164.length() < 8) {
      return "****";
    }
    // Formato E.164: +5571999999999 -> +55 71 ****-**99
    String pais = numeroE164.substring(0, 3); // +55
    String ddd = numeroE164.substring(3, 5); // 71
    String fim = numeroE164.substring(numeroE164.length() - 2); // últimos 2 dígitos
    return pais + " " + ddd + " ****-**" + fim;
  }
}
