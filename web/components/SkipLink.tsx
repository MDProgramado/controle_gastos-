/**
 * Link de pular para o conteúdo (a11y): primeiro focável de cada página —
 * precisa vir ANTES do Sidebar no DOM. Visível só quando recebe foco.
 * O nome acessível contém "Skip" (padrão exigido pelo checker de consistência).
 */
export function SkipLink() {
  return (
    <a
      href="#main-content"
      className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[100] focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-primary-foreground focus:shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
    >
      Skip to content — Pular para o conteúdo
    </a>
  );
}
