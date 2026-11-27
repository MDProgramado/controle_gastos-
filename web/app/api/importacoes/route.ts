import { encaminharParaApi } from "@/lib/bff";
import { IMPORTACAO_TIMEOUT_MS } from "@/lib/bff-helpers";

const MAXIMO_ARQUIVOS = 5;
const LIMITE_ARQUIVO_BYTES = 4 * 1024 * 1024;
const LIMITE_LOTE_BYTES = 4.5 * 1024 * 1024;

export const maxDuration = 120;

export async function POST(request: Request) {
  let form: FormData;
  try {
    form = await request.formData();
  } catch {
    return Response.json({ erro: "corpo inválido" }, { status: 400 });
  }

  const arquivos = form
    .getAll("arquivos")
    .filter((valor): valor is File => valor instanceof File);

  if (arquivos.length === 0) {
    return Response.json({ erro: "Envie pelo menos um arquivo PDF." }, { status: 400 });
  }
  if (arquivos.length > MAXIMO_ARQUIVOS) {
    return Response.json(
      { erro: "Você pode enviar até 5 PDFs por vez. Remova um arquivo para continuar." },
      { status: 413 },
    );
  }

  let soma = 0;
  for (const arquivo of arquivos) {
    soma += arquivo.size;
    if (arquivo.size > LIMITE_ARQUIVO_BYTES) {
      return Response.json(
        {
          erro: `O arquivo ${arquivo.name} excede o limite de 4 MB por arquivo. Envie um PDF menor.`,
        },
        { status: 413 },
      );
    }
    const tipoOk = arquivo.type === "application/pdf" || /\.pdf$/i.test(arquivo.name);
    if (!tipoOk) {
      return Response.json(
        {
          erro: "Este arquivo não é um PDF. Envie faturas de cartão ou extratos bancários em PDF.",
        },
        { status: 400 },
      );
    }
  }
  if (soma > LIMITE_LOTE_BYTES) {
    return Response.json(
      {
        erro: `O envio somaria ${(soma / (1024 * 1024)).toFixed(1).replace(".", ",")} MB e o limite é 4,5 MB por requisição. Envie menos arquivos.`,
      },
      { status: 413 },
    );
  }

  const formAPI = new FormData();
  for (const arquivo of arquivos) {
    formAPI.append("arquivos", arquivo, arquivo.name);
  }

  return encaminharParaApi("/api/importacoes/pdf", {
    method: "POST",
    body: formAPI,
    contentType: null,
    timeoutMs: IMPORTACAO_TIMEOUT_MS,
  });
}

export async function GET(request: Request) {
  const searchParams = new URL(request.url).searchParams;
  return encaminharParaApi("/api/importacoes", {
    query: {
      pagina: String(parseInteiroSeguro(searchParams.get("pagina"), 0)),
      tamanho: String(parseInteiroSeguro(searchParams.get("tamanho"), 20)),
    },
  });
}

function parseInteiroSeguro(valor: string | null, padrao: number): number {
  const numero = Number(valor ?? "");
  if (!Number.isInteger(numero) || numero < 0 || numero > 1000) {
    return padrao;
  }
  return numero;
}
