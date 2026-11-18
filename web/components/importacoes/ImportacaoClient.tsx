"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Spinner } from "@/components/Spinner";
import { MONETARY } from "@/components/DashboardKPIs";
import {
  ID_LIMITES,
  UploadDropzone,
} from "@/components/importacoes/UploadDropzone";
import { BarraProgressoImportacao } from "@/components/importacoes/BarraProgressoImportacao";
import {
  ImportFileList,
  type EstadoArquivo,
} from "@/components/importacoes/ImportFileList";
import { ResumoMesImportacao } from "@/components/importacoes/ResumoMesImportacao";
import { AlertasImportacao } from "@/components/importacoes/AlertasImportacao";
import {
  RevisaoItens,
  type CategoriaOption,
} from "@/components/importacoes/RevisaoItens";
import { ResultadoConfirmacao } from "@/components/importacoes/ResultadoConfirmacao";
import { ImportacoesPendentes } from "@/components/importacoes/ImportacoesPendentes";
import {
  ApiError,
  confirmarImportacao,
  obterImportacaoPendente,
  type ConfirmarItemRequest,
  type ImportacaoAnaliseResponse,
  type ImportacaoHistorico,
} from "@/lib/api-client";
import {
  centavos,
  formatarBytes,
  itemTemTag,
  origemInferida,
  somarCentavos,
  type AlertaLista,
  type ArquivoValido,
  type ItemRevisao,
  type SelecaoItem,
} from "@/components/importacoes/ImportacaoTipos";
import { useImportacaoBackground } from "@/contexts/ImportacaoContext";

/**
 * Máquina de estados da página de importação (05-UI-SPEC §6.0):
 * envio → analisando → revisão → confirmando → resultado, com volta para
 * revisão preservando a seleção quando a confirmação falha (retry
 * idempotente — documentos já confirmados são pulados).
 *
 * Nada é gravado antes do usuário confirmar (IMP-09); o BFF valida os
 * limites de novo (D-29) e o erro 401 redireciona para /login?expirada=1.
 */

type Estado = "envio" | "analisando" | "revisao" | "confirmando" | "resultado";

type ErroAnalise = { titulo: string; descricao: string };

const MAXIMO_ARQUIVOS = 5;
const LIMITE_ARQUIVO_BYTES = 4 * 1024 * 1024; // 4 MB
const LIMITE_LOTE_BYTES = 4.5 * 1024 * 1024; // 4,5 MB (limite de corpo da Vercel)
const TOLERANCIA_FATURA_CENTAVOS = 5; // R$ 0,05 (mesma tolerância da API)

type ImportacaoClientProps = {
  categorias: CategoriaOption[];
  historicoInicial: ImportacaoHistorico | null;
};

