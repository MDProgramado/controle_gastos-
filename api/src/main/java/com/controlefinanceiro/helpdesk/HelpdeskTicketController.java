package com.controlefinanceiro.helpdesk;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/helpdesk")
public class HelpdeskTicketController {

  private static final int TAMANHO_MAXIMO = 100;

  private final HelpdeskService helpdeskService;

  public HelpdeskTicketController(HelpdeskService helpdeskService) {
    this.helpdeskService = helpdeskService;
  }

  @PostMapping("/tickets")
  public ResponseEntity<TicketResponse> criar(@Valid @RequestBody TicketRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(helpdeskService.criarTicket(request));
  }

  @GetMapping("/tickets")
  public TicketPage listar(
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return helpdeskService.listarDoUsuario(status, pagina(page, size));
  }

  @GetMapping("/tickets/{id}")
  public TicketDetalheResponse buscar(@PathVariable Long id) {
    return helpdeskService.buscarDoUsuario(id);
  }

  @PostMapping("/tickets/{id}/mensagens")
  public ResponseEntity<TicketMensagemResponse> responder(
      @PathVariable Long id, @Valid @RequestBody TicketMensagemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(helpdeskService.responderComoUsuario(id, request));
  }

  private PageRequest pagina(int page, int size) {
    if (page < 0 || size < 1) {
      throw new IllegalArgumentException("page e size devem ser positivos");
    }
    Sort ordem = Sort.by(Sort.Direction.DESC, "criadoEm").and(Sort.by(Sort.Direction.DESC, "id"));
    return PageRequest.of(page, Math.min(size, TAMANHO_MAXIMO), ordem);
  }
}
