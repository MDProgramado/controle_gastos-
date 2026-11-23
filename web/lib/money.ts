export type ValorMonetario = string | number;

const ZERO = BigInt(0);
const CEM = BigInt(100);

function textoDoValor(valor: ValorMonetario): string | null {
  if (typeof valor === "number") {
    return Number.isFinite(valor) ? valor.toString() : null;
  }
  return valor;
}

export function centavosDeString(valor: ValorMonetario): bigint | null {
  const textoOriginal = textoDoValor(valor);
  if (textoOriginal === null) {
    return null;
  }

  let texto = textoOriginal.trim().replace(/\s+/g, "").replace(/^R\$/, "");

  if (!texto) {
    return null;
  }

  const temPonto = texto.includes(".");
  const temVirgula = texto.includes(",");

  if (temPonto && temVirgula) {
    const decimal = texto.lastIndexOf(".") > texto.lastIndexOf(",") ? "." : ",";
    const milhar = decimal === "." ? "," : ".";
    texto = texto.split(milhar).join("").replace(decimal, ".");
  } else if (temVirgula) {
    texto = texto.replace(/\./g, "").replace(",", ".");
  } else if (temPonto) {
    const partes = texto.split(".");
    if (partes.length === 2 && partes[0].length > 0 && /^\d{3}$/.test(partes[1])) {
      texto = partes.join("");
    }
  }

  if (!/^\d+(\.\d{1,2})?$/.test(texto)) {
    return null;
  }

  const [inteira, casas = ""] = texto.split(".");
  const centavos = BigInt(inteira) * CEM + BigInt(casas.padEnd(2, "0"));

  return centavos > ZERO ? centavos : null;
}

export function centavosAssinadosDeString(
  valor: ValorMonetario,
): bigint | null {
  const textoOriginal = textoDoValor(valor);
  if (textoOriginal === null) {
    return null;
  }

  const texto = textoOriginal.trim();
  if (!texto) {
    return null;
  }

  const negativo = texto.startsWith("-");
  const semSinal = texto.replace(/^[+-]/, "");
  const centavos = centavosDeString(semSinal);

  if (centavos === null) {
    return null;
  }

  return negativo && centavos > ZERO ? -centavos : centavos;
}

export function formatarValorApi(centavos: bigint): string {
  const texto = centavos.toString().padStart(3, "0");
  return `${texto.slice(0, -2)}.${texto.slice(-2)}`;
}

export function normalizarValorParaApi(valor: string): string | null {
  const centavos = centavosDeString(valor);
  return centavos === null ? null : formatarValorApi(centavos);
}

export function validarValorLote(valor: string): string | null {
  if (!valor.trim()) {
    return "Informe um valor maior que zero";
  }

  if (centavosDeString(valor) !== null) {
    return null;
  }

  const texto = valor.trim().replace(/\s+/g, "").replace(/^R\$/, "");
  if (/\d+[.,]\d{3,}$/.test(texto)) {
    return "Use no máximo 2 casas decimais";
  }

  return "Informe um valor maior que zero";
}
