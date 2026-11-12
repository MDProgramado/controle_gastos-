# Phase 5: Importação de PDFs e Análise por IA - Pattern Map

**Mapped:** 2026-11-12
**Files analyzed:** 48 (novos/modificados — 24 backend, 19 frontend, 5 testes)
**Analogs found:** 44 / 48 (4 sem análogo próximo)

---

## File Classification

### Backend — `api/`

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `db/migration/V3__importacoes_pdf.sql` | migration | DDL | `db/migration/V1__schema.sql` | exact |
| `importacao/ImportacaoController.java` | controller | request-response (multipart) | `lancamento/LancamentoController.java` | role-match |
| `importacao/ImportacaoService.java` | service | CRUD + orquestração | `lancamento/LancamentoService.java` | exact |
| `importacao/PdfTextoExtractor.java` | utility | file-I/O (leitura PDF) | **sem análogo** (1ª extração de PDF) | none |
| `importacao/SanitizadorTexto.java` | utility | transform (regex) | `ai/AiService.parseValor` | role-match (transform) |
| `importacao/ClassificadorDocumento.java` | utility | transform (heurística) | `ai/AiService.interpretar` | role-match |
| `importacao/IaExtracaoService.java` | service | request-response (IA) | `ai/AiService.java` | exact |
| `importacao/MapeadorCategoria.java` | utility | transform (normalização) | `web/components/ai/chat-panel.tsx#encontrarCategoriaId` | role-match |
| `importacao/Deduplicador.java` | utility | transform (SHA-256/chave) | **sem análogo** (1º hash do projeto) | none |
| `importacao/AnalisadorConsolidado.java` | service | transform (agregação) | `ai/AiContextoService.java` | role-match |
| `importacao/ImportacaoDocumento.java` | model (JPA) | CRUD | `lancamento/Lancamento.java` | exact |
| `importacao/ImportacaoItem.java` | model (JPA) | CRUD | `lancamento/Lancamento.java` | exact |
| `importacao/ImportacaoFatura.java` | model (JPA) | CRUD | `categoria/Categoria.java` | exact |
| `importacao/FaturaCartao.java` | model (JPA) | CRUD | `lancamento/Lancamento.java` | exact |
| `importacao/ContaAPagar.java` | model (JPA) | CRUD | `lancamento/Lancamento.java` | exact |
| `importacao/*Repository.java` (5 repos) | model (JPA repo) | CRUD | `lancamento/LancamentoRepository.java` | exact |
| `importacao/*Response.java` (DTOs) | DTO | request-response | `lancamento/LancamentoResponse.java` | exact |
| `importacao/ConfirmarImportacaoRequest.java` | DTO | request-response | `lancamento/LancamentoLoteRequest.java` | exact |
| `common/ArquivoInvalidoException.java` (+类似的) | exception | error | `common/MonetarioException.java` | exact |
| `config/ProblemDetailExceptionHandler.java` (mod) | config | error | — (o próprio arquivo) | exact |
| `ai/AiClient.java` (mod — cascata) | service (client) | request-response (IA) | — (o próprio arquivo) | exact |
| `ai/AiService.java` (mod — delegação) | service | request-response (IA) | — (o próprio arquivo) | exact |
| `lancamento/Lancamento.java` (mod — `origem`, `documento_id`, `chave_dedupe`) | model (JPA) | CRUD | — (o próprio arquivo) | exact |
| `lancamento/LancamentoRepository.java` (mod — query dedupe) | model (JPA repo) | CRUD | — (o próprio arquivo) | exact |
| `config/SecurityConfig.java` (mod — rota `/api/importacoes`) | config | request-response | — (o próprio arquivo) | exact |
| `resources/application.yml` (mod — cascata gratuita) | config | config | — (o próprio arquivo) | exact |

### Frontend — `web/`

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `app/importacoes/page.tsx` | page (Server Component) | request-response | `app/lancamentos/page.tsx` | exact |
| `app/api/importacoes/route.ts` | BFF route | request-response (multipart) | `app/api/lancamentos/lote/route.ts` | role-match |
| `app/api/importacoes/[id]/confirmar/route.ts` | BFF route | request-response | `app/api/ai/chat/route.ts` | exact |
| `app/api/importacoes/[id]/route.ts` (DELETE) | BFF route | request-response | `app/api/categorias/route.ts` (GET) | role-match |
| `components/ImportacoesClient.tsx` | component (Client) | state machine | `components/LancamentosPageClient.tsx` | exact |
| `components/importacoes/UploadDropzone.tsx` | component (Client) | file-I/O | **sem análogo** (1º dropzone) | none |
| `components/importacoes/ImportFileList.tsx` | component (Client) | CRUD (lista) | `components/LancamentosTable.tsx` | role-match |
| `components/importacoes/RevisaoItens.tsx` | component (Client) | CRUD (lista) | `components/LancamentosTable.tsx` | role-match |
| `components/importacoes/ImportItemRow.tsx` | component (Client) | CRUD (linha) | `components/LancamentosTable.tsx#renderRow` | exact |
| `components/importacoes/ResumoMesImportacao.tsx` | component (Client) | transform (agregação) | `components/DashboardKPIs.tsx` | exact |
| `components/importacoes/AlertasImportacao.tsx` | component (Client) | lista | `components/LancamentosTable.tsx` (Alert) | role-match |
| `components/importacoes/ResultadoImportacao.tsx` | component (Client) | request-response | `components/ai/chat-panel.tsx` (estado erro/sucesso) | role-match |
| `components/importacoes/ImportacoesPendentes.tsx` | component (Client) | CRUD (histórico) | `components/LancamentosTable.tsx` + `DeleteConfirmationDialog` | role-match |
| `components/importacoes/tipos.ts` | tipos compartilhados | — | `components/lote-tipos.ts` | exact |
| `components/ui/checkbox.tsx` (novo shadcn) | component | — | `components/ui/switch.tsx` (radix wrapper) | role-match |
| `lib/api.ts` (mod — server helpers) | lib | request-response | `lib/api.ts` (padrão existente) | exact |
| `lib/api-client.ts` (mod — client helpers) | lib | request-response | `lib/api-client.ts` (`chatComIA`/`gravarLancamentos`) | exact |
| `components/ui/sidebar.tsx` (mod — nav) | component | — | — (o próprio arquivo) | exact |
| `components/TopNav.tsx` (mod — ROTAS) | component | — | — (o próprio arquivo) | exact |
| `components/DeleteConfirmationDialog.tsx` (mod — `descartar`) | component | — | — (o próprio arquivo) | exact |

### Testes

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `test/.../importacao/ImportacaoIntegrationTest.java` | test | request-response | `test/.../ai/AiIntegrationTest.java` | exact |
| `test/.../importacao/SanitizadorTextoTest.java` | test | unit | `test/.../ai/AiServiceTest.java` | exact |
| `test/.../importacao/MapeadorCategoriaTest.java` | test | unit | `test/.../ai/AiServiceTest.java` | exact |
| `test/.../ai/AiClientCascataTest.java` (guard paid model) | test | unit | `test/.../ai/AiServiceTest.java` | role-match |
| `test/.../LancamentoDedupeIntegrationTest.java` | test | request-response | `test/.../LancamentoLoteIntegrationTest.java` | exact |

---

## Pattern Assignments

### `api/src/main/resources/db/migration/V3__importacoes_pdf.sql` (migration, DDL)

**Analog:** `api/src/main/resources/db/migration/V1__schema.sql`

O DDL novo segue o **mesmo estilo literal** da V1 — não há Java migration para DDL (a V2 é Java só porque era *seed de dados*, não schema).

**Header obrigatório** (V1 linhas 1-5):
```sql
-- Migration IMUTÁVEL (INF-01 / T-1-01): este arquivo não pode ser alterado depois de aplicado.
-- Qualquer correção exige uma nova versão V<n>__ com o DDL corrigido.
-- R3: empresa_id presente em TODAS as tabelas (multiempresa desde a Fase 2).
```

**Padrão de tabela** (V1 linhas 34-45) — `empresa_id` NOT NULL + FK, `NUMERIC(14,2)` com `CHECK (valor > 0)`:
```sql
CREATE TABLE lancamento (
  id           BIGSERIAL PRIMARY KEY,
  empresa_id   BIGINT NOT NULL REFERENCES empresa(id),   -- sempre do token
  usuario_id   BIGINT NOT NULL REFERENCES usuario(id),
  categoria_id BIGINT NOT NULL REFERENCES categoria(id),
  tipo         VARCHAR(10) NOT NULL,             -- ENTRADA | SAIDA
  valor        NUMERIC(14,2) NOT NULL CHECK (valor > 0), -- R1
  data         DATE NOT NULL,
  descricao    VARCHAR(200),
  criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ
);
```

**Padrão de índice** (V1 linhas 47-49):
```sql
CREATE INDEX idx_lanc_empresa_data ON lancamento (empresa_id, data);
CREATE INDEX idx_lanc_empresa_cat  ON lancamento (empresa_id, categoria_id);
CREATE INDEX idx_cat_empresa_tipo  ON categoria (empresa_id, tipo);
```

