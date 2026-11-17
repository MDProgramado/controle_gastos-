-- Adiciona UNIQUE constraint em empresa.nome para permitir upsert (ON CONFLICT) usado nos testes de integração.
ALTER TABLE empresa ADD CONSTRAINT uk_empresa_nome UNIQUE (nome);
