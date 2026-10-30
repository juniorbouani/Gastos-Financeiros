/**
 * Integra segurança da conta, notificações, categorias e orçamentos.
 * Fluxo: carregar dados → validar a ação → chamar a API → atualizar a interface.
 */
(async () => {
  const obterElemento = (id) => document.getElementById(id),
    tipoPagina = document.body.dataset.panelPage;
  const token = sessionStorage.getItem("token");
  let usuario,
    emExecucao = false,
    sessaoEncerrada = false,
    editing = null,
    paginaAtual = 0,
    totalPaginas = 0;
  const formatarMoeda = (v) =>
    v != null && Number.isFinite(Number(v))
      ? Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
      : "—";
  function exibirMensagem(texto, erro = false) {
    obterElemento("panel-message").textContent = texto;
    obterElemento("panel-message").dataset.error = String(erro);
  }
  function criarElemento(tag, texto, classe = "") {
    const elemento = document.createElement(tag);
    elemento.className = classe;
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
  }
  function criarBotao(texto, acao) {
    const botao = criarElemento("button", texto);
    botao.type = "button";
    botao.addEventListener("click", () => executarAcao(acao));
    return botao;
  }
  function limparCamposDeSenha() {
    for (const id of ["current-password", "new-password", "confirm-password"])
      if (obterElemento(id)) obterElemento(id).value = "";
  }
  // Limpa o acesso desta aba; a revogação no servidor depende do fluxo de segurança.
  function encerrarSessaoLocal(evento) {
    evento?.preventDefault();
    sessaoEncerrada = true;
    limparCamposDeSenha();
    obterElemento("panel-list")?.replaceChildren();
    sessionStorage.removeItem("token");
    window.location.replace("../index.html");
  }
  for (const link of document.querySelectorAll(".logout"))
    link.addEventListener("click", encerrarSessaoLocal);
  window.addEventListener("pageshow", (evento) => {
    if (evento.persisted) window.location.reload();
  });
  if (!token) {
    encerrarSessaoLocal();
    return;
  }
  class ErroDaApi extends Error {
    constructor(texto, status) {
      super(texto);
      this.status = status;
    }
  }
  // Centraliza HTTP, autenticação e tratamento das respostas da API.
  async function requisitarApi(caminho, metodo = "GET", corpo) {
    const controller = new AbortController(),
      timer = setTimeout(() => controller.abort(), 15000);
    try {
      const resposta = await fetch(`../${caminho}`, {
        method: metodo,
        headers: {
          Authorization: `Bearer ${token}`,
          ...(corpo ? { "Content-Type": "application/json" } : {}),
        },
        ...(corpo ? { body: JSON.stringify(corpo) } : {}),
        signal: controller.signal,
        cache: "no-store",
      });
      if (resposta.status === 401) {
        encerrarSessaoLocal();
        throw new ErroDaApi("Sessão inválida ou expirada.", 401);
      }
      if (!resposta.ok) {
        const dadosResposta = await resposta.json().catch(() => null);
        const values =
          dadosResposta && typeof dadosResposta === "object"
            ? Object.values(dadosResposta)
                .filter((v) => typeof v === "string")
                .join(" ")
            : "";
        throw new ErroDaApi(
          resposta.status < 500
            ? dadosResposta?.message || values || `Requisição recusada (HTTP ${resposta.status}).`
            : `Erro no servidor (HTTP ${resposta.status}).`,
          resposta.status,
        );
      }
      return resposta.status === 204 ? null : await resposta.json();
    } finally {
      clearTimeout(timer);
    }
  }
  function obterMensagemErro(erro) {
    return erro.name === "AbortError"
      ? "O servidor demorou para responder."
      : erro instanceof TypeError
        ? "A conexão falhou."
        : erro.message;
  }
  function atualizarPaginacao() {
    if (tipoPagina !== "notifications") return;
    obterElemento("page-previous").disabled =
      emExecucao || sessaoEncerrada || paginaAtual <= 0 || !totalPaginas;
    obterElemento("page-next").disabled =
      emExecucao || sessaoEncerrada || paginaAtual + 1 >= totalPaginas;
    obterElemento("only-unread").disabled = emExecucao;
    obterElemento("read-all").disabled = emExecucao;
  }
  // Evita ações simultâneas e apresenta erros sem repetir operações automaticamente.
  async function executarAcao(acao) {
    if (emExecucao || sessaoEncerrada) return;
    emExecucao = true;
    obterElemento("panel-refresh").disabled = true;
    if (obterElemento("budget-month")) obterElemento("budget-month").disabled = true;
    atualizarPaginacao();
    try {
      await acao();
    } catch (erro) {
      if (!sessaoEncerrada)
        exibirMensagem(
          `${obterMensagemErro(erro)} Atualize os dados para conferir o resultado antes de repetir uma alteração.`,
          true,
        );
    } finally {
      emExecucao = false;
      obterElemento("panel-refresh").disabled = false;
      if (obterElemento("budget-month")) obterElemento("budget-month").disabled = false;
      atualizarPaginacao();
    }
  }
  function normalizarValor(texto) {
    const m = /^(\d+)(?:[,.](\d{1,2}))?$/.exec(texto.trim());
    if (!m) throw new Error("Informe valor sem separador de milhar, como 500,50.");
    const c = Number(m[1]) * 100 + Number((m[2] || "").padEnd(2, "0"));
    if (!Number.isSafeInteger(c) || c <= 0)
      throw new Error("O limite deve ser positivo com até duas casas decimais.");
    return (c / 100).toFixed(2);
  }
  async function atualizarAposSalvar(texto, refresh) {
    exibirMensagem(texto);
    try {
      await refresh();
      if (!sessaoEncerrada) exibirMensagem(texto);
    } catch (erro) {
      if (!sessaoEncerrada)
        exibirMensagem(
          `${texto} Não foi possível atualizar a tela: ${obterMensagemErro(erro)}`,
          true,
        );
    }
  }
  // Atualiza a página de notificações e as ações de leitura da própria conta.
  async function carregarNotificacoes() {
    const list = obterElemento("panel-list");
    list.replaceChildren(criarElemento("p", "Carregando notificações..."));
    const only = obterElemento("only-unread").value === "true";
    const params = new URLSearchParams({
      onlyUnread: String(only),
      page: String(paginaAtual),
      size: "20",
    });
    params.append("sort", "dataCriacao,desc");
    params.append("sort", "id,desc");
    let paginaNotificacoes = await requisitarApi(`notifications?${params}`);
    if (sessaoEncerrada) return;
    if (
      !Array.isArray(paginaNotificacoes.content) ||
      !Number.isInteger(paginaNotificacoes.totalPages) ||
      !Number.isInteger(paginaNotificacoes.number)
    )
      throw new Error("Paginação de notificações inválida.");
    if (paginaNotificacoes.totalPages > 0 && paginaAtual >= paginaNotificacoes.totalPages) {
      paginaAtual = paginaNotificacoes.totalPages - 1;
      return carregarNotificacoes();
    }
    paginaAtual = paginaNotificacoes.number;
    totalPaginas = paginaNotificacoes.totalPages;
    list.replaceChildren();
    if (!paginaNotificacoes.content.length)
      list.appendChild(
        criarElemento(
          "p",
          only ? "Nenhuma notificação não lida." : "Nenhuma notificação nesta conta.",
        ),
      );
    for (const n of paginaNotificacoes.content) {
      const row = criarElemento("article", undefined, `panel-row ${n.lida ? "" : "unread"}`);
      row.append(
        criarElemento("strong", n.lida ? "Lida" : "Não lida"),
        criarElemento("p", n.mensagem),
        criarElemento(
          "p",
          String(n.dataCriacao || "")
            .replace("T", " ")
            .slice(0, 19),
        ),
      );
      if (!n.lida)
        row.appendChild(
          criarBotao("Marcar como lida", async () => {
            await requisitarApi(`notifications/${n.id}/read`, "PUT");
            await atualizarAposSalvar("Notificação marcada como lida.", carregarNotificacoes);
          }),
        );
      list.appendChild(row);
    }
    obterElemento("page-label").textContent = totalPaginas
      ? `Página ${paginaAtual + 1} de ${totalPaginas}`
      : "Nenhuma página";
    try {
      const count = await requisitarApi("notifications/unread-count");
      if (!sessaoEncerrada)
        obterElemento("notification-count").textContent = `${count} notificação(ões) não lida(s).`;
    } catch (erro) {
      if (!sessaoEncerrada)
        obterElemento("notification-count").textContent = "Contagem indisponível.";
    }
    atualizarPaginacao();
  }
  async function carregarCategorias() {
    obterElemento("panel-list").replaceChildren(criarElemento("p", "Carregando categorias..."));
    const categorias = await requisitarApi(`categories/user/${usuario.id}`);
    if (sessaoEncerrada) return;
    if (!Array.isArray(categorias)) throw new Error("Categorias inválidas.");
    const list = obterElemento("panel-list");
    list.replaceChildren();
    if (!categorias.length) list.appendChild(criarElemento("p", "Nenhuma categoria criada."));
    for (const c of categorias) {
      const row = criarElemento("article", undefined, "panel-row");
      row.append(
        criarElemento("h2", `${c.icone || ""} ${c.name}`),
        criarBotao("Editar", async () => {
          editing = c.id;
          obterElemento("category-name").value = c.name;
          obterElemento("category-icon").value = c.icone || "";
          exibirMensagem("Edite os campos e salve a categoria.");
        }),
        criarBotao("Excluir", async () => {
          if (
            !window.confirm(
              `Excluir a categoria ${c.name}? Categorias usadas em transações ou orçamentos podem não ser removidas.`,
            )
          )
            return;
          await requisitarApi(`categories/${c.id}`, "DELETE");
          if (editing === c.id) cancelarEdicaoCategoria();
          await atualizarAposSalvar("Categoria excluída.", carregarCategorias);
        }),
      );
      list.appendChild(row);
    }
  }
  function cancelarEdicaoCategoria() {
    editing = null;
    obterElemento("panel-form").reset();
    obterElemento("panel-submit").textContent = "Salvar categoria";
  }
  async function carregarCategoriasDoOrcamento() {
    const categorias = await requisitarApi(`categories/user/${usuario.id}`);
    if (sessaoEncerrada) return;
    if (!Array.isArray(categorias)) throw new Error("Categorias inválidas.");
    const select = obterElemento("budget-category");
    select.replaceChildren();
    const empty = criarElemento("option", "Selecione uma categoria");
    empty.value = "";
    select.appendChild(empty);
    for (const c of categorias) {
      const option = criarElemento("option", c.name);
      option.value = String(c.id);
      select.appendChild(option);
    }
    obterElemento("panel-submit").disabled = !categorias.length;
  }
  // Consulta o orçamento do mês e monta os controles de limite por categoria.
  async function carregarOrcamentos() {
    const month = obterElemento("budget-month").value;
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) throw new Error("Selecione um mês válido.");
    obterElemento("panel-list").replaceChildren(criarElemento("p", "Carregando orçamentos..."));
    const orcamentos = await requisitarApi(
      `budgets/user/${usuario.id}?month=${encodeURIComponent(month)}`,
    );
    if (sessaoEncerrada) return;
    if (!Array.isArray(orcamentos)) throw new Error("Orçamentos inválidos.");
    const list = obterElemento("panel-list");
    list.replaceChildren();
    if (!orcamentos.length) list.appendChild(criarElemento("p", "Nenhum orçamento neste mês."));
    for (const b of orcamentos) {
      const row = criarElemento("article", undefined, "panel-row");
      row.append(
        criarElemento("h2", `${b.categoria} · ${b.mesReferencia}`),
        criarElemento(
          "p",
          `Limite: ${formatarMoeda(b.limite)} · Gasto: ${formatarMoeda(b.gasto)} · Restante: ${formatarMoeda(b.restante)}${b.estourado ? " · Limite ultrapassado" : ""}`,
        ),
      );
      const label = criarElemento("label", "Novo limite (R$)");
      const input = criarElemento("input");
      input.id = `budget-limit-${b.budgetId}`;
      input.inputMode = "decimal";
      input.value = String(b.limite);
      label.htmlFor = input.id;
      row.append(
        label,
        input,
        criarBotao("Salvar novo limite", async () => {
          const limite = normalizarValor(input.value);
          await requisitarApi(`budgets/${b.budgetId}/limit`, "PUT", { limite });
          await atualizarAposSalvar("Limite atualizado.", carregarOrcamentos);
        }),
        criarBotao("Excluir orçamento", async () => {
          if (
            !window.confirm(
              `Excluir o orçamento de ${b.categoria} em ${b.mesReferencia}? Os gastos já registrados permanecem no histórico.`,
            )
          )
            return;
          await requisitarApi(`budgets/${b.budgetId}`, "DELETE");
          await atualizarAposSalvar("Orçamento excluído.", carregarOrcamentos);
        }),
      );
      list.appendChild(row);
    }
  }
  async function atualizarDados() {
    exibirMensagem("Atualizando...");
    if (tipoPagina === "notifications") await carregarNotificacoes();
    else if (tipoPagina === "categories") await carregarCategorias();
    else if (tipoPagina === "budgets") {
      await carregarCategoriasDoOrcamento();
      await carregarOrcamentos();
    } else {
      limparCamposDeSenha();
      exibirMensagem("Informe a senha atual para continuar.");
      return;
    }
    if (!sessaoEncerrada) exibirMensagem("Dados atualizados.");
  }
  obterElemento("panel-refresh").addEventListener("click", () => executarAcao(atualizarDados));
  try {
    usuario = await requisitarApi("users/me");
    if (sessaoEncerrada) return;
    if (usuario.id == null) throw new Error("Perfil inválido.");
    if (tipoPagina === "notifications") {
      obterElemento("only-unread").addEventListener("change", () =>
        executarAcao(async () => {
          paginaAtual = 0;
          await atualizarDados();
        }),
      );
      obterElemento("page-previous").addEventListener("click", () =>
        executarAcao(async () => {
          paginaAtual = Math.max(0, paginaAtual - 1);
          await carregarNotificacoes();
        }),
      );
      obterElemento("page-next").addEventListener("click", () =>
        executarAcao(async () => {
          paginaAtual++;
          await carregarNotificacoes();
        }),
      );
      obterElemento("read-all").addEventListener("click", () =>
        executarAcao(async () => {
          await requisitarApi("notifications/read-all", "PUT");
          paginaAtual = 0;
          await atualizarAposSalvar(
            "Todas as notificações foram marcadas como lidas.",
            carregarNotificacoes,
          );
        }),
      );
    } else if (["password", "close"].includes(tipoPagina)) {
      obterElemento("panel-submit").disabled = false;
      obterElemento("panel-form").addEventListener("submit", (evento) => {
        evento.preventDefault();
        return executarAcao(async () => {
          const current = obterElemento("current-password").value;
          if (!current) throw new Error("Informe a senha atual.");
          if (tipoPagina === "password") {
            const password = obterElemento("new-password").value;
            if (password !== obterElemento("confirm-password").value)
              throw new Error("As novas senhas não coincidem.");
            if (password.length < 8 || new TextEncoder().encode(password).length > 72)
              throw new Error("Use pelo menos 8 caracteres e até 72 bytes em UTF-8.");
            try {
              await requisitarApi("users/me/password", "PUT", {
                senhaAtual: current,
                novaSenha: password,
              });
            } finally {
              limparCamposDeSenha();
            }
          } else {
            if (obterElemento("close-word").value !== "ENCERRAR")
              throw new Error("Digite ENCERRAR exatamente como indicado.");
            if (
              !window.confirm(
                "Encerrar a conta? O acesso será desativado, os cartões cancelados e as chaves Pix removidas. O histórico será preservado.",
              )
            )
              return;
            try {
              await requisitarApi("users/me/close", "POST", {
                senhaAtual: current,
                confirmacao: "ENCERRAR",
              });
            } finally {
              limparCamposDeSenha();
            }
          }
          if (sessaoEncerrada) return;
          sessionStorage.setItem(
            "flowbank.accountMessage",
            tipoPagina === "password"
              ? "Senha alterada. Entre novamente com a nova senha."
              : "Conta encerrada. O acesso foi desativado e o histórico foi preservado.",
          );
          encerrarSessaoLocal();
        });
      });
    } else if (tipoPagina === "categories") {
      obterElemento("panel-submit").disabled = false;
      obterElemento("category-cancel").addEventListener("click", () => {
        if (!emExecucao) {
          cancelarEdicaoCategoria();
          exibirMensagem("Edição cancelada.");
        }
      });
      obterElemento("panel-form").addEventListener("submit", (evento) => {
        evento.preventDefault();
        return executarAcao(async () => {
          const name = obterElemento("category-name").value.trim();
          if (!name) throw new Error("Informe o nome da categoria.");
          const icone = obterElemento("category-icon").value.trim();
          await requisitarApi(
            editing ? `categories/${editing}` : "categories",
            editing ? "PUT" : "POST",
            { name, icone },
          );
          cancelarEdicaoCategoria();
          await atualizarAposSalvar("Categoria salva.", carregarCategorias);
        });
      });
    } else if (tipoPagina === "budgets") {
      const now = new Date();
      obterElemento("budget-month").value =
        `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
      obterElemento("budget-month").addEventListener("change", () =>
        executarAcao(carregarOrcamentos),
      );
      obterElemento("panel-form").addEventListener("submit", (evento) => {
        evento.preventDefault();
        return executarAcao(async () => {
          const categoryId = Number(obterElemento("budget-category").value);
          if (!Number.isSafeInteger(categoryId) || categoryId <= 0)
            throw new Error("Selecione uma categoria.");
          const mesReferencia = obterElemento("budget-month").value;
          if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(mesReferencia))
            throw new Error("Selecione um mês válido.");
          const limite = normalizarValor(obterElemento("budget-limit").value);
          await requisitarApi("budgets", "POST", { categoryId, mesReferencia, limite });
          obterElemento("budget-limit").value = "";
          await atualizarAposSalvar("Orçamento criado.", carregarOrcamentos);
        });
      });
    }
    await executarAcao(atualizarDados);
  } catch (erro) {
    if (!sessaoEncerrada)
      exibirMensagem(`${obterMensagemErro(erro)} Recarregue a página para tentar novamente.`, true);
  }
})();
