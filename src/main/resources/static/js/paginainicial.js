/**
 * Carrega perfil, saldo e resumos da tela inicial com o token autenticado.
 * Fluxo: carregar dados → validar a ação → chamar a API → atualizar a interface.
 */
// Esta tela usa o token salvo pelo login na mesma aba.
(async () => {
  const obterElemento = (id) => document.getElementById(id);
  const moeda = (valor) => {
    if (valor === null || valor === undefined || valor === "" || !Number.isFinite(Number(valor)))
      return "—";
    return Number(valor).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  };
  let token = sessionStorage.getItem("token");
  let encerrada = false;
  let carregando = false;
  let atualizarSaldo = null;
  let saldoVisivel = true;
  const encerrarSessaoLocal = document.querySelector(".logout");

  function sair() {
    encerrada = true;
    sessionStorage.removeItem("token");
    obterElemento("saudacao").textContent = "Sessão encerrada";
    obterElemento("saldo").textContent = "—";
    obterElemento("chave-pix").textContent = "—";
    obterElemento("cartoes-total").textContent = "—";
    obterElemento("cartoes-disponivel").textContent = "—";
    obterElemento("cartoes-quantidade").textContent = "—";
    obterElemento("cartoes-status").textContent = "—";
    obterElemento("gastos-total").textContent = "—";
    obterElemento("gastos-lista").replaceChildren();
    window.location.replace("../index.html");
  }
  encerrarSessaoLocal.addEventListener("click", (evento) => {
    evento.preventDefault();
    sair();
  });
  window.addEventListener("pageshow", (evento) => {
    // Ao voltar pelo histórico, confira novamente a sessão e os dados.
    if (evento.persisted) window.location.reload();
  });
  if (!token) {
    sair();
    return;
  }

  function dataHora(nome) {
    const agora = new Date();
    const hora = agora.getHours();
    const saudacao =
      hora >= 5 && hora < 12 ? "Bom dia" : hora >= 12 && hora < 18 ? "Boa tarde" : "Boa noite";
    obterElemento("saudacao").textContent = `${saudacao}, ${nome}!`;
    obterElemento("data-atual").textContent = agora.toLocaleDateString("pt-BR", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    });
  }
  function mostrarSaldo() {
    obterElemento("saldo").textContent = saldoVisivel ? moeda(atualizarSaldo) : "••••••••";
    obterElemento("icone-olho").src = saldoVisivel ? "../img/Ver.svg" : "../img/Naover.svg";
    obterElemento("btn-toggle-saldo").setAttribute(
      "aria-label",
      saldoVisivel ? "Ocultar saldo" : "Mostrar saldo",
    );
  }
  obterElemento("btn-toggle-saldo").addEventListener("click", () => {
    saldoVisivel = !saldoVisivel;
    mostrarSaldo();
  });

  // Centraliza HTTP, autenticação e tratamento das respostas da API.
  async function requisitarApi(caminho, perfil = false) {
    const controle = new AbortController();
    const timeout = setTimeout(() => controle.abort(), 15000);
    try {
      const resposta = await fetch(`../${caminho}`, {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
        signal: controle.signal,
      });
      if (resposta.status === 401 || (perfil && resposta.status === 403)) {
        sair();
        throw new Error("Sessão expirada. Entre novamente.");
      }
      if (!resposta.ok)
        throw new Error(`Falha ao consultar ${caminho.split("?")[0]} (HTTP ${resposta.status}).`);
      return await resposta.json();
    } finally {
      clearTimeout(timeout);
    }
  }

  function renderGastos(resumo) {
    obterElemento("gastos-total").textContent = moeda(resumo.totalSpent);
    const lista = obterElemento("gastos-lista");
    lista.replaceChildren();
    if (!Array.isArray(resumo.categories)) throw new Error("Resumo de categorias inválido.");
    if (!resumo.categories.length) {
      const vazio = document.createElement("p");
      vazio.textContent = "Nenhum gasto registrado neste mês.";
      lista.appendChild(vazio);
      return;
    }
    for (const categoria of resumo.categories) {
      const item = document.createElement("div");
      item.className = "gasto-item";
      const rotulo = document.createElement("div");
      rotulo.className = "gasto-label";
      const nome = document.createElement("span");
      nome.textContent = categoria.categoryName || "Sem categoria";
      const valor = document.createElement("strong");
      valor.textContent = moeda(categoria.total);
      rotulo.append(nome, valor);
      const barra = document.createElement("div");
      barra.className = "progress-bar";
      const preenchimento = document.createElement("div");
      preenchimento.className = "progress-fill";
      const percentual = Number(categoria.percentage);
      preenchimento.style.width = `${Number.isFinite(percentual) ? Math.min(100, Math.max(0, percentual)) : 0}%`;
      barra.appendChild(preenchimento);
      item.append(rotulo, barra);
      lista.appendChild(item);
    }
  }

  async function carregar() {
    if (carregando || encerrada) return;
    carregando = true;
    const botao = obterElemento("recarregar-painel");
    botao.disabled = true;
    obterElemento("painel-mensagem").textContent = "Carregando seus dados...";
    obterElemento("painel-mensagem").dataset.erro = "false";
    // Nunca mantenha valores antigos após uma tentativa que falhe.
    atualizarSaldo = null;
    mostrarSaldo();
    obterElemento("chave-pix").textContent = "Carregando...";
    obterElemento("cartoes-disponivel").textContent = "—";
    obterElemento("cartoes-total").textContent = "—";
    obterElemento("cartoes-quantidade").textContent = "Carregando...";
    obterElemento("cartoes-status").textContent = "Consultando cartões...";
    obterElemento("gastos-total").textContent = "—";
    obterElemento("gastos-lista").textContent = "Carregando categorias...";
    try {
      const usuario = await requisitarApi("users/me", true);
      if (encerrada) return;
      if (usuario.id == null || typeof usuario.name !== "string")
        throw new Error("Resposta de usuário inválida.");
      dataHora(usuario.name.trim().split(/\s+/)[0] || "usuário");
      atualizarSaldo = usuario.balance;
      mostrarSaldo();
      const agora = new Date();
      const mes = `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, "0")}`;
      obterElemento("gastos-titulo").textContent =
        `Gastos de ${agora.toLocaleDateString("pt-BR", { month: "long", year: "numeric" })}`;
      const tarefas = [
        {
          nome: "Pix",
          executar: async () => {
            const chaves = await requisitarApi(`pix-keys/user/${encodeURIComponent(usuario.id)}`);
            if (encerrada) return;
            if (!Array.isArray(chaves)) throw new Error("Resposta de Pix inválida.");
            obterElemento("chave-pix").textContent = chaves.length
              ? `${chaves.length} chave(s) cadastrada(s)`
              : "Nenhuma cadastrada";
          },
          erro: () => {
            obterElemento("chave-pix").textContent = "Indisponível";
          },
        },
        {
          nome: "Cartões",
          executar: async () => {
            const cartoes = await requisitarApi(
              `credit-cards/user/${encodeURIComponent(usuario.id)}`,
            );
            if (encerrada) return;
            if (!Array.isArray(cartoes)) throw new Error("Resposta de cartões inválida.");
            const total = cartoes.reduce((s, c) => s + Number(c.limiteTotal), 0);
            const disponivel = cartoes.reduce((s, c) => s + Number(c.limiteDisponivel), 0);
            obterElemento("cartoes-quantidade").textContent = `${cartoes.length} cartão(ões)`;
            obterElemento("cartoes-total").textContent = moeda(total);
            obterElemento("cartoes-disponivel").textContent = moeda(disponivel);
            obterElemento("cartoes-status").textContent = cartoes.length
              ? "Soma dos limites de todos os cartões cadastrados, incluindo bloqueados."
              : "Você ainda não possui cartões.";
          },
          erro: () => {
            obterElemento("cartoes-quantidade").textContent = "Indisponível";
            obterElemento("cartoes-status").textContent = "Não foi possível consultar os cartões.";
          },
        },
        {
          nome: "Gastos",
          executar: async () => {
            const resumo = await requisitarApi(`summary/monthly?month=${mes}`);
            if (!encerrada) renderGastos(resumo);
          },
          erro: () => {
            obterElemento("gastos-total").textContent = "—";
            obterElemento("gastos-lista").textContent = "Não foi possível consultar os gastos.";
          },
        },
      ];
      const resultados = await Promise.allSettled(tarefas.map((t) => t.executar()));
      if (encerrada) return;
      const falhas = [];
      resultados.forEach((resultado, i) => {
        if (resultado.status === "rejected") {
          tarefas[i].erro();
          falhas.push(tarefas[i].nome);
        }
      });
      obterElemento("painel-mensagem").textContent = falhas.length
        ? `Dados carregados parcialmente. Falha em: ${falhas.join(", ")}. Clique em Atualizar dados para tentar novamente.`
        : "Dados atualizados.";
      obterElemento("painel-mensagem").dataset.erro = String(falhas.length > 0);
    } catch (erro) {
      if (!encerrada) {
        obterElemento("saudacao").textContent = "Não foi possível carregar o perfil";
        obterElemento("chave-pix").textContent = "Indisponível";
        obterElemento("cartoes-quantidade").textContent = "Indisponível";
        obterElemento("cartoes-status").textContent = "Perfil indisponível.";
        obterElemento("gastos-lista").textContent = "Perfil indisponível.";
        obterElemento("painel-mensagem").textContent =
          erro.name === "AbortError"
            ? "O servidor demorou para responder. Tente atualizar os dados."
            : erro instanceof TypeError
              ? "Não foi possível conectar ao servidor. Tente atualizar os dados."
              : erro.message;
        obterElemento("painel-mensagem").dataset.erro = "true";
      }
    } finally {
      carregando = false;
      botao.disabled = false;
    }
  }
  obterElemento("recarregar-painel").addEventListener("click", carregar);
  await carregar();
})();