**Regras da V3 derivadas de D-08/D-09/D-12:**
- `importacao_documento`: `empresa_id`, `usuario_id`, `hash_sha256 VARCHAR(64) NOT NULL`, `nome_arquivo`, `tamanho_bytes BIGINT`, `status VARCHAR(20)` (`PENDENTE|SEM_TEXTO|DUPLICADO|CONFIRMADO|DESCARTADO`), `modelo_usado VARCHAR(80)`, `degradado BOOLEAN NOT NULL DEFAULT false`, `criado_em`.
- **UNIQUE (empresa_id, hash_sha256)** ⇒ D-09 (reenvio do mesmo arquivo é `DUPLICADO`, sem reprocessar).
- `importacao_item`: `documento_id FK`, `categoria_id FK NULL` (a IA pode não mapear), `tipo VARCHAR(16)` (`RECEITA|DESPESA_PAGA|A_PAGAR`), `valor NUMERIC(14,2) NOT NULL CHECK (valor > 0)`, `data DATE NOT NULL`, `descricao VARCHAR(200)`, `origem VARCHAR(10)` (`CARTAO|EXTRATO`), `parcela_atual INT`, `parcela_total INT`, `status VARCHAR(20)` (`PENDENTE|IMPORTADO|REJEITADO|DESCARTADO`), `motivo_rejeicao VARCHAR(200)`, `lancamento_id BIGINT NULL` (D-07 idempotência), `conta_a_pagar_id BIGINT NULL`, `chave_dedupe VARCHAR(64) NULL`, `selecionado BOOLEAN NOT NULL DEFAULT true`.
- `importacao_fatura`: `documento_id FK`, `total NUMERIC(14,2)`, `vencimento DATE`, `cartao_final VARCHAR(4)`.
- `fatura_cartao`: `empresa_id`, `usuario_id`, `categoria_id` (**tipo SAÍDA** — D-23), `total NUMERIC(14,2)`, `vencimento DATE`, `descricao VARCHAR(200)`, `documento_id`.
- `conta_a_pagar`: `empresa_id`, `usuario_id`, `categoria_id` (**tipo SAÍDA** — D-23), `valor NUMERIC(14,2) NOT NULL CHECK (valor > 0)`, `data DATE`, `descricao VARCHAR(200)`, `parcela_atual INT`, `parcela_total INT`, `documento_id`, `pago BOOLEAN NOT NULL DEFAULT false`.
- `ALTER TABLE lancamento ADD COLUMN origem VARCHAR(10)`, `documento_id BIGINT`, `chave_dedupe VARCHAR(64)` (D-08) + **índice único parcial por empresa**:
  `CREATE UNIQUE INDEX idx_lanc_empresa_chave_dedupe ON lancamento (empresa_id, chave_dedupe) WHERE chave_dedupe IS NOT NULL;`
- ⚠️ **NÃO** gravar nada em `lancamento.tipo` além de `ENTRADA|SAIDA` (D-05) — `A_PAGAR` fica só em `conta_a_pagar`. O `DashboardRepository` filtra `l.tipo = 'ENTRADA'/'SAIDA'`, então o saldo do dashboard permanece exato ao centavo (DSH-05).

**Regra de localização:** `application.yml` (linhas 16-19) já lista `classpath:db/migration` — o `.sql` novo é detectado automaticamente, sem mexer no yml.

---

### `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoController.java` (controller, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java`

**Imports pattern** (LancamentoController linhas 1-14):
```java
import com.controlefinanceiro.common.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
```

**Estrutura da classe** (linhas 22-30) — controller fino, delega ao service:
```java
@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

  private final LancamentoService lancamentoService;

  public LancamentoController(LancamentoService lancamentoService) {
    this.lancamentoService = lancamentoService;
  }
```

**Handlers** (linhas 32-63) — o tenant vem **sempre** de `TenantContext.getEmpresaId()`, nunca do body:
```java
  /** POST /api/lancamentos — cria lançamento. */
  @PostMapping
  public ResponseEntity<LancamentoResponse> criar(@Valid @RequestBody LancamentoRequest request) {
    return ResponseEntity.status(201)
        .body(lancamentoService.criar(TenantContext.getEmpresaId(), request));
  }

  /** DELETE /api/lancamentos/{id} — exclui lançamento. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable Long id) {
    lancamentoService.excluir(id, TenantContext.getEmpresaId());
    return ResponseEntity.noContent().build();
  }
```

**Aplicação na Fase 5:**
```java
@RestController
@RequestMapping("/api/importacoes")
public class ImportacaoController {
  // POST /pdf         multipart — @RequestParam("arquivos") List<MultipartFile>  (D-01)
  // GET  /            histórico (paginado) — TenantContext.getEmpresaId()
  // GET  /{id}        detalhe da importação + itens (para reidratar a revisão)
  // POST /{id}/confirmar  @Valid @RequestBody ConfirmarImportacaoRequest  (D-02, D-07)
  // DELETE /{id}      descarte (staging) — 204
}
```
- **Multipart:** usar `@RequestParam("arquivos") List<MultipartFile> arquivos` (nome do campo conforme D-01) — **nunca** `@RequestBody` em multipart.
- **Limite de upload:** `spring.servlet.multipart.max-file-size=8MB` / `max-request-size=10MB` no `application.yml` (D-28 diz que a API pode ser mais permissiva que o teto da Vercel de 4 MB/arquivo).
- **Segurança (D-04):** a validação de content-type + magic bytes `%PDF` + tamanho + quantidade é feita **no service**, não no controller — o controller só valida formato.

---

### `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoService.java` (service, CRUD)

**Analog:** `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java`

**Imports pattern** (LancamentoService linhas 1-22):
```java
import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.common.MonetarioException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

**Criação de entidade com tenant do token** (linhas 71-81) — padrão exato para `ImportacaoDocumento`/`ImportacaoItem`/`ContaAPagar`:
```java
    Lancamento lancamento = new Lancamento();
    lancamento.setEmpresa(entityManager.getReference(Empresa.class, empresaId));
    lancamento.setUsuario(usuarioAtual());
    lancamento.setCategoria(categoria);
    lancamento.setTipo(request.tipo());
    lancamento.setValor(request.valor());
    lancamento.setData(request.data());
    lancamento.setDescricao(request.descricao());

    Lancamento salvo = lancamentoRepository.save(lancamento);
    return new LancamentoResponse(salvo);
```

**Validação monetária** (linhas 207-219) — **reusar em `ImportacaoService`, `IaExtracaoService` e `AnalisadorConsolidado`**:
```java
  /** Valida valor monetário: escala <= 2 e positivo (LAN-04, LAN-07). */
  private void validarValor(BigDecimal valor) {
    // LAN-04: rejeita (não arredonda) se mais de 2 casas decimais
    if (valor.scale() > 2) {
      throw new MonetarioException(
          "Valor monetário deve ter no máximo 2 casas decimais. Valor informado tem %d casas."
              .formatted(valor.scale()));
    }
    // LAN-07: valor deve ser positivo
    if (valor.signum() <= 0) {
      throw new MonetaryException("Valor deve ser positivo. Valor informado: %s".formatted(valor));
    }
  }
```
> Na Fase 5 o valor **vem da IA** (string) — a escala já é 2 após `parseValor`, mas a checagem `scale() > 2` continua válida como defesa em profundidade.

**Validação categoria×tipo** (linhas 225-231) — essencial em D-23 (`A_PAGAR` ⇒ categoria **SAÍDA**):
```java
  private void validarIntegridadeCategoriaTipo(Categoria categoria, String tipoLancamento) {
    if (!categoria.getTipo().equals(tipoLancamento)) {
      throw new com.controlefinanceiro.common.CategoriaTipoIncompativelException(
          "Categoria '%s' é do tipo '%s', incompatível com lançamento do tipo '%s'"
              .formatted(categoria.getNome(), categoria.getTipo(), tipoLancamento));
    }
  }
```

**`usuarioAtual()` pelo SecurityContextHolder** (linhas 233-242) — **copiar literalmente** em `ImportacaoService`:
```java
  /** Retorna o usuário autenticado a partir do principal no SecurityContext (R3). */
  private Usuario usuarioAtual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UsuarioAuth usuarioAuth) {
      return usuarioRepository
          .findByEmail(usuarioAuth.subject())
          .orElseThrow(() -> new NaoAutorizadoException("Usuário não encontrado"));
    }
    throw new NaoAutorizadoException();
  }
```

**Transação tudo-ou-nada no lote** (linhas 185-205) — modelo exato para `confirmar()` (D-07, D-10):
```java
  @Transactional
  public List<LancamentoResponse> processarLote(Long empresaId, LancamentoLoteRequest lote) {
    List<LancamentoLoteRequest.LancamentoLinha> linhas =
        lote.linhas() == null ? List.of() : lote.linhas();

    if (linhas.size() > 500) {
      throw new LoteDemasiadoGrandeException(
          "Limite de 500 linhas excedido para lote de lançamentos");
    }

    List<LancamentoResponse> criados = new ArrayList<>(linhas.size());
    for (int i = 0; i < linhas.size(); i++) {
      try {
        criados.add(criar(empresaId, linhas.get(i).toRequest()));
      } catch (RuntimeException ex) {
        // LOT-02: reporta o índice base 0 da linha inválida e aborta o lote inteiro
        throw new LinhaInvalidaException(i, ex.getMessage());
      }
    }
    return criados;
  }
```
> Em `confirmar()` a **idempotência** (D-07) substitui o `throw`: se o item de staging já está `IMPORTADO`, **retornar o id gravado** em vez de regravar. O `@Transactional` garante rollback total se qualquer item falhar.

**Listagem com filtro** (linhas 157-171) — padrão para `GET /api/importacoes`:
```java
  @Transactional(readOnly = true)
  public LancamentoPage listar(
      Long empresaId, Integer ano, Integer mes, String tipo, Long categoriaId) {
    Page<Lancamento> page =
        lancamentoRepository.findByEmpresaIdAndFiltros(
            empresaId, ano, mes, tipo, categoriaId,
            org.springframework.data.domain.PageRequest.of(0, 50));
    List<LancamentoResponse> lista =
        page.getContent().stream().map(LancamentoResponse::new).toList();
    return new LancamentoPage(lista, page.getTotalElements());
  }
```

---

### `api/src/main/java/com/controlefinanceiro/importacao/PdfTextoExtractor.java` (utility, file-I/O)

**Sem análogo no codebase** — é a primeira extração de PDF. Usar o estilo de "componente Spring com dependência injetada" + snake_case_limpo.

**Padrão de classe do projeto** (todos os componentes são `@Component`/`@Service` com construtor explícito — `AiClient` linhas 39-79):
```java
@Component
public class AiClient {
  private final RestClient restClient;
  public AiClient(@Value("${...}") String apiKey) { ... }
}
```

**Aplicação (PDFBox, D-11):**
```java
@Component
public class PdfTextoExtractor {
  private static final int TAMANHO_MAX_CHARS_POR_CHUNK = 12000; // chunking (D-18)

