package com.controlefinanceiro.categoria.grupo;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.CategoriaNaoEncontradaException;
import com.controlefinanceiro.common.CategoriaTipoIncompativelException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaGrupo503020Service {

  public static final String NECESSIDADE = "NECESSIDADE";
  public static final String DESEJO = "DESEJO";
  public static final String FUTURO = "FUTURO";
  public static final String PADRAO = DESEJO;

  private static final Set<String> GRUPOS = Set.of(NECESSIDADE, DESEJO, FUTURO);

  private final CategoriaRepository categoriaRepository;
  private final CategoriaGrupo503020Repository grupoRepository;

  public CategoriaGrupo503020Service(
      CategoriaRepository categoriaRepository, CategoriaGrupo503020Repository grupoRepository) {
    this.categoriaRepository = categoriaRepository;
    this.grupoRepository = grupoRepository;
  }

  @Transactional(readOnly = true)
  public List<CategoriaGrupo503020Response> listar(Long empresaId) {
    Map<Long, String> grupos =
        grupoRepository.findByEmpresaId(empresaId).stream()
            .collect(
                Collectors.toMap(
                    CategoriaGrupo503020::getCategoriaId,
                    CategoriaGrupo503020::getGrupo,
                    (a, b) -> a));

    return categoriaRepository.findByEmpresaId(empresaId).stream()
        .filter(Categoria::isAtivo)
        .filter(categoria -> "SAIDA".equalsIgnoreCase(categoria.getTipo()))
        .map(
            categoria -> {
              String grupo = grupos.get(categoria.getId());
              boolean padrao = grupo == null;
              return new CategoriaGrupo503020Response(
                  categoria.getId(),
                  categoria.getNome(),
                  categoria.getTipo(),
                  padrao ? PADRAO : grupo,
                  padrao);
            })
        .toList();
  }

  @Transactional
  public CategoriaGrupo503020Response salvar(Long empresaId, Long categoriaId, String grupo) {
    validarGrupo(grupo);
    Categoria categoria =
        categoriaRepository
            .findByIdAndEmpresaId(categoriaId, empresaId)
            .orElseThrow(
                () ->
                    new CategoriaNaoEncontradaException("Categoria não encontrada para o tenant."));
    if (!"SAIDA".equalsIgnoreCase(categoria.getTipo())) {
      throw new CategoriaTipoIncompativelException(
          "Apenas categorias de SAÍDA podem ser mapeadas no 50/30/20.");
    }

    CategoriaGrupo503020 mapeamento =
        grupoRepository
            .findByEmpresaIdAndCategoriaId(empresaId, categoriaId)
            .orElseGet(
                () -> {
                  CategoriaGrupo503020 novo = new CategoriaGrupo503020();
                  novo.setEmpresaId(empresaId);
                  novo.setCategoriaId(categoriaId);
                  return novo;
                });
    mapeamento.setGrupo(grupo);
    grupoRepository.save(mapeamento);

    return new CategoriaGrupo503020Response(
        categoria.getId(), categoria.getNome(), categoria.getTipo(), grupo, false);
  }

  private void validarGrupo(String grupo) {
    if (!GRUPOS.contains(grupo)) {
      throw new IllegalArgumentException("Grupo inválido. Use NECESSIDADE, DESEJO ou FUTURO.");
    }
  }
}
