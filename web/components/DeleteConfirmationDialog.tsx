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

type Action = "excluir" | "desativar";

interface DeleteConfirmationDialogProps {
  isOpen: boolean;
  onConfirm: () => void;
  onClose: () => void;
  action: Action;
  target: string;
}

/**
 * Diálogo reutilizável de confirmação destrutiva (excluir lançamento /
 * desativar categoria). Nunca usa `window.confirm` (D-24).
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
}: DeleteConfirmationDialogProps) {
  const confirmLabel = action === "excluir" ? "Excluir" : "Desativar";
  const titulo = `Confirmar ${action}`;
  const texto = `Tem certeza que deseja ${action} este ${target}?`;

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
