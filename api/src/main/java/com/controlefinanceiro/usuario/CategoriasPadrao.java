package com.controlefinanceiro.usuario;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.empresa.Empresa;
import java.util.List;

public final class CategoriasPadrao {

  private static final Object[][] ITENS = {
    new Object[] {"Alimentação", "SAIDA", true},
    new Object[] {"Saúde/farmácia", "SAIDA", true},
    new Object[] {"Lazer", "SAIDA", true},
    new Object[] {"Transporte", "SAIDA", false},
    new Object[] {"Moradia", "SAIDA", false},
    new Object[] {"Educação", "SAIDA", false},
    new Object[] {"Vestuário", "SAIDA", false},
    new Object[] {"Tecnologia", "SAIDA", false},
    new Object[] {"Serviços", "SAIDA", false},
    new Object[] {"Assinaturas", "SAIDA", false},
    new Object[] {"Impostos", "SAIDA", false},
    new Object[] {"Outros", "SAIDA", false},
    new Object[] {"Vendas de produtos", "ENTRADA", false},
    new Object[] {"Prestação de serviços", "ENTRADA", false},
    new Object[] {"Rendimentos e reembolsos", "ENTRADA", true},
    new Object[] {"Outras receitas", "ENTRADA", false}
  };

  private CategoriasPadrao() {}

  public static List<Categoria> para(Empresa empresa) {
    return List.of(ITENS).stream()
        .map(
            item -> {
              Categoria categoria = new Categoria();
              categoria.setEmpresa(empresa);
              categoria.setNome((String) item[0]);
              categoria.setTipo((String) item[1]);
              categoria.setPessoal((Boolean) item[2]);
              categoria.setAtivo(true);
              return categoria;
            })
        .toList();
  }
}