export function ImportacaoClient({
  categorias,
  historicoInicial,
}: ImportacaoClientProps) {
  const router = useRouter();
  const irParaLogin = React.useCallback(
    () => router.push("/login?expirada=1"),
    [router],
  );

  const {
    statusBg,
    progressoBg,
    totalArquivosBg,
    resultadoBg,
    erroBg,
    iniciarAnaliseBackground,
    limparStatusBackground,
  } = useImportacaoBackground();

  const [estadoInterno, setEstado] = React.useState<Estado>(
    statusBg === "analisando" ? "analisando" : "envio",
  );
  const estado = statusBg === "analisando" ? "analisando" : estadoInterno;
  const [arquivos, setArquivos] = React.useState<ArquivoValido[]>([]);
  const [errosValidacao, setErrosValidacao] = React.useState<string[]>([]);
  const [erroAnalise, setErroAnalise] = React.useState<ErroAnalise | null>(
    null,
  );
  const [analise, setAnalise] =
    React.useState<ImportacaoAnaliseResponse | null>(null);
  const [selecao, setSelecao] = React.useState<Record<number, SelecaoItem>>({});
  const [erroConfirmacao, setErroConfirmacao] = React.useState<string | null>(
    null,
  );
  const [erroAbrirPendente, setErroAbrirPendente] = React.useState<
    string | null
  >(null);
  const [carregandoPendenteId, setCarregandoPendenteId] = React.useState<
    number | null
  >(null);
  const [resultado, setResultado] = React.useState<{
    lancamentos: number;
    contas: number;
    desmarcados: number;
  } | null>(null);
  const [refreshHistorico, setRefreshHistorico] = React.useState(0);
  const [announce, setAnnounce] = React.useState("");
  const [docsConfirmados, setDocsConfirmados] = React.useState<Set<number>>(
    new Set(),
  );

  // 5.1 — Controle de etapa da barra de progresso
  type EtapaImportacao =
    | "ENVIO"
    | "LEITURA"
    | "CATEGORIZACAO"
    | "GRAVACAO"
    | "CONCLUIDO";
  const [etapaProgresso, setEtapaProgresso] = React.useState<EtapaImportacao>(
    "ENVIO",
  );

  const envioH2Ref = React.useRef<HTMLHeadingElement>(null);
  const resumoH2Ref = React.useRef<HTMLHeadingElement>(null);
  const resultadoRef = React.useRef<HTMLHeadingElement>(null);
  const erroRef = React.useRef<HTMLHeadingElement>(null);
  const prevEstadoRef = React.useRef<Estado | null>(null);

  /* ============================ seleção inicial ============================ */

  const iniciarSelecao = React.useCallback(
    (resposta: ImportacaoAnaliseResponse) => {
      const inicial: Record<number, SelecaoItem> = {};
      for (const doc of resposta.documentos) {
        for (const item of doc.itens) {
          inicial[item.id] = {
            incluir: true,
            categoriaId: item.categoriaId,
            manual: false,
          };
        }
      }
      setSelecao(inicial);
    },
    [],
  );

  /* =================== sincronização com análise em background =================== */

  React.useEffect(() => {
    if (statusBg === "analisando") {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      if (statusBg === "concluido" && resultadoBg) {
        setAnalise(resultadoBg);
        iniciarSelecao(resultadoBg);
        setDocsConfirmados(new Set());
        setErroAnalise(null);
        setErroConfirmacao(null);
        setEstado("revisao");
        setRefreshHistorico((k) => k + 1);
        setAnnounce("Análise concluída. Revise os itens antes de gravar.");
        limparStatusBackground();
        return;
      }

      if (statusBg === "erro" && erroBg) {
        setErroAnalise(erroBg);
        setEstado("envio");
        setAnnounce(`${erroBg.titulo}. ${erroBg.descricao}`);
        limparStatusBackground();
      }
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [statusBg, resultadoBg, erroBg, iniciarSelecao, limparStatusBackground]);

  /* ============================ foco entre estados ============================ */

  React.useEffect(() => {
    const anterior = prevEstadoRef.current;
    prevEstadoRef.current = estado;
    if (anterior === null) return; // primeira renderização: sem roubo de foco
    if (anterior === "analisando" && estado === "revisao") {
      resumoH2Ref.current?.focus();
    }
    if (estado === "resultado") {
      resultadoRef.current?.focus();
    }
  }, [estado]);

  React.useEffect(() => {
    if (erroConfirmacao) erroRef.current?.focus();
  }, [erroConfirmacao]);

  /* ============================ envio e validação ============================ */

  const adicionarArquivos = (novos: FileList | File[]) => {
    const erros = new Set<string>();
    const validos: ArquivoValido[] = [];
    for (const arquivo of Array.from(novos)) {
      const tipoOk =
        arquivo.type === "application/pdf" || /\.pdf$/i.test(arquivo.name);
      if (!tipoOk) {
        erros.add(
          "Este arquivo não é um PDF. Envie faturas de cartão ou extratos bancários em PDF.",
        );
        continue;
      }
      if (arquivo.size > LIMITE_ARQUIVO_BYTES) {
        erros.add(
          `O arquivo ${arquivo.name} tem ${formatarBytes(arquivo.size)} e o limite é 4 MB por arquivo. Envie um PDF menor.`,
        );
        continue;
      }
      if (arquivos.length + validos.length >= MAXIMO_ARQUIVOS) {
        erros.add(
          "Você pode enviar até 5 PDFs por vez. Remova um arquivo para continuar.",
        );
        continue;
      }
      validos.push({
        id: `${arquivo.name}-${arquivo.lastModified}-${arquivo.size}-${validos.length}`,
        file: arquivo,
        origem: origemInferida(arquivo.name),
      });
    }
    setErrosValidacao(erros.size > 0 ? Array.from(erros) : []);
    if (validos.length > 0) setArquivos((atual) => [...atual, ...validos]);
  };

  const removerArquivo = (id: string) => {
    setArquivos((atual) => atual.filter((a) => a.id !== id));
    setErrosValidacao([]);
  };

  const limparLista = () => {
    setArquivos([]);
    setErrosValidacao([]);
  };

  const novaImportacao = () => {
    limparStatusBackground();
    setArquivos([]);
    setErrosValidacao([]);
    setErroAnalise(null);
    setErroAbrirPendente(null);
    setAnalise(null);
    setSelecao({});
    setErroConfirmacao(null);
    setResultado(null);
    setDocsConfirmados(new Set());
    setEstado("envio");
    setAnnounce("Preparando nova importação.");
  };

  const somaLoteBytes = arquivos.reduce((total, a) => total + a.file.size, 0);
  const loteAcimaDoLimite = somaLoteBytes > LIMITE_LOTE_BYTES;
  const ctaDesabilitado =
    arquivos.length === 0 ||
    loteAcimaDoLimite ||
    estado === "analisando" ||
    statusBg === "analisando";

  const analisar = async () => {
    if (ctaDesabilitado) return;
    setErroAnalise(null);
    setErroAbrirPendente(null);
    setEstado("analisando");
    setEtapaProgresso("ENVIO");
    setAnnounce("Enviando arquivos para análise…");
    const timerLeitura = setTimeout(() => setEtapaProgresso("LEITURA"), 800);
    const timerCat = setTimeout(() => setEtapaProgresso("CATEGORIZACAO"), 2200);
    try {
      await iniciarAnaliseBackground(arquivos.map((a) => a.file));
    } finally {
      clearTimeout(timerLeitura);
      clearTimeout(timerCat);
    }
  };

  /* ===================== reabrir importação pendente (F5 / depois) ===================== */

  const abrirImportacaoPendente = React.useCallback(
    async (documentoId: number) => {
      setCarregandoPendenteId(documentoId);
      setErroAbrirPendente(null);
      setErroAnalise(null);
      setErroConfirmacao(null);
      try {
        const resposta = await obterImportacaoPendente(documentoId);
        setAnalise(resposta);
        iniciarSelecao(resposta);
        setDocsConfirmados(new Set());
        setResultado(null);
        setEstado("revisao");
        setAnnounce("Importação pendente carregada para revisão.");
        requestAnimationFrame(() => resumoH2Ref.current?.focus());
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          irParaLogin();
          return;
        }
        const mensagem =
          e instanceof Error && e.message
            ? e.message
            : "Não foi possível abrir esta importação. Tente novamente em instantes.";
        setErroAbrirPendente(mensagem);
        setAnnounce(mensagem);
      } finally {
        setCarregandoPendenteId(null);
      }
    },
    [iniciarSelecao, irParaLogin],
  );

  /* ============================ seleção e totais ============================ */

  const itens: ItemRevisao[] = React.useMemo(() => {
    if (!analise) return [];
    return analise.documentos.flatMap((doc) =>
      doc.itens.map((item) => ({
        ...item,
        documentoId: doc.documentoId,
        documentoNome: doc.nomeArquivo,
        documentoDegradado: doc.degradado === true,
      })),
    );
  }, [analise]);

  const marcados = React.useMemo(
    () => itens.filter((i) => selecao[i.id]?.incluir),
    [itens, selecao],
  );
  const semCategoria = React.useMemo(
    () => marcados.filter((i) => !selecao[i.id]?.categoriaId),
    [marcados, selecao],
  );

  // Totais recalculados a partir dos itens marcados — soma em centavos
  // inteiros (regra do centavo: a diferença para o exibido é sempre 0,00).
  const totais = React.useMemo(() => {
    const receita = somarCentavos(
      marcados.filter((i) => i.tipo === "RECEITA").map((i) => i.valor),
    );
    const pago = somarCentavos(
      marcados.filter((i) => i.tipo === "DESPESA_PAGA").map((i) => i.valor),
    );
    const aPagar = somarCentavos(
      marcados.filter((i) => i.tipo === "A_PAGAR").map((i) => i.valor),
    );
    return { receita, pago, aPagar, saldo: receita - pago - aPagar };
  }, [marcados]);

  // Divergência com a fatura: soma CARTÃO (DESPESA_PAGA + A_PAGAR) marcada
  // vs total da fatura, mesma tolerância da API (± R$ 0,05).
  const faturaCentavos =
    analise?.resumo.totalFatura != null
      ? Math.round(analise.resumo.totalFatura * 100)
      : null;
  const somaCartaoCentavos = React.useMemo(
    () =>
      somarCentavos(
        marcados
          .filter(
            (i) =>
              i.origem === "CARTAO" &&
              (i.tipo === "DESPESA_PAGA" || i.tipo === "A_PAGAR"),
          )
          .map((i) => i.valor),
      ),
    [marcados],
  );
  const divergenciaCentavos =
    faturaCentavos !== null &&
    Math.abs(somaCartaoCentavos - faturaCentavos) > TOLERANCIA_FATURA_CENTAVOS
      ? Math.abs(somaCartaoCentavos - faturaCentavos)
      : null;

  const alternarItem = (itemId: number, incluir: boolean) => {
    setSelecao((s) => ({ ...s, [itemId]: { ...s[itemId], incluir } }));
  };

  const trocarCategoria = (itemId: number, categoriaId: number | null) => {
    setSelecao((s) => ({
      ...s,
      [itemId]: { ...s[itemId], categoriaId, manual: true },
    }));
  };

  /* ============================ alertas ============================ */

  const alertas: AlertaLista[] = React.useMemo(() => {
    if (!analise) return [];
    const lista: AlertaLista[] = [];

    for (const doc of analise.documentos) {
      if (doc.status === "DUPLICADO") {
        lista.push({
          id: `arq-dup-${doc.nomeArquivo}`,
          kind: "ARQUIVO_DUPLICADO",
          titulo: "Arquivo já importado",
          descricao: `${doc.nomeArquivo} já foi importado antes. Ele não será processado de novo.`,
        });
      } else if (doc.status === "SEM_TEXTO") {
        lista.push({
          id: `sem-texto-${doc.nomeArquivo}`,
          kind: "SEM_TEXTO",
          titulo: "PDF sem texto extraível",
          descricao: `${doc.nomeArquivo} é digitalizado e não tem texto extraível. Ele foi ignorado nesta análise — envie a versão digital do PDF.`,
          aviso: true,
        });
      } else if (doc.status === "ERRO" && doc.mensagem) {
        lista.push({
          id: `erro-${doc.nomeArquivo}`,
          kind: "ERRO_DOC",
          titulo: "Erro na análise",
          descricao: doc.mensagem,
        });
      }
    }

    const rejeitados = analise.documentos.reduce(
      (total, d) => total + d.rejeitados.length,
      0,
    );
    if (rejeitados > 0) {
      lista.push({
        id: "rejeitados",
        kind: "ITEM_REJEITADO",
        titulo: "Itens ignorados pela leitura",
        descricao:
          rejeitados === 1
            ? "1 item não pôde ser lido e foi ignorado. Nada foi gravado a partir dele."
            : `${rejeitados} itens não puderam ser lidos e foram ignorados. Nada foi gravado a partir deles.`,
      });
    }

    for (const alerta of analise.resumo.alertas) {
      if (
        alerta.tipo === "CATEGORIA_REVISAR" ||
        alerta.tipo === "DIVERGENCIA_FATURA" ||
        alerta.tipo === "GASTO_ATIPICO"
      ) {
        continue;
      }
      if (alerta.tipo === "DUPLICIDADE") {
        const n = itens.filter((i) =>
          itemTemTag(i.alerta, "DUPLICIDADE"),
        ).length;
        lista.push({
          id: "duplicidade",
          kind: "DUPLICIDADE",
          titulo: "Possível duplicidade",
          descricao: `${n} ${n === 1 ? "lançamento tem" : "lançamentos têm"} a mesma data, valor e descrição. Revise se são compras distintas.`,
        });
      } else if (alerta.tipo === "FATURA_NO_EXTRATO") {
        lista.push({
          id: "fatura-no-extrato",
          kind: "PAGAMENTO_FATURA",
          titulo: "Pagamento da própria fatura",
          descricao:
            "O pagamento desta fatura aparece no extrato. Confira se não é a mesma compra sendo contada duas vezes.",
        });
      } else {
        lista.push({
          id: `api-${alerta.tipo}`,
          kind: alerta.tipo,
          titulo: "Atenção",
          descricao: alerta.mensagem,
        });
      }
    }

    for (const gasto of analise.resumo.gastosAtipicos) {
      const item = itens.find(
        (i) =>
          i.descricao === gasto.descricao &&
          Math.round(i.valor * 100) === Math.round(gasto.valor * 100),
      );
      lista.push({
        id: `atipico-${gasto.categoria}-${Math.round(gasto.valor * 100)}`,
        kind: "GASTO_ATIPICO",
        titulo: "Gasto atípico",
        descricao: `${gasto.descricao} de ${MONETARY.format(gasto.valor)} está acima do padrão da categoria ${gasto.categoria} neste período.`,
        aviso: true,
        alvoItemId: item?.id,
      });
    }

    if (divergenciaCentavos !== null && faturaCentavos !== null) {
      lista.push({
        id: "divergencia",
        kind: "DIVERGENCIA_SOMA",
        titulo: "Soma diferente do total da fatura",
        descricao: `A soma dos itens (${MONETARY.format(centavos(somaCartaoCentavos))}) não bate com o total da fatura (${MONETARY.format(centavos(faturaCentavos))}). Diferença de ${MONETARY.format(centavos(divergenciaCentavos))}.`,
      });
    }

    const revisarCategoria = marcados.filter(
      (i) =>
        itemTemTag(i.alerta, "REVISAR_CATEGORIA") && !selecao[i.id]?.manual,
    );
    if (revisarCategoria.length > 0) {
      const n = revisarCategoria.length;
      lista.push({
        id: "revisar-categoria",
        kind: "REVISAR_CATEGORIA",
        titulo: "Categoria para revisar",
        descricao: `${n} ${n === 1 ? "item usa" : "itens usam"} a categoria padrão porque a IA não reconheceu a categoria original. Escolha a categoria certa na lista abaixo.`,
        alvoItemId: revisarCategoria[0]?.id,
      });
    }

    return lista;
  }, [
    analise,
    itens,
    marcados,
    selecao,
    somaCartaoCentavos,
    faturaCentavos,
    divergenciaCentavos,
  ]);

  /* ============================ confirmação ============================ */

  const confirmarDesabilitado =
    estado === "confirmando" ||
    marcados.length === 0 ||
    semCategoria.length > 0;

  const confirmar = async () => {
    if (!analise || confirmarDesabilitado) return;
    setErroConfirmacao(null);
    setEstado("confirmando");
    setEtapaProgresso("GRAVACAO");
    setAnnounce("Confirmando importação…");

    const porDocumento = new Map<number, ConfirmarItemRequest[]>();
    for (const item of marcados) {
      if (item.documentoId == null) continue;
      const lista = porDocumento.get(item.documentoId) ?? [];
      lista.push({
        itemId: item.id,
        incluir: true,
        categoriaIdOverride: selecao[item.id]?.categoriaId ?? null,
      });
      porDocumento.set(item.documentoId, lista);
    }

    let lancamentos = 0;
    let contas = 0;
    try {
      for (const [documentoId, itensDoc] of porDocumento) {
        if (docsConfirmados.has(documentoId)) continue;
        const resposta = await confirmarImportacao(documentoId, {
          itens: itensDoc,
        });
        lancamentos += resposta.lancamentoIds.length;
        contas += resposta.contaAPagarIds.length;
        setDocsConfirmados((s) => new Set(s).add(documentoId));
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        irParaLogin();
        return;
      }
      const mensagem =
        e instanceof ApiError && e.message
          ? e.message
          : "Nenhum lançamento foi gravado. Verifique a importação e tente novamente.";
      setErroConfirmacao(mensagem);
      setEstado("revisao");
      setAnnounce(mensagem);
      return;
    }

    setEtapaProgresso("CONCLUIDO");
    setResultado({
      lancamentos,
      contas,
      desmarcados: itens.length - marcados.length,
    });
    setEstado("resultado");
    setRefreshHistorico((k) => k + 1);
    setAnnounce("Importação confirmada.");
  };

  /* ============================ estados derivados ============================ */

  const estadosArquivos: Record<string, EstadoArquivo> = React.useMemo(() => {
    const mapa: Record<string, EstadoArquivo> = {};
    for (const a of arquivos) {
      mapa[a.id] = estado === "analisando" ? "analisando" : "aguardando";
    }
    return mapa;
  }, [arquivos, estado]);

  const progressoAtual = Math.round(progressoBg);
  const totalEmAnalise =
    arquivos.length > 0 ? arquivos.length : totalArquivosBg;
  const nAnalisando =
    totalEmAnalise > 0
      ? Math.max(
          1,
          Math.min(
            totalEmAnalise,
            Math.ceil((progressoAtual / 95) * totalEmAnalise),
          ),
        )
      : 0;

  const docsAnalisadosCount = React.useMemo(
    () =>
      analise?.documentos.filter((d) => d.status === "ANALISADO").length ?? 0,
    [analise],
  );
  const subtextoDocumentos = React.useMemo(() => {
    const n = docsAnalisadosCount;
    if (n === 0) return "Nenhum documento analisado nesta importação.";
    const tipos: string[] = [];
    for (const doc of analise?.documentos ?? []) {
      if (doc.status !== "ANALISADO") continue;
      const rotulo =
        doc.tipoDocumento === "CARTAO"
          ? "fatura de cartão"
          : doc.tipoDocumento === "EXTRATO"
            ? "extrato bancário"
            : null;
      if (rotulo && !tipos.includes(rotulo)) tipos.push(rotulo);
    }
    if (tipos.length === 0) return `${n} documentos analisados`;
    if (n === 1) return `1 documento · ${tipos[0]}`;
    return `${n} documentos · ${tipos.join(" e ")}`;
  }, [analise, docsAnalisadosCount]);

  /* ============================ render ============================ */

  return (
    <div>
      <p role="status" aria-live="polite" className="sr-only">
        {announce}
      </p>

      {/* ============ Seção 1 — Enviar PDFs (estados envio / analisando) ============ */}
      {(estado === "envio" || estado === "analisando") && (
        <section className="py-6 md:py-8" aria-labelledby="h2-envio">
          <h2
            id="h2-envio"
            ref={envioH2Ref}
            tabIndex={-1}
            className="mb-6 text-lg font-semibold text-foreground"
          >
            Enviar PDFs
          </h2>

          {estado === "envio" && erroAnalise && (
            <Alert variant="destructive" className="mb-4">
              <AlertTitle>{erroAnalise.titulo}</AlertTitle>
              <AlertDescription>{erroAnalise.descricao}</AlertDescription>
              {arquivos.length > 0 && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => void analisar()}
                >
                  Tentar novamente
                </Button>
              )}
            </Alert>
          )}

          {estado === "analisando" ? (
            <div>
              <BarraProgressoImportacao
                modo="analise"
                progresso={progressoAtual}
                totalArquivos={totalEmAnalise}
                arquivoAtualIndex={nAnalisando}
              />
              <p className="mt-3 text-sm text-muted-foreground">
                Você pode navegar para outras abas enquanto processamos —
                avisaremos no topo quando terminar.
              </p>
              {arquivos.length > 0 && (
                <ImportFileList
                  arquivos={arquivos}
                  estados={estadosArquivos}
                  onRemover={removerArquivo}
                  disabled
                />
              )}
              <div className="mt-4 space-y-2">
                <Skeleton className="h-10 w-full rounded-md" />
                <Skeleton className="h-10 w-full rounded-md" />
                <Skeleton className="h-10 w-full rounded-md" />
              </div>
              <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
                <Button
                  type="button"
                  disabled
                  className="h-11 w-full sm:w-auto"
                >
                  <Spinner /> Analisando…
                </Button>
              </div>
            </div>
          ) : (
            <>
              <UploadDropzone onArquivosSelecionados={adicionarArquivos} />
              <p id={ID_LIMITES} className="mt-2 text-sm text-muted-foreground">
                Até 5 PDFs por importação, 4 MB por arquivo.
              </p>

              {errosValidacao.length > 0 && (
                <Alert variant="destructive" className="mt-3">
                  <AlertDescription>
                    <ul className="list-inside list-disc space-y-1">
                      {errosValidacao.map((msg, i) => (
                        <li key={i}>{msg}</li>
                      ))}
                    </ul>
                  </AlertDescription>
                </Alert>
              )}

              {loteAcimaDoLimite && (
                <Alert variant="destructive" className="mt-3">
                  <AlertDescription>
                    O envio somaria {formatarBytes(somaLoteBytes)} e o limite é
                    4,5 MB por requisição. Envie menos arquivos.
                  </AlertDescription>
                </Alert>
              )}

              {arquivos.length > 0 && (
                <div className="mt-4">
                  <ImportFileList
                    arquivos={arquivos}
                    estados={estadosArquivos}
                    onRemover={removerArquivo}
                  />
                </div>
              )}

              <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
                <Button
                  type="button"
                  onClick={() => void analisar()}
                  disabled={ctaDesabilitado}
                  className="h-11 w-full sm:w-auto"
                >
                  Analisar PDFs
                </Button>
                {arquivos.length > 0 && (
                  <Button type="button" variant="ghost" onClick={limparLista}>
                    Limpar lista
                  </Button>
                )}
              </div>
              {arquivos.length === 0 && (
                <p className="mt-2 text-sm text-muted-foreground">
                  Selecione ao menos um PDF para analisar.
                </p>
              )}
              <p className="mt-3 text-sm text-muted-foreground">
                Nenhum lançamento é gravado antes da sua confirmação.
              </p>
            </>
          )}
        </section>
      )}

      {/* ============ Seções 2–4 — Revisão (estados revisão / confirmando) ============ */}
      {(estado === "revisao" || estado === "confirmando") && analise && (
        <>
          <section className="py-6 md:py-8" aria-labelledby="h2-resumo">
            <div className="mb-6">
              <h2
                id="h2-resumo"
                ref={resumoH2Ref}
                tabIndex={-1}
                className="text-lg font-semibold text-foreground"
              >
                Resumo do mês
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {subtextoDocumentos}
              </p>
            </div>

            <TooltipProvider delayDuration={100}>
              <div className="mb-6 flex flex-wrap gap-2">
                {analise.documentos.map((doc) => (
                  <span
                    key={doc.nomeArquivo}
                    className="inline-flex items-center gap-1.5 rounded-full border border-border bg-card px-2.5 py-0.5 text-xs"
                  >
                    <span
                      className="max-w-[220px] truncate text-foreground"
                      title={doc.nomeArquivo}
                    >
                      {doc.nomeArquivo}
                    </span>
                    {doc.degradado === true && (
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Badge variant="warning">
                            Extraído com modelo de fallback
                          </Badge>
                        </TooltipTrigger>
                        <TooltipContent>
                          Este documento foi analisado por um modelo de reserva,
                          menos preciso. Confira os valores antes de confirmar.
                        </TooltipContent>
                      </Tooltip>
                    )}
                    <Badge
                      variant={
                        doc.status === "ANALISADO"
                          ? "success"
                          : doc.status === "SEM_TEXTO"
                            ? "warning"
                            : doc.status === "ERRO"
                              ? "destructive"
                              : "outline"
                      }
                    >
                      {doc.status === "ANALISADO"
                        ? "Pronto"
                        : doc.status === "SEM_TEXTO"
                          ? "Ignorado"
                          : doc.status === "ERRO"
                            ? "Erro"
                            : "Duplicado"}
                    </Badge>
                  </span>
                ))}
              </div>
            </TooltipProvider>

            <ResumoMesImportacao
              totalRecebido={centavos(totais.receita)}
              totalPago={centavos(totais.pago)}
              totalComprometido={centavos(totais.aPagar)}
              saldoPrevisto={centavos(totais.saldo)}
              fatura={{
                total: analise.resumo.totalFatura,
                dataVencimento: analise.resumo.dataVencimento,
                divergencia:
                  divergenciaCentavos !== null
                    ? centavos(divergenciaCentavos)
                    : null,
              }}
            />
          </section>

          <section className="py-6 md:py-8" aria-labelledby="h2-alertas">
            <div className="mb-4 flex items-center gap-3">
              <h2
                id="h2-alertas"
                className="text-lg font-semibold text-foreground"
              >
                Alertas
              </h2>
              {alertas.length > 0 && (
                <Badge variant="outline">{alertas.length}</Badge>
              )}
            </div>

            {erroConfirmacao && (
              <Alert variant="destructive" className="mb-4">
                <AlertTitle ref={erroRef} tabIndex={-1}>
                  Não foi possível confirmar
                </AlertTitle>
                <AlertDescription>{erroConfirmacao}</AlertDescription>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => void confirmar()}
                >
                  Tentar novamente
                </Button>
              </Alert>
            )}

            <AlertasImportacao alertas={alertas} />
          </section>

          <section className="py-6 md:py-8" aria-labelledby="h2-itens">
            <div className="mb-6 flex items-center gap-3">
              <h2
                id="h2-itens"
                className="text-lg font-semibold text-foreground"
              >
                Itens para revisar
              </h2>
              <Badge variant="outline">{itens.length} itens</Badge>
            </div>

            <RevisaoItens
              itens={itens}
              selecao={selecao}
              categorias={categorias}
              onToggleItem={alternarItem}
              onCategoriaChange={trocarCategoria}
              disabled={estado === "confirmando"}
            />
          </section>

          {/* Barra de confirmação — sticky no rodapé do main */}
          <div className="sticky bottom-0 z-10 border-t border-border bg-background/95 px-1 py-3 backdrop-blur">
            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p aria-live="polite" className="text-sm text-muted-foreground">
                  {marcados.length} de {itens.length} itens marcados ·{" "}
                  {MONETARY.format(centavos(totais.saldo))}
                </p>
                {marcados.length === 0 && (
                  <p className="text-sm text-muted-foreground">
                    Marque ao menos um item para confirmar.
                  </p>
                )}
                {marcados.length > 0 && semCategoria.length > 0 && (
                  <p className="text-sm text-muted-foreground">
                    Escolha a categoria dos itens marcados antes de confirmar.
                  </p>
                )}
              </div>
              <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center">
                <Button type="button" variant="ghost" onClick={novaImportacao}>
                  Analisar outro arquivo
                </Button>
                <Button
                  type="button"
                  onClick={() => void confirmar()}
                  disabled={confirmarDesabilitado}
                  className="h-11"
                >
                  {estado === "confirmando" ? (
                    <BarraProgressoImportacao
                      modo="confirmacao"
                      progresso={
                        etapaProgresso === "CONCLUIDO"
                          ? 100
                          : etapaProgresso === "GRAVACAO"
                            ? 50
                            : 0
                      }
                      documentosConcluidos={docsConfirmados.size}
                      totalDocumentos={
                        analise?.documentos.length ?? marcados.length
                      }
                      documentoAtualNome="Gravando lançamentos…"
                    />
                  ) : (
                    "Confirmar importação"
                  )}
                </Button>
              </div>
            </div>
          </div>
        </>
      )}

      {/* ============ Estado — resultado da confirmação ============ */}
      {estado === "resultado" && resultado && (
        <section className="py-6 md:py-8">
          <ResultadoConfirmacao
            lancamentos={resultado.lancamentos}
            contasAPagar={resultado.contas}
            desmarcados={resultado.desmarcados}
            tituloRef={resultadoRef}
            onNova={novaImportacao}
          />

          <div className="mt-6">
            <ResumoMesImportacao
              totalRecebido={centavos(totais.receita)}
              totalPago={centavos(totais.pago)}
              totalComprometido={centavos(totais.aPagar)}
              saldoPrevisto={centavos(totais.saldo)}
              fatura={{
                total: analise?.resumo.totalFatura ?? null,
                dataVencimento: analise?.resumo.dataVencimento ?? null,
                divergencia:
                  divergenciaCentavos !== null
                    ? centavos(divergenciaCentavos)
                    : null,
              }}
            />
          </div>
        </section>
      )}

      {/* ============ Seção 5 — Importações pendentes (sempre visível) ============ */}
      <section className="py-6 md:py-8" aria-labelledby="h2-historico">
        <h2
          id="h2-historico"
          className="mb-6 text-lg font-semibold text-foreground"
        >
          Importações pendentes
        </h2>

        {erroAbrirPendente && (
          <Alert variant="destructive" className="mb-4">
            <AlertTitle>Não foi possível abrir a importação</AlertTitle>
            <AlertDescription>{erroAbrirPendente}</AlertDescription>
          </Alert>
        )}

        <ImportacoesPendentes
          inicial={historicoInicial}
          refreshKey={refreshHistorico}
          onSessaoExpirada={irParaLogin}
          onRevisar={abrirImportacaoPendente}
          carregandoRevisaoId={carregandoPendenteId}
        />
      </section>
    </div>
  );
}
