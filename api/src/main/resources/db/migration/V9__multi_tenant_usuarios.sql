-- Multi-tenant (Fase 1 / D-08 a D-11): migration aditiva, imutável depois de aplicada.
-- V1..V8 não são alteradas: nenhum checksum de migration aplicada é modificado.

ALTER TABLE usuario
  ADD COLUMN google_sub VARCHAR(255),
  ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN token_version INT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX idx_usuario_google_sub_unico ON usuario (google_sub)
  WHERE google_sub IS NOT NULL;

CREATE TABLE usuario_convite (
  id          BIGSERIAL PRIMARY KEY,
  empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
  email       VARCHAR(160) NOT NULL,
  role        VARCHAR(16) NOT NULL,
  token_hash  CHAR(64) NOT NULL,
  expira_em   TIMESTAMPTZ NOT NULL,
  usado_em    TIMESTAMPTZ,
  criado_por  BIGINT NOT NULL REFERENCES usuario(id),
  criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_usuario_convite_token_hash ON usuario_convite (token_hash);
CREATE INDEX idx_usuario_convite_empresa_email ON usuario_convite (empresa_id, email);
CREATE INDEX idx_lanc_empresa_usuario_data ON lancamento (empresa_id, usuario_id, data);
CREATE INDEX idx_importacao_empresa_usuario ON importacao_documento (empresa_id, usuario_id);
