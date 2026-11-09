package com.controlefinanceiro.categoria;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de categoria (padrão P1; segue {@link
 * com.controlefinanceiro.usuario.UsuarioRepository}). Filtra por empresa_id (tenant, R3) e ordena
 * por nome.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

  /**
   * Categorias de uma empresa, ordenadas por nome. Usa o índice existente idx_cat_empresa_tipo.
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query("select c from Categoria c where c.empresa.id = :empresaId order by c.nome asc")
  List<Categoria> findByEmpresaId(@Param("empresaId") Long empresaId);
}
