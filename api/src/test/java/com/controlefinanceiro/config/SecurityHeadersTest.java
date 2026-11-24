package com.controlefinanceiro.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlefinanceiro.usuario.UsuarioRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfig.class, SecurityHeadersTest.TestConfig.class})
class SecurityHeadersTest {

  @Autowired private WebApplicationContext context;

  @Autowired private FilterChainProxy springSecurityFilterChain;

  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
  }

  @Test
  void respostaComOriginExternoNaoExpoeCorsETemHeadersDeSeguranca() throws Exception {
    MvcResult resultado =
        mvc.perform(get("/api/health").secure(true).header("Origin", "https://externo.example"))
            .andExpect(status().isOk())
            .andReturn();

    var resposta = resultado.getResponse();
    assertThat(resposta.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(resposta.getHeader("X-Frame-Options")).isEqualTo("DENY");
    assertThat(resposta.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    assertThat(resposta.getHeader("Access-Control-Allow-Origin")).isNull();

    String hsts = resposta.getHeader("Strict-Transport-Security");
    assertThat(hsts).as("HSTS").isNotNull().contains("includeSubDomains");
    Matcher maxAge = Pattern.compile("max-age=(\\d+)").matcher(hsts);
    assertThat(maxAge.find()).as("HSTS com max-age: %s", hsts).isTrue();
    assertThat(Long.parseLong(maxAge.group(1))).isGreaterThanOrEqualTo(31_536_000L);

    assertThat(resposta.getHeader("Cache-Control")).isNull();
    assertThat(resposta.getHeader("Pragma")).isNull();
    assertThat(resposta.getHeader("Expires")).isNull();
  }

  @TestConfiguration
  @EnableWebMvc
  static class TestConfig {

    @Bean
    UsuarioRepository usuarioRepository() {
      return mock(UsuarioRepository.class);
    }

    @Bean
    JwtDecoder jwtDecoder() {
      return token -> {
        throw new JwtException("decoder de teste sem uso neste slice");
      };
    }

    @RestController
    static class HealthEndpoint {

      @GetMapping("/api/health")
      String health() {
        return "OK";
      }
    }
  }
}
