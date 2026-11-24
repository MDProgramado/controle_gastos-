package com.controlefinanceiro.ai.tools;

import com.controlefinanceiro.ai.AiResponse;
import java.util.List;

public record ResultadoFerramenta(String conteudo, List<AiResponse.AiCitacao> citacoes) {

  public ResultadoFerramenta(String conteudo) {
    this(conteudo, List.of());
  }
}
