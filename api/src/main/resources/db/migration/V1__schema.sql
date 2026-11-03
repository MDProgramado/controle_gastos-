-- Migration IMUTÁVEL (INF-01 / T-1-01): este arquivo não pode ser alterado depois de aplicado.
-- Qualquer correção exige uma nova versão V<n>__ com o DDL corrigido.
-- DDL literal do PLAN §3.4: empresa, usuario, categoria, lancamento + 3 índices.
-- R1: lancamento.valor NUMERIC(14,2) com CHECK (valor > 0).
-- R3: empresa_id presente em TODAS as tabelas (multiempresa desde a Fase 2).

CREATE TABLE empresa (
  id          BIGSERIAL PRIMARY KEY,
  nome        VARCHAR(120) NOT NULL,
  criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE usuario (
  id             BIGSERIAL PRIMARY KEY,
  empresa_id     BIGINT NOT NULL REFERENCES empresa(id),
  email          VARCHAR(160) NOT NULL UNIQUE,
  senha_hash     VARCHAR(100) NOT NULL,          -- BCrypt
  nome           VARCHAR(120) NOT NULL,
  role           VARCHAR(16)  NOT NULL DEFAULT 'USER',  -- ADMIN | USER
  ativo          BOOLEAN NOT NULL DEFAULT true,
  criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categoria (
  id          BIGSERIAL PRIMARY KEY,
  empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
  nome        VARCHAR(80) NOT NULL,
  tipo        VARCHAR(10) NOT NULL,              -- ENTRADA | SAIDA
  pessoal     BOOLEAN NOT NULL DEFAULT false,    -- marca [P]
  ativo       BOOLEAN NOT NULL DEFAULT true,
  UNIQUE (empresa_id, nome, tipo)
);

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

CREATE INDEX idx_lanc_empresa_data ON lancamento (empresa_id, data);
CREATE INDEX idx_lanc_empresa_cat  ON lancamento (empresa_id, categoria_id);
CREATE INDEX idx_cat_empresa_tipo  ON categoria (empresa_id, tipo);
