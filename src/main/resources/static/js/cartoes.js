/**
 * Gerencia cartões, consultas de faturas e operações confirmadas pelo usuário.
 * Fluxo: carregar dados → validar a ação → chamar a API → atualizar a interface.
 */
(async () => {
  const obterElemento = (id) => document.getElementById(id);
  const token = sessionStorage.getItem("token");
  const pagina = document.body.dataset.cardsPage;
  const pendingKey = "flowbank.cardsPending";
  let usuario,
    cartoes = [],
    selecionado = null,
    fatura = null;
  let ocupado = false,
    encerrada = false,
    saldoVisivel = true;
  const moeda = (v) =>
    v != null && Number.isFinite(Number(v))
      ? Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
      : "—";
  const formatarData = (v) =>
    /^(\d{4})-(\d{2})-(\d{2})$/.test(v || "") ? v.split("-").reverse().join("/") : "Não informada";
  function mensagem(texto, erro = false) {
    obterElemento("cards-mensagem").textContent = texto;
    obterElemento("cards-mensagem").dataset.erro = String(erro);
  }
  function criarElemento(tag, texto, classe = "") {
    const elemento = document.createElement(tag);
    elemento.className = classe;
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
  }
  function botao(texto, acao, disabled = false) {
    const n = criarElemento("button", texto);
    n.type = "button";
    n.disabled = disabled;
    n.addEventListener("click", acao);
    return n;
  }
  function sair() {
    encerrada = true;
    sessionStorage.removeItem("token");
    for (const id of [
      "cards-lista",
      "cards-faturas",
      "cards-lancamentos",
      "cards-selecionado",
      "cards-fatura-titulo",
    ])
      obterElemento(id)?.replaceChildren();
    obterElemento("cards-saldo").textContent = "—";
    window.location.replace("../index.html");
  }
  document.querySelector(".logout").addEventListener("click", (evento) => {
    evento.preventDefault();
    sair();
  });
  window.addEventListener("pageshow", (evento) => {
    if (evento.persisted) window.location.reload();
  });
  if (!token) {
    sair();
    return;
  }
  class ErroDaApi extends Error {
    constructor(texto, status) {
      super(texto);
      this.status = status;
    }
  }
  // Centraliza HTTP, autenticação e tratamento das respostas da API.
  async function requisitarApi(caminho, opcoes = {}) {
    const controle = new AbortController();
    const timeout = setTimeout(() => controle.abort(), 15000);
    try {
      const resposta = await fetch(`../${caminho}`, {
        ...opcoes,
        headers: {
          Authorization: `Bearer ${token}`,
          ...(opcoes.body ? { "Content-Type": "application/json" } : {}),
        },
        signal: controle.signal,
        cache: "no-store",
      });
      if (resposta.status === 401) {
        sair();
        throw new ErroDaApi("Sessão expirada. Entre novamente.", 401);
      }
      if (!resposta.ok) {
        const dadosResposta = await resposta.json().catch(() => null);
        const lerCamposFormulario =
          dadosResposta && typeof dadosResposta === "object"
            ? Object.values(dadosResposta)
                .filter((v) => typeof v === "string")
                .join(" ")
            : "";
        throw new ErroDaApi(
          resposta.status < 500
            ? dadosResposta?.message ||
              lerCamposFormulario ||
              `Requisição recusada (HTTP ${resposta.status}).`
            : `Falha no servidor (HTTP ${resposta.status}).`,
          resposta.status,
        );
      }
      return resposta.status === 204 ? null : await resposta.json();
    } finally {
      clearTimeout(timeout);
    }
  }
  function erroTexto(erro) {
    return erro.name === "AbortError"
      ? "O servidor demorou para responder."
      : erro instanceof TypeError
        ? "A conexão falhou."
        : erro.message;
  }
  function normalizarValor(texto, permiteZero = false) {
    const m = /^(\d+)(?:[,.](\d{1,2}))?$/.exec(texto.trim());
    if (!m) throw new Error("Informe um valor como 1000,50, sem separador de milhar.");
    const c = Number(m[1]) * 100 + Number((m[2] || "").padEnd(2, "0"));
    if (!Number.isSafeInteger(c) || c < 0 || (!permiteZero && c === 0))
      throw new Error("Informe um valor válido com até duas casas decimais.");
    return (c / 100).toFixed(2);
  }
  function atualizarSaldo() {
    obterElemento("cards-saldo").textContent = saldoVisivel ? moeda(usuario?.balance) : "••••••••";
    obterElemento("cards-olho").textContent = saldoVisivel ? "Ocultar saldo" : "Mostrar saldo";
  }
  obterElemento("cards-olho").addEventListener("click", () => {
    saldoVisivel = !saldoVisivel;
    atualizarSaldo();
  });
  function pendente() {
    try {
      const p = JSON.parse(sessionStorage.getItem(pendingKey) || "null");
      return p && String(p.owner) === String(usuario.id) ? p : null;
    } catch {
      return null;
    }
  }
  function mostrarPendencia() {
    const caixa = obterElemento("cards-pendencia");
    caixa.replaceChildren();
    const p = pendente();
    if (!p) return;
    caixa.appendChild(
      criarElemento(
        "p",
        `Resultado ainda não confirmado: ${p.label}. Atualize os cartões, as faturas e o saldo para conferir antes de repetir a operação.`,
      ),
    );
    caixa.appendChild(
      botao("Já conferi os dados: liberar novas operações", () => {
        if (
          !window.confirm(
            "Você conferiu o resultado? Repetir uma compra, solicitação ou pagamento pode duplicar a operação.",
          )
        )
          return;
        sessionStorage.removeItem(pendingKey);
        mostrarPendencia();
        mensagem("Novas operações liberadas após sua conferência.");
      }),
    );
  }
  // A atualização visual é feita fora desta função: uma falha de consulta não desfaz uma operação concluída.
  async function alterar(label, caminho, metodo, corpo, validar) {
    if (pendente()) {
      mostrarPendencia();
      throw new Error("Confira a operação pendente antes de enviar outra.");
    }
    const pendencia = { owner: usuario.id, label, created: Date.now() };
    sessionStorage.setItem(pendingKey, JSON.stringify(pendencia));
    try {
      const resultado = await requisitarApi(caminho, {
        method: metodo,
        ...(corpo ? { body: JSON.stringify(corpo) } : {}),
      });
      if (!validar(resultado)) throw new Error("Resposta inesperada após o envio.");
      sessionStorage.removeItem(pendingKey);
      mostrarPendencia();
      return resultado;
    } catch (erro) {
      if (erro instanceof ErroDaApi && [400, 401, 403, 404, 409, 422, 429].includes(erro.status))
        sessionStorage.removeItem(pendingKey);
      if (!encerrada) mostrarPendencia();
      throw erro;
    }
  }
  async function acao(acao) {
    if (ocupado || encerrada) return;
    ocupado = true;
    try {
      await acao();
    } catch (erro) {
      if (!encerrada) mensagem(erroTexto(erro), true);
    } finally {
      ocupado = false;
    }
  }
  async function atualizarPerfil() {
    const u = await requisitarApi("users/me");
    if (!encerrada) {
      if (u.id == null) throw new Error("Perfil inválido.");
      usuario = u;
      atualizarSaldo();
    }
  }
  async function aposAlteracao(texto) {
    if (encerrada) return;
    mensagem(texto);
    try {
      await carregarCartoes();
    } catch (erro) {
      if (!encerrada)
        mensagem(
          `${texto} Não foi possível atualizar os dados: ${erroTexto(erro)} Use Atualizar cartões.`,
          true,
        );
    }
  }
  async function carregarCartoes() {
    await atualizarPerfil();
    if (encerrada) return;
    const dados = await requisitarApi(`credit-cards/user/${encodeURIComponent(usuario.id)}`);
    if (encerrada) return;
    if (!Array.isArray(dados)) throw new Error("Lista de cartões inválida.");
    cartoes = dados;
    const lista = obterElemento("cards-lista");
    lista.replaceChildren();
    if (!cartoes.length) {
      lista.appendChild(
        criarElemento("p", "Você ainda não possui cartões. Solicite um cartão para começar."),
      );
    }
    for (const c of cartoes) {
      const item = criarElemento("article", undefined, "cards-linha");
      item.append(
        criarElemento("h2", `Cartão ${c.numeroMascarado} · ${c.status}`),
        criarElemento(
          "p",
          `Titular: ${usuario.name} · Validade: ${formatarData(c.dataExpiracao)} · ${c.descartavel ? "Descartável" : "Virtual"}`,
        ),
        criarElemento(
          "p",
          `Limite total: ${moeda(c.limiteTotal)} · Disponível: ${moeda(c.limiteDisponivel)}`,
        ),
      );
      item.appendChild(botao("Selecionar e ver faturas", () => acao(() => selecionarCartao(c))));
      if (c.status !== "CANCELADO")
        item.appendChild(
          botao(c.status === "BLOQUEADO" ? "Desbloquear" : "Bloquear", () =>
            acao(async () => {
              const desbloquear = c.status === "BLOQUEADO";
              if (
                !window.confirm(
                  `${desbloquear ? "Desbloquear" : "Bloquear"} o cartão ${c.numeroMascarado}?`,
                )
              )
                return;
              await alterar(
                `${desbloquear ? "Desbloqueio" : "Bloqueio"} do cartão #${c.id}`,
                `credit-cards/${c.id}/${desbloquear ? "unblock" : "block"}`,
                "PUT",
                null,
                (r) => r?.id === c.id,
              );
              await aposAlteracao("Status do cartão atualizado.");
            }),
          ),
        );
      lista.appendChild(item);
    }
    if (selecionado) {
      const atual = cartoes.find((c) => String(c.id) === String(selecionado.id));
      if (atual) await selecionarCartao(atual);
      else {
        selecionado = null;
        obterElemento("cards-detalhes").hidden = true;
      }
    }
    mostrarPendencia();
  }
  async function selecionarCartao(c) {
    selecionado = c;
    fatura = null;
    obterElemento("cards-detalhes").hidden = false;
    obterElemento("cards-fatura-detalhes").hidden = true;
    obterElemento("cards-selecionado").textContent = `Cartão selecionado: ${c.numeroMascarado}`;
    obterElemento("cards-novo-limite").value = String(c.limiteTotal);
    obterElemento("cards-faturas").replaceChildren(criarElemento("p", "Carregando faturas..."));
    try {
      const faturas = await requisitarApi(`credit-cards/${c.id}/invoices`);
      if (encerrada) return;
      if (!Array.isArray(faturas)) throw new Error("Resposta de faturas inválida.");
      const lista = obterElemento("cards-faturas");
      lista.replaceChildren();
      if (!faturas.length)
        lista.appendChild(criarElemento("p", "Nenhuma fatura gerada para este cartão."));
      for (const f of faturas) {
        const item = criarElemento("article", undefined, "cards-linha");
        item.append(
          criarElemento("h3", `Fatura ${f.mesReferencia} · ${f.status}`),
          criarElemento("p", `Vencimento: ${formatarData(f.dataVencimento)}`),
          criarElemento(
            "p",
            `Total: ${moeda(f.valorTotal)} · Pago: ${moeda(f.valorPago)} · Restante: ${moeda(f.valorRestante)}`,
          ),
          botao("Ver lançamentos e pagar", () => acao(() => selecionarFatura(f))),
        );
        lista.appendChild(item);
      }
    } catch (erro) {
      if (!encerrada)
        obterElemento("cards-faturas").replaceChildren(
          criarElemento("p", `Não foi possível consultar as faturas: ${erroTexto(erro)}`),
        );
      throw erro;
    }
  }
  async function selecionarFatura(f) {
    fatura = null;
    obterElemento("cards-fatura-detalhes").hidden = false;
    obterElemento("cards-pagamento-form").hidden = true;
    obterElemento("cards-fatura-titulo").textContent =
      `Fatura ${f.mesReferencia} · Saldo devedor: ${moeda(f.valorRestante)}`;
    obterElemento("cards-lancamentos").replaceChildren(
      criarElemento("p", "Carregando lançamentos..."),
    );
    try {
      const itens = await requisitarApi(`credit-cards/invoices/${f.id}/items`);
      if (encerrada) return;
      if (!Array.isArray(itens)) throw new Error("Resposta de lançamentos inválida.");
      const lista = obterElemento("cards-lancamentos");
      lista.replaceChildren();
      if (!itens.length) lista.appendChild(criarElemento("p", "Nenhum lançamento nesta fatura."));
      for (const i of itens) {
        const item = criarElemento("div", undefined, "cards-linha");
        item.append(
          criarElemento("strong", `${i.descricao} · ${moeda(i.valor)}`),
          criarElemento(
            "p",
            `${formatarData(i.data)} · Parcela ${i.parcelaAtual}/${i.totalParcelas} · ${i.estornado ? "Estornado" : "Lançado"} · #${i.id}`,
          ),
        );
        lista.appendChild(item);
      }
      fatura = f;
      obterElemento("cards-pagamento-form").hidden = Number(f.valorRestante) <= 0;
      obterElemento("cards-pagamento-valor").value = "";
    } catch (erro) {
      if (!encerrada)
        obterElemento("cards-lancamentos").replaceChildren(
          criarElemento("p", `Consulta indisponível: ${erroTexto(erro)}`),
        );
      throw erro;
    }
  }
  function prepararCartoes() {
    obterElemento("cards-atualizar").addEventListener("click", () =>
      acao(async () => {
        await carregarCartoes();
        if (!encerrada) mensagem("Dados atualizados.");
      }),
    );
    obterElemento("cards-limite-form").addEventListener("submit", (evento) => {
      evento.preventDefault();
      return acao(async () => {
        if (!selecionado) throw new Error("Selecione um cartão.");
        const c = selecionado;
        const limite = normalizarValor(obterElemento("cards-novo-limite").value, true);
        if (
          !window.confirm(`Alterar o limite total de ${c.numeroMascarado} para ${moeda(limite)}?`)
        )
          return;
        await alterar(
          `Alteração de limite do cartão #${c.id}`,
          `credit-cards/${c.id}/limit`,
          "PUT",
          { novoLimite: limite },
          (r) => r?.id === c.id,
        );
        await aposAlteracao("Limite atualizado.");
      });
    });
    obterElemento("cards-compra-form").addEventListener("submit", (evento) => {
      evento.preventDefault();
      return acao(async () => {
        if (!selecionado || selecionado.status !== "ATIVO")
          throw new Error("Selecione um cartão ativo.");
        const c = selecionado;
        const valor = normalizarValor(obterElemento("cards-compra-valor").value);
        const parcelas = Number(obterElemento("cards-compra-parcelas").value);
        const descricao = obterElemento("cards-compra-descricao").value.trim();
        if (!descricao) throw new Error("Informe a descrição da compra.");
        if (!Number.isSafeInteger(parcelas) || parcelas <= 0)
          throw new Error("Informe um número inteiro positivo de parcelas.");
        if (Number(valor) > Number(c.limiteDisponivel))
          throw new Error("Limite disponível insuficiente.");
        if (
          !window.confirm(
            `Registrar compra de ${moeda(valor)} em ${parcelas}x no cartão ${c.numeroMascarado}? Descrição: ${descricao}`,
          )
        )
          return;
        await alterar(
          `Compra de ${moeda(valor)} no cartão #${c.id}`,
          `credit-cards/${c.id}/purchases`,
          "POST",
          { descricao, valor, totalParcelas: parcelas },
          (r) => r?.id != null,
        );
        obterElemento("cards-compra-form").reset();
        await aposAlteracao("Compra registrada e faturas geradas pelo servidor.");
      });
    });
    obterElemento("cards-pagar-total").addEventListener("click", () => {
      if (fatura) obterElemento("cards-pagamento-valor").value = String(fatura.valorRestante);
    });
    obterElemento("cards-pagamento-form").addEventListener("submit", (evento) => {
      evento.preventDefault();
      return acao(async () => {
        if (!fatura) throw new Error("Selecione uma fatura.");
        const f = fatura;
        const valor = normalizarValor(obterElemento("cards-pagamento-valor").value);
        if (Number(valor) > Number(f.valorRestante))
          throw new Error("O pagamento excede o saldo devedor da fatura.");
        if (Number(valor) > Number(usuario.balance))
          throw new Error("Saldo em conta insuficiente.");
        if (
          !window.confirm(
            `Pagar ${moeda(valor)} da fatura ${f.mesReferencia}? Esse valor será debitado da sua conta.`,
          )
        )
          return;
        await alterar(
          `Pagamento de ${moeda(valor)} da fatura #${f.id}`,
          `credit-cards/invoices/${f.id}/pay`,
          "POST",
          { valor },
          (r) => r?.id === f.id,
        );
        obterElemento("cards-pagamento-form").hidden = true;
        fatura = null;
        await aposAlteracao(
          "Pagamento concluído. O servidor atualizou o saldo e o limite disponível.",
        );
      });
    });
  }
  function prepararSolicitacao() {
    let solicitado = false;
    obterElemento("cards-solicitar").disabled = false;
    obterElemento("cards-request-form").addEventListener("submit", (evento) => {
      evento.preventDefault();
      return acao(async () => {
        if (solicitado) return;
        const limite = normalizarValor(obterElemento("cards-limite").value);
        const descartavel = obterElemento("cards-descartavel").value === "true";
        if (
          !window.confirm(
            `Solicitar cartão ${descartavel ? "descartável" : "virtual"} com limite de ${moeda(limite)}?`,
          )
        )
          return;
        await alterar(
          "Solicitação de novo cartão",
          "credit-cards",
          "POST",
          { limiteTotal: limite, descartavel },
          (r) => r?.id != null,
        );
        solicitado = true;
        if (!encerrada) {
          obterElemento("cards-solicitar").disabled = true;
          mensagem("Cartão criado. Abra Meus cartões e faturas para consultá-lo.");
          window.location.assign("cartoes.html");
        }
      });
    });
  }
  try {
    await atualizarPerfil();
    if (encerrada) return;
    mostrarPendencia();
    if (pagina === "request") prepararSolicitacao();
    else {
      prepararCartoes();
      await acao(carregarCartoes);
    }
    if (!encerrada && !pendente() && obterElemento("cards-mensagem").dataset.erro !== "true")
      mensagem("Dados carregados.");
  } catch (erro) {
    if (!encerrada) mensagem(`${erroTexto(erro)} Recarregue a página para tentar novamente.`, true);
  }
})();
