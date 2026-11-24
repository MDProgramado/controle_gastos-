package com.controlefinanceiro.categoria.grupo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaGrupo503020Repository extends JpaRepository<CategoriaGrupo503020, Long> {

  List<CategoriaGrupo503020> findByEmpresaIdAndCategoriaIdIn(
      Long empresaId, List<Long> categoriaIds);

  List<CategoriaGrupo503020> findByEmpresaId(Long empresaId);

  Optional<CategoriaGrupo503020> findByEmpresaIdAndCategoriaId(Long empresaId, Long categoriaId);
}
