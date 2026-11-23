package com.controlefinanceiro.usuario;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConviteRepository extends JpaRepository<Convite, Long> {

  @Query("select c from Convite c left join fetch c.empresa where c.tokenHash = :tokenHash")
  Optional<Convite> findByTokenHash(@Param("tokenHash") String tokenHash);

  @Query(
      "select c from Convite c where c.empresa.id = :empresaId and c.email = :email and c.usadoEm"
          + " is null")
  Optional<Convite> findByEmpresaIdAndEmail(
      @Param("empresaId") Long empresaId, @Param("email") String email);
}
