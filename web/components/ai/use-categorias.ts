import { useCallback, useEffect, useState } from "react";
import { listarCategorias, type CategoriaDisponivel } from "@/lib/api-client";

/**
 * Hook que fornece as categorias REAIS do tenant para o chat.
 *
 * A lista vem do backend (GET /api/categorias via BFF do Next). IDs são os do banco — usar
 * mock aqui gravaria o lançamento na categoria errada.
 */
export function useCategorias() {
  const [categorias, setCategorias] = useState<CategoriaDisponivel[]>([]);

  const carregar = useCallback(async () => {
    const lista = await listarCategorias();
    setCategorias(lista.filter((c) => c.ativo !== false));
  }, []);

  useEffect(() => {
    let ativo = true;
    listarCategorias().then((lista) => {
      if (!ativo) return;
      setCategorias(lista.filter((c) => c.ativo !== false));
    });
    return () => {
      ativo = false;
    };
  }, []);

  return { categorias, carregar, recarregar: carregar };
}
