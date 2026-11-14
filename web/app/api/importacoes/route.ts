import { cookies } from "next/headers";

/**
 * BFF da importação de PDFs (fase 5, D-28).
 *
 * O browser não fala direto com a API: ela exige o header Authorization
 * (Bearer) e o cookie "sess" é httpOnly. Esta rota valida os limites do envio
 * (D-29) e repassa o multipart para `${API_URL}/api/importacoes/pdf` sem
 * modificar o conteúdo — a API valida magic bytes e hash por arquivo.
 *
 * Limites do BFF (D-29, mais apertados que a API 8 MB/40 MB): até 5 arquivos,
 * 4 MB por arquivo e 4,5 MB no total (limite de corpo da Vercel). Estourar
 * qualquer um devolve 413 com mensagem clara em pt-BR, sem chegar à API.
 */

const MAXIMO_ARQUIVOS = 5;
const LIMITE_ARQUIVO_BYTES = 4 * 1024 * 1024; // 4 MB por arquivo
const LIMITE_LOTE_BYTES = 4.5 * 1024 * 1024; // limite de corpo da requisição (Vercel)

export async function POST(request: Request) {
  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

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
    // Tipo informado pelo browser pode vir vazio em alguns sistemas — a
    // extensão .pdf também vale; o conteúdo real é validado pela API (magic
    // bytes %PDF no início do arquivo).
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

  let res: Response;
  try {
    res = await fetch(`${process.env.API_URL}/api/importacoes/pdf`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      // Sem Content-Type manual: o fetch gera o multipart com boundary.
      body: formAPI,
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { erro: "Não foi possível conectar. Tente novamente em instantes." },
      { status: 502 },
    );
  }

  const body = await res.text();
  return new Response(body, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("content-type") ?? "application/json" },
  });
}

/** GET — histórico paginado de importações do tenant (passthrough do Bearer). */
export async function GET(request: Request) {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

  const searchParams = new URL(request.url).searchParams;
  const pagina = parseInteiroSeguro(searchParams.get("pagina"), 0);
  const tamanho = parseInteiroSeguro(searchParams.get("tamanho"), 20);

  let res: Response;
  try {
    res = await fetch(
      `${process.env.API_URL}/api/importacoes?pagina=${pagina}&tamanho=${tamanho}`,
      {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      },
    );
  } catch {
    return Response.json(
      { erro: "Não foi possível conectar. Tente novamente em instantes." },
      { status: 502 },
    );
  }

  const body = await res.text();
  return new Response(body, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("content-type") ?? "application/json" },
  });
}

/** Aceita só inteiros não negativos; qualquer coisa fora vira o padrão. */
function parseInteiroSeguro(valor: string | null, padrao: number): number {
  const numero = Number(valor ?? "");
  if (!Number.isInteger(numero) || numero < 0 || numero > 1000) {
    return padrao;
  }
  return numero;
}
