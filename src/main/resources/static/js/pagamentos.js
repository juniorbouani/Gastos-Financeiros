/**
 * Registra boletos internos após conferência e acompanha resultados incertos.
 * Fluxo: carregar dados → validar a ação → chamar a API → atualizar a interface.
 */
(async () => {
  const obterElemento = (id) => document.getElementById(id);
  const pagina = document.body.dataset.paymentsPage;
  const token = sessionStorage.getItem("token");
  const pendingKey = "flowbank.boletoPending";
  let usuario,
    preparado = null,
    ocupado = false,
    encerrada = false,
    visivel = true;
  const moeda = (v) =>
    v != null && Number.isFinite(Number(v))
      ? Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
      : "—";
  function mensagem(texto, erro = false) {
    obterElemento("pagamentos-mensagem").textContent = texto;
    obterElemento("pagamentos-mensagem").dataset.erro = String(erro);
  }
  function criarElemento(tag, texto, classe = "") {
    const elemento = document.createElement(tag);
    elemento.className = classe;
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
  }
  function sair() {
    encerrada = true;
    sessionStorage.removeItem("token");
    preparado = null;
    for (const id of ["pagamentos-historico", "pagamento-resumo"])
      obterElemento(id)?.replaceChildren();
    obterElemento("pagamentos-saldo").textContent = "—";
    window.location.replace("../../index.html");
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
      const resposta = await fetch(`../../${caminho}`, {
        ...opcoes,
        headers: {
          Authorization: `Bearer ${token}`,
          ...(opcoes.body ? { "Content-Type": "application/json" } : {}),
        },
        cache: "no-store",
        signal: controle.signal,
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
      return await resposta.json();
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
  function atualizarSaldo() {
    obterElemento("pagamentos-saldo").textContent = visivel ? moeda(usuario?.balance) : "••••••••";
    obterElemento("pagamentos-olho").textContent = visivel ? "Ocultar saldo" : "Mostrar saldo";
  }
  obterElemento("pagamentos-olho").addEventListener("click", () => {
    visivel = !visivel;
    atualizarSaldo();
  });
  async function perfil() {
    const u = await requisitarApi("users/me");
    if (encerrada) return;
    if (u.id == null || typeof u.name !== "string") throw new Error("Perfil inválido.");
    usuario = u;
    atualizarSaldo();
  }
  function pendente() {
    try {
      const p = JSON.parse(sessionStorage.getItem(pendingKey) || "null");
      return p && String(p.owner) === String(usuario.id) ? p : null;
    } catch {
      return null;
    }
  }
  function mostrarPendencia() {
    const caixa = obterElemento("pagamentos-pendencia");
    caixa.replaceChildren();
    const p = pendente();
    if (!p) return;
    caixa.appendChild(
      criarElemento(
        "p",
        `Resultado não confirmado para o registro de ${moeda(p.value)}. Consulte o extrato e o saldo antes de repetir. Não há reenvio automático.`,
      ),
    );
    const link = criarElemento("a", "Consultar extrato");
    link.href = "extrato.html";
    caixa.appendChild(link);
    const liberar = criarElemento("button", "Já conferi o extrato: liberar novo registro");
    liberar.type = "button";
    liberar.addEventListener("click", () => {
      if (
        !window.confirm(
          "Você conferiu o extrato? Um novo envio poderá gerar outro débito no FlowBank.",
        )
      )
        return;
      sessionStorage.removeItem(pendingKey);
      window.location.replace("transferirPagar.html");
    });
    caixa.appendChild(liberar);
  }
  function normalizarValor(texto) {
    const m = /^(\d+)(?:[,.](\d{1,2}))?$/.exec(texto.trim());
    if (!m) throw new Error("Informe um valor como 125,50, sem separador de milhar.");
    const c = Number(m[1]) * 100 + Number((m[2] || "").padEnd(2, "0"));
    if (!Number.isSafeInteger(c) || c <= 0)
      throw new Error("Informe um valor positivo com até duas casas decimais.");
    return (c / 100).toFixed(2);
  }
  async function historico() {
    const lista = obterElemento("pagamentos-historico");
    lista.replaceChildren();
    const dados = await requisitarApi("transactions/type/BOLETO");
    if (encerrada) return;
    if (!Array.isArray(dados)) throw new Error("Histórico inválido.");
    const recentes = [...dados]
      .sort((a, b) => String(b.date).localeCompare(String(a.date)) || Number(b.id) - Number(a.id))
      .slice(0, 20);
    if (!recentes.length) {
      lista.appendChild(criarElemento("p", "Nenhum registro de boleto nesta conta."));
      return;
    }
    const itens = [];
    for (const t of recentes) {
      const enviou = String(t.sender?.id) === String(usuario.id),
        recebeu = String(t.receiver?.id) === String(usuario.id);
      if (!enviou && !recebeu) throw new Error("Histórico contém transação de outra conta.");
      const propria = enviou && recebeu;
      const concluida = t.status === "COMPLETED";
      const status =
        {
          COMPLETED: "Registro concluído no FlowBank",
          PEDING: "Pendente",
          PENDING: "Pendente",
          FAILED: "Falhou",
        }[t.status] || "Status não informado";
      const dataFormatada = /^(\d{4})-(\d{2})-(\d{2})$/.test(t.date || "")
        ? t.date.split("-").reverse().join("/")
        : "Data não informada";
      const item = criarElemento("article", undefined, "pagamento-linha");
      item.append(
        criarElemento(
          "strong",
          `${propria ? "Mesma conta" : enviou ? "Saída" : "Entrada"} · ${concluida && !propria ? (enviou ? "− " : "+ ") : ""}${moeda(t.value)}`,
        ),
        criarElemento("p", t.description || "Boleto"),
        criarElemento("p", `${dataFormatada} · ${status} · #${t.id}`),
        criarElemento("p", `Referência: ${t.destination || "Não informada"}`),
      );
      itens.push(item);
    }
    lista.replaceChildren(...itens);
  }
  async function atualizar() {
    if (ocupado || encerrada) return;
    ocupado = true;
    obterElemento("pagamentos-atualizar").disabled = true;
    mensagem("Atualizando dados...");
    obterElemento("pagamentos-historico").replaceChildren();
    try {
      await perfil();
      if (encerrada) return;
      await historico();
      if (!encerrada) {
        mostrarPendencia();
        mensagem("Dados atualizados.");
      }
    } catch (erro) {
      if (!encerrada) {
        usuario.balance = null;
        atualizarSaldo();
        mensagem(erroTexto(erro), true);
      }
    } finally {
      ocupado = false;
      obterElemento("pagamentos-atualizar").disabled = false;
    }
  }
  async function prepararFormulario() {
    let categoriasDisponiveis = [];
    if (pendente()) {
      mostrarPendencia();
      return;
    }
    try {
      const categorias = await requisitarApi(`categories/user/${usuario.id}`);
      if (encerrada) return;
      if (!Array.isArray(categorias)) throw new Error("Categorias inválidas.");
      categoriasDisponiveis = categorias;
      for (const categoria of categorias) {
        const option = criarElemento("option", categoria.name);
        option.value = String(categoria.id);
        obterElemento("pagamento-categoria").appendChild(option);
      }
    } catch (erro) {
      if (encerrada) return;
      mensagem(
        "Categorias indisponíveis. Você pode registrar sem categoria ou recarregar a página.",
        true,
      );
    }
    obterElemento("pagamento-conferir").disabled = false;
    obterElemento("pagamento-form").addEventListener("submit", async (evento) => {
      evento.preventDefault();
      if (ocupado) return;
      if (pendente()) {
        mostrarPendencia();
        return;
      }
      ocupado = true;
      obterElemento("pagamento-conferir").disabled = true;
      try {
        preparado = null;
        // Remove somente separadores usuais; não aceita letras escondidas no código.
        const codigo = obterElemento("pagamento-codigo")
          .value.trim()
          .replace(/[.\s-]/g, "");
        if (!/^\d{1,120}$/.test(codigo))
          throw new Error("Informe somente os números do código de referência.");
        const descricao = obterElemento("pagamento-descricao").value.trim();
        if (!descricao)
          throw new Error("Informe uma descrição ou o beneficiário informado no boleto.");
        const valor = normalizarValor(obterElemento("pagamento-valor").value);
        await perfil();
        if (encerrada) return;
        if (Number(valor) > Number(usuario.balance))
          throw new Error("Saldo insuficiente para esse registro.");
        const categoriaSelecionada = obterElemento("pagamento-categoria").value;
        const categoryId = categoriaSelecionada ? Number(categoriaSelecionada) : null;
        if (categoryId != null && (!Number.isSafeInteger(categoryId) || categoryId <= 0))
          throw new Error("Categoria inválida.");
        const categoryName =
          categoriasDisponiveis.find((c) => c.id === categoryId)?.name || "Sem categoria";
        preparado = {
          owner: usuario.id,
          categoryId,
          categoryName,
          code: codigo,
          description: descricao,
          value: valor,
          created: Date.now(),
        };
        obterElemento("pagamento-resumo").replaceChildren(
          criarElemento("p", `Descrição informada: ${descricao}`),
          criarElemento("p", `Código informado: ${codigo}`),
          criarElemento("p", `Valor a debitar no FlowBank: ${moeda(valor)}`),
          criarElemento("p", `Categoria: ${categoryName}`),
        );
        obterElemento("pagamento-form").hidden = true;
        obterElemento("pagamento-confirmacao").hidden = false;
        obterElemento("pagamento-confirmar").disabled = false;
        mensagem("Confira os dados. Nenhum débito foi enviado ainda.");
      } catch (erro) {
        if (!encerrada) mensagem(erroTexto(erro), true);
      } finally {
        ocupado = false;
        obterElemento("pagamento-conferir").disabled = false;
      }
    });
    obterElemento("pagamento-editar").addEventListener("click", () => {
      if (ocupado || pendente()) return;
      preparado = null;
      obterElemento("pagamento-confirmacao").hidden = true;
      obterElemento("pagamento-form").hidden = false;
      obterElemento("pagamento-confirmar").disabled = true;
    });
    obterElemento("pagamento-confirmar").addEventListener("click", async () => {
      if (ocupado || !preparado || encerrada) return;
      if (pendente()) {
        mostrarPendencia();
        return;
      }
      if (
        String(preparado.owner) !== String(usuario.id) ||
        Date.now() - preparado.created > 600000
      ) {
        mensagem("Conferência expirada. Volte e confira os dados novamente.", true);
        obterElemento("pagamento-confirmar").disabled = true;
        return;
      }
      if (
        !window.confirm(
          `Confirmar débito de ${moeda(preparado.value)} no saldo do FlowBank? Isso registra um BOLETO no projeto e não realiza liquidação bancária.`,
        )
      )
        return;
      ocupado = true;
      obterElemento("pagamento-confirmar").disabled = true;
      let enviado = false;
      try {
        await perfil();
        if (encerrada) return;
        if (Number(preparado.value) > Number(usuario.balance))
          throw new Error("Saldo insuficiente. Volte e confira o valor.");
        const hoje = new Date();
        const date = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, "0")}-${String(hoje.getDate()).padStart(2, "0")}`;
        sessionStorage.setItem(pendingKey, JSON.stringify(preparado));
        enviado = true;
        const resultado = await requisitarApi("transactions", {
          method: "POST",
          body: JSON.stringify({
            description: preparado.description,
            value: preparado.value,
            date,
            type: "BOLETO",
            senderId: usuario.id,
            destination: preparado.code,
            categoryId: preparado.categoryId,
          }),
        });
        if (encerrada) return;
        if (resultado?.id == null || resultado.status !== "COMPLETED")
          throw new Error("Resposta inesperada após o envio.");
        sessionStorage.removeItem(pendingKey);
        preparado = null;
        mostrarPendencia();
        obterElemento("pagamento-confirmar").textContent = "Registro concluído";
        obterElemento("pagamento-editar").disabled = true;
        mensagem(
          `Registro concluído no FlowBank. Transação #${resultado.id}. Confira o débito no extrato. Este registro não é comprovante de liquidação bancária.`,
        );
        usuario.balance = null;
        atualizarSaldo();
        try {
          await perfil();
        } catch {
          /* Uma falha de atualização não desfaz o registro confirmado. */
        }
      } catch (erro) {
        const recusado =
          erro instanceof ErroDaApi && [400, 401, 403, 404, 409, 422, 429].includes(erro.status);
        if (!enviado || recusado) {
          if (enviado) sessionStorage.removeItem(pendingKey);
          if (!encerrada) {
            mensagem(erroTexto(erro), true);
            obterElemento("pagamento-confirmar").disabled = false;
          }
        } else if (!encerrada) {
          mostrarPendencia();
          mensagem(
            "Resultado do envio não confirmado. Confira o extrato antes de repetir o registro.",
            true,
          );
        }
      } finally {
        ocupado = false;
      }
    });
  }
  try {
    await perfil();
    if (encerrada) return;
    mostrarPendencia();
    if (pagina === "home") {
      obterElemento("pagamentos-atualizar").addEventListener("click", atualizar);
      await atualizar();
    } else {
      await prepararFormulario();
      if (!pendente() && obterElemento("pagamentos-mensagem").dataset.erro !== "true")
        mensagem("Preencha os dados para conferir antes de registrar.");
    }
  } catch (erro) {
    if (!encerrada) mensagem(`${erroTexto(erro)} Recarregue a página para tentar novamente.`, true);
  }
})();
