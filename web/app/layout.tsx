import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Controle Financeiro",
  description: "Controle de entradas e saídas da empresa",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR" className="dark">
      <body>{children}</body>
    </html>
  );
}