  public ResultadoExtracao extrair(InputStream entrada) throws IOException {
    try (PDDocument doc = Loader.loadPDF(entrada)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true); // dica do CONTEXT §Specific Ideas
      String texto = stripper.getText(doc);
      if (texto == null || texto.isBlank()) {
        return new ResultadoExtracao(SemTexto.INSTANCE, List.of(), List.of());
      }
      // normalizar espaços múltiplos antes de enviar à IA
    }
  }
}
```
- **D-12:** PDF sem texto ⇒ resultado `SEM_TEXTO` — **não é exceção**, é um estado do documento; o lote continua.
- **D-14:** o PDF bruto **não é persistido** — só o hash SHA-256 (calculado sobre os bytes *antes* de carregar no PDFBox) e metadados.
- **D-01/D-04:** validar magic bytes `%PDF` lendo os **primeiros 4 bytes** do stream (`"PK"` não, `"%PDF"` sim), antes de tentar parsear.

---

### `api/src/main/java/com/controlefinanceiro/importacao/SanitizadorTexto.java` (utility, transform)

**Analog:** `api/src/main/java/com/controlefinanceiro/ai/AiService.java` (`parseValor`, linhas 334-348) — mesma natureza: função pura de transformação de string, defensiva, sem exceção.

**Padrão de método utilitário no projeto** (AiService linhas 334-348):
```java
  /** Converte o texto em BigDecimal com escala 2 e HALF_EVEN (R1 — sem double/float). */
  private BigDecimal parseValor(String s) {
    if (s == null) {
      return ZERO;
    }
    String limpo = s.trim().replace(",", ".").replaceAll("[^0-9.]", "");
    if (limpo.isBlank()) {
      return ZERO;
    }
    try {
      return new BigDecimal(limpo).setScale(2, RoundingMode.HALF_EVEN);
    } catch (NumberFormatException e) {
      return ZERO;
    }
  }
```

**Aplicação (D-13):** mascarar **antes** de qualquer chamada à OpenRouter:
- CPF → `***.***.***-**`
- CNPJ → `**.***.***/****-**`
- cartão (13–19 dígitos) → últimos 4 dígitos apenas (`**** **** **** 1234`)
- agência/conta → mascarar
- ⚠️ A ordem importa: **cartão primeiro** (sequência longa), depois CPF/CNPJ, para o cartão não ser quebrado em pedaços.

---

### `api/src/main/java/com/controlefinanceiro/importacao/IaExtracaoService.java` (service, request-response IA)

**Analog:** `api/src/main/java/com/controlefinanceiro/ai/AiService.java`

**Padrão de prompt (system message) — text block** (AiService linhas 94-132):
```java
    String systemPrompt =
        """
      Você é um assistente financeiro de uma empresa. Responda em português do Brasil.
      Data de hoje: %s (America/Sao_Paulo). ...

      Regras de ouro:
      - Nunca invente valores financeiros. ...
      """
            .formatted(hoje, hoje, hoje, dadosReais, knowledgeStore.obterConhecimento());

    mensagens.add(Map.of("role", "system", "content", systemPrompt));
```

**Padrão de parse defensivo — item inválido não derruba o documento** (D-20, IMP-06): no `AiService`, `parseLancamento` devolve `null` em campo inválido (linhas 241-274):
```java
  private AiResponse.AiPropostaLancamento parseLancamento(String linha) {
    String[] partes = linha.split("\\|", -1);
    if (partes.length < 4) {
      return null;
    }
    String tipo = partes[1].trim().toUpperCase();
    if (!tipo.equals("ENTRADA") && !tipo.equals("SAIDA")) {
      return null;
    }
    BigDecimal valor = parseValor(partes[2]);
    if (valor.signum() <= 0) {
      return null;
    }
    ...
  }
```
> Na Fase 5, cada item inválido vira uma entrada na lista `rejeitados` com **motivo**, e o restante do documento é preservado.

**Padrão de resposta da IA — record com nested records** (AiResponse linhas 16-32):
```java
public record AiResponse(
    String intent,
    String resposta,
    List<AiConsultaDado> dados,
    AiPropostaLancamento proposta,
    List<AiPropostaLancamento> propostas,
    List<String> opcoes) {

  public record AiConsultaDado(String label, BigDecimal valor) {}

  public record AiPropostaLancamento(
      String tipo, BigDecimal valor, String data, String categoriaId,
      String categoriaSugerida, String descricao) {}
}
```

**Aplicação (D-20):** o prompt da Fase 5 injeta a **lista exata das categorias do tenant** (proíbe inventar) e exige **um único objeto JSON sem markdown**. Contrato esperado (CONTEXT §Specific Ideas):
```json
{"itens":[{"data":"2026-11-03","descricao":"Supermercado X","valor":"123.45","tipo":"DESPESA_PAGA","categoria":"Alimentação","origem":"EXTRATO","parcelaAtual":null,"parcelaTotal":null}],"fatura":{"total":"1500.00","vencimento":"2026-12-10"}}
```
- O parse **remove cercas ```** e valida campo a campo.
- `valor` chega como **string** → converter com `new BigDecimal(s).setScale(2, RoundingMode.HALF_EVEN)` (nunca `Double.parseDouble`).
- **D-18:** `temperature = 0.0`, `max_tokens = 8000` (reduzir para 4000 se o modelo rejeitar).

---

### `api/src/main/java/com/controlefinanceiro/importacao/MapeadorCategoria.java` (utility, transform)

**Analog:** `web/components/ai/chat-panel.tsx` — `normalizarTexto` + `encontrarCategoriaId` (linhas 57-78). A **mesma lógica já existe no front**; o backend precisa da sua própria versão (a IA roda no servidor).

```tsx
/** Normaliza texto para comparar nomes ignorando acentos e caixa. */
function normalizarTexto(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
}

/**
 * Procura a categoria do banco que corresponde ao nome sugerido pela IA (ignora acentos e
 * caixa). Devolve null quando não há correspondência — nesse caso o usuário escolhe.
 */
function encontrarCategoriaId(
  nome: string | undefined,
  categorias: Array<{ id: number; nome: string }>,
): string | null {
  if (!nome) return null
  const alvo = normalizarTexto(nome)
  const achada = categorias.find((c) => normalizarTexto(c.nome) === alvo)
  return achada ? String(achada.id) : null
}
```

**Equivalente Java (D-24):** `java.text.Normalizer.normalize(s, Normalizer.Form.NFD)` + `replaceAll("\\p{M}", "")` + `toLowerCase(Locale.ROOT)`. Sinônimos: `saude`→`Saúde/farmácia`, `alimentacao`→`Alimentação`, `salario`→`Rendimentos e reembolsos`.

**Fallback (D-24):** sem correspondência ⇒ `Outros` (SAÍDA) / `Outras receitas` (ENTRADA) + sinalizar `revisarCategoria = true` (gera o alerta `REVISAR_CATEGORIA`).

---

### `api/src/main/java/com/controlefinanceiro/importacao/Deduplicador.java` (utility, transform)

**Sem análogo** — é o primeiro uso de hash do projeto (nenhuma ocorrência de `MessageDigest`/`sha256` no codebase).

**Padrão de projeto para utilidade sem estado:** componente Spring `@Component` com construtor vazio, sem injeção de dependências (ex.: `AiKnowledgeStore`, `AiMemoryStore` — classes `@Component` simples).

**Regras (D-08/D-09):**
- **SHA-256 do arquivo** (hex, 64 chars) → `importacao_documento.hash_sha256`; UNIQUE `(empresa_id, hash_sha256)` ⇒ status `DUPLICADO` sem reprocessar.
- **`chave_dedupe` do lançamento** = SHA-256 de `tipo + data + valor + categoria` — `valor` **sempre** com `setScale(2, HALF_EVEN)` antes do hash, senão `10.00` e `10.0` geram chaves diferentes.
- Índice único parcial: `CREATE UNIQUE INDEX ... ON lancamento (empresa_id, chave_dedupe) WHERE chave_dedupe IS NOT NULL` (D-08).
- A consulta de verificação reusa o padrão de filtro do `LancamentoRepository`:
```java
  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.chaveDedupe = :chaveDedupe")
  Optional<Lancamento> findByEmpresaIdAndChaveDedupe(
      @Param("empresaId") Long empresaId, @Param("chaveDedupe") String chaveDedupe);
