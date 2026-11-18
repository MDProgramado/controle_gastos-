# Plano — WhatsApp: modelo de telefone e fluxo de verificação

**Branch:** `fix/whatsapp-verificacao` (worktree: `../controle-gastos-whatsapp`)  
**Escopo:** apenas modelo de telefone + fluxo de verificação. Sem webhook. Sem alterações em `web/`. Sem commit de credenciais.

## 1. Migration (Flyway)

- `V2__whatsapp_telefone.sql` (próximo número após V1; V3 existe mas é de outra fase)
  - Tabela `usuario_telefone`: id, usuario_id, empresa_id, numero_e164 (unique), verificado_em, criado_em, ativo
  - Tabela `whatsapp_codigo_verificacao`: id, telefone_id, codigo_hash, expira_em, tentativas, criado_em, usado_em
  - Tabela `auditoria_telefone`: id, admin_id, usuario_id, numero_mascarado, acao, criado_em

## 2. Entidades JPA + Repositórios

- `UsuarioTelefone` + `UsuarioTelefoneRepository`
- `WhatsappCodigoVerificacao` + `WhatsappCodigoVerificacaoRepository`
- `AuditoriaTelefone` + `AuditoriaTelefoneRepository`

## 3. Service + Controller + DTOs

- `UsuarioTelefoneService`:
  - `vincularTelefone(adminId, empresaId, usuarioId, numeroE164)` — ADMIN, mesma empresa, desativa anterior, gera código, envia via Meta
  - `verificarCodigo(usuarioAutenticadoId, numeroE164, codigo)` — só o dono, 3 tentativas/hora, expiração 10 min, define verificado_em
  - `mascararNumero(numeroE164)` — +55 71 ****-**99
- `UsuarioTelefoneController`:
  - `POST /api/usuarios/{id}/telefone` — exige ADMIN (SecurityConfig + service)
  - `POST /api/telefones/verificar` — qualquer usuário autenticado (dono do número)

## 4. SecurityConfig

- Adicionar `.requestMatchers(HttpMethod.POST, "/api/usuarios/**").hasRole("ADMIN")`

## 5. Testes de integração (Testcontainers)

- ADMIN de outra empresa tenta vincular: 403
- USER tenta vincular: 403
- Código errado 3 vezes: bloqueio na 4ª dentro da hora
- Código expirado: rejeitado
- Outro usuário confirma código alheio: rejeitado
- Troca de número: anterior inativo
