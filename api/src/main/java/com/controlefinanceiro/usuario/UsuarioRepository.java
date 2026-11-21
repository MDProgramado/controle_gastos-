package com.controlefinanceiro.usuario;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  /**
   * {@code join fetch} na empresa: com {@code open-in-view=false}, a lazy association precisa estar
   * carregada na mesma consulta — o login lê {@code empresa.id} para a claim {@code empresa_id} do
   * JWT (key-link do plano).
   */
  @Query("select u from Usuario u left join fetch u.empresa where u.email = :email")
  Optional<Usuario> findByEmail(@Param("email") String email);

  @Query(
      "select u from Usuario u left join fetch u.empresa where u.id = :id and u.empresa.id ="
          + " :empresaId")
  Optional<Usuario> findByIdAndEmpresaId(@Param("id") Long id, @Param("empresaId") Long empresaId);

  @Query(
      "select u from Usuario u left join fetch u.empresa where u.empresa.id = :empresaId and"
          + " u.email = :email")
  Optional<Usuario> findByEmpresaIdAndEmail(
      @Param("empresaId") Long empresaId, @Param("email") String email);

  Optional<Usuario> findByGoogleSub(String googleSub);
}
