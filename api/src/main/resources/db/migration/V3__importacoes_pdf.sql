-- Migration IMUTÁVEL (INF-01): este arquivo não pode ser alterado depois de aplicado.
-- Qualquer correção exige uma nova versão V<n>__ com o DDL corrigido.
-- DDL da Fase 5 (importação de PDFs com IA): tabelas de staging + definitivas.
-- Estende lancamento com origem/documento_id/chave_dedupe sem alterar o contrato tipo=ENTRADA|SAIDA.
-- R3: empresa_id em TODAS as tabelas, sempre do token JWT (nunca do body).
-- LAN-05: valores monetários NUMERIC(14,2) com CHECK (valor > 0).

CREATE TABLE importacao_documento (
  id           BIGSERIAL PRIMARY KEY,
  empresa_id   BIGINT NOT NULL REFERENCES empresa(id),
  usuario_id   BIGINT NOT NULL REFERENCES usuario(id),
  nome_arquivo VARCHAR(255) NOT NULL,
  tipo_documento VARCHAR(16) NOT NULL,        -- CARTAO | EXTRATO
  hash_sha256  CHAR(64) NOT NULL,
  tamanho_bytes INT NOT NULL,
  paginas      INT,
  status       VARCHAR(16) NOT NULL,          -- ANALISADO | CONFIRMADO | DESCARTADO | DUPLICADO | SEM_TEXTO
  criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (empresa_id, hash_sha256)            -- reenvio do mesmo arquivo não duplica
);

CREATE TABLE importacao_item (
  id              BIGSERIAL PRIMARY KEY,
  empresa_id      BIGINT NOT NULL REFERENCES empresa(id),
  documento_id    BIGINT NOT NULL REFERENCES importacao_documento(id),
  data            DATE NOT NULL,
  descricao       VARCHAR(200) NOT NULL,
  valor           NUMERIC(14,2) NOT NULL CHECK (valor > 0), -- LAN-05
  tipo            VARCHAR(16) NOT NULL,                -- RECEITA | DESPESA_PAGA | A_PAGAR
  categoria_nome  VARCHAR(80),
  categoria_id    BIGINT REFERENCES categoria(id),     -- resolvida no tenant, pode ser nula
  origem          VARCHAR(16) NOT NULL,                -- CARTAO | EXTRATO
  parcela_atual   INT,
  parcela_total   INT,
  status          VARCHAR(16) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE | IGNORADO | IMPORTADO
  lancamento_id   BIGINT,
  conta_a_pagar_id BIGINT,
  alerta          VARCHAR(200),
  criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_importacao_item_documento ON importacao_item (documento_id);
CREATE INDEX idx_importacao_item_empresa_status ON importacao_item (empresa_id, status);

CREATE TABLE importacao_fatura (
  id              BIGSERIAL PRIMARY KEY,
  empresa_id      BIGINT NOT NULL REFERENCES empresa(id),
  documento_id    BIGINT NOT NULL REFERENCES importacao_documento(id),
  total           NUMERIC(14,2) NOT NULL CHECK (total >= 0),
  data_vencimento DATE NOT NULL,
  mes_referencia  DATE,
  data_fechamento DATE,
  criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE fatura_cartao (
  id              BIGSERIAL PRIMARY KEY,
  empresa_id      BIGINT NOT NULL REFERENCES empresa(id),
  documento_id    BIGINT NOT NULL REFERENCES importacao_documento(id),
  mes_referencia  DATE,
  data_fechamento DATE,
  data_vencimento DATE NOT NULL,
  total           NUMERIC(14,2) NOT NULL CHECK (total >= 0),
  criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conta_a_pagar (
  id                BIGSERIAL PRIMARY KEY,
  empresa_id        BIGINT NOT NULL REFERENCES empresa(id),
  fatura_id         BIGINT REFERENCES fatura_cartao(id),
  categoria_id      BIGINT NOT NULL REFERENCES categoria(id),
  descricao         VARCHAR(200) NOT NULL,
  valor             NUMERIC(14,2) NOT NULL CHECK (valor > 0), -- LAN-05
  data              DATE NOT NULL,
  parcela_atual     INT,
  parcela_total     INT,
  vencimento        DATE,
  origem            VARCHAR(16) NOT NULL,                -- CARTAO | EXTRATO
  criado_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_conta_a_pagar_empresa_vencimento ON conta_a_pagar (empresa_id, vencimento);

-- Extende lancamento com rastreamento de origem da importação (sem alterar tipo=ENTRADA|SAIDA).
ALTER TABLE lancamento
  ADD COLUMN origem VARCHAR(16),                       -- CARTAO | EXTRATO | MANUAL
  ADD COLUMN documento_id BIGINT REFERENCES importacao_documento(id),
  ADD COLUMN chave_dedupe VARCHAR(64);

CREATE UNIQUE INDEX idx_lanc_dedupe ON lancamento (empresa_id, chave_dedupe) WHERE chave_dedupe IS NOT NULL;
