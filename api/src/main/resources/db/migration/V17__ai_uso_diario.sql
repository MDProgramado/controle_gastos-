-- Uso diário da IA por empresa/usuário (tokens e custo da OpenRouter), base para precificação
-- do SaaS. Upsert acumulativo pela chave única (empresa, usuário, dia, operação, modelo).
CREATE TABLE ai_uso_diario (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    data DATE NOT NULL,
    operacao VARCHAR(20) NOT NULL,
    modelo VARCHAR(200) NOT NULL,
    tokens_entrada BIGINT NOT NULL DEFAULT 0,
    tokens_saida BIGINT NOT NULL DEFAULT 0,
    custo_usd NUMERIC(12,6) NOT NULL DEFAULT 0,
    chamadas INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_ai_uso_diario UNIQUE (empresa_id, usuario_id, data, operacao, modelo)
);

CREATE INDEX idx_ai_uso_diario_empresa_data ON ai_uso_diario (empresa_id, data);
