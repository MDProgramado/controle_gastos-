function origem(url: string | undefined): string | null {
  if (!url?.trim()) return null;
  try {
    return new URL(url).origin.toLowerCase();
  } catch {
    return null;
  }
}

export function urlApiServidor(): string | null {
  const apiUrl = (process.env.API_URL ?? "").trim();

  if (process.env.VERCEL_ENV === "preview") {
    const preview = (process.env.API_URL_PREVIEW ?? "").trim();
    if (!preview) return null;

    const origemProducao = origem(process.env.API_URL_PRODUCAO);
    const origemPreview = origem(preview);
    if (!origemProducao || !origemPreview || origemPreview === origemProducao) return null;
    return preview;
  }

  return apiUrl || null;
}
