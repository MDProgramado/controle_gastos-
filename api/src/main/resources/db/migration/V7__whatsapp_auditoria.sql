-- Migration V7: colunas de auditoria de canal WhatsApp em lancamento.
--
-- canal: origem do lançamento (WHATSAPP, MANUAL). Valores nulos para lançamentos
-- criados antes desta migration (não preencher automaticamente).
-- message_id_origem: id da mensagem WhatsApp de origem, para idempotência.
-- Índice único parcial por empresa_id + message_id_origem impede lançamento duplicado.

ALTER TABLE lancamento ADD COLUMN canal VARCHAR(16);

ALTER TABLE lancamento
  ADD CONSTRAINT lancamento_canal_check
  CHECK (canal IS NULL OR canal IN ('WHATSAPP', 'MANUAL'));

ALTER TABLE lancamento ADD COLUMN message_id_origem VARCHAR(255);

CREATE UNIQUE INDEX lancamento_message_id_origem_uq
  ON lancamento (empresa_id, message_id_origem)
  WHERE message_id_origem IS NOT NULL;
