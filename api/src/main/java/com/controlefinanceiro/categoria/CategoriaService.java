package com.controlefinanceiro.categoria;

import com.controlefinanceiro.common.CategoriaDuplicadaException;
import com.controlefinanceiro.common.CategoriaNaoEncontradaException;
import com.controlefinanceiro.common.CategoriaTipoIncompativelException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.empresa.Empresa;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de categorias (segue o padrão de {@link com.controlefinanceiro.auth.AuthService}). R3:
 * empresa_id vem exclusivamente de {@link TenantContext#getEmpresaId()}, nunca do body.
 */
@Service
public class CategoriaService {

  private final CategoriaRepository categoriaRepository;

  private final EntityManager entityManager;

  public CategoriaService(CategoriaRepository categoriaRepository, EntityManager entityManager) {
    this.categoriaRepository = categoriaRepository;
    this.entityManager = entityManager;
  }

  /**
   * Lista categorias da empresa autenticada.
   *
   * @param empresaId tenant extraído do token via TenantContext
   */
  @Transactional(readOnly = true)
  public List<CategoriaResponse> listar(Long empresaId) {
    return categoriaRepository.findByEmpresaId(empresaId).stream()
        .map(CategoriaResponse::new)
        .toList();
  }

  /**
   * Cria uma nova categoria para a empresa autenticada.
   *
   * @param empresaId tenant extraído do token via TenantContext
   * @param request dados da categoria a criar
   * @return resposta da categoria criada
   */
  @Transactional
  public CategoriaResponse criar(Long empresaId, CategoriaRequest request) {
    validarTipo(request.tipo());

    if (categoriaRepository.existsByEmpresaIdAndNomeAndTipo(
        empresaId, request.nome(), request.tipo())) {
      throw new CategoriaDuplicadaException(
          "Já existe categoria com o nome '%s' e tipo '%s' nesta empresa"
              .formatted(request.nome(), request.tipo()));
    }

    Categoria categoria = new Categoria();
    categoria.setEmpresa(entityManager.getReference(Empresa.class, empresaId));
    categoria.setNome(request.nome());
    categoria.setTipo(request.tipo());
    categoria.setPessoal(request.pessoal());
    categoria.setAtivo(true);

    return new CategoriaResponse(categoriaRepository.save(categoria));
  }

  /**
   * Edita uma categoria existente da empresa autenticada.
   *
   * @param id id da categoria
   * @param empresaId tenant extraído do token via TenantContext
   * @param request novos dados da categoria
   * @return resposta da categoria atualizada
   */
  @Transactional
  public CategoriaResponse editar(Long id, Long empresaId, CategoriaRequest request) {
    validarTipo(request.tipo());

    Categoria categoria =
        categoriaRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(
                () ->
                    new CategoriaNaoEncontradaException(
                        "Categoria não encontrada: id=%d, empresa=%d".formatted(id, empresaId)));

    categoria.setNome(request.nome());
    categoria.setTipo(request.tipo());
    categoria.setPessoal(request.pessoal());

    return new CategoriaResponse(categoriaRepository.save(categoria));
  }

  /**
   * Desativa uma categoria existente da empresa autenticada (define ativo=false). Lançamentos
   * históricos permanecem visíveis — não há exclusão em cascata.
   *
   * @param id id da categoria
   * @param empresaId tenant extraído do token via TenantContext
   */
  @Transactional
  public void desativar(Long id, Long empresaId) {
    Categoria categoria =
        categoriaRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(
                () ->
                    new CategoriaNaoEncontradaException(
                        "Categoria não encontrada: id=%d, empresa=%d".formatted(id, empresaId)));

    categoria.setAtivo(false);
    categoriaRepository.save(categoria);
  }

  private void validarTipo(String tipo) {
    if (!"ENTRADA".equals(tipo) && !"SAIDA".equals(tipo)) {
      throw new CategoriaTipoIncompativelException(
          "Tipo de categoria inválido: '%s'. Valores permitidos: ENTRADA, SAIDA".formatted(tipo));
    }
  }
}
