import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { expect, test } from "@playwright/test";
import {
  centavosAssinadosDeString,
  centavosDeString,
  formatarValorApi,
  normalizarValorParaApi,
  validarValorLote,
} from "../lib/money";
import {
  agruparCategorias,
  formatarBRL,
  formatarPercentual,
  progressoPercentual,
  valoresParaGrafico,
} from "../lib/format";
import {
  atualizarPropostaPorMensagem,
  criarPropostasEditaveis,
  marcarPropostasConfirmadas,
  propostasDaMensagem,
  valorPropostaParaApi,
} from "../lib/chat";
import { respostaErroBff, urlApiSegura } from "../lib/bff-helpers";
import { descreverResultadoConfirmacao } from "../lib/importacao-resultado";
import { podeAlternarStatus } from "../lib/categorias";
import { resultadoErroLote } from "../lib/lancamentos";
import type { ChatMessage } from "../lib/ai/types";

test("parseia valores em centavos sem ponto flutuante", () => {
  expect(centavosDeString("0,01")).toBe(BigInt(1));
  expect(centavosDeString("1.234,56")).toBe(BigInt(123456));
  expect(centavosDeString("1234.56")).toBe(BigInt(123456));
  expect(centavosDeString("1.234")).toBe(BigInt(123400));
  expect(centavosDeString("0,001")).toBeNull();
  expect(centavosDeString("-1,00")).toBeNull();
});

test("parseia valores assinados para saldo e variações", () => {
  expect(centavosAssinadosDeString("-2850.00")).toBe(BigInt(-285000));
  expect(centavosAssinadosDeString("+1,00")).toBe(BigInt(100));
  expect(centavosAssinadosDeString("1234.56")).toBe(BigInt(123456));
  expect(centavosAssinadosDeString("0,001")).toBeNull();
});

test("parseia números retornados pela API sem chamar trim", () => {
  expect(centavosDeString(1234.56)).toBe(BigInt(123456));
  expect(centavosDeString(0.01)).toBe(BigInt(1));
  expect(centavosAssinadosDeString(-2850.5)).toBe(BigInt(-285050));
  expect(centavosAssinadosDeString(0)).toBeNull();
});

test("head line do kit usa script externo sem innerHTML", () => {
  const layout = readFileSync(resolve(process.cwd(), "app/layout.tsx"), "utf8");

  expect(layout).not.toContain("dangerouslySetInnerHTML");
  expect(layout).toContain('src="/assets/motion/head-line.js"');
});

test("normaliza valor para a API com exatamente duas casas", () => {
  expect(formatarValorApi(BigInt(1))).toBe("0.01");
  expect(formatarValorApi(BigInt(123456))).toBe("1234.56");
  expect(normalizarValorParaApi("1.234,56")).toBe("1234.56");
  expect(normalizarValorParaApi("0,001")).toBeNull();
});

test("valida valor do lote com mensagem por caso", () => {
  expect(validarValorLote("")).toBe("Informe um valor maior que zero");
  expect(validarValorLote("0,00")).toBe("Informe um valor maior que zero");
  expect(validarValorLote("0,001")).toBe("Use no máximo 2 casas decimais");
  expect(validarValorLote("1234.56")).toBeNull();
});

test("formata BRL e percentual a partir de centavos", () => {
  expect(formatarBRL(BigInt(123456))).toBe("R$ 1.234,56");
  expect(formatarBRL(BigInt(1))).toBe("R$ 0,01");
  expect(formatarPercentual(BigInt(123456), BigInt(200000))).toBe("61,73%");
  expect(progressoPercentual(BigInt(123456), BigInt(200000))).toBe(61.73);
});

test("agrupa top 5 e Outros ordenando por centavos", () => {
  const categorias = [
    { id: 1, nome: "A", total: "100.00" },
    { id: 2, nome: "B", total: "90.00" },
    { id: 3, nome: "C", total: "80.00" },
    { id: 4, nome: "D", total: "70.00" },
    { id: 5, nome: "E", total: "60.00" },
    { id: 6, nome: "F", total: "50.00" },
    { id: 7, nome: "G", total: "10.00" },
  ];

  expect(agruparCategorias(categorias).map((c) => c.nome)).toEqual([
    "A",
    "B",
    "C",
    "D",
    "E",
    "Outros",
  ]);
  expect(agruparCategorias(categorias).at(-1)?.centavos).toBe(BigInt(6000));
});

