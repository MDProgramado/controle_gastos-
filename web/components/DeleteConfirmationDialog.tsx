"use client";

import * as React from "react";
import { cn } from "@/lib/utils";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
  DialogTrigger,
} from "@radix-ui/react-dialog";
import { Button } from "@/components/ui/button";

type Action = "excluir" | "desativar" | "descartar";

interface DeleteConfirmationDialogProps {
  isOpen: boolean;
  onConfirm: () => void;
  onClose: () => void;
  action: Action;
  target: string;
  /**
   * Texto do diálogo — quando presente substitui o padrão gerado de
   * `Tem certeza que deseja ${action} este ${target}?`. Usado pelo descarte
   * de importação (05-UI-SPEC §6.5).
   */
  descricao?: string;
}

/**
 * Diálogo reutilizável de confirmação destrutiva (excluir lançamento /
 * desativar categoria / descartar importação pendente). Nunca usa
 * `window.confirm` (D-24).
 *
 * Controlado externamente via `isOpen`/`onClose`. O chamador dispara
 * `onConfirm` após o usuário clicar no botão de confirmação.
 */
export function DeleteConfirmationDialog({
  isOpen,
  onConfirm,
  onClose,
  action,
  target,
  descricao,
}: DeleteConfirmationDialogProps) {
  const confirmLabel =
    action === "excluir" ? "Excluir" : action === "descartar" ? "Descartar" : "Desativar";
  const titulo = action === "descartar" ? "Confirmar descarte" : `Confirmar ${action}`;
  const textoPadrao =
    action === "descartar"
      ? "Tem certeza que deseja descartar esta importação pendente? Os itens revisados serão perdidos e o arquivo poderá ser enviado de novo."
      : `Tem certeza que deseja ${action} este ${target}?`;
  const texto = descricao ?? textoPadrao;

  return (
    <Dialog open={isOpen} onOpenChange={(aberto: boolean) => {
      if (!aberto) onClose();
    }}>
      <DialogTrigger asChild>
        <button style={{ display: "none" }} aria-hidden="true" />
      </DialogTrigger>
      <DialogContent
        className={cn(
          "fixed left-[50%] top-[50%] z-50 flex max-h-[90dvh] w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%] flex-col overflow-hidden rounded-lg border border-border bg-card p-6 text-card-foreground shadow-lg md:w-full md:max-w-md",
        )}
      >
        <DialogTitle className="font-semibold">{titulo}</DialogTitle>
        <DialogDescription className="mt-2 text-sm text-foreground">{texto}</DialogDescription>

        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="destructive"
            onClick={() => {
              onConfirm();
              onClose();
            }}
            className="w-full sm:w-auto"
          >
            {confirmLabel}
          </Button>
          <Button type="button" variant="ghost" onClick={onClose} className="w-full sm:w-auto">
            Cancelar
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
