CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE rag_chunk (
  chunk_id        VARCHAR(128) PRIMARY KEY,
  source_id       VARCHAR(64) NOT NULL,
  titulo          TEXT,
  organizacao     TEXT,
  tipo            VARCHAR(40),
  nivel           VARCHAR(120),
  tema            JSONB,
  licenca         TEXT,
  url             TEXT,
  vigencia        VARCHAR(80),
  uso_recomendado TEXT,
  idioma          VARCHAR(16),
  arquivo_origem  VARCHAR(255),
  n_chars         INTEGER,
  texto           TEXT NOT NULL,
  embedding       vector(1536) NOT NULL,
  tsv             TSVECTOR GENERATED ALWAYS AS (
    to_tsvector(
      'portuguese',
      coalesce(titulo, '') || ' ' || coalesce(organizacao, '') || ' ' || coalesce(texto, '')
    )
  ) STORED
);

CREATE INDEX idx_rag_chunk_source ON rag_chunk (source_id);
CREATE INDEX idx_rag_chunk_tipo ON rag_chunk (tipo);
CREATE INDEX idx_rag_chunk_vigencia ON rag_chunk (vigencia);
CREATE INDEX idx_rag_chunk_tsv ON rag_chunk USING GIN (tsv);
CREATE INDEX idx_rag_chunk_embedding ON rag_chunk USING hnsw (embedding vector_cosine_ops);

CREATE TABLE categoria_grupo_503020 (
  id          BIGSERIAL PRIMARY KEY,
  empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
  categoria_id BIGINT NOT NULL REFERENCES categoria(id),
  grupo       VARCHAR(16) NOT NULL CHECK (grupo IN ('NECESSIDADE', 'DESEJO', 'FUTURO')),
  UNIQUE (empresa_id, categoria_id)
);

INSERT INTO categoria_grupo_503020 (empresa_id, categoria_id, grupo)
SELECT
  c.empresa_id,
  c.id,
  CASE
    WHEN c.nome IN (
      'Alimentação', 'Saúde/farmácia', 'Transporte', 'Moradia', 'Educação', 'Impostos'
    ) THEN 'NECESSIDADE'
    WHEN c.nome IN (
      'Lazer', 'Vestuário', 'Tecnologia', 'Serviços', 'Assinaturas', 'Outros'
    ) THEN 'DESEJO'
    ELSE 'DESEJO'
  END
FROM categoria c
WHERE c.tipo = 'SAIDA'
ON CONFLICT (empresa_id, categoria_id) DO NOTHING;
