import { expect, test } from "@playwright/test";
import {
  GRUPOS_503020,
  GRUPO_PADRAO_503020,
  grupoLabel,
  grupoPercentual,
  isGrupo503020,
} from "../lib/grupos-503020";

test("admitte apenas os três grupos do 50/30/20", () => {
  expect(GRUPOS_503020).toEqual(["NECESSIDADE", "DESEJO", "FUTURO"]);
  expect(GRUPO_PADRAO_503020).toBe("DESEJO");
  expect(isGrupo503020("NECESSIDADE")).toBe(true);
  expect(isGrupo503020("DESEJO")).toBe(true);
  expect(isGrupo503020("FUTURO")).toBe(true);
  expect(isGrupo503020("INVESTIMENTO")).toBe(false);
  expect(isGrupo503020(null)).toBe(false);
});

test("expõe rótulos e metas do 50/30/20", () => {
  expect(grupoLabel("NECESSIDADE")).toBe("Necessidades");
  expect(grupoLabel("DESEJO")).toBe("Desejos");
  expect(grupoLabel("FUTURO")).toBe("Futuro");
  expect(grupoPercentual("NECESSIDADE")).toBe(50);
  expect(grupoPercentual("DESEJO")).toBe(30);
  expect(grupoPercentual("FUTURO")).toBe(20);
});
