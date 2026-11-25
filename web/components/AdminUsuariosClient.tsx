"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { useRouter } from "next/navigation";
import { Pencil, RefreshCw, Trash2, UserPlus, Users } from "lucide-react";
import {
  actionAtualizarUsuario,
  actionConvidarUsuario,
  actionDesativarUsuario,
  actionExcluirUsuario,
  type ResultadoAcaoUsuario,
} from "@/app/admin/usuarios/actions";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Spinner } from "@/components/Spinner";
import {
  podeEditarUsuario,
  type RoleUsuario,
  type UsuarioAdmin,
} from "@/lib/usuarios-admin";

type Estado = "carregando" | "pronto" | "erro";
type SucessoAcao = Extract<ResultadoAcaoUsuario, { ok: true }>;

type Props = {
  roleAutor: RoleUsuario;
  emailAutor: string;
};

const ERRO_CONEXAO = "Não foi possível conectar. Tente novamente em instantes.";

const CLASSE_CAMPO =
  "h-10 w-full rounded-lg border border-input bg-background px-3 text-sm text-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50";

const DICA_ALTERACAO =
  "Não é possível alterar sua própria conta nem o último ADMIN ativo.";

function rotuloRole(role: RoleUsuario): string {
  return role === "ADMIN" ? "Administrador" : "Usuário";
}

async function lerErro(resposta: Response): Promise<string> {
  const corpo = (await resposta.json().catch(() => null)) as {
    detail?: string;
    erro?: string;
  } | null;
  return corpo?.detail ?? corpo?.erro ?? ERRO_CONEXAO;
}

