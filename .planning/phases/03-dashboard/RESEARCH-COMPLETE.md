## RESEARCH COMPLETE

**Phase:** 3 - Dashboard
**Confidence:** HIGH

### Key Findings

- **Aggregation endpoint needed:** `GET /api/dashboard?ano=&mes=` on Spring Boot API performing SUM/GROUP BY in SQL (D-01). Web must never sum the truncated listagem (50 items max). Verified against existing `LancamentoRepository` patterns and `V1__schema.sql`.
- **Money handling:** BigDecimal + NUMERIC(14,2) already established. No double/float. Jackson BigDecimal serialization config needs verification (ASSUMED).
- **Tenant filtering:** All queries filter by `empresa_id` from `TenantContext.getEmpresaId()` (JWT). Already enforced in existing controllers/repositories.
- **Comparison logic:** Absolute R$ + percentual for entradas/saídas/saldo; base zero → "—" + absolute only (D-05, D-06). Month-closed vs month-closed (D-07).
- **Category breakdown:** LEFT JOIN to include zero-value SAÍDA categories; no "Outros" grouping; percentual over total saídas (D-08, D-09, D-10).
- **Frontend:** `input type="month"` nativo, URL state `?mes=YYYY-MM`, setas ‹ ›, shadcn/ui Charts + Recharts (package `recharts` tagged [ASSUMED] — needs human verify before install).
- **CRUD screens:** Reuse existing endpoints (`/api/lancamentos`, `/api/categorias`). New Next.js pages at `/lancamentos` and `/categorias`. ADMIN-only writes already enforced by SecurityConfig; UI reflects role.
- **config.json:** `nyquist_validation: false` — Validation Architecture section omitted from RESEARCH.md.

### File Created
`.planning/phases/03-dashboard/03-RESEARCH.md`

### Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Standard Stack | HIGH | Versões verificadas diretamente em package.json e código existente |
| Architecture | HIGH | Padrões verificados no código existente (LancamentoRepository, TenantContext, web/lib/api.ts, SecurityConfig) |
| Pitfalls | MEDIUM | Baseados em padrões conhecidos do domínio; alguns itens [ASSUMED] |

### Open Questions

1. Jackson BigDecimal serialization config — precisa verificar `application.properties`/`application.yml` da API
2. Versão do `recharts` — `npm view recharts versions` no diretório web/
3. Pacote de destino para DTOs de agregação: `dashboard` vs `lancamento`
4. Formulários CRUD: seguir padrão `LoginForm` (controlado, sem RHF/zod)
5. Total para aviso de truncamento "mostrando 50 de N" (D-22)

### Ready for Planning
Research complete. Planner can now create PLAN.md files.