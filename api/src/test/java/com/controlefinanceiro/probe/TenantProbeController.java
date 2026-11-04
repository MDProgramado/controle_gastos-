package com.controlefinanceiro.probe;

import com.controlefinanceiro.common.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * APENAS TESTE (nunca promover para main): simula um client malicioso mandando {@code empresa_id}
 * no body. Ecoa o tenant do {@link TenantContext} — se o body definisse tenant, a resposta seria
 * 999; sendo 1, prova que só o token manda (R3/AUTH-01). Ativo somente no perfil "it".
 */
@RestController
@Profile("it")
@RequestMapping("/api/test")
public class TenantProbeController {

  /** Corpo aceito arbitrariamente (JsonNode não tem campos desconhecidos — só o eco importa). */
  @PostMapping("/tenant")
  public Map<String, Long> tenantDoToken(@RequestBody JsonNode corpoHostilIgnorado) {
    return Map.of("empresa_id", TenantContext.getEmpresaId());
  }
}