export function AdminUsuariosClient({ roleAutor, emailAutor }: Props) {
  const router = useRouter();

  const [usuarios, setUsuarios] = useState<UsuarioAdmin[]>([]);
  const [estado, setEstado] = useState<Estado>("carregando");
  const [erroLista, setErroLista] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [erroAcao, setErroAcao] = useState<string | null>(null);
  const [processando, setProcessando] = useState(false);
  const carregamentoAtivo = useRef(0);

  const [editando, setEditando] = useState<UsuarioAdmin | null>(null);
  const [nomeEdicao, setNomeEdicao] = useState("");
  const [roleEdicao, setRoleEdicao] = useState<RoleUsuario>("USER");
  const [desativando, setDesativando] = useState<UsuarioAdmin | null>(null);
  const [excluindo, setExcluindo] = useState<UsuarioAdmin | null>(null);
  const [conviteAberto, setConviteAberto] = useState(false);
  const [emailConvite, setEmailConvite] = useState("");
  const [roleConvite, setRoleConvite] = useState<RoleUsuario>("USER");
  const [linkConvite, setLinkConvite] = useState<string | null>(null);

  const idAutor = useMemo(() => {
    const email = emailAutor.trim().toLowerCase();
    return (
      usuarios.find((usuario) => usuario.email?.trim().toLowerCase() === email)
        ?.id ?? null
    );
  }, [emailAutor, usuarios]);

  const buscarUsuarios = useCallback(async () => {
    const seq = ++carregamentoAtivo.current;
    const resposta = await fetch("/api/usuarios", { cache: "no-store" });
    if (resposta.status === 401) {
      router.push("/login?expirada=1");
      return { seq, dados: null };
    }
    if (!resposta.ok) throw new Error(await lerErro(resposta));

    const dados = (await resposta.json().catch(() => null)) as
      | UsuarioAdmin[]
      | null;
    return { seq, dados: Array.isArray(dados) ? dados : [] };
  }, [router]);

  useEffect(() => {
    let ativo = true;
    void buscarUsuarios()
      .then(({ seq, dados }) => {
        if (!ativo || !dados || seq !== carregamentoAtivo.current) return;
        setUsuarios(dados);
        setEstado("pronto");
      })
      .catch((erro: unknown) => {
        if (!ativo) return;
        setErroLista(erro instanceof Error ? erro.message : ERRO_CONEXAO);
        setEstado("erro");
      });
    return () => {
      ativo = false;
    };
  }, [buscarUsuarios]);

  async function carregar() {
    setEstado("carregando");
    setErroLista(null);
    try {
      const { seq, dados } = await buscarUsuarios();
      if (!dados || seq !== carregamentoAtivo.current) return;
      setUsuarios(dados);
      setEstado("pronto");
    } catch (erro) {
      setErroLista(erro instanceof Error ? erro.message : ERRO_CONEXAO);
      setEstado("erro");
    }
  }

  function recarregar() {
    void carregar();
  }

  function podeAlterar(usuario: UsuarioAdmin): boolean {
    return podeEditarUsuario(roleAutor, idAutor, {
      ...usuario,
      usuarios,
    });
  }

  async function executarAcao(
    acao: () => Promise<ResultadoAcaoUsuario>,
    aoSucesso: (resultado: SucessoAcao) => void,
  ) {
    setProcessando(true);
    setErroAcao(null);
    setAviso(null);

    try {
      const resultado = await acao();
      if (!resultado.ok) {
        setErroAcao(resultado.mensagem);
        return;
      }
      aoSucesso(resultado);
      setAviso(resultado.mensagem);
      await carregar();
    } catch {
      setErroAcao(ERRO_CONEXAO);
    } finally {
      setProcessando(false);
    }
  }

  function abrirEdicao(usuario: UsuarioAdmin) {
    setErroAcao(null);
    setAviso(null);
    setNomeEdicao(usuario.nome);
    setRoleEdicao(usuario.role);
    setEditando(usuario);
  }

  async function salvarEdicao(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editando) return;

    const id = editando.id;
    await executarAcao(
      () => actionAtualizarUsuario(id, nomeEdicao, roleEdicao),
      () => setEditando(null),
    );
  }

  async function confirmarDesativacao() {
    if (!desativando) return;
    const id = desativando.id;
    await executarAcao(
      () => actionDesativarUsuario(id),
      () => setDesativando(null),
    );
  }

  async function confirmarExclusao() {
    if (!excluindo) return;
    const id = excluindo.id;
    await executarAcao(
      () => actionExcluirUsuario(id),
      () => setExcluindo(null),
    );
  }

  async function enviarConvite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await executarAcao(
      () => actionConvidarUsuario(emailConvite, roleConvite),
      (resultado) => {
        setLinkConvite(
          resultado.token
            ? `${window.location.origin}/convite/${encodeURIComponent(resultado.token)}`
            : null,
        );
        setEmailConvite("");
      },
    );
  }

  return (
    <section
      aria-labelledby="usuarios-h"
      className="overflow-hidden rounded-xl border border-border bg-card"
    >
      <div className="flex flex-col gap-4 border-b border-border px-5 py-4 md:flex-row md:items-end md:justify-between">
        <div className="space-y-1">
          <h2
            id="usuarios-h"
            className="text-base font-semibold text-foreground"
          >
            Contas da empresa
          </h2>
          <p className="text-xs text-muted-foreground">
            Edite perfis, desative acessos e convide novas pessoas.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            variant="outline"
            className="h-10"
            onClick={recarregar}
            disabled={estado === "carregando" || processando}
          >
            <RefreshCw />
            Atualizar
          </Button>
          <Button
            type="button"
            className="h-10"
            onClick={() => {
              setErroAcao(null);
              setAviso(null);
              setLinkConvite(null);
              setConviteAberto(true);
            }}
            disabled={processando}
          >
            <UserPlus />
            Convidar usuário
          </Button>
        </div>
      </div>

      {aviso && (
        <div className="px-5 pt-4">
          <Alert>
            <AlertDescription>{aviso}</AlertDescription>
          </Alert>
        </div>
      )}
      {erroAcao && (
        <div className="px-5 pt-4">
          <Alert variant="destructive">
            <AlertDescription>{erroAcao}</AlertDescription>
          </Alert>
        </div>
      )}

      {estado === "carregando" && (
        <div className="flex min-h-32 items-center justify-center gap-2 px-5 py-10 text-sm text-muted-foreground">
          <Spinner />
          Carregando usuários…
        </div>
      )}

      {estado === "erro" && (
        <div className="space-y-3 p-5">
          <Alert variant="destructive">
            <AlertDescription>{erroLista}</AlertDescription>
          </Alert>
          <Button
            type="button"
            variant="outline"
            onClick={recarregar}
          >
            <RefreshCw />
            Tentar novamente
          </Button>
        </div>
      )}

      {estado === "pronto" && usuarios.length === 0 && (
        <div className="flex flex-col items-center gap-3 px-5 py-12 text-center">
          <Users aria-hidden="true" className="size-7 text-muted-foreground" />
          <div className="space-y-1">
            <p className="text-sm font-medium text-foreground">
              Nenhum usuário encontrado
            </p>
            <p className="text-sm text-muted-foreground">
              Convide uma pessoa para começar a gerenciar a empresa.
            </p>
          </div>
        </div>
      )}

      {estado === "pronto" && usuarios.length > 0 && (
        <>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="px-5">Nome</TableHead>
                  <TableHead>E-mail</TableHead>
                  <TableHead>Perfil</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {usuarios.map((usuario) => {
                  const alteravel = podeAlterar(usuario);
                  return (
                    <TableRow key={usuario.id}>
                      <TableCell className="px-5">
                        <span className="block font-medium text-foreground">
                          {usuario.nome}
                        </span>
                        <span className="text-xs tabular-nums text-muted-foreground">
                          #{usuario.id}
                        </span>
                      </TableCell>
                      <TableCell className="max-w-56 truncate text-muted-foreground">
                        {usuario.email}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            usuario.role === "ADMIN" ? "default" : "outline"
                          }
                        >
                          {rotuloRole(usuario.role)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={usuario.ativo ? "success" : "secondary"}
                        >
                          {usuario.ativo ? "Ativo" : "Inativo"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-wrap justify-end gap-2">
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={!alteravel || processando}
                            title={alteravel ? undefined : DICA_ALTERACAO}
                            onClick={() => abrirEdicao(usuario)}
                          >
                            <Pencil />
                            Editar
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={
                              !alteravel || !usuario.ativo || processando
                            }
                            title={alteravel ? undefined : DICA_ALTERACAO}
                            onClick={() => {
                              setErroAcao(null);
                              setAviso(null);
                              setDesativando(usuario);
                            }}
                          >
                            Desativar acesso
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="ghost"
                            className="text-destructive hover:text-destructive"
                            disabled={!alteravel || !usuario.ativo || processando}
                            title={alteravel ? undefined : DICA_ALTERACAO}
                            onClick={() => {
                              setErroAcao(null);
                              setAviso(null);
                              setExcluindo(usuario);
                            }}
                          >
                            <Trash2 />
                            Excluir
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
          <p className="border-t border-border px-5 py-3 text-xs text-muted-foreground">
            Excluir uma conta é uma exclusão lógica: o acesso é desativado e os
            dados permanecem na empresa.
          </p>
        </>
      )}

      <Dialog
        open={editando !== null}
        onOpenChange={(aberto) => {
          if (!aberto && !processando) setEditando(null);
        }}
      >
        <DialogContent>
          <form onSubmit={(event) => void salvarEdicao(event)}>
            <DialogHeader>
              <DialogTitle>Editar usuário</DialogTitle>
              <DialogDescription>
                Altere o nome e o perfil de {editando?.nome ?? "este usuário"}.
              </DialogDescription>
            </DialogHeader>
            <DialogBody className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="editar-nome">Nome</Label>
                <Input
                  id="editar-nome"
                  value={nomeEdicao}
                  maxLength={120}
                  required
                  className="h-10"
                  onChange={(event) => setNomeEdicao(event.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="editar-perfil">Perfil</Label>
                <select
                  id="editar-perfil"
                  value={roleEdicao}
                  onChange={(event) =>
                    setRoleEdicao(event.target.value as RoleUsuario)
                  }
                  className={CLASSE_CAMPO}
                >
                  <option value="USER">Usuário</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
            </DialogBody>
            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                disabled={processando}
                onClick={() => setEditando(null)}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={processando}>
                {processando ? <Spinner /> : null}
                Salvar alterações
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog
        open={desativando !== null}
        onOpenChange={(aberto) => {
          if (!aberto && !processando) setDesativando(null);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Desativar acesso</DialogTitle>
            <DialogDescription>
              {desativando?.nome ?? "Este usuário"} não poderá entrar novamente.
              Os dados da conta permanecem na empresa.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              disabled={processando}
              onClick={() => setDesativando(null)}
            >
              Cancelar
            </Button>
            <Button
              type="button"
              disabled={processando}
              onClick={() => void confirmarDesativacao()}
            >
              {processando ? <Spinner /> : null}
              Desativar acesso
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={excluindo !== null}
        onOpenChange={(aberto) => {
          if (!aberto && !processando) setExcluindo(null);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirmar exclusão lógica</DialogTitle>
            <DialogDescription>
              O acesso de {excluindo?.nome ?? "este usuário"} será desativado e
              não poderá ser usado para entrar. Os lançamentos e demais dados
              permanecem na empresa. Deseja continuar?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              disabled={processando}
              onClick={() => setExcluindo(null)}
            >
              Cancelar
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={processando}
              onClick={() => void confirmarExclusao()}
            >
              {processando ? <Spinner /> : null}
              Excluir conta
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={conviteAberto}
        onOpenChange={(aberto) => {
          if (!aberto && !processando) setConviteAberto(false);
        }}
      >
        <DialogContent>
          <form onSubmit={(event) => void enviarConvite(event)}>
            <DialogHeader>
              <DialogTitle>Convidar usuário</DialogTitle>
              <DialogDescription>
                O convite é criado para o e-mail informado com o perfil escolhido.
              </DialogDescription>
            </DialogHeader>
            <DialogBody className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="convite-email">E-mail</Label>
                <Input
                  id="convite-email"
                  type="email"
                  value={emailConvite}
                  maxLength={320}
                  required
                  className="h-10"
                  placeholder="pessoa@empresa.com"
                  onChange={(event) => setEmailConvite(event.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="convite-perfil">Perfil</Label>
                <select
                  id="convite-perfil"
                  value={roleConvite}
                  onChange={(event) =>
                    setRoleConvite(event.target.value as RoleUsuario)
                  }
                  className={CLASSE_CAMPO}
                >
                  <option value="USER">Usuário</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
              {linkConvite && (
                <div className="space-y-1.5">
                  <Label htmlFor="convite-link">Link do convite</Label>
                  <Input
                    id="convite-link"
                    readOnly
                    value={linkConvite}
                    onFocus={(event) => event.target.select()}
                    className="h-auto min-h-10 py-2 text-xs"
                  />
                  <p className="text-xs text-muted-foreground">
                    Envie este link ao convidado.
                  </p>
                </div>
              )}
            </DialogBody>
            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                disabled={processando}
                onClick={() => setConviteAberto(false)}
              >
                Fechar
              </Button>
              <Button type="submit" disabled={processando}>
                {processando ? <Spinner /> : null}
                Criar convite
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </section>
  );
}