```

---

### `api/src/main/java/com/controlefinanceiro/importacao/AnalisadorConsolidado.java` (service, transform)

**Analog:** `api/src/main/java/com/controlefinanceiro/ai/AiContextoService.java`

**Padrão de normalização monetária** (AiContextoService linhas 138-150):
```java
  private BigDecimal normalizar(BigDecimal valor) {
    if (valor == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }
    return valor.setScale(2, RoundingMode.HALF_EVEN);
  }

  private String formatarMoeda(BigDecimal valor) {
    java.text.DecimalFormat formato =
        new java.text.DecimalFormat(
            "#,##0.00", java.text.DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
    return "R$ " + formato.format(normalizar(valor));
  }
```

**Aplicação (D-25/D-26):**
- Totais do mês: `totalRecebido` (RECEITA), `totalPago` (DESPESA_PAGA), `totalComprometido` (A_PAGAR), `saldoPrevisto = recebido − pago`.
- **Divergência**: `|somaItens − totalFatura| > 0.05` ⇒ alerta `DIVERGENCIA_SOMA`. Comparar com `BigDecimal.compareTo` / `abs()`, **nunca** `double`.
- **Gasto atípico (D-26)**: `valor > 2 × mediana da categoria` — mediana calculada em `BigDecimal` (ordenar a lista de `BigDecimal` e escolher o elemento do meio; para lista par, média dos dois com `RoundingMode.HALF_EVEN`).
- **Duplicidade no lote**: items com mesma `(data, valor, descrição normalizada)` ⇒ alerta `DUPLICIDADE`.
- **Pagamento da própria fatura**: item `DESPESA_PAGA` com valor ≈ total da fatura e descrição contendo a palavra da fatura ⇒ alerta `PAGAMENTO_FATURA`.

---

### `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoDocumento.java` / `ImportacaoItem.java` / `ImportacaoFatura.java` / `FaturaCartao.java` / `ContaAPagar.java` (model, JPA)

**Analog:** `api/src/main/java/com/controlefinanceiro/lancamento/Lancamento.java`

**Cabeçalho de imports + classe** (Lancamento linhas 1-27):
```java
package com.controlefinanceiro.lancamento;

import com.controlefinanceiro.empresa.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidade da tabela lancamento (padrão P1; DDL do PLAN §3.4). R3: empresa via {@link Empresa}
 * (@ManyToOne), nunca de body. LAN-05: valor em BigDecimal com NUMERIC(14,2), nenhum double/float.
 */
@Entity
@Table(name = "lancamento")
public class Lancamento {
```

**Tenant como `@ManyToOne` LAZY** (Lancamento linhas 33-36):
```java
  /** R3: tenant sempre do token — nunca de body; NOT NULL no schema. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;
```

**Valor monetário** (Lancamento linhas 49-51):
```java
  /** LAN-05: NUMERIC(14,2) no banco, scale=2, nenhum double/float. */
  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal valor;
```

**Timestamps** (Lancamento linhas 59-75):
```java
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em")
  private OffsetDateTime atualizadoEm;

  @PrePersist
  void aoPersistir() {
    if (criadoEm == null) {
      criadoEm = OffsetDateTime.now();
    }
  }

  @PreUpdate
  void aoAtualizar() {
    atualizadoEm = OffsetDateTime.now();
  }
```

⚠️ **Atenção ao `@ManyToOne` com `optional=false`:** `ImportacaoItem.categoria_id` é **NULLABLE** (a IA pode não mapear a categoria até a revisão do usuário) → usar `@ManyToOne(fetch = FetchType.LAZY)` **sem** `optional = false` e `@JoinColumn(name = "categoria_id", nullable = true)`.

Getters/setters: **um por campo, boilerplate explícito** (sem Lombok no projeto) — ver Lancamento linhas 77-156.

---

### `api/src/main/java/com/controlefinanceiro/importacao/*Repository.java` (model, JPA repo)

**Analog:** `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java`

**Estrutura + filtro por tenant** (LancamentoRepository linhas 15-37):
```java
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.data between :dataInicio and :dataFim order by l.data desc")
  List<Lancamento> findByEmpresaIdAndDataBetween(
      @Param("empresaId") Long empresaId,
      @Param("dataInicio") java.time.LocalDate dataInicio,
      @Param("dataFim") java.time.LocalDate dataFim);
```

**Busca por id + tenant (isolamento)** — `CategoriaRepository` linhas 30-33:
```java
  @Query("select c from Categoria c where c.id = :id and c.empresa.id = :empresaId")
  Optional<Categoria> findByIdAndEmpresaId(
      @Param("id") Long id, @Param("empresaId") Long empresaId);
```

**Contagem/flag de existência** — `CategoriaRepository` linhas 42-45:
```java
  @Query(
      "select count(c) > 0 from Categoria c where c.empresa.id = :empresaId and c.nome = :nome and c.tipo = :tipo")
  boolean existsByEmpresaIdAndNomeAndTipo(
      @Param("empresaId") Long empresaId, @Param("nome") String nome, @Param("tipo") String tipo);
```

⚠️ `DashboardRepository` (linhas 18-19) usa `@Repository` explícito; `LancamentoRepository`/`CategoriaRepository` **não** usam — seguir o padrão do pacote (sem `@Repository`).

---

### `api/src/main/java/com/controlefinanceiro/importacao/*Response.java` (DTO, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoResponse.java`

**Record imutável + construtor de entidade** (LancamentoResponse linhas 11-29):
```java
/**
 * Resposta de lançamento (DTO de saída). Segue o padrão de {@link
 * com.controlefinanceiro.auth.LoginResponse} (record imutável). R3 gate: SEM campo empresa_id — o
 * tenant vem exclusivamente do token JWT.
 */
public record LancamentoResponse(
    Long id,
    String tipo,
    BigDecimal valor,
    LocalDate data,
    String descricao,
    String categoriaNome,
    String categoriaTipo) {

  public LancamentoResponse(Lancamento lancamento) {
    this(
        lancamento.getId(),
        lancamento.getTipo(),
        lancamento.getValor(),
        lancamento.getData(),
        lancamento.getDescricao(),
        lancamento.getCategoria().getNome(),
        lancamento.getCategoria().getTipo());
  }
}
```
⚠️ **R3 gate estrutural:** nenhum DTO de resposta ou request pode ter `empresa_id`. O `LancamentoRequest` declara isso explicitamente (LancamentoRequest linhas 9-16):
```java
/**
 * DTO de entrada para criar/editar lançamento (DTO de entrada, padrão P4).
 *
 * <p><strong>R3 — gate estrutural:</strong> NUNCA campo empresa_id — o tenant vem exclusivamente do
 * token JWT via {@link com.controlefinanceiro.common.TenantContext#getEmpresaId()}.
 *
 * <p>LAN-04: {@code @Digits(integer=12, fraction=2)} rejeita precoce valores com mais de 2 casas
 * decimais; o serviço faz defesa em profundidade com {@code scale() > 2}.
 */
public record LancamentoRequest(
    @NotNull Long categoriaId,
    @NotNull String tipo,
    @NotNull @Digits(integer = 12, fraction = 2) BigDecimal valor,
    @NotNull LocalDate data,
    String descricao) {}
```

**DTO de lote com linha aninhada + `toRequest()`** (LancamentoLoteRequest linhas 18-28):
```java
public record LancamentoLoteRequest(List<LancamentoLinha> linhas) {

  /** Uma linha do lote — mesmos campos de {@link LancamentoRequest}, sem tenant. */
  public record LancamentoLinha(
      Long categoriaId, String tipo, BigDecimal valor, LocalDate data, String descricao) {

    /** Converte a linha do lote para o DTO unitário reutilizado pelo service. */
    public LancamentoRequest toRequest() {
      return new LancamentoRequest(categoriaId, tipo, valor, data, descricao);
    }
  }
}
```
> `ConfirmarImportacaoRequest` segue esse shape: `record ConfirmarImportacaoRequest(List<ItemConfirmado> itens)` com `ItemConfirmado(Long itemId, Long categoriaId, Boolean selecionado)`. **O `itemId` é o id do staging** — é ele que torna a confirmação idempotente (D-07).

**Serialização de BigDecimal:** `application.yml` (linhas 20-28) já garante JSON "plain" (sem notação científica) — os valores chegam na web como **string**:
```yaml
  jackson:
    time-zone: America/Sao_Paulo
    deserialization:
      fail-on-unknown-properties: true
    serialization:
      write-bigdecimal-as-plain: true
```
⚠️ `fail-on-unknown-properties: true` ⇒ **qualquer campo extra no body JSON é 400**. O payload de confirmação deve conter **exatamente** os campos do record.

---

### `api/src/main/java/com/controlefinanceiro/common/*Exception.java` (exception, error)

**Analog:** `api/src/main/java/com/controlefinanceiro/common/MonetarioException.java` (12 linhas) e `ServicoIaIndisponivelException.java` (18 linhas)

**Padrão mínimo** (MonetarioException linhas 1-12):
```java
package com.controlefinanceiro.common;

/**
 * Exceção de validação monetária (LAN-04, LAN-07): lançada quando o valor tem mais de 2 casas
 * decimais (rejeitado, nunca arredondado) ou quando o valor não é positivo.
 */
public class MonacoException extends RuntimeException {

  public MonetaryException(String mensagem) {
    super(mensagem);
  }
}
```

**Padrão com causa** (ServicoIaIndisponivelException linhas 9-17):
```java
public class ServicoIaIndisponivelException extends RuntimeException {

  public ServicoIaIndisponivelException(String mensagem) {
    super(mensagem);
  }

  public ServicoIaIndisponivelException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
```

**Exceções novas sugeridas:** `ArquivoInvalidoException` (tipo/tamanho/quantidade/magic bytes — D-04), `ImportacaoNaoEncontradaException` (404), `ImportacaoJaConfirmadaException` (idempotência — mas **não** deve ser erro; devolver o estado atual).

---

### `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` (mod, error)

**Analog:** o próprio arquivo.

**Padrão de handler** (linhas 63-67, 74-79, 96-103):
```java
  /** Valor monetário inválido (>2 casas decimais ou <= 0) → 400 ProblemDetail (LAN-04, LAN-07). */
  @ExceptionHandler(MonetarioException.class)
  public ProblemDetail monetarioInvalido(MonetarioException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** Categoria não encontrada para a empresa autenticada → 404 ProblemDetail. */
  @ExceptionHandler(CategoriaNaoEncontradaException.class)
  public ProblemDetail categoriaNaoEncontrada(CategoriaNaoEncontradaException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  /**
   * Serviço de IA (OpenRouter) indisponível ou sobrecarregado → 503 com mensagem que o usuário pode
   * entender (fase 4).
   */
  @ExceptionHandler(ServicoIaIndisponivelException.class)
  public ProblemDetail servicoIaIndisponivel(ServicoIaIndisponivelException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }
```
⚠️ **`ServicoIaIndisponivelException` já está mapeado para 503** (D-22) — a falha total da cascata **reutiliza** essa exceção, sem novo handler.

⚠️ **404 de importação**: `CategoriaNaoEncontradaException` resolve o caso, mas semanticamente é melhor uma `ImportacaoNaoEncontradaException` mapeada para 404.

**Erro de validação (@Valid) já é genérico** (linhas 40-47) — não precisa de handler novo:
```java
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail argumentosInvalidos(MethodArgumentNotValidException ex) {
    String campos =
        ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, campos);
  }
```

⚠️ **`MaxUploadSizeExceededException`** (arquivo > limite do multipart) **não tem handler** — sem ele o erro vira 500 genérico. Adicionar handler mapeando para 400 `ProblemDetail` com a mensagem de D-28.

---

### `api/src/main/java/com/controlefinanceiro/ai/AiClient.java` (mod — cascata de modelos, D-15..D-17, D-21)

**Analog:** o próprio arquivo.

**O que existe hoje e precisa ser estendido:**

1. **Camada 1 (servidor OpenRouter) já existe** — `montarCorpo` (linhas 180-194):
```java
  private Map<String, Object> montarCorpo(List<Map<String, Object>> mensagens, List<String> lista) {
    Map<String, Object> corpo = new java.util.LinkedHashMap<>();
    if (lista.size() > 1) {
      corpo.put("models", lista);
      corpo.put("route", "fallback");
    } else if (lista.size() == 1) {
      corpo.put("model", lista.get(0));
    } else {
      corpo.put("model", modelo);
    }
    corpo.put("messages", mensagens);
    corpo.put("temperature", 0.4);
    corpo.put("max_tokens", 800);
    return corpo;
  }
```
⚠️ **`temperature` e `max_tokens` estão hardcoded** — D-18 exige `temperature=0.0` e `max_tokens=8000` (4000 em retry). **Generalizar**: `completar(mensagens, temperatura, maxTokens)` ou uma nova sobrecarga `completarExtracao(...)`, **preservando** `completar(List<Map<String,Object>>)` com os defaults atuais (D-19 — os testes `AiServiceTest`/`AiIntegrationTest` mockam `aiClient.completar(any())` e **não podem quebrar**).

2. **Retry de 429** — uma única repetição com pausa fixa (linhas 121-141):
```java
  private String executar(
      List<Map<String, Object>> mensagens, List<String> lista, String etapa, boolean repetirEm429) {
    Map<String, Object> corpo = montarCorpo(mensagens, lista);
    try {
      return extrairConteudo(chamar(corpo), etapa);
    } catch (RestClientResponseException e) {
      if (repetirEm429 && e.getStatusCode().value() == 429) {
        try {
          Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
          return extrairConteudo(chamar(corpo), etapa);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new ServicoIaIndisponivelException(
              "O serviço de IA está temporariamente indisponível. Tente novamente em instantes.",
              interrupted);
        }
      }
      throw e;
    }
  }
```
⚠️ **D-16 exige backoff 1,5s → 3s → 6s, 3 tentativas por modelo, honrando `Retry-After`** — a pausa fixa de 1500ms (linha 44: `PAUSA_ENTRE_TENTATIVAS_MS = 1500L`) é apenas o **primeiro** degrau.
⚠️ **D-17 exige gatilhos de avanço**: 429, **402** (cota), **5xx** e **JSON inválido**. Hoje só 429 é tratado, e a lista `modelosReserva` é o único avanço (linhas 93-111).

3. **O modelo usado NÃO é retornado** — `extrairConteudo` faz o log mas devolve só `String` (linhas 212-244):
```java
    JsonNode content = raiz.path("choices").path(0).path("message").path("content");
    if (content.isMissingNode() || content.isNull()) {
      throw new ServicoIaIndisponivelException("O serviço de IA devolveu uma resposta inesperada.");
    }

    // Log do modelo que atendeu: com fallback em cascata é essencial saber qual foi usado
    LOG.info(
        "Resposta da IA servida pelo modelo {} (etapa: {})",
        raiz.path("model").asText("desconhecido"),
        etapa);

    return content.asText("");
```
⚠️ **D-21 exige expor `modeloUsado` e `degradado`** na resposta da API. **Solução:** criar um record de retorno (ex.: `RespostaIa(String conteudo, String modeloUsado, boolean degradado)`) e adicionar um método **novo** (`completarComModelo`) — **não** alterar a assinatura de `completar`, ou os testes existentes quebram.

4. **Fim da cascata ⇒ 503** — `indisponivel` (linhas 144-153):
```java
  private ServicoIaIndisponivelException indisponivel(RestClientResponseException e) {
    if (e.getStatusCode().value() == 429) {
      return new ServicoIaIndisponivelException(
          "O serviço de IA está sobrecarregado agora (limite de requisições). Tente novamente em"
              + " alguns instantes.",
          e);
    }
    return new ServicoIaIndisponivelException(
        "Não foi possível falar com o serviço de IA agora. Tente novamente em instantes.", e);
  }
```
⚠️ Precisa de uma variante para **402** (cota) e **5xx** — as mensagens da UI (UI-SPEC §7.4) diferenciam "sobrecarregada" (429/402) de "rede".

5. **`extrairConteudo` já detecta `error` no corpo** (linhas 225-229):
```java
    JsonNode erro = raiz.get("error");
    if (erro != null && !erro.isNull()) {
      String msg = erro.path("message").asText("erro desconhecido");
      throw new ServicoIaIndisponivelException("O serviço de IA retornou erro: " + msg);
    }
```

---

### `api/src/main/java/com/controlefinanceiro/ai/AiService.java` (mod — delegação, D-19)

**Analog:** o próprio arquivo.

**Padrão de delegação a serviço especializado** — o `AiService` já injeta `AiContextoService` (linhas 30-44):
```java
  private final AiClient aiClient;
  private final AiMemoryStore memoryStore;
  private final AiKnowledgeStore knowledgeStore;
  private final AiContextoService contextoService;

  public AiService(
      AiClient aiClient,
      AiMemoryStore memoryStore,
      AiKnowledgeStore knowledgeStore,
      AiContextoService contextoService) {
    this.aiClient = aiClient;
    ...
  }
```
⚠️ **D-19:** "O chat existente (`AiService`) passa a **delegar** para a mesma cascata, preservando os defaults atuais (temperature 0.4, max_tokens 800)". Ou seja: `AiService` continua chamando `aiClient.completar(mensagens)` — a cascata nova fica **dentro do `AiClient`**. O `AiService` **não muda** (exceto, se necessário, uma linha de assinatura interna que não afete os testes).

⚠️ **Os testes `AiServiceTest` e `AiIntegrationTest` mockam `aiClient.completar(any())`** (AiServiceTest linha 41, AiIntegrationTest linha 53) — **a assinatura `completar(List<Map<String,Object>>)` deve permanecer idêntica**.

---

### `api/src/main/resources/application.yml` (mod — cascata gratuita, D-15)

**Analog:** o próprio arquivo.

**Estado atual (linhas 38-52):**
```yaml
ai:
  openrouter:
    api-key: ${OPENROUTER_API_KEY:}
    modelo: ${AI_OPENROUTER_MODELO:google/gemma-4-31b-it:free}
    modelos: ${AI_OPENROUTER_MODELOS:google/gemma-4-31b-it:free,google/gemma-4-26b-a4b-it:free,openrouter/free}
    modelos-reserva: ${AI_OPENROUTER_MODELOS_RESERVA:google/gemma-4-31b-it}
```

🚨 **BUG CRÍTICO (D-15, CONTEXT §Specific Ideas):** a linha 52 — `modelos-reserva: ...google/gemma-4-31b-it` — lista um modelo **PAGO** (sem `:free`). Quando a cascata gratuita falha, o cliente cai no **último recurso pago** — violando IMP-08 ("nenhum modelo pago é usado") e o success criterion 3 da Fase 5 ("nenhuma chamada usa modelo pago").

**Correção obrigatória:**
```yaml
    # Último recurso: NÃO pode ser modelo pago (IMP-08 / D-15).
    # Desativado por padrão — a cascata gratuita é a única camada.
    # Para reativar um recurso, use SOMENTE um id :free ou openrouter/free.
    modelos-reserva: ${AI_OPENROUTER_MODELOS_RESERVA:none}
```
> O sentinela `none` **já é suportado** pelo código — `AiClient.listaDeModelos` (linhas 47-48, 162-174) filtra `DESATIVA = {"none","off","null","-","nenhum"}` e devolve `List.of()`.

**Novas chaves sugeridas (Fase 5):**
```yaml
  importacao:
    max-arquivos-por-requisicao: 5
    max-bytes-por-arquivo: 4194304      # 4 MB (mesmo teto do BFF, D-28)
    max-bytes-por-requisicao: 4718592  # 4,5 MB (limite do corpo da Vercel)

spring:
  servlet:
    multipart:
      max-file-size: 8MB      # a API pode ser mais permissiva que a Vercel (D-28)
      max-request-size: 10MB
```

**Cascata gratuita esperada (D-15):** `google/gemma-4-31b-it:free` → `google/gemma-4-26b-a4b-it:free` → `openrouter/free` — a lista `modelos` **já está correta**; só a `modelos-reserva` precisa de correção.

**Teste de guarda (D-15):** um teste que falha se algum id da cascata **não** contém `:free` **e** não é `openrouter/free`:
```java
  @Test
  @DisplayName("cascata nao contem modelo pago")
  void cascataNaoContemModeloPago() {
    // lê ai.openrouter.modelos + ai.openrouter.modelos-reserva do contexto Spring
    // falha se algum id não for ":free" nem "openrouter/free"
  }
```

---

### `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java` (mod — rota nova)

**Analog:** o próprio arquivo.

**Regras de rota** (linhas 37-47):
```java
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/login", "/api/health")
                    .permitAll()
                    // GET liberado para ADMIN e USER; escrita restrita a ADMIN (T-2-05, CAT-05)
                    .requestMatchers(HttpMethod.GET, "/api/categorias")
                    .hasAnyRole("ADMIN", "USER")
                    .requestMatchers("/api/categorias/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
```
⚠️ `/api/importacoes/**` cai em `anyRequest().authenticated()` ⇒ **ADMIN e USER podem usar** (UI-SPEC §5.1: "qualquer role; escrita permitida — igual a `/lancamentos`"). **Nenhuma mudança no SecurityConfig é estritamente necessária** — mas se a fase quiser explicitar, adicionar antes do `anyRequest()`:
```java
                    .requestMatchers("/api/importacoes/**")
                    .hasAnyRole("ADMIN", "USER")
```

---

## Shared Patterns

### 1. Tenant (`empresa_id`) — exclusivo do token (R3 / AUTH-03 / IMP-13)

**Source:** `api/src/main/java/com/controlefinanceiro/common/TenantContext.java` (linhas 11-27)

```java
public final class TenantContext {

  private TenantContext() {}

  /**
   * @return o {@code empresaId} do token autenticado atual.
   */
  public static Long getEmpresaId() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao != null
        && autenticacao.isAuthenticated()
        && autenticacao.getPrincipal() instanceof UsuarioAuth usuario) {
      return usuario.empresaId();
    }
    throw new NaoAutorizadoException();
  }
}
```
**Apply to:** todos os controllers (via `TenantContext.getEmpresaId()`), todos os services (parâmetro `Long empresaId`), todas as queries de repositório (`l.empresa.id = :empresaId`), todas as entidades (`@ManyToOne(fetch = LAZY, optional = false) @JoinColumn(name = "empresa_id")`).
🚨 **No multipart, NENHUM campo do formulário define tenant** (D-10) — `empresa_id` nunca vem do `MultipartFile`.

### 2. Dinheiro — `BigDecimal` apenas (LAN-04/LAN-05/LAN-07)

**Source:** `LancamentoService.validarValor` (linhas 207-219) + `AiContextoService.normalizar` (linhas 138-143)

```java
  if (valor.scale() > 2) {
    throw new MonetaryException(
        "Valor monetário deve ter no máximo 2 casas decimais. Valor informado tem %d casas."
            .formatted(valor.scale()));
  }
  if (valor.signum() <= 0) {
    throw new MonetaryException("Valor deve ser positivo. Valor informado: %s".formatted(valor));
  }
```
```java
  private BigDecimal normalizar(BigDecimal valor) {
    if (valor == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }
    return valor.setScale(2, RoundingMode.HALF_EVEN);
  }
```
**Apply to:** todos os serviços de agregação, parsing da IA, deduplicação e gravação. **Zero** `double`/`float` no código (inclusive em comparações como `|soma − total| > 0.05` — usar `BigDecimal("0.05")`).

### 3. Erros — RFC 7807 `ProblemDetail`

**Source:** `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java`

**Apply to:** todo erro da API. O front lê o campo `detail` (ver `web/lib/api-client.ts#extrairDetalhe`, linhas 67-75).
- 400 → validação (`MonetarioException`, `CategoriaTipoIncompativelException`, `LinhaInvalidaException`, `ArquivoInvalidoException`)
- 404 → `ImportacaoNaoEncontradaException`
- 401 → `NaoAutorizadoException`
- 503 → `ServicoIaIndisponivelException` (D-22 — **já mapeado**)

### 4. Jackson — `ObjectMapper` para parse de JSON da IA

**Source:** `AiClient` (linha 43: `private static final ObjectMapper MAPPER = new ObjectMapper();`) e `extrairConteudo` (linhas 212-244)

**Apply to:** `IaExtracaoService` — parse do JSON estrito da IA, remoção das cercas ``` e validação campo a campo.

### 5. Formatação monetária pt-BR (backend)

**Source:** `AiContextoService.formatarMoeda` (linhas 145-150) e `AiService.formatarMoeda` (linhas 310-316)

```java
  private String formatarMoeda(BigDecimal valor) {
    java.text.DecimalFormat formato =
        new java.text.DecimalFormat(
            "#,##0.00", java.text.DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
    return "R$ " + formato.format(normalizar(valor));
  }
```

### 6. Frontend — BFF (o browser nunca fala com a API)

**Source:** `web/app/api/lancamentos/lote/route.ts` (47 linhas) e `web/app/api/ai/chat/route.ts` (47 linhas)

```ts
export async function POST(request: Request) {
  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

  let corpo: string;
  try {
    corpo = await request.text();
  } catch {
    return Response.json({ erro: "corpo inválido" }, { status: 400 });
  }

  let res: Response;
  try {
    res = await fetch(`${process.env.API_URL}/api/lancamentos/lote`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: corpo,
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { erro: "Não foi possível conectar. Tente novamente em instantes." },
      { status: 502 },
    );
  }

  const body = await res.text();
  return new Response(body, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("content-type") ?? "application/json" },
  });
}
```
**Variação para multipart (D-01):** em vez de `request.text()`, usar `await request.formData()` e repassar o `FormData` **sem** setar `Content-Type` (o browser define o boundary):
```ts
  const formData = await request.formData();
  res = await fetch(`${process.env.API_URL}/api/importacoes/pdf`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
    cache: "no-store",
  });
