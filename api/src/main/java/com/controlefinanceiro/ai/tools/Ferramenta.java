package com.controlefinanceiro.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;

public interface Ferramenta {

  String nome();

  ResultadoFerramenta executar(JsonNode args, ContextoFerramenta contexto);
}
