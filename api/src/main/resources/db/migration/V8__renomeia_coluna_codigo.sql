-- Renomeia a coluna da tabela de códigos para o nome mapeado pela entidade.
-- A V6 já foi aplicada em produção; alterar o SQL versionado quebraria o checksum do Flyway.

ALTER TABLE whatsapp_codigo_verificacao
  RENAME COLUMN telefone_id TO usuario_telefone_id;