```
⚠️ **Limite da Vercel: 4,5 MB de corpo (D-28)** — o BFF valida tamanho/quantidade **antes** de repassar (mensagens exatas em UI-SPEC §6.1).

### 7. Frontend — helpers server-side (`web/lib/api.ts`)

**Source:** `web/lib/api.ts` — `getMe` (linhas 23-38), `getLancamentos` (linhas 110-135), `criarLote` (linhas 205-227)

```ts
export async function getLancamentos(
  ano: number,
  mes: number,
): Promise<{ lancamentos: LancamentoData[]; total: number } | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;

  try {
    const res = await fetch(
      `${process.env.API_URL}/api/lancamentos?ano=${ano}&mes=${mes}`,
      {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      },
    );
    if (!res.ok) return null;
    const resJson = await res.json() as {
      lancamentos: LancamentoData[];
      total: number;
    };
    return { lancamentos: resJson.lancamentos, total: resJson.total };
  } catch {
    return null;
  }
}
```
**Apply to:** `getImportacoes()` (histórico) e `getImportacao(id)` (detalhe) — mesma forma: `null` quando não há cookie ou a API não responde OK; o chamador decide o redirect.

### 8. Frontend — helpers client-side (`web/lib/api-client.ts`)

**Source:** `web/lib/api-client.ts` — `chatComIA` (linhas 41-65) e `gravarLancamentos` (linhas 107-123)

```ts
export async function chatComIA(
  request: AiChatRequest,
): Promise<AiChatResponse | null> {
  try {
    const res = await fetch("/api/ai/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) {
      const detalhe = await extrairDetalhe(res);
      throw new Error(
        detalhe ??
          "Não foi possível falar com o serviço de IA. Tente novamente mais tarde.",
      );
    }
    return (await res.json()) as AiChatResponse;
  } catch (e) {
    if (e instanceof Error) throw e;
    return null;
  }
}
```
```ts
/** Lê o campo "detail" de um ProblemDetail (RFC 7807) devolvido pela API. */
async function extrairDetalhe(res: Response): Promise<string | null> {
  try {
    const corpo = (await res.json()) as { detail?: string };
    return typeof corpo?.detail === "string" ? corpo.detail : null;
  } catch {
    return null;
  }
}
```
**Apply to:** `analisarPDFs(files)` (multipart — **não** setar `Content-Type`, usar `FormData`), `confirmarImportacao(id, payload)`, `listarImportacoes()`, `descartarImportacao(id)`.
⚠️ `extrairDetalhe` é **privado** (não exportado) — se os novos helpers precisarem dele, **exportar** ou replicar localmente.

### 9. Frontend — shell de página (Server Component)

**Source:** `web/app/lancamentos/page.tsx` (linhas 1-24, 93-157)

```tsx
import { redirect } from "next/navigation";
import { getMe } from "@/lib/api";
import { Sidebar } from "@/components/ui/sidebar";
import { SidebarContent } from "@/components/ui/sidebar";
import { SidebarInset } from "@/components/ui/sidebar";
import { Header } from "@/components/header";
import { SkipLink } from "@/components/SkipLink";
import { KitBoot } from "@/components/KitBoot";

