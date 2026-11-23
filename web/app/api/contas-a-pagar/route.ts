import { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

export async function GET(request: NextRequest) {
  return encaminharParaApi("/api/contas-a-pagar", {
    query: new URL(request.url).searchParams,
  });
}
