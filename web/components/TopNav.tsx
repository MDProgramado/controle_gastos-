"use client";

import * as React from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import Image from "next/image";
import { itensDeNavegacao, type RoleUsuario } from "@/lib/navegacao";

interface TopNavProps {
  activeRoute: string;
  userRole?: RoleUsuario | null;
}

/**
 * Navegação top compartilhada entre as 3 rotas (/, /lancamentos, /categorias)
 * + LogoutButton. Item ativo em teal #0F766E; inativo em muted-foreground #64748B.
 */
export function TopNav({ activeRoute, userRole = null }: TopNavProps) {
  const estaAtivo = (href: string) =>
    activeRoute === href || (href !== "/" && activeRoute.startsWith(href));
  const rotas = React.useMemo(() => itensDeNavegacao(userRole), [userRole]);

  return (
    <nav className="sticky top-0 z-20 w-full border-b border-border bg-background/90 backdrop-blur supports-[backdrop-filter]:bg-background/70">
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
            <Image 
            src="ogum.svg"
            alt="logo"
            width={100}
            height={100}
            />
            <span className="text-foreground font-semibold text-sm">PLUTO </span>
            
            
          </Link>
        </div>

        {/* Itens de navegação */}
        <div className="hidden md:flex items-center gap-1">
          {rotas.map((rota) => (
            <Link
              key={rota.href}
              href={rota.href}
              className={cn(
                "flex items-center gap-2 px-3 py-2 text-sm rounded-lg transition-colors",
                estaAtivo(rota.href)
                  ? "bg-accent text-primary font-semibold"
                  : "text-muted-foreground hover:text-foreground hover:bg-muted",
              )}
              aria-current={estaAtivo(rota.href) ? "page" : undefined}
            >
              {rota.label}
            </Link>
          ))}
        </div>

      </div>
    </nav>
  );
}