export const instant = false;

export default async function LancamentosPage() {
  const usuario = await getMe();
  if (!usuario) {
    redirect("/login?expirada=1");
  }
  ...
}
```
```tsx
    <Sidebar>
      <SkipLink />
      <SidebarContent />
      <SidebarInset>
        <div className="flex flex-col h-full w-full overflow-hidden">
          <Header titulo="Lançamentos" />
          <main
            id="main-content"
            className="flex-1 overflow-y-auto bg-background px-4 py-6 md:px-8 lg:px-10"
          >
            <div className="mx-auto w-full max-w-[1280px]">
              ...
              <KitBoot />
            </div>
          </main>
        </div>
      </SidebarInset>
    </Sidebar>
```
**Apply to:** `web/app/importacoes/page.tsx` — `<Header titulo="Importar PDFs" />`, `export const instant = false`, `getMe()` + `redirect("/login?expirada=1")`.

### 10. Frontend — h1 único + hero lead

**Source:** `web/app/page.tsx` (linhas 89-100)
```tsx
                    <h1
                      id="hero-h"
                      className="sr-only text-2xl font-semibold tracking-tight text-foreground md:not-sr-only md:text-3xl"
                    >
                      Visão geral
                    </h1>
                    <p className="hero__lead mt-2 line-clamp-2 max-w-[52ch] text-sm text-muted-foreground">
                      Entradas, saídas e saldo do mês em uma tela só — ...
                    </p>
