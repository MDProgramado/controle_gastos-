package com.controlefinanceiro.whatsapp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link UsuarioTelefone}. Busca por número E.164 (identifica o remetente no
 * webhook) e por empresa (tenant).
 */
@Repository
public interface UsuarioTelefoneRepository extends JpaRepository<UsuarioTelefone, Long> {

  Optional<UsuarioTelefone> findByNumeroE164(String numeroE164);

  Optional<UsuarioTelefone> findFirstByUsuarioIdAndAtivoTrue(Long usuarioId);

  @Query("select t from UsuarioTelefone t where t.empresa.id = :empresaId and t.ativo = true")
  java.util.List<UsuarioTelefone> findByEmpresaIdAndAtivoTrue(@Param("empresaId") Long empresaId);
}
