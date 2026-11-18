-- Migration V5: ajuste de tipo da coluna hash_sha256 para VARCHAR(64).
-- Corrigido para permitir armazenamento correto do hash SHA-256 hexadecimal (64 caracteres).
ALTER TABLE importacao_documento ALTER COLUMN hash_sha256 TYPE VARCHAR(64);
