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
          "rounded-lg border border-border bg-card p-6 shadow-lg sm:max-w-md",
        )}
      >
        <DialogTitle className="font-semibold">{titulo}</DialogTitle>
        <DialogDescription className="mt-2 text-sm text-foreground">{texto}</DialogDescription>

        <div className="mt-6 flex flex-row-reverse gap-2">
          <Button
            type="button"
            variant="destructive"
            onClick={() => {
              onConfirm();
              onClose();
            }}
          >
            {confirmLabel}
          </Button>
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
