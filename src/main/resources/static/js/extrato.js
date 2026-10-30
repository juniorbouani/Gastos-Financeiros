// Consulta somente dados do usuário autenticado. Não altera transações.
(async () => {
  const el = id => document.getElementById(id);
  const lista = el("extrato-lista");
  const mensagem = el("extrato-mensagem");
  const anterior = el("extrato-anterior");
  const proxima = el("extrato-proxima");
  const atualizar = el("extrato-atualizar");
  const token = sessionStorage.getItem("token");
  let usuario = null;
  let pagina = 0;
  let totalPaginas = 0;
  let ocupado = false;
  let encerrada = false;

  function sair() {
    encerrada = true;
    sessionStorage.removeItem("token");
    lista.replaceChildren();
    el("extrato-titular").textContent = "Sessão encerrada";
    el("extrato-pagina").textContent = "—";
    mensagem.textContent = "Entre novamente para consultar o extrato.";
    window.location.replace("../../index.html");
  }
  document.querySelector(".logout").addEventListener("click", evento => { evento.preventDefault(); sair(); });
  window.addEventListener("pageshow", evento => { if (evento.persisted) window.location.reload(); });
  if (!token) { sair(); return; }

  function controles() {
    atualizar.disabled = ocupado || encerrada;
    anterior.disabled = ocupado || encerrada || totalPaginas === 0 || pagina <= 0;
    proxima.disabled = ocupado || encerrada || pagina + 1 >= totalPaginas;
    lista.setAttribute("aria-busy", String(ocupado));
  }
  async function api(caminho) {
    const controle = new AbortController();
    const timeout = setTimeout(() => controle.abort(), 15000);
    try {
      const resposta = await fetch(`../../${caminho}`, {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
        signal: controle.signal,
      });
      if (resposta.status === 401 || resposta.status === 403) {
        sair();
        throw new Error("Sessão inválida ou acesso não autorizado. Entre novamente.");
      }
      if (!resposta.ok) throw new Error(`Não foi possível carregar o extrato (HTTP ${resposta.status}).`);
      return await resposta.json();
    } finally { clearTimeout(timeout); }
  }
  function dataFormatada(data) {
    // LocalDate não contém horário; não converter meia-noite UTC para horário local.
    const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(data || "");
    return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : "Data não informada";
  }
  function no(tag, classe, texto) {
    const elemento = document.createElement(tag);
    elemento.className = classe;
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
  }
  function renderTransacao(t) {
    const remetente = t.sender?.id != null && String(t.sender.id) === String(usuario.id);
    const destinatario = t.receiver?.id != null && String(t.receiver.id) === String(usuario.id);
    if (!remetente && !destinatario) throw new Error("A resposta contém uma transação de outro usuário.");
    const propriaConta = remetente && destinatario;
    const direcao = propriaConta ? "neutro" : remetente ? "saida" : "entrada";
    const tipos = { PIX: "Pix", BOLETO: "Boleto", TRANSFERENCIA: "Transferência", TED: "TED", ESTORNO: "Estorno" };
    const estados = { COMPLETED: "Concluída", PEDING: "Pendente", PENDING: "Pendente", FAILED: "Falhou" };
    const tipo = tipos[t.type] || "Transação";
    const sentido = propriaConta ? "Mesma conta" : remetente ? "Saída" : "Entrada";
    const status = estados[t.status] || "Status não informado";
    const item = no("div", `extrato-item ${direcao}`);
    const info = no("div", "extrato-info");
    const detalhes = no("div", "extrato-detalhes");
    detalhes.appendChild(no("p", "extrato-descricao", t.description || tipo));
    const contraparte = propriaConta ? "Movimentação para a própria conta" : remetente
      ? `Para: ${t.receiver?.name || t.destination || "Destino externo"}`
      : `De: ${t.sender?.name || "Remetente não informado"}`;
    detalhes.appendChild(no("span", "extrato-data", `${dataFormatada(t.date)} · ${contraparte}`));
    const referencia = t.reversalOfId != null ? ` · Referente à transação #${t.reversalOfId}` : "";
    detalhes.appendChild(no("span", "extrato-status", `${status} · Transação #${t.id}${referencia}${t.categoryName ? ` · ${t.categoryName}` : ""}`));
    info.append(no("span", `badge-tipo ${direcao}`, `${tipo} · ${sentido}`), detalhes);
    const numero = Number(t.value);
    const valorValido = t.value !== null && t.value !== undefined && t.value !== "" && Number.isFinite(numero) && numero >= 0;
    const valor = valorValido ? numero.toLocaleString("pt-BR", { style: "currency", currency: "BRL" }) : "Valor indisponível";
    // Sinais de débito/crédito só para movimentos concluídos entre contas distintas.
    const sinal = t.status !== "COMPLETED" || propriaConta || !valorValido ? "" : remetente ? "− " : "+ ";
    const cor = t.status === "COMPLETED" && !propriaConta ? `valor-${direcao}` : "neutro";
    item.append(info, no("span", `extrato-valor ${cor}`, `${sinal}${valor}`));
    return item;
  }

  async function carregar(destino = 0) {
    if (ocupado || encerrada) return;
    ocupado = true;
    totalPaginas = 0;
    controles();
    lista.replaceChildren();
    el("extrato-pagina").textContent = "—";
    mensagem.dataset.erro = "false";
    mensagem.textContent = "Carregando extrato...";
    try {
      if (!usuario) {
        usuario = await api("users/me");
        if (encerrada) return;
        if (usuario.id == null || typeof usuario.name !== "string") { usuario = null; throw new Error("Resposta de usuário inválida."); }
        el("extrato-titular").textContent = `Extrato de ${usuario.name}`;
      }
      const parametros = new URLSearchParams({ page: String(destino), size: "20" });
      parametros.append("sort", "date,desc");
      parametros.append("sort", "id,desc");
      let dados = await api(`transactions?${parametros}`);
      if (encerrada) return;
      function validar(p) {
        if (!Array.isArray(p.content) || !Number.isInteger(p.totalPages) || p.totalPages < 0 || !Number.isInteger(p.number) || p.number < 0 || !Number.isInteger(p.totalElements) || p.totalElements < 0) throw new Error("Formato de paginação inesperado na resposta do servidor.");
      }
      validar(dados);
      // Se a quantidade de páginas mudou, retorna à última página disponível.
      if (dados.totalPages > 0 && destino >= dados.totalPages) {
        parametros.set("page", String(dados.totalPages - 1));
        dados = await api(`transactions?${parametros}`);
        if (encerrada) return;
        validar(dados);
      }
      // Monta todos os itens antes de exibir, evitando uma lista parcial em caso de erro.
      const itens = dados.content.map(renderTransacao);
      lista.replaceChildren(...itens);
      pagina = dados.number;
      totalPaginas = dados.totalPages;
      el("extrato-pagina").textContent = totalPaginas ? `Página ${pagina + 1} de ${totalPaginas}` : "Nenhuma página";
      mensagem.textContent = dados.totalElements ? `${dados.totalElements} transação(ões). Exibindo ${itens.length} nesta página.` : "Nenhuma transação encontrada para esta conta.";
    } catch (erro) {
      if (!encerrada) {
        totalPaginas = 0;
        mensagem.dataset.erro = "true";
        mensagem.textContent = erro.name === "AbortError" ? "O servidor demorou para responder. Clique em Atualizar extrato." : erro instanceof TypeError ? "Não foi possível conectar ao servidor. Clique em Atualizar extrato." : erro.message;
      }
    } finally { ocupado = false; controles(); }
  }
  anterior.addEventListener("click", () => carregar(Math.max(0, pagina - 1)));
  proxima.addEventListener("click", () => carregar(pagina + 1));
  atualizar.addEventListener("click", () => carregar(0));
  await carregar(0);
})();

