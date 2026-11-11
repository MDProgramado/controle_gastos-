import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

/** Wrapper do `Loader2` (lucide-react) — único ícone da fase 1 (UI-SPEC). */
export function Spinner({ className }: { className?: string }) {
  return (
    <Loader2 data-slot="spinner" aria-hidden="true" className={cn("size-4 animate-spin", className)} />
  );
}
