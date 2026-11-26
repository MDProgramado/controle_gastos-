ALTER TABLE conta_a_pagar
  ADD COLUMN pago BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN pago_em TIMESTAMPTZ,
  ADD COLUMN lancamento_id BIGINT REFERENCES lancamento(id),
  ADD COLUMN versao INT NOT NULL DEFAULT 0;

ALTER TABLE conta_a_pagar
  ADD CONSTRAINT uk_conta_a_pagar_lancamento UNIQUE (lancamento_id);

CREATE INDEX idx_conta_a_pagar_empresa_usuario_pago
  ON conta_a_pagar (empresa_id, usuario_id, pago);