```
**Apply to:** `web/app/importacamentos/page.tsx` — h1 "Importar PDFs" (sr-only no mobile) + lead "Envie faturas de cartão e extratos bancários em PDF. A análise monta os lançamentos do mês para você revisar antes de gravar."

### 11. Frontend — Client Component state machine

**Source:** `web/components/LancamentosPageClient.tsx` (linhas 66-97, 140-173)

```tsx
export function LancamentosPageClient({ ... }: LancamentosPageClientProps) {
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const [formAberto, setFormAberto] = React.useState(false)
  const [modo, setModo] = React.useState<"criar" | "editar">("criar")

  const handleCriar = React.useCallback(
    async (request: {...}): Promise<boolean> => {
      setLoading(true)
      setError(null)
      try {
        const resultado = await onCriar({...})
        if (resultado) {
          resetForm()
          window.location.reload()
          return true
        } else {
          setError("Erro ao criar lançamento. Tente novamente.")
          return false
        }
      } finally {
        setLoading(false)
      }
    },
    [onCriar, resetForm],
  )
```
**Apply to:** `web/components/ImportacoesClient.tsx` — estados `envio|analisando|revisao|confirmando|resultado|erro` (UI-SPEC §6.0), `loading`/`error` para o spinner, `React.useCallback` nos handlers.

### 12. Frontend — tipos compartilhados

**Source:** `web/components/lote-tipos.ts` (37 linhas)
```ts
/**
 * Tipos compartilhados entre o Sheet de lote (LoteSheet) e a linha do
 * lote (LoteLinha). Ficam num arquivo proprio para os dois nao se
 * importarem mutuamente.
 */
export type CampoLote = "categoria" | "tipo" | "valor" | "data"
export type CategoriaOption = {
  id: number
  nome: string
  tipo: string
  pessoal: boolean
  ativo: boolean
}
```
**Apply to:** `web/components/importacoes/tipos.ts` — tipos `ImportacaoItem`, `ImportacaoAlerta` (com `kind` da UI-SPEC §6.2), `EstadoImportacao`, `CategoriaOption` (reutilizar o tipo já existente ou importar de `lote-tipos`).

### 13. Frontend — formatação monetária

**Source:** `web/components/DashboardKPIs.tsx` (linhas 7-10)
```tsx
export const MONETARY = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
})
```
**Apply to:** `ResumoMesImportacao.tsx` e `ImportItemRow.tsx` — **importar** `MONETARY` de `@/components/DashboardKPIs`, **não recriar** (UI-SPEC §4.1).

### 14. Frontend — linha editorial de stats

**Source:** `web/components/DashboardKPIs.tsx` (linhas 100-137)
```tsx
    <div
      className="grid w-full grid-cols-1 gap-x-6 gap-y-8 md:grid-cols-3"
      data-moment="counter-rolls"
    >
      {stats.map((s) => (
        <div
          key={s.label}
          className={cn(
            "flex flex-col border-t border-border pt-4",
            s.destaque && "md:order-none",
          )}
        >
          <div className="flex items-center justify-between gap-2">
            <span className={cn("font-medium text-muted-foreground", s.destaque ? "text-base text-foreground" : "text-sm")}>
              {s.label}
            </span>
            {s.badge}
          </div>
          <p className={cn("t-stat mt-3 font-semibold tracking-tight tabular-nums", s.destaque ? "text-3xl" : "text-xl", s.classe)}>
            {MONETARY.format(s.valor)}
          </p>
          <p className="mt-1.5 text-sm text-muted-foreground">{s.legenda}</p>
        </div>
      ))}
    </div>
```
**Apply to:** `ResumoMesImportacao.tsx` — `grid-cols-1 gap-x-6 gap-y-8 md:grid-cols-4`, `border-t border-border pt-4`, `text-xl font-semibold tabular-nums` (UI-SPEC §6.2).

### 15. Frontend — tabela densa + estados loading/erro/vazio

**Source:** `web/components/LancamentosTable.tsx` (linhas 74-109, 189-228)

```tsx
  if (loading) {
    return (
      <div className="rounded-lg border border-input bg-card shadow-sm">
        <div className="p-6 flex flex-col gap-4">
          <div className="h-8 w-1/3 rounded-md bg-muted animate-pulse" />
          <div className="h-8 w-1/2 rounded-md bg-muted animate-pulse" />
          <div className="h-8 w-3/4 rounded-md bg-muted animate-pulse" />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-lg border border-input bg-card shadow-sm p-6">
        <Alert variant="destructive" role="alert" className="mb-4">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
        <Button variant="outline" onClick={onRetry}>Tentar novamente</Button>
      </div>
    )
  }

  if (lancamentos.length === 0) {
    return (
      <div className="rounded-lg border border-input bg-card shadow-sm">
        <div className="p-12 text-center">
          <p className="text-sm text-muted-foreground">Nenhum lançamento neste período</p>
        </div>
      </div>
    )
  }
```
```tsx
      <div className="rounded-lg border border-input bg-card shadow-sm">
        {/* Mobile: a tabela vira lista de cards (a coluna Ações era cortada).
            No md+ a tabela de verdade, com scroll horizontal se precisar. */}
        <div className="divide-y divide-border md:hidden">
          {paginados.map((l) => renderCard(l, onEditar, onExcluir, loading))}
        </div>

        <div className="hidden md:block">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead>
                <tr className="border-b bg-muted text-muted-foreground">
                  <th className="p-4">Data</th>
                  ...
```
**Apply to:** `ImportacoesPendentes.tsx` (histórico) e `RevisaoItens.tsx` (linha de item).
⚠️ UI-SPEC §Responsive: na revisão **a linha vira bloco no mobile** (não tabela rolável) — mas no histórico a UI-SPEC §6.5 pede tabela no desktop e lista no mobile (o padrão do `LancamentosTable` serve exatamente).

### 16. Frontend — diálogo destrutivo (nunca `window.confirm`)

**Source:** `web/components/DeleteConfirmationDialog.tsx` (linhas 14-41, 57-72)
```tsx
type Action = "excluir" | "desativar";

interface DeleteConfirmationDialogProps {
  isOpen: boolean;
  onConfirm: () => void;
  onClose: () => void;
  action: Action;
  target: string;
}
...
  const confirmLabel = action === "excluir" ? "Excluir" : "Desativar";
  const titulo = `Confirmar ${action}`;
  const texto = `Tem certeza que deseja ${action} este ${target}?`;
```
```tsx
        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="destructive"
            onClick={() => { onConfirm(); onClose(); }}
            className="w-full sm:w-auto"
          >
            {confirmLabel}
          </Button>
          <Button type="button" variant="ghost" onClick={onClose} className="w-full sm:w-auto">
            Cancelar
          </Button>
        </div>
```
**Modificação da Fase 5 (UI-SPEC §4.3):**
```tsx
type Action = "excluir" | "desativar" | "descartar";

interface DeleteConfirmationDialogProps {
  ...
  descricao?: string;   // quando presente, substitui o texto padrão
}
...
  const confirmLabel = action === "excluir" ? "Excluir" : action === "desativar" ? "Desativar" : "Descartar";
  const titulo = action === "descartar" ? "Confirmar descarte" : `Confirmar ${action}`;
  const texto = descricao ?? `Tem certeza que deseja ${action} este ${target}?`;
```
Texto padrão do descarte (UI-SPEC §7.6): "Tem certeza que deseja descartar esta importação pendente? Os itens revisados serão perdidos e o arquivo poderá ser enviado de novo."

### 17. Frontend — navegação (item de sidebar)

**Source:** `web/components/ui/sidebar.tsx` (linhas 12-18)
```tsx
import { Home, DollarSign, Layout, LogOut, ChevronLeft, ChevronRight } from "lucide-react"

const sidebarItems = [
  { href: "/", label: "Dashboard", icon: Home },
  { href: "/lancamentos", label: "Lançamentos", icon: DollarSign },
  { href: "/categorias", label: "Categorias", icon: Layout },
] as const
```
**Modificação (UI-SPEC §4.3):** adicionar **após** "Categorias":
```tsx
import { ..., FileUp } from "lucide-react"

const sidebarItems = [
  { href: "/", label: "Dashboard", icon: Home },
  { href: "/lancamentos", label: "Lançamentos", icon: DollarSign },
  { href: "/categorias", label: "Categorias", icon: Layout },
  { href: "/importacamentos", label: "Importar PDF", icon: FileUp },
] as const
```
Estado ativo é **automático** (`pathname === item.href`, linha 143) e o estilo ativo é `bg-accent text-accent-foreground` (linhas 124-125).

### 18. Frontend — TopNav (sincronia)

**Source:** `web/components/TopNav.tsx` (linhas 12-16)
```tsx
const ROTAS: RouteItem[] = [
  { href: "/", label: "Dashboard" },
  { href: "/lancamentos", label: "Lançamentos" },
  { href: "/categorias", label: "Categorias" },
];
```
**Modificação (D-31 / UI-SPEC §4.3):** adicionar `{ href: "/importacamentos", label: "Importar PDF" }`.

### 19. Frontend — tokens de cor (kit dark verde)

**Source:** `web/app/globals.css` (linhas 56-78)
```css
:root {
  --background: #0F3D2E;
  --muted: rgba(157, 187, 169, 0.15);
  --muted-foreground: #9DBBA9;
  --card: #0F3D2E;
  --card-foreground: #A8D5BA;
  --primary: #4C956C;
  --primary-foreground: #FFFFFF;
  --accent: #164532;
  --accent-foreground: #A8D5BA;
  --destructive: #E5484D;
  --destructive-foreground: #FFFFFF;
  --border: #9DBBA9;
  --ring: #A8D5BA;
  --positive: #A8D5BA;
  --positive-foreground: #0F3D2E;
}
```
**Apply to:** todos os componentes novos — usar **apenas** classes Tailwind mapeadas para esses tokens (`bg-card`, `text-foreground`, `text-muted-foreground`, `text-positive`, `text-destructive`, `border-border`, `bg-accent`, `bg-primary`, `text-primary-foreground`). 🚨 **Nunca** `#0F766E`, `#F8FAFC`, `#DC2626`, `#16A34A`, gradientes, `text-white` cru, roxo/violeta (UI-SPEC §Color).

### 20. Frontend — componentes shadcn disponíveis