test("agrupa categorias com totais numéricos da API", () => {
  const categorias = [
    { id: 1, nome: "A", total: 100.5 },
    { id: 2, nome: "B", total: 90 },
    { id: 3, nome: "C", total: 0.01 },
  ];

  expect(
    agruparCategorias(categorias).map((c) => [c.nome, c.centavos]),
  ).toEqual([
    ["A", BigInt(10050)],
    ["B", BigInt(9000)],
    ["C", BigInt(1)],
  ]);
});

test("converte centavos para number somente no dado do gráfico", () => {
  const agrupadas = agruparCategorias([
    { id: 1, nome: "A", total: 100.5 },
    { id: 2, nome: "B", total: 90 },
  ]);
  const dados = valoresParaGrafico(agrupadas, [
    "var(--chart-1)",
    "var(--chart-2)",
  ]);

  expect(dados.map((d) => [d.name, d.value])).toEqual([
    ["A", 10050],
    ["B", 9000],
  ]);
  expect(dados.every((d) => typeof d.value === "number")).toBe(true);
});

const HOJE = new Date().toISOString().slice(0, 10);

const proposta = {
  tipo: "SAIDA" as const,
  valor: 1234.56,
  data: HOJE,
  categoriaId: null,
  categoriaSugerida: "Mercado",
};

test("lê propostas da própria mensagem, sem índice global", () => {
  const mensagem: ChatMessage = {
    id: "m1",
    role: "assistant",
    content: "ok",
    criadoEm: new Date(),
    lancamentoProposta: proposta,
    lancamentoPropostas: [proposta, { ...proposta, valor: 10 }],
  };

  expect(propostasDaMensagem(mensagem)).toHaveLength(2);
});

test("mantém estado de propostas separado por mensagem", () => {
  const inicial = criarPropostasEditaveis("m1", [proposta], () => "7");
  const atualizado = atualizarPropostaPorMensagem(
    inicial,
    "m1",
    "prop-m1-0",
    { confirmada: true },
  );
  const outraMensagem = criarPropostasEditaveis("m2", [proposta], () => "9");

  expect(inicial.m1[0].confirmada).toBe(false);
  expect(atualizado.m1[0].confirmada).toBe(true);
  expect(
    marcarPropostasConfirmadas(outraMensagem, "m2", ["prop-m2-0"]).m2[0]
      .confirmada,
  ).toBe(true);
});

test("converte valor editado da proposta sem parseFloat", () => {
  expect(valorPropostaParaApi("1.234,56")).toBe(1234.56);
  expect(valorPropostaParaApi("0,01")).toBe(0.01);
  expect(valorPropostaParaApi("0,001")).toBeNull();
});

test("BFF aceita apenas rotas próprias e devolve 502 padronizado", () => {
  expect(urlApiSegura("/api/categorias")).toBe("/api/categorias");
  expect(urlApiSegura("/api/../auth/me")).toBeNull();
  expect(urlApiSegura("https://example.com/api/categorias")).toBeNull();

  const resposta = respostaErroBff(502, "Não foi possível conectar");
  expect(resposta.status).toBe(502);
  expect(resposta.headers.get("content-type")).toContain("application/json");
});

test("descreve sucesso parcial sem afirmar que nada foi gravado", () => {
  expect(
    descreverResultadoConfirmacao({ lancamentos: 2, contas: 1, falhas: 0 }).titulo,
  ).toBe("Importação confirmada");
  const parcial = descreverResultadoConfirmacao({
    lancamentos: 2,
    contas: 0,
    falhas: 1,
  });
  expect(parcial.parcial).toBe(true);
  expect(parcial.titulo).toBe("Importação parcial");
  expect(parcial.descricao).not.toContain("Nada mais será criado");
  const nenhum = descreverResultadoConfirmacao({
    lancamentos: 0,
    contas: 0,
    falhas: 1,
  });
  expect(nenhum.titulo).toBe("Nenhum lançamento foi gravado");
});

test("extrai índice da linha do erro estruturado do backend", () => {
  expect(
    resultadoErroLote({ detail: "Linha 3 inválida: valor", indiceLinha: 3 }, 400),
  ).toEqual({ ok: false, mensagem: "Linha 3 inválida: valor", indiceLinha: 3 });
  const falha = resultadoErroLote({ erro: "falha" }, 502);
  expect(falha.ok).toBe(false);
  if (!falha.ok) expect(falha.mensagem).toBe("falha");
  const generica = resultadoErroLote({}, 500);
  expect(generica.ok).toBe(false);
  if (!generica.ok) expect(generica.mensagem).toBe("Não foi possível salvar o lote.");
});

test("categoria inativa não pode ser reativada pelo switch", () => {
  expect(podeAlternarStatus({ ativo: true })).toBe(true);
  expect(podeAlternarStatus({ ativo: false })).toBe(false);
});
