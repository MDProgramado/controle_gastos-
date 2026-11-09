package com.controlefinanceiro.categoria;

import java.util.List;
import java.util.Optional;
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

  /**
   * Busca categoria por id dentro da empresa (isolamento de tenant).
   *
   * @param id id da categoria
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query("select c from Categoria c where c.id = :id and c.empresa.id = :empresaId")
  Optional<Categoria> findByIdAndEmpresaId(
      @Param("id") Long id, @Param("empresaId") Long empresaId);

  /**
   * Verifica se já existe categoria com mesmo nome, tipo e empresa (unicidade por empresa_id + nome
   * + tipo).
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   * @param nome nome da categoria
   * @param tipo tipo da categoria (ENTRADA | SAIDA)
   */
  @Query(
      "select count(c) > 0 from Categoria c where c.empresa.id = :empresaId and c.nome = :nome and c.tipo = :tipo")
  boolean existsByEmpresaIdAndNomeAndTipo(
      @Param("empresaId") Long empresaId, @Param("nome") String nome, @Param("tipo") String tipo);
}
