-- Migration V6: tabela usuario_telefone, whatsapp_mensagem_processada e
-- whatsapp_confirmacao_pendente para o canal WhatsApp.
--
-- R3: empresa_id em todas as tabelas, sempre do token JWT (nunca do body).
-- Limite: 3 tentativas de código por número por hora.
-- Código válido por 10 minutos.
-- Confirmacao pendente expira em no máximo 24h (dentro da janela de retry da Meta).

CREATE TABLE usuario_telefone (
  id           BIGSERIAL PRIMARY KEY,
  usuario_id   BIGINT NOT NULL REFERENCES usuario(id),
  empresa_id   BIGINT NOT NULL REFERENCES empresa(id),
  numero_e164  VARCHAR(20) NOT NULL UNIQUE,
  verificado_em TIMESTAMPTZ,
  criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
  ativo        BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_usuario_telefone_numero ON usuario_telefone (numero_e164);
CREATE INDEX idx_usuario_telefone_usuario ON usuario_telefone (usuario_id);
CREATE INDEX idx_usuario_telefone_empresa ON usuario_telefone (empresa_id);

CREATE TABLE whatsapp_mensagem_processada (
  id            BIGSERIAL PRIMARY KEY,
  message_id    VARCHAR(255) NOT NULL UNIQUE,
  processada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  resultado     VARCHAR(40)
);

CREATE INDEX idx_whatsapp_msg_message_id ON whatsapp_mensagem_processada (message_id);

CREATE TABLE whatsapp_confirmacao_pendente (
  id                BIGSERIAL PRIMARY KEY,
  usuario_telefone_id BIGINT NOT NULL REFERENCES usuario_telefone(id),
  proposta          JSONB NOT NULL,
  expira_em         TIMESTAMPTZ NOT NULL,
  status            VARCHAR(16) NOT NULL DEFAULT 'PENDENTE',
  criado_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_whatsapp_conf_telefone_status ON whatsapp_confirmacao_pendente (usuario_telefone_id, status);

-- Limite de 24h para confirmação pendente (dentro da janela de retry da Meta)
ALTER TABLE whatsapp_confirmacao_pendente
  ADD CONSTRAINT whatsapp_conf_expira_limite
  CHECK (expira_em <= criado_em + interval '24 hours');

CREATE TABLE whatsapp_codigo_verificacao (
  id                BIGSERIAL PRIMARY KEY,
  telefone_id       BIGINT NOT NULL REFERENCES usuario_telefone(id),
  codigo_hash       VARCHAR(100) NOT NULL,
  expira_em         TIMESTAMPTZ NOT NULL,
  tentativas        INT NOT NULL DEFAULT 0,
  usado_em          TIMESTAMPTZ,
  criado_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_whatsapp_codigo_telefone ON whatsapp_codigo_verificacao (telefone_id);

CREATE TABLE auditoria_telefone (
  id                BIGSERIAL PRIMARY KEY,
  admin_id          BIGINT NOT NULL REFERENCES usuario(id),
  usuario_id        BIGINT NOT NULL REFERENCES usuario(id),
  numero_mascarado  VARCHAR(40) NOT NULL,
  acao              VARCHAR(20) NOT NULL,
  criado_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_telefone_usuario ON auditoria_telefone (usuario_id);
CREATE INDEX idx_auditoria_telefone_admin ON auditoria_telefone (admin_id);
