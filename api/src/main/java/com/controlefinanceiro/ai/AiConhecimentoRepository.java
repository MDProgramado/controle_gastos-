package com.controlefinanceiro.ai;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiConhecimentoRepository extends JpaRepository<AiConhecimento, Long> {
  Optional<AiConhecimento> findByEmpresaId(Long empresaId);
}
