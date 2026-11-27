package com.controlefinanceiro.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ProblemDetailExceptionHandlerTest {

  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc =
        MockMvcBuilders.standaloneSetup(new DummyController())
            .setControllerAdvice(new ProblemDetailExceptionHandler())
            .build();
  }

  @Test
  void rotaInexistenteRetorna404ProblemDetail() throws Exception {
    mvc.perform(get("/nao-existe"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void metodoNaoSuportadoRetorna405ProblemDetail() throws Exception {
    mvc.perform(get("/somente-post"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
  }

  @Test
  void tipoInvalidoDePathVariableRetorna400ProblemDetail() throws Exception {
    mvc.perform(get("/api/lancamentos/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @RestController
  static class DummyController {
    @PostMapping("/somente-post")
    void somentePost() {}

    @GetMapping("/api/lancamentos/{id}")
    void lancamento(@org.springframework.web.bind.annotation.PathVariable Long id) {}

    @PostMapping("/eco")
    void eco(@RequestBody String body) {}
  }
}
