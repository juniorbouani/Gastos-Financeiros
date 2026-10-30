/**
 * Consulta o catálogo e confirma pedidos com identificador estável contra duplicidade.
 * Fluxo: carregar dados → validar a ação → chamar a API → atualizar a interface.
 */
(async () => {
  const obterElemento = (id) => document.getElementById(id),
    tipoPagina = document.body.dataset.shopPage,
    idProduto = document.body.dataset.productId;
  const token = sessionStorage.getItem("token");
  let usuario,
    produto,
    cartoes = [],
    compraEmConferencia = null,
    emExecucao = false,
    sessaoEncerrada = false,
    compraConcluida = false,
    paginaPedidos = 0,
    totalPaginas = 0;
  const formatarMoeda = (v) =>
    v != null && Number.isFinite(Number(v))
      ? Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
      : "—";
  function exibirMensagem(texto, erro = false) {
    obterElemento("shop-message").textContent = texto;
    obterElemento("shop-message").dataset.error = String(erro);
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
  // Limpa o acesso desta aba; a revogação no servidor depende do fluxo de segurança.
  function encerrarSessaoLocal(evento) {
    evento?.preventDefault();
    sessaoEncerrada = true;
    compraEmConferencia = null;
    for (const id of ["shop-orders", "shop-summary", "shop-result"])
      obterElemento(id)?.replaceChildren();
    sessionStorage.removeItem("token");
    window.location.replace("../index.html");
  }
  document.querySelector(".logout").addEventListener("click", encerrarSessaoLocal);
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
        cache: "no-store",
        signal: controller.signal,
      });
      if (resposta.status === 401) {
        encerrarSessaoLocal();
        throw new ErroDaApi("Sessão expirada. Entre novamente.", 401);
      }
      if (!resposta.ok) {
        const dadosResposta = await resposta.json().catch(() => null);
        const texts =
          dadosResposta && typeof dadosResposta === "object"
            ? Object.values(dadosResposta)
                .filter((v) => typeof v === "string")
                .join(" ")
            : "";
        throw new ErroDaApi(
          resposta.status < 500
            ? dadosResposta?.message || texts || `Requisição recusada (HTTP ${resposta.status}).`
            : `Erro no servidor (HTTP ${resposta.status}).`,
          resposta.status,
        );
      }
      return await resposta.json();
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
  function atualizarControles() {
    if (tipoPagina === "catalog") {
      obterElemento("shop-previous").disabled =
        emExecucao || sessaoEncerrada || paginaPedidos <= 0 || !totalPaginas;
      obterElemento("shop-next").disabled =
        emExecucao || sessaoEncerrada || paginaPedidos + 1 >= totalPaginas;
      obterElemento("shop-refresh").disabled = emExecucao;
    } else {
      obterElemento("shop-confirm").disabled =
        emExecucao || sessaoEncerrada || compraConcluida || !compraEmConferencia;
      obterElemento("shop-edit").disabled = emExecucao || compraConcluida;
    }
  }
  // Evita ações simultâneas e apresenta erros sem repetir operações automaticamente.
  async function executarAcao(acao) {
    if (emExecucao || sessaoEncerrada) return;
    emExecucao = true;
    atualizarControles();
    try {
      await acao();
    } catch (erro) {
      if (!sessaoEncerrada) exibirMensagem(obterMensagemErro(erro), true);
    } finally {
      emExecucao = false;
      atualizarControles();
    }
  }
  const pendingKey = () => `flowbank.shopPending.${usuario.id}`;
  function obterPedidoPendente() {
    try {
      return JSON.parse(sessionStorage.getItem(pendingKey()) || "null");
    } catch {
      return null;
    }
  }
  function montarCorpoPedido(pedido) {
    return {
      productId: pedido.productId,
      cardId: pedido.cardId,
      totalParcelas: pedido.totalParcelas,
      requestId: pedido.requestId,
    };
  }
  function validarPedidoPendente(pedido) {
    return (
      pedido &&
      String(pedido.owner) === String(usuario.id) &&
      typeof pedido.requestId === "string" &&
      /^[a-f0-9-]{36}$/i.test(pedido.requestId) &&
      typeof pedido.productId === "string" &&
      Number.isSafeInteger(pedido.cardId) &&
      Number.isInteger(pedido.totalParcelas) &&
      pedido.totalParcelas >= 1 &&
      pedido.totalParcelas <= 12
    );
  }
  // Oferece consulta e recuperação para uma operação cujo resultado ainda não foi confirmado.
  function exibirPedidoPendente() {
    const box = obterElemento("shop-pending");
    box.replaceChildren();
    const p = obterPedidoPendente();
    if (!p) return;
    if (!validarPedidoPendente(p)) {
      box.appendChild(
        criarElemento(
          "p",
          "Há um identificador de pedido inválido armazenado. Confira Meus pedidos antes de iniciar outra compra.",
        ),
      );
      return;
    }
    box.appendChild(
      criarElemento(
        "p",
        `Há uma compra aguardando confirmação do resultado: ${p.name} · ${formatarMoeda(p.total)} · ${p.totalParcelas}x. Consulte o resultado ou reenvie a mesma operação com o mesmo identificador.`,
      ),
    );
    box.append(
      criarBotao("Consultar resultado", async () => {
        try {
          const result = await requisitarApi(
            `shop/orders/request/${encodeURIComponent(p.requestId)}`,
          );
          await concluirPedido(result, p);
        } catch (erro) {
          if (erro instanceof ErroDaApi && erro.status === 404) {
            exibirMensagem(
              "Pedido ainda não encontrado. Você pode reenviar a mesma confirmação: o servidor preserva o identificador para impedir duplicidade.",
              true,
            );
            return;
          }
          throw erro;
        }
      }),
      criarBotao("Reenviar a mesma confirmação", async () => {
        if (
          !window.confirm(
            `Confirmar novamente a mesma operação de ${p.name}, ${formatarMoeda(p.total)}, ${p.totalParcelas}x no cartão ${p.cardMask}? O mesmo identificador será usado para evitar outra cobrança.`,
          )
        )
          return;
        await confirmarPedido(p);
      }),
    );
  }
  function criarCartaoProduto(produto, link = true) {
    const tile = criarElemento("article", undefined, "shop-tile"),
      img = criarElemento("img");
    if (!/^[A-Za-z0-9_-]+\.jpg$/.test(produto.imagem) || !Number.isFinite(Number(produto.preco)))
      throw new Error("Produto inválido.");
    img.src = `../img/${produto.imagem}`;
    img.alt = produto.nome;
    tile.append(
      img,
      criarElemento("h2", produto.nome),
      criarElemento("p", formatarMoeda(produto.preco)),
    );
    if (link) {
      if (!/^[A-Za-z0-9_-]+\.html$/.test(produto.pagina))
        throw new Error("Página do produto inválida.");
      const a = criarElemento("a", "Ver produto e comprar");
      a.href = produto.pagina;
      tile.appendChild(a);
    }
    return tile;
  }
  async function carregarPedidos() {
    const list = obterElemento("shop-orders");
    list.replaceChildren(criarElemento("p", "Carregando pedidos..."));
    const params = new URLSearchParams({ page: String(paginaPedidos), size: "20" });
    params.append("sort", "id,desc");
    const dadosResposta = await requisitarApi(`shop/orders?${params}`);
    if (sessaoEncerrada) return;
    if (
      !Array.isArray(dadosResposta.content) ||
      !Number.isInteger(dadosResposta.totalPages) ||
      !Number.isInteger(dadosResposta.number)
    )
      throw new Error("Histórico de pedidos inválido.");
    if (dadosResposta.totalPages > 0 && paginaPedidos >= dadosResposta.totalPages) {
      paginaPedidos = dadosResposta.totalPages - 1;
      return carregarPedidos();
    }
    paginaPedidos = dadosResposta.number;
    totalPaginas = dadosResposta.totalPages;
    list.replaceChildren();
    if (!dadosResposta.content.length)
      list.appendChild(criarElemento("p", "Você ainda não possui pedidos no Shop."));
    for (const o of dadosResposta.content) {
      const row = criarElemento("article", undefined, "shop-order");
      row.append(
        criarElemento("h3", `Pedido #${o.id} · ${o.produto}`),
        criarElemento("p", `${formatarMoeda(o.total)} · ${o.totalParcelas}x · Cartão ${o.cartao}`),
        criarElemento(
          "p",
          `Registrado em ${String(o.createdAt || "")
            .replace("T", " ")
            .slice(0, 19)} · Primeira fatura #${o.firstInvoiceId}`,
        ),
      );
      list.appendChild(row);
    }
    obterElemento("shop-page-label").textContent = totalPaginas
      ? `Página ${paginaPedidos + 1} de ${totalPaginas}`
      : "Nenhuma página";
  }
  function formatarParcelamento(total, n) {
    const cents = Math.round(Number(total) * 100),
      base = Math.floor(cents / n),
      last = cents - base * (n - 1);
    return n === 1
      ? `1x de ${formatarMoeda(total)}`
      : base === last
        ? `${n}x de ${formatarMoeda(base / 100)}`
        : `${n - 1}x de ${formatarMoeda(base / 100)} e última de ${formatarMoeda(last / 100)}`;
  }
  async function concluirPedido(resultado, pedido) {
    if (sessaoEncerrada) return;
    if (
      !resultado ||
      resultado.id == null ||
      resultado.requestId !== pedido.requestId ||
      resultado.productId !== pedido.productId ||
      String(resultado.cardId) !== String(pedido.cardId) ||
      resultado.totalParcelas !== pedido.totalParcelas
    )
      throw new Error(
        "Resposta inesperada do pedido. Consulte o resultado antes de iniciar outra compra.",
      );
    sessionStorage.removeItem(pendingKey());
    exibirPedidoPendente();
    compraConcluida = true;
    compraEmConferencia = null;
    exibirMensagem(
      `Compra registrada! Pedido #${resultado.id}, total ${formatarMoeda(resultado.total)}. Consulte os lançamentos em Cartões e faturas.`,
    );
    if (tipoPagina === "product") {
      obterElemento("shop-form").hidden = true;
      obterElemento("shop-confirmation").hidden = true;
      const a = criarElemento("a", "Ver cartões e faturas");
      a.href = "../cartoes/cartoes.html";
      obterElemento("shop-result").replaceChildren(
        criarElemento("h2", `Pedido #${resultado.id} confirmado`),
        criarElemento(
          "p",
          `${resultado.produto} · ${formatarMoeda(resultado.total)} · ${resultado.totalParcelas}x · ${resultado.cartao}`,
        ),
        a,
      );
    } else {
      try {
        await carregarPedidos();
      } catch {
        if (!sessaoEncerrada)
          exibirMensagem(
            `Pedido #${resultado.id} confirmado. Não foi possível atualizar a lista; use Atualizar pedidos.`,
            true,
          );
      }
    }
  }
  // Reutiliza o identificador do pedido para permitir recuperação sem nova cobrança.
  async function confirmarPedido(pedido) {
    sessionStorage.setItem(pendingKey(), JSON.stringify(pedido));
    exibirPedidoPendente();
    try {
      const result = await requisitarApi("shop/orders", "POST", montarCorpoPedido(pedido));
      await concluirPedido(result, pedido);
    } catch (erro) {
      const refused =
        erro instanceof ErroDaApi && [400, 401, 403, 404, 422, 429].includes(erro.status);
      if (refused) {
        sessionStorage.removeItem(pendingKey());
        if (!sessaoEncerrada) exibirPedidoPendente();
      } else if (!sessaoEncerrada) {
        exibirPedidoPendente();
        exibirMensagem(
          "O resultado da compra ainda não foi confirmado. Use Consultar resultado ou Reenviar a mesma confirmação. Não crie outro pedido para repetir esta compra.",
          true,
        );
        return;
      }
      throw erro;
    }
  }
  async function carregarProduto() {
    produto = await requisitarApi(`shop/products/${encodeURIComponent(idProduto)}`);
    if (sessaoEncerrada) return;
    obterElemento("shop-product").replaceChildren(criarCartaoProduto(produto, false));
    const dadosResposta = await requisitarApi(`credit-cards/user/${usuario.id}`);
    if (sessaoEncerrada) return;
    if (!Array.isArray(dadosResposta)) throw new Error("Lista de cartões inválida.");
    const now = new Date(),
      today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
    cartoes = dadosResposta.filter(
      (c) => c.status === "ATIVO" && c.dataExpiracao && c.dataExpiracao >= today,
    );
    const select = obterElemento("shop-card");
    for (const c of cartoes) {
      const o = criarElemento(
        "option",
        `${c.numeroMascarado} · Disponível ${formatarMoeda(c.limiteDisponivel)}`,
      );
      o.value = String(c.id);
      select.appendChild(o);
    }
    for (let n = 1; n <= 12; n++) {
      const o = criarElemento("option", formatarParcelamento(produto.preco, n));
      o.value = String(n);
      obterElemento("shop-installments").appendChild(o);
    }
    obterElemento("shop-installments").value = "1";
    obterElemento("shop-review").disabled = !cartoes.length;
    if (!cartoes.length)
      exibirMensagem(
        "Você não possui cartão ativo e válido. Solicite ou desbloqueie um cartão em Cartões e faturas.",
        true,
      );
    obterElemento("shop-form").addEventListener("submit", (evento) => {
      evento.preventDefault();
      return executarAcao(async () => {
        if (compraConcluida) return;
        if (obterPedidoPendente()) {
          exibirPedidoPendente();
          throw new Error("Confira o resultado da compra pendente antes de iniciar outra.");
        }
        const card = cartoes.find((c) => String(c.id) === obterElemento("shop-card").value),
          n = Number(obterElemento("shop-installments").value);
        if (!card) throw new Error("Selecione um cartão ativo.");
        if (!Number.isInteger(n) || n < 1 || n > 12)
          throw new Error("Selecione de 1 a 12 parcelas.");
        if (Number(card.limiteDisponivel) < Number(produto.preco))
          throw new Error("Limite disponível insuficiente para o total da compra.");
        if (!crypto.randomUUID)
          throw new Error(
            "Seu navegador precisa oferecer geração segura de identificadores para comprar.",
          );
        compraEmConferencia = {
          owner: usuario.id,
          productId: produto.id,
          cardId: card.id,
          totalParcelas: n,
          requestId: crypto.randomUUID(),
          name: produto.nome,
          total: produto.preco,
          cardMask: card.numeroMascarado,
        };
        obterElemento("shop-summary").replaceChildren(
          criarElemento("p", `Produto: ${produto.nome} · Uma unidade`),
          criarElemento("p", `Total: ${formatarMoeda(produto.preco)}`),
          criarElemento("p", formatarParcelamento(produto.preco, n)),
          criarElemento("p", `Cartão: ${card.numeroMascarado}`),
          criarElemento(
            "p",
            "A compra compromete o limite total agora; o pagamento das faturas debita o saldo da conta.",
          ),
        );
        obterElemento("shop-form").hidden = true;
        obterElemento("shop-confirmation").hidden = false;
        exibirMensagem("Confira os dados. Nenhuma compra foi enviada ainda.");
      });
    });
    obterElemento("shop-edit").addEventListener("click", () => {
      if (emExecucao || obterPedidoPendente() || compraConcluida) return;
      compraEmConferencia = null;
      obterElemento("shop-confirmation").hidden = true;
      obterElemento("shop-form").hidden = false;
      atualizarControles();
    });
    obterElemento("shop-confirm").addEventListener("click", () =>
      executarAcao(async () => {
        if (!compraEmConferencia || compraConcluida) return;
        if (obterPedidoPendente()) {
          exibirPedidoPendente();
          throw new Error("Consulte o pedido pendente ou reenvie a mesma confirmação.");
        }
        if (
          !window.confirm(
            `Comprar ${compraEmConferencia.name} por ${formatarMoeda(compraEmConferencia.total)}, em ${compraEmConferencia.totalParcelas}x no cartão ${compraEmConferencia.cardMask}? A compra será lançada nas faturas do FlowBank.`,
          )
        )
          return;
        await confirmarPedido(compraEmConferencia);
      }),
    );
  }
  try {
    usuario = await requisitarApi("users/me");
    if (sessaoEncerrada) return;
    if (usuario.id == null) throw new Error("Perfil inválido.");
    exibirPedidoPendente();
    if (tipoPagina === "catalog") {
      const products = await requisitarApi("shop/products");
      if (sessaoEncerrada) return;
      if (!Array.isArray(products)) throw new Error("Catálogo inválido.");
      obterElemento("shop-products").replaceChildren(...products.map((p) => criarCartaoProduto(p)));
      obterElemento("shop-refresh").addEventListener("click", () =>
        executarAcao(async () => {
          paginaPedidos = 0;
          await carregarPedidos();
          exibirPedidoPendente();
          exibirMensagem("Pedidos atualizados.");
        }),
      );
      obterElemento("shop-previous").addEventListener("click", () =>
        executarAcao(async () => {
          paginaPedidos = Math.max(0, paginaPedidos - 1);
          await carregarPedidos();
        }),
      );
      obterElemento("shop-next").addEventListener("click", () =>
        executarAcao(async () => {
          paginaPedidos++;
          await carregarPedidos();
        }),
      );
      await executarAcao(carregarPedidos);
    } else await executarAcao(carregarProduto);
    if (!sessaoEncerrada && obterElemento("shop-message").dataset.error !== "true")
      exibirMensagem("Dados carregados.");
  } catch (erro) {
    if (!sessaoEncerrada)
      exibirMensagem(`${obterMensagemErro(erro)} Recarregue a página para tentar novamente.`, true);
  }
  atualizarControles();
})();
