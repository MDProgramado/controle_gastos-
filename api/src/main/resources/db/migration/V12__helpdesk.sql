CREATE TABLE helpdesk_ticket (
  id            BIGSERIAL PRIMARY KEY,
  empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
  usuario_id    BIGINT NOT NULL REFERENCES usuario(id),
  assunto       VARCHAR(120) NOT NULL,
  descricao     TEXT NOT NULL,
  status        VARCHAR(20) NOT NULL DEFAULT 'ABERTO'
    CHECK (status IN ('ABERTO', 'EM_ANDAMENTO', 'RESOLVIDO', 'FECHADO')),
  criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ
);

CREATE TABLE helpdesk_mensagem (
  id         BIGSERIAL PRIMARY KEY,
  ticket_id  BIGINT NOT NULL REFERENCES helpdesk_ticket(id) ON DELETE CASCADE,
  empresa_id BIGINT NOT NULL REFERENCES empresa(id),
  usuario_id BIGINT NOT NULL REFERENCES usuario(id),
  origem     VARCHAR(16) NOT NULL CHECK (origem IN ('USER', 'ADMIN')),
  conteudo   TEXT NOT NULL,
  criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_helpdesk_ticket_empresa_usuario_status
  ON helpdesk_ticket (empresa_id, usuario_id, status);
CREATE INDEX idx_helpdesk_mensagem_ticket ON helpdesk_mensagem (ticket_id, id);
