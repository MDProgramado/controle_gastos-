package com.controlefinanceiro.ai;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMemoriaSessaoRepository extends JpaRepository<AiMemoriaSessao, Long> {

  Optional<AiMemoriaSessao> findByEmpresaIdAndUsuarioIdAndSessionId(
      Long empresaId, Long usuarioId, String sessionId);

  long countByEmpresaIdAndUsuarioId(Long empresaId, Long usuarioId);
}
