"use client";

import * as React from "react";
import Link from "next/link";
import { cn } from "cn";
import { LogoutButton } from "@/components/LogoutButton";

type RouteItem = {
  href: string;
  label: string;
};

const ROTAS: RouteItem[] = [
  { href: "/", label: "Dashboard" },
  { href: "/lancamentos", label: "Lançamentos" },
  { href: "/categorias", label: "Categorias" },
];

interface TopNavProps {
  activeRoute: string;
  userRole: "ADMIN" | "USER";
}

/**
 * Navegação top compartilhada entre as 3 rotas (/, /lancamentos, /categorias)
 * + LogoutButton. Item ativo em teal #0F766E; inativo em muted-foreground #64748B.
 */
export function TopNav({ activeRoute, userRole }: TopNavProps) {
  const estaAtivo = (href: string) =>
    activeRoute === href || (href !== "/" && activeRoute.startsWith(href));

  // userRole mantido na interface para contrato; todas as rotas são visíveis
  // para ADMIN e USER (restrição de escrita está nas ações das páginas, D-23).
  void userRole;

  return (
    <nav className="sticky top-0 z-20 w-full border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex h-16 items-center justify-between px-4">
        {/* Marca */}
        <div className="mr-auto flex items-center gap-2">
          <Link
            href="/"
            className={cn(
              "flex items-center gap-2 px-3",
              estaAtivo("/")
                ? "text-primary font-semibold"
                : "text-muted-foreground hover:text-foreground",
            )}
          >
            <span className="text-[#0F766E] font-bold text-sm">Controle</span>
            <span className="text-muted-foreground font-semibold text-sm">
              Financeiro
            </span>
          </Link>
        </div>

        {/* Itens de navegação */}
        <div className="hidden md:flex items-center gap-1">
          {ROTAS.map((rota) => (
            <Link
              key={rota.href}
              href={rota.href}
              className={cn(
                "flex items-center gap-2 px-3 py-2 text-sm rounded-lg transition-colors",
                estaAtivo(rota.href)
                  ? "bg-accent text-primary font-medium"
                  : "text-muted-foreground hover:text-foreground hover:bg-muted",
              )}
              aria-current={estaAtivo(rota.href) ? "page" : undefined}
            >
              {rota.label}
            </Link>
          ))}
        </div>

        {/* Ações à direita */}
        <div className="flex items-center gap-2">
          <LogoutButton />
        </div>
      </div>
    </nav>
  );
}
