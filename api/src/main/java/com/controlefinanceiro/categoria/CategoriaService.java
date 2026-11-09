package com.controlefinanceiro.categoria;

import com.controlefinanceiro.common.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de leitura de categorias (segue o padrão de {@link
 * com.controlefinanceiro.auth.AuthService}). R3: empresa_id vem exclusivamente de {@link
 * TenantContext#getEmpresaId()}, nunca do body.
 */
@Service
public class CategoriaService {

  private final CategoriaRepository categoriaRepository;

  public CategoriaService(CategoriaRepository categoriaRepository) {
    this.categoriaRepository = categoriaRepository;
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
}
