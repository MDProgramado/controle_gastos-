package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro e verificação de números de WhatsApp. Gera código de 6 dígitos com hash BCrypt, válido
 * por 10 minutos, com limite de 5 tentativas por número por hora.
 */
@Service
public class WhatsappCadastroService {

  private static final int CODIGO_DIGITOS = 6;
  private static final int CODIGO_VALIDADE_MINUTOS = 10;
  private static final int MAX_TENTATIVAS_POR_HORA = 5;

  private final SecureRandom random = new SecureRandom();
  private final UsuarioTelefoneRepository telefoneRepository;
  private final WhatsappCodigoVerificacaoRepository codigoRepository;
  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final WhatsappEnvioService envioService;

  @Value("${whatsapp.template.codigo:}")
  private String templateCodigo;

  public WhatsappCadastroService(
      UsuarioTelefoneRepository telefoneRepository,
      WhatsappCodigoVerificacaoRepository codigoRepository,
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      WhatsappEnvioService envioService) {
    this.telefoneRepository = telefoneRepository;
    this.codigoRepository = codigoRepository;
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.envioService = envioService;
  }

  /**
   * Cadastra um número de telefone para o usuário/empresa. Gera código de 6 dígitos, salva com hash
   * e envia via Meta. Idempotente: se o número já existir para a mesma empresa, reutiliza o
   * registro. Se o número já estiver vinculado a outra empresa, rejeita.
   */
  @Transactional
  public UsuarioTelefone cadastrarNumero(Long usuarioId, Long empresaId, String numeroE164) {
    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(
                () -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));

    Empresa empresa = usuario.getEmpresa();
    if (empresa == null || !empresa.getId().equals(empresaId)) {
      throw new IllegalArgumentException("Empresa não pertence ao usuário");
    }

    // F1: Rejeita se o número já estiver vinculado a uma empresa diferente
    Optional<UsuarioTelefone> telefoneExistente = telefoneRepository.findByNumeroE164(numeroE164);
    if (telefoneExistente.isPresent()
        && !telefoneExistente.get().getEmpresa().getId().equals(empresaId)) {
      throw new IllegalArgumentException("Número de telefone já está vinculado a outra empresa");
    }

    UsuarioTelefone telefone =
        telefoneExistente
            .map(
                t -> {
                  t.setUsuario(usuario);
                  t.setEmpresa(empresa);
                  t.setAtivo(true);
                  t.setVerificadoEm(null);
                  return t;
                })
            .orElseGet(
                () -> {
                  UsuarioTelefone novo = new UsuarioTelefone();
                  novo.setUsuario(usuario);
                  novo.setEmpresa(empresa);
                  novo.setNumeroE164(numeroE164);
                  novo.setAtivo(true);
                  return novo;
                });

    telefone = telefoneRepository.save(telefone);

    // F2: Rate limit — não permite novo código se um foi enviado nos últimos 2 minutos
    List<WhatsappCodigoVerificacao> recentes =
        codigoRepository.findByUsuarioTelefoneIdOrderByCriadoEmDesc(telefone.getId());
    OffsetDateTime agora = OffsetDateTime.now();
    for (WhatsappCodigoVerificacao c : recentes) {
      if (!c.isExpirado()
          && c.getCriadoEm() != null
          && c.getCriadoEm().isAfter(agora.minusMinutes(2))) {
        throw new IllegalArgumentException(
            "Um código já foi enviado recentemente. Aguarde alguns minutos antes de solicitar outro.");
      }
    }

    // Remove códigos anteriores não expirados para este telefone
    for (WhatsappCodigoVerificacao c : recentes) {
      if (!c.isExpirado() && c.getTentativas() < MAX_TENTATIVAS_POR_HORA) {
        codigoRepository.delete(c);
      }
    }

    // Gera código de 6 dígitos
    String codigo = gerarCodigo();
    String codigoHash = passwordEncoder.encode(codigo);
    OffsetDateTime expiraEm =
        OffsetDateTime.now().plus(CODIGO_VALIDADE_MINUTOS, ChronoUnit.MINUTES);

    WhatsappCodigoVerificacao verificacao = new WhatsappCodigoVerificacao();
    verificacao.setUsuarioTelefone(telefone);
    verificacao.setCodigoHash(codigoHash);
    verificacao.setExpiraEm(expiraEm);
    verificacao.setTentativas(0);
    codigoRepository.save(verificacao);

    // Envia o código via Meta (template aprovado)
    envioService.enviarCodigo(numeroE164, codigo, templateCodigo);

    return telefone;
  }

  /**
   * Verifica o código de 6 dígitos informado pelo usuário. Se válido, marca {@code verificado_em =
   * now()}. Limite de 5 tentativas por hora.
   *
   * @return true se o código foi aceito e o número verificado
   */
  @Transactional
  public boolean verificarCodigo(Long usuarioId, String numeroE164, String codigo) {
    UsuarioTelefone telefone =
        telefoneRepository
            .findByNumeroE164(numeroE164)
            .orElseThrow(() -> new IllegalArgumentException("Número não cadastrado"));

    if (!telefone.getUsuario().getId().equals(usuarioId)) {
      throw new IllegalArgumentException("Número não pertence a este usuário");
    }

    if (!telefone.isAtivo()) {
      throw new IllegalArgumentException("Número inativo");
    }

    List<WhatsappCodigoVerificacao> validos =
        codigoRepository.findValidByUsuarioTelefoneId(telefone.getId(), OffsetDateTime.now());
    if (validos.isEmpty()) {
      throw new IllegalArgumentException("Código expirado ou excedeu o limite de tentativas");
    }

    WhatsappCodigoVerificacao codigoAtual = validos.get(0);
    if (codigoAtual.isExpirado()) {
      throw new IllegalArgumentException("Código expirado");
    }

    if (!passwordEncoder.matches(codigo, codigoAtual.getCodigoHash())) {
      codigoAtual.incrementarTentativa();
      codigoRepository.save(codigoAtual);
      throw new IllegalArgumentException("Código inválido");
    }

    telefone.setVerificadoEm(OffsetDateTime.now());
    telefoneRepository.save(telefone);

    // Invalida o código usado
    codigoRepository.delete(codigoAtual);

    return true;
  }

  private String gerarCodigo() {
    BigInteger num = new BigInteger(CODIGO_DIGITOS * 4, random);
    int valor = num.intValue() % (int) Math.pow(10, CODIGO_DIGITOS);
    return String.format("%0" + CODIGO_DIGITOS + "d", valor);
  }

  /** Busca telefone vinculado pelo número E.164. */
  public UsuarioTelefone buscarPorNumero(String numeroE164) {
    return telefoneRepository
        .findByNumeroE164(numeroE164)
        .orElseThrow(() -> new IllegalArgumentException("Número não cadastrado: " + numeroE164));
  }
}