| Componente | Import | Notas |
|---|---|---|
| `Button` | `@/components/ui/button` | variantes `default|outline|ghost|destructive`; `size="icon-xs"` |
| `Badge` | `@/components/ui/badge` | variantes `default|secondary|destructive|outline|success|warning` (badge.tsx linhas 9-21) |
| `Alert`/`AlertTitle`/`AlertDescription`/`AlertAction` | `@/components/ui/alert` | `variant="default"\|"destructive"`; já tem `role="alert"` (alert.tsx linhas 27-31) |
| `Card`/`CardHeader`/`CardTitle`/`CardContent` | `@/components/ui/card` | grupo de itens da revisão (UI-SPEC §6.2) |
| `Select`/`SelectTrigger`/`SelectValue`/`SelectContent`/`SelectItem` | `@/components/ui/select` | select de categoria por item |
| `Progress` | `@/components/ui/progress` | barra da análise; preenchimento `bg-primary` (progress.tsx linha 21) |
| `Skeleton` | `@/components/ui/skeleton` | loading da lista de itens |
| `Spinner` | `@/components/Spinner` | `aria-hidden="true"` por padrão (Spinner.tsx linha 7) |
| `Label` | `@/components/ui/label` | rótulos |
| `Tooltip` | `@/components/ui/tooltip` | badge "modelo de fallback" |
| `Checkbox` | `@/components/ui/checkbox` | **NOVO** — `npx shadcn add checkbox` (registry oficial, sem gate de vetting) |

**Padrão de import (implícito nas classes Tailwind v4)** — `web/components/LancamentosTable.tsx` (linhas 1-14):
```tsx
"use client"

import * as React from "react"
import { Select } from "@/components/ui/select"
import { SelectTrigger } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { MONETARY } from "@/components/DashboardKPIs"
```
⚠️ O projeto importa **um por linha** de `components/ui/*` (sem barrel) — seguir esse estilo.

### 21. Backend — teste de integração (Testcontainers + `@MockBean`)

**Source:** `api/src/test/java/com/controlefinanceiro/ai/AiIntegrationTest.java` (linhas 29-48, 60-72)
```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Fixture óbvio de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;
  @Autowired private ObjectMapper mapper;
  @MockBean private AiClient aiClient;

  private String tokenAdmin;

  @BeforeEach
  void login() throws Exception {
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);
  }
```
```java
    when(aiClient.completar(any())).thenReturn("LANCAMENTO|SAIDA|45.00|2026-11-10||Almoço");
    ...
    AiResponse dto = mapper.readValue(response.getBody(), AiResponse.class);
    assertThat(dto.proposta().valor()).isEqualByComparingTo("45.00");
```
**Apply to:** `ImportacaoIntegrationTest` — `@MockBean AiClient` (a IA é mockada; **nenhuma chamada de rede**), login com `admin@controle.com` / `SENHA_FIXTURE`, assertions com `isEqualByComparingTo` para BigDecimal.

**Limpeza entre testes** — `LancamentoLoteIntegrationTest` (linhas 49-53):
```java
  @AfterEach
  void apagarLancamentosECategoriasCriadosNosTestes() {
    jdbc.update("DELETE FROM lancamento");
    jdbc.update("DELETE FROM categoria WHERE id > 16");
  }
```
> Para a Fase 5: `DELETE FROM importacao_item`, `importacao_documento`, `conta_a_pagar`, `fatura_cartao` na ordem (FK).

### 22. Backend — teste de unidade (Mockito)

**Source:** `api/src/test/java/com/controlefinanceiro/ai/AiServiceTest.java` (linhas 20-51)
```java
class AiServiceTest {

  private AiClient aiClient;
  private AiMemoryStore memoryStore;
  ...
  @BeforeEach
  void setup() {
    aiClient = mock(AiClient.class);
    memoryStore = new AiMemoryStore();
    ...
    aiService = new AiService(aiClient, memoryStore, knowledgeStore, contextoService);
  }

  @Test
  @DisplayName("mensagem com gastei retorna lancamento com proposta")
  void mensagemComGasteiRetornaLancamentoComProposta() {
    when(aiClient.completar(any())).thenReturn("LANCAMENTO|SAIDA|45.00|2026-11-10||Almoço");
    ...
    assertThat(resposta.proposta().valor()).isEqualByComparingTo("45.00");
  }
```
**Apply to:** `SanitizadorTextoTest`, `MapeadorCategoriaTest`, `DeduplicadorTest`, `AiClientCascataTest`.

⚠️ **`@DisplayName` em português** é o padrão do projeto (AiServiceTest linhas 39, 54, 85, 102, 120, 133, 147, 158, 175, 186, 195).

### 23. Backend — Google Java Format (spotless roda no `validate`)

**Source:** `api/pom.xml` (linhas 124-143)
```xml
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>${spotless.version}</version>
        <configuration>
          <java>
            <lineEndings>UNIX</lineEndings>
            <googleJavaFormat/>
          </java>
        </configuration>
        <executions>
          <execution>
            <id>spotless-check</id>
            <phase>validate</phase>
            <goals><goal>check</goal></goals>
          </execution>
        </executions>
      </plugin>
```
🚨 **Todo `.java` novo precisa estar formatado com Google Java Format** — o build **falha** no `validate` se não estiver. Corrigir com `./mvnw spotless:apply`.

---

## No Analog Found

| File | Role | Data Flow | Reason | Guidance |
|------|------|-----------|--------|-----------|
| `api/.../importacao/PdfTextoExtractor.java` | utility | file-I/O (PDF) | Nenhuma leitura de arquivo no codebase — o único I/O é HTTP (OpenRouter) | Usar `@Component` + construtor explícito; PDFBox `Loader.loadPDF` + `PDFTextStripper.setSortByPosition(true)`; resultado como record com estado `SEM_TEXTO` (não exceção) |
| `api/.../importacao/Deduplicador.java` | utility | transform (hash) | Nenhuma ocorrência de `MessageDigest`/`sha256`/`DigestUtils` no codebase | `MessageDigest.getInstance("SHA-256")` + `HexFormat.of().formatHex(...)`; `valor.setScale(2, HALF_EVEN)` **antes** do hash |
| `web/components/importacoes/UploadDropzone.tsx` | component | file-I/O (browser) | Nenhum input de arquivo no codebase | Seguir UI-SPEC §6.1 literalmente: `role="button"`, `tabIndex={0}`, `onKeyDown` Enter/Space, `<input type="file" accept="application/pdf,.pdf" multiple className="sr-only">` |
| `web/components/ui/checkbox.tsx` | component | — | Radix `checkbox` ainda não instalado | `npx shadcn add checkbox` (registry oficial shadcn — **sem** gate de vetting); copiar o wrapper do `components/ui/switch.tsx` |

---

## Metadata

**Analog search scope:**
- `api/src/main/java/com/controlefinanceiro/{ai,auth,categoria,common,config,dashboard,db/migration,empresa,lancamento,usuario,health}/**`
- `api/src/main/resources/{application.yml,db/migration/**}`
- `api/src/test/java/com/controlefinanceiro/**`
- `web/app/{page.tsx,lancamentos/page.tsx,categorias/page.tsx,api/**}`
- `web/components/{*.tsx,ui/*.tsx,ai/*}`
- `web/lib/{api.ts,api-client.ts,ai/*}`

**Files scanned:** 79 (source) + 4 (planning docs)
**Patterns extracted:** 23 shared + 30 per-file assignments
**Pattern extraction date:** 2026-11-12

---

## 🚨 Riscos / Findings para o Planner

1. **`application.yml` linha 52 usa modelo PAGO** (`google/gemma-4-31b-it` sem `:free`) em `modelos-reserva` — viola IMP-08/D-15. Correção obrigatória: `modelos-reserva: ${AI_OPENROUTER_MODELOS_RESERVA:none}` (o sentinela `none` **já é suportado** por `AiClient.listaDeModelos`).
2. **`AiClient.completar` tem `temperature`/`max_tokens` hardcoded** (0.4 / 800) — D-18 exige 0.0 / 8000. Generalizar **sem quebrar** a assinatura `completar(List<Map<String,Object>>)` (os testes `AiServiceTest`/`AiIntegrationTest` mockam `completar(any())`).
3. **`AiClient` não expõe o modelo usado** — D-21 exige `modeloUsado` + `degradado` na resposta. `extrairConteudo` já lê `raiz.path("model")` mas só faz log (linhas 238-241). Adicionar um método **novo** (ex.: `completarComModelo`) que devolve um record, sem alterar `completar`.
4. **Cascata só trata 429** — D-17 exige avanço em 429, 402, 5xx e JSON inválido; D-16 exige backoff 1,5s → 3s → 6s com 3 tentativas por modelo e honra ao `Retry-After`. Hoje há **uma** repetição com pausa fixa de 1500ms.
5. **`MaxUploadSizeExceededException` não tem handler** no `ProblemDetailExceptionHandler` ⇒ multipart estourado vira 500 genérico em vez de 400 com mensagem de D-28.
6. **`ImportacaoItem.categoria_id` precisa ser NULLABLE** — não copiar `optional = false` do `Lancamento` (a IA pode não mapear a categoria até a revisão).
7. **`web/lib/api-client.ts#extrairDetalhe` é privado** — os novos helpers client-side precisam dele exportado ou uma cópia local.
8. **JSON estrito da API**: `fail-on-unknown-properties: true` ⇒ o payload de confirmação não pode ter campos extras; e `write-bigdecimal-as-plain: true` ⇒ todos os valores chegam na web como **string**.
9. **`web/components/ui/*` não tem barrel** — importar um componente por linha (`import { Button } from "@/components/ui/button"`).
10. **Teto da Vercel = 4,5 MB de corpo** — o BFF precisa validar ANTES de repassar, e a API pode aceitar 8 MB/arquivo (D-28). O `formData` repassado **não** pode ter `Content-Type` setado manualmente (boundary).

---

*Phase: 05-importa-o-de-pdfs-e-an-lise-por-ia*
*Pattern map generated 2026-11-12 by gsd-pattern-mapper*
