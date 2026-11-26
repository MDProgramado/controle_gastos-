DO $$
DECLARE
  seed_id BIGINT;
  empresa_antiga_id BIGINT;
  cat RECORD;
  categoria_seed_id BIGINT;
BEGIN
  SELECT id INTO seed_id FROM empresa WHERE nome = 'Controle Financeiro';

  IF seed_id IS NULL THEN
    RAISE EXCEPTION 'V14: empresa seed "Controle Financeiro" não encontrada';
  END IF;

  IF (SELECT count(*) FROM empresa WHERE nome = 'Controle Financeiro') > 1 THEN
    RAISE EXCEPTION 'V14: mais de uma empresa seed "Controle Financeiro"';
  END IF;

  FOR empresa_antiga_id IN
    SELECT e.id
    FROM empresa e
    WHERE e.nome = 'Minha Empresa'
      AND EXISTS (
        SELECT 1
        FROM usuario u
        WHERE u.empresa_id = e.id
          AND u.google_sub IS NOT NULL
      )
  LOOP
    FOR cat IN SELECT id, nome, tipo FROM categoria WHERE empresa_id = empresa_antiga_id LOOP
      SELECT id INTO categoria_seed_id
      FROM categoria
      WHERE empresa_id = seed_id
        AND nome = cat.nome
        AND tipo = cat.tipo
      LIMIT 1;

      IF categoria_seed_id IS NULL THEN
        INSERT INTO categoria (empresa_id, nome, tipo)
        VALUES (seed_id, cat.nome, cat.tipo)
        RETURNING id INTO categoria_seed_id;
      END IF;

      UPDATE lancamento SET categoria_id = categoria_seed_id WHERE categoria_id = cat.id;
      UPDATE importacao_item SET categoria_id = categoria_seed_id WHERE categoria_id = cat.id;
      UPDATE conta_a_pagar SET categoria_id = categoria_seed_id WHERE categoria_id = cat.id;
    END LOOP;

    DELETE FROM categoria_grupo_503020 WHERE empresa_id = empresa_antiga_id;

    UPDATE usuario
    SET empresa_id = seed_id,
        role = 'USER',
        token_version = token_version + 1
    WHERE empresa_id = empresa_antiga_id;

    UPDATE lancamento SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE importacao_documento SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE importacao_item SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE importacao_fatura SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE fatura_cartao SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE conta_a_pagar SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE helpdesk_ticket SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE helpdesk_mensagem SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE ai_memoria_sessao SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;
    UPDATE usuario_convite SET empresa_id = seed_id WHERE empresa_id = empresa_antiga_id;

    DELETE FROM categoria WHERE empresa_id = empresa_antiga_id;
    DELETE FROM empresa WHERE id = empresa_antiga_id;
  END LOOP;
END $$;
