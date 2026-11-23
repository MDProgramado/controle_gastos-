package com.controlefinanceiro.ai;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiMemoriaMensagemRepository extends JpaRepository<AiMemoriaMensagem, Long> {

  List<AiMemoriaMensagem> findBySessaoIdOrderByIdAsc(Long sessaoId);

  /** Últimas 20 mensagens para o prompt — ordem invertida no {@link AiMemoryStore}. */
  List<AiMemoriaMensagem> findTop20BySessaoIdOrderByIdDesc(Long sessaoId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from AiMemoriaMensagem m where m.sessao.id = :sessaoId")
  void deleteBySessaoId(@Param("sessaoId") Long sessaoId);
}
