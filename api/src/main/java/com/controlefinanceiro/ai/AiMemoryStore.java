package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.UsuarioContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiMemoryStore {

  private final AiMemoriaSessaoRepository sessaoRepository;
  private final AiMemoriaMensagemRepository mensagemRepository;
  private final Map<String, AiSession> sessoesEmMemoria = new ConcurrentHashMap<>();

  @Autowired
  public AiMemoryStore(
      AiMemoriaSessaoRepository sessaoRepository, AiMemoriaMensagemRepository mensagemRepository) {
    this.sessaoRepository = Objects.requireNonNull(sessaoRepository, "sessaoRepository");
    this.mensagemRepository = Objects.requireNonNull(mensagemRepository, "mensagemRepository");
  }

  public AiMemoryStore() {
    this.sessaoRepository = null;
    this.mensagemRepository = null;
  }

  private boolean persistente() {
    return sessaoRepository != null;
  }

  private static String chave(String sessionId, String empresaId, String usuarioId) {
    return empresaId + "::" + usuarioId + "::" + sessionId;
  }

  private static Long id(String valor) {
    return Long.valueOf(Objects.requireNonNull(valor));
  }

  public AiSession obterOuCriar(String sessionId, String empresaId) {
    return obterOuCriar(sessionId, empresaId, UsuarioContext.getUsuarioId().toString());
  }

  public AiSession obterOuCriar(String sessionId, String empresaId, String usuarioId) {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(empresaId, "empresaId");
    Objects.requireNonNull(usuarioId, "usuarioId");
    if (!persistente()) {
      return sessoesEmMemoria.computeIfAbsent(
          chave(sessionId, empresaId, usuarioId), k -> new AiSession(sessionId, empresaId));
    }
    return carregar(obterOuCriarEntidade(sessionId, empresaId, usuarioId));
  }

  public AiSession obter(String sessionId, String empresaId) {
    return obter(sessionId, empresaId, UsuarioContext.getUsuarioId().toString());
  }

  public AiSession obter(String sessionId, String empresaId, String usuarioId) {
    if (sessionId == null || empresaId == null || usuarioId == null) {
      return null;
    }
    if (!persistente()) {
      AiSession sessao = sessoesEmMemoria.get(chave(sessionId, empresaId, usuarioId));
      return sessao != null && empresaId.equals(sessao.empresaId()) ? sessao : null;
    }
    return sessaoRepository
        .findByEmpresaIdAndUsuarioIdAndSessionId(id(empresaId), id(usuarioId), sessionId)
        .map(this::carregar)
        .orElse(null);
  }

  @Transactional
  public void adicionarMensagem(String sessionId, String empresaId, AiMessage mensagem) {
    adicionarMensagem(sessionId, empresaId, UsuarioContext.getUsuarioId().toString(), mensagem);
  }

  @Transactional
  public void adicionarMensagem(
      String sessionId, String empresaId, String usuarioId, AiMessage mensagem) {
    Objects.requireNonNull(mensagem, "mensagem");
    if (!persistente()) {
      AiSession sessao = obterOuCriar(sessionId, empresaId, usuarioId);
      synchronized (sessao) {
        List<AiMessage> historico = sessao.mensagens();
        if (deveIgnorar(historico, mensagem)) {
          return;
        }
        sessao.adicionar(mensagem);
      }
      return;
    }
    AiMemoriaSessao entidade = obterOuCriarEntidade(sessionId, empresaId, usuarioId);
    List<AiMemoriaMensagem> recentes =
        mensagemRepository.findTop20BySessaoIdOrderByIdDesc(entidade.getId());
    AiMemoriaMensagem ultima = recentes.isEmpty() ? null : recentes.get(0);
    if (ultima != null && deveIgnorar(ultima.getPapel(), ultima.getConteudo(), mensagem)) {
      return;
    }
    mensagemRepository.save(new AiMemoriaMensagem(entidade, mensagem.role(), mensagem.content()));
    entidade.setAtualizadoEm(Instant.now());
    sessaoRepository.save(entidade);
  }

  private boolean deveIgnorar(List<AiMessage> historico, AiMessage mensagem) {
    if (historico.isEmpty()) return false;
    AiMessage ultima = historico.get(historico.size() - 1);
    return deveIgnorar(ultima.role(), ultima.content(), mensagem);
  }

  private boolean deveIgnorar(String roleAnterior, String conteudoAnterior, AiMessage mensagem) {
    if (roleAnterior == null || mensagem.role() == null) return false;
    if (!roleAnterior.equals(mensagem.role())) return false;
    return Objects.equals(conteudoAnterior, mensagem.content());
  }

  public List<AiMessage> obterHistorico(String sessionId, String empresaId) {
    return obterHistorico(sessionId, empresaId, UsuarioContext.getUsuarioId().toString());
  }

  public List<AiMessage> obterHistorico(String sessionId, String empresaId, String usuarioId) {
    if (!persistente()) {
      AiSession sessao = obter(sessionId, empresaId, usuarioId);
      return sessao == null ? List.of() : sessao.mensagens();
    }
    return sessaoRepository
        .findByEmpresaIdAndUsuarioIdAndSessionId(id(empresaId), id(usuarioId), sessionId)
        .map(entidade -> historico(entidade.getId()))
        .orElseGet(List::of);
  }

  private List<AiMessage> historico(Long sessaoId) {
    List<AiMessage> mensagens = new ArrayList<>();
    for (AiMemoriaMensagem mensagem :
        mensagemRepository.findTop20BySessaoIdOrderByIdDesc(sessaoId)) {
      mensagens.add(new AiMessage(mensagem.getPapel(), mensagem.getConteudo()));
    }
    Collections.reverse(mensagens);
    return mensagens;
  }

  @Transactional
  public void limpar(String sessionId, String empresaId) {
    limpar(sessionId, empresaId, UsuarioContext.getUsuarioId().toString());
  }

  @Transactional
  public void limpar(String sessionId, String empresaId, String usuarioId) {
    if (!persistente()) {
      AiSession sessao = obter(sessionId, empresaId, usuarioId);
      if (sessao != null) {
        sessao.limpar();
      }
      return;
    }
    sessaoRepository
        .findByEmpresaIdAndUsuarioIdAndSessionId(id(empresaId), id(usuarioId), sessionId)
        .ifPresent(entidade -> mensagemRepository.deleteBySessaoId(entidade.getId()));
  }

  public int totalSessoes() {
    return persistente() ? (int) sessaoRepository.count() : sessoesEmMemoria.size();
  }

  private AiMemoriaSessao obterOuCriarEntidade(
      String sessionId, String empresaId, String usuarioId) {
    return sessaoRepository
        .findByEmpresaIdAndUsuarioIdAndSessionId(id(empresaId), id(usuarioId), sessionId)
        .orElseGet(
            () ->
                sessaoRepository.saveAndFlush(
                    new AiMemoriaSessao(id(empresaId), id(usuarioId), sessionId)));
  }

  private AiSession carregar(AiMemoriaSessao entidade) {
    AiSession sessao = new AiSession(entidade.getSessionId(), entidade.getEmpresaId().toString());
    historico(entidade.getId()).forEach(sessao::adicionar);
    return sessao;
  }
}
