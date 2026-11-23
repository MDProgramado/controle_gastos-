-- Isolamento por usuário (Fase 3 / D-03, D-16): migration aditiva, imutável depois de aplicada.
-- V1..V9 não são alteradas: nenhum checksum de migration aplicada é modificado.

ALTER TABLE importacao_item
  ADD COLUMN usuario_id BIGINT REFERENCES usuario(id);
UPDATE importacao_item ii
  SET usuario_id = d.usuario_id
  FROM importacao_documento d
  WHERE d.id = ii.documento_id;
ALTER TABLE importacao_item ALTER COLUMN usuario_id SET NOT NULL;
CREATE INDEX idx_importacao_item_empresa_usuario_status
  ON importacao_item (empresa_id, usuario_id, status);

ALTER TABLE importacao_fatura
  ADD COLUMN usuario_id BIGINT REFERENCES usuario(id);
UPDATE importacao_fatura f
  SET usuario_id = d.usuario_id
  FROM importacao_documento d
  WHERE d.id = f.documento_id;
ALTER TABLE importacao_fatura ALTER COLUMN usuario_id SET NOT NULL;

ALTER TABLE fatura_cartao
  ADD COLUMN usuario_id BIGINT REFERENCES usuario(id);
UPDATE fatura_cartao f
  SET usuario_id = d.usuario_id
  FROM importacao_documento d
  WHERE d.id = f.documento_id;
ALTER TABLE fatura_cartao ALTER COLUMN usuario_id SET NOT NULL;

ALTER TABLE conta_a_pagar
  ADD COLUMN usuario_id BIGINT REFERENCES usuario(id);
UPDATE conta_a_pagar c
  SET usuario_id = d.usuario_id
  FROM fatura_cartao f
  JOIN importacao_documento d ON d.id = f.documento_id
  WHERE f.id = c.fatura_id;
UPDATE conta_a_pagar c
  SET usuario_id = i.usuario_id
  FROM importacao_item i
  WHERE i.conta_a_pagar_id = c.id
    AND c.usuario_id IS NULL;
ALTER TABLE conta_a_pagar ALTER COLUMN usuario_id SET NOT NULL;
CREATE INDEX idx_conta_a_pagar_empresa_usuario_vencimento
  ON conta_a_pagar (empresa_id, usuario_id, vencimento);

CREATE TABLE ai_memoria_sessao (
  id            BIGSERIAL PRIMARY KEY,
  empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
  usuario_id    BIGINT NOT NULL REFERENCES usuario(id),
  session_id    VARCHAR(255) NOT NULL,
  criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (empresa_id, usuario_id, session_id)
);

CREATE TABLE ai_memoria_mensagem (
  id         BIGSERIAL PRIMARY KEY,
  sessao_id  BIGINT NOT NULL REFERENCES ai_memoria_sessao(id) ON DELETE CASCADE,
  papel      VARCHAR(16) NOT NULL,
  conteudo   TEXT NOT NULL,
  criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_memoria_mensagem_sessao ON ai_memoria_mensagem (sessao_id, id);
