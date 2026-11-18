/** Consulta o saldo e os cartões do usuário autenticado, sem movimentar valores. */
(async () => {
  const elemento = (id) => document.getElementById(id);
  const token = sessionStorage.getItem("token");
  const camposValores = ["dinheiro-saldo", "dinheiro-limite-total", "dinheiro-limite-livre", "dinheiro-limite-usado"];
  let carregando = false;
  let encerrada = false;

  function informar(texto, erro = false) {
    elemento("dinheiro-mensagem").textContent = texto;
    elemento("dinheiro-mensagem").dataset.erro = String(erro);
  }

  function sair(evento) {
    evento?.preventDefault();
    encerrada = true;
    sessionStorage.removeItem("token");
    for (const id of camposValores) elemento(id).textContent = "—";
    elemento("dinheiro-cartoes-lista").replaceChildren();
    elemento("dinheiro-quantidade").textContent = "";
    window.location.replace("../index.html");
  }

  for (const link of document.querySelectorAll(".logout")) link.addEventListener("click", sair);
  window.addEventListener("pageshow", (evento) => {
    if (evento.persisted) window.location.reload();
  });
  if (!token) {
    sair();
    return;
  }

  // Somas em centavos inteiros, sem acumular diferenças de ponto flutuante.
  function centavos(valor) {
    if (typeof valor !== "string" && typeof valor !== "number") throw new Error("Valor monetário inválido.");
    if (typeof valor === "number" && (!Number.isFinite(valor) || valor > Number.MAX_SAFE_INTEGER / 100)) {
      throw new Error("Valor monetário fora da precisão suportada pela interface.");
    }
    const partes = /^(\d+)(?:\.(\d{1,2}))?$/.exec(String(valor));
    if (!partes) throw new Error("Valor monetário inválido.");
    return BigInt(partes[1]) * 100n + BigInt((partes[2] || "").padEnd(2, "0"));
  }

  function moeda(valor) {
    const inteiro = (valor / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    return `R$ ${inteiro},${(valor % 100n).toString().padStart(2, "0")}`;
  }

  async function consultar(caminho, perfil = false) {
    const controle = new AbortController();
    const tempoLimite = setTimeout(() => controle.abort(), 15000);
    try {
      const resposta = await fetch(`../${caminho}`, {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
        signal: controle.signal,
      });
      if (resposta.status === 401 || (perfil && resposta.status === 403)) {
        sair();
        throw new Error("Sessão expirada.");
      }
      if (!resposta.ok) throw new Error(`Não foi possível consultar os dados (HTTP ${resposta.status}).`);
      return await resposta.json();
    } finally {
      clearTimeout(tempoLimite);
    }
  }

  function criarElemento(tag, classe, texto) {
    const criado = document.createElement(tag);
    if (classe) criado.className = classe;
    if (texto !== undefined) criado.textContent = texto;
    return criado;
  }

  function validarCartao(cartao) {
    const situacoes = { ATIVO: "Ativo", BLOQUEADO: "Bloqueado", CANCELADO: "Cancelado" };
    if (!cartao || !situacoes[cartao.status]) throw new Error("Resposta de cartão inválida.");
    const total = centavos(cartao.limiteTotal);
    const livre = centavos(cartao.limiteDisponivel);
    if (livre > total) throw new Error("Os limites retornados pelo servidor são inconsistentes.");
    return { cartao, total, livre, utilizado: total - livre, situacao: situacoes[cartao.status] };
  }

  function criarCartao(dados) {
    const artigo = criarElemento("article", "dinheiro-cartao");
    const cabecalho = criarElemento("div", "dinheiro-cartao-cabecalho");
    cabecalho.append(
      criarElemento("h3", "", dados.cartao.numeroMascarado || "Cartão cadastrado"),
      criarElemento("span", "dinheiro-situacao", `${dados.situacao}${dados.cartao.descartavel ? " · Descartável" : ""}`),
    );
    const detalhes = criarElemento("dl");
    for (const [rotulo, valor] of [["Limite total", dados.total], ["Limite livre", dados.livre], ["Limite utilizado", dados.utilizado]]) {
      const item = criarElemento("div");
      item.append(criarElemento("dt", "", rotulo), criarElemento("dd", "", moeda(valor)));
      detalhes.append(item);
    }
    artigo.append(cabecalho, detalhes);
    return artigo;
  }

  function mostrarCartoes(cartoes) {
    if (!Array.isArray(cartoes)) throw new Error("Resposta de cartões inválida.");
    const dados = cartoes.map(validarCartao);
    const considerados = dados.filter((item) => item.cartao.status !== "CANCELADO");
    const total = considerados.reduce((soma, item) => soma + item.total, 0n);
    const livre = considerados.reduce((soma, item) => soma + item.livre, 0n);
    const itens = dados.map(criarCartao);
    if (!itens.length) itens.push(criarElemento("p", "", "Você ainda não possui cartões cadastrados."));
    elemento("dinheiro-cartoes-lista").replaceChildren(...itens);
    elemento("dinheiro-limite-total").textContent = moeda(total);
    elemento("dinheiro-limite-livre").textContent = moeda(livre);
    elemento("dinheiro-limite-usado").textContent = moeda(total - livre);
    elemento("dinheiro-quantidade").textContent = `${dados.length} cartão(ões) cadastrado(s). Cancelados não entram no resumo de limites.`;
  }

  function mensagemErro(erro) {
    if (erro.name === "AbortError") return "O servidor demorou para responder.";
    if (erro instanceof TypeError) return "Não foi possível conectar ao servidor.";
    return erro.message;
  }

  async function carregar() {
    if (carregando || encerrada) return;
    carregando = true;
    elemento("dinheiro-atualizar").disabled = true;
    elemento("dinheiro-conteudo").setAttribute("aria-busy", "true");
    for (const id of camposValores) elemento(id).textContent = "—";
    elemento("dinheiro-cartoes-lista").textContent = "Carregando...";
    elemento("dinheiro-quantidade").textContent = "Consultando cartões...";
    informar("Carregando seus dados...");
    try {
      const usuario = await consultar("users/me", true);
      if (encerrada) return;
      if (!usuario || usuario.id == null) throw new Error("Resposta de usuário inválida.");
      elemento("dinheiro-saldo").textContent = moeda(centavos(usuario.balance));
      try {
        const cartoes = await consultar(`credit-cards/user/${encodeURIComponent(usuario.id)}`);
        if (encerrada) return;
        mostrarCartoes(cartoes);
        informar("Saldo e limites atualizados.");
      } catch (erro) {
        if (encerrada) return;
        elemento("dinheiro-cartoes-lista").textContent = "Não foi possível consultar os cartões. Use Atualizar dados para tentar novamente.";
        elemento("dinheiro-quantidade").textContent = "Cartões indisponíveis.";
        informar(`Saldo consultado. ${mensagemErro(erro)}`, true);
      }
    } catch (erro) {
      if (encerrada) return;
      elemento("dinheiro-cartoes-lista").textContent = "Consulte seu perfil novamente para carregar os cartões.";
      elemento("dinheiro-quantidade").textContent = "Dados indisponíveis.";
      informar(`${mensagemErro(erro)} Use Atualizar dados para tentar novamente.`, true);
    } finally {
      carregando = false;
      elemento("dinheiro-atualizar").disabled = false;
      elemento("dinheiro-conteudo").setAttribute("aria-busy", "false");
    }
  }

  elemento("dinheiro-atualizar").addEventListener("click", carregar);
  await carregar();
})();
