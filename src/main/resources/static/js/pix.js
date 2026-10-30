// Integração Pix com os endpoints existentes. Nenhuma operação é enviada sem clique do usuário.
(async () => {
  const el = id => document.getElementById(id);
  const pagina = document.body.dataset.pixPage;
  const token = sessionStorage.getItem("token");
  const draftKey = "flowbank.pixDraft";
  let usuario;
  let encerrada = false;
  let ocupado = false;
  let saldoVisivel = true;
  const moeda = valor => Number.isFinite(Number(valor)) && valor != null
    ? Number(valor).toLocaleString("pt-BR", { style: "currency", currency: "BRL" }) : "—";
  function mensagem(texto, erro = false) {
    el("pix-mensagem").textContent = texto;
    el("pix-mensagem").dataset.erro = String(erro);
  }
  function no(tag, texto, classe = "") {
    const n = document.createElement(tag);
    n.className = classe;
    if (texto !== undefined) n.textContent = texto;
    return n;
  }
  function sair() {
    encerrada = true;
    sessionStorage.removeItem("token");
    // Mantém o rascunho incerto para não autorizar um reenvio após novo login.
    for (const id of ["pix-chaves", "pix-historico", "pix-resumo"]) el(id)?.replaceChildren();
    el("pix-saldo").textContent = "—";
    window.location.replace("../index.html");
  }
  document.querySelector(".logout").addEventListener("click", e => { e.preventDefault(); sair(); });
  window.addEventListener("pageshow", e => { if (e.persisted) window.location.reload(); });
  if (!token) { sair(); return; }

  class ApiError extends Error {
    constructor(texto, status) { super(texto); this.status = status; }
  }
  async function api(caminho, opcoes = {}) {
    const controle = new AbortController();
    const timeout = setTimeout(() => controle.abort(), 15000);
    try {
      const resposta = await fetch(`../${caminho}`, {
        ...opcoes,
        headers: { Authorization: `Bearer ${token}`, ...(opcoes.body ? { "Content-Type": "application/json" } : {}) },
        signal: controle.signal,
        cache: "no-store",
      });
      if (resposta.status === 401) { sair(); throw new ApiError("Sessão expirada. Entre novamente.", 401); }
      if (!resposta.ok) {
        const dados = await resposta.json().catch(() => null);
        const campos = dados && typeof dados === "object" ? Object.values(dados).filter(v => typeof v === "string").join(" ") : "";
        throw new ApiError(resposta.status < 500 ? dados?.message || campos || `Requisição recusada (HTTP ${resposta.status}).` : `Falha no servidor (HTTP ${resposta.status}).`, resposta.status);
      }
      return resposta.status === 204 ? null : await resposta.json();
    } finally { clearTimeout(timeout); }
  }
  function erroTexto(erro) {
    return erro.name === "AbortError" ? "O servidor demorou para responder." : erro instanceof TypeError ? "A conexão com o servidor falhou." : erro.message;
  }
  function renderSaldo() {
    el("pix-saldo").textContent = saldoVisivel ? moeda(usuario?.balance) : "••••••••";
    el("pix-olho").textContent = saldoVisivel ? "Ocultar saldo" : "Mostrar saldo";
  }
  el("pix-olho").addEventListener("click", () => { saldoVisivel = !saldoVisivel; renderSaldo(); });
  function chaveDoCadastro(tipo) {
    if (tipo === "CPF") return usuario.cpf?.replace(/\D/g, "") || "";
    if (tipo === "EMAIL") return usuario.email?.trim().toLowerCase() || "";
    if (tipo === "TELEFONE") return (usuario.numeroTelefone || usuario.numeorTelefone || "").replace(/\D/g, "");
    return "";
  }
  function lerDraft() {
    try { return JSON.parse(sessionStorage.getItem(draftKey) || "null"); } catch { return null; }
  }
  function salvarDraft(draft) { sessionStorage.setItem(draftKey, JSON.stringify(draft)); }
  function bloqueado(draft) { return draft && String(draft.owner) === String(usuario.id) && ["sending", "uncertain"].includes(draft.state); }
  function avisoIncerto() {
    mensagem("Há uma transferência sem confirmação de resultado. Consulte o extrato antes de iniciar outra. Não reenvie o mesmo pagamento. Após conferir, use o botão abaixo para liberar uma nova operação.", true);
    if (el("pix-liberar")) return;
    const liberar = no("button", "Já conferi o extrato: liberar nova operação");
    liberar.id = "pix-liberar";
    liberar.type = "button";
    liberar.addEventListener("click", () => {
      if (!window.confirm("Você conferiu o extrato e entende que uma nova transferência poderá gerar outro débito?")) return;
      sessionStorage.removeItem(draftKey);
      window.location.replace("transferir.html");
    });
    el("pix-mensagem").after(liberar);
  }

  async function carregarChaves() {
    const lista = el("pix-chaves");
    lista.replaceChildren();
    const chaves = await api(`pix-keys/user/${encodeURIComponent(usuario.id)}`);
    if (encerrada) return;
    if (!Array.isArray(chaves)) throw new Error("Resposta de chaves inválida.");
    if (!chaves.length) { lista.appendChild(no("p", "Nenhuma chave cadastrada.")); return; }
    for (const chave of chaves) {
      const item = no("div", undefined, "pix-linha");
      item.append(no("strong", chave.keyType), no("p", chave.keyValue));
      const copiar = no("button", "Copiar"); copiar.type = "button";
      copiar.addEventListener("click", async () => {
        try { await navigator.clipboard.writeText(chave.keyValue); mensagem("Chave copiada."); }
        catch { mensagem("Não foi possível copiar automaticamente. Selecione e copie a chave exibida.", true); }
      });
      const excluir = no("button", "Excluir"); excluir.type = "button";
      excluir.addEventListener("click", async () => {
        if (ocupado || !window.confirm(`Excluir a chave ${chave.keyValue}?`)) return;
        ocupado = true; excluir.disabled = true;
        try {
          await api(`pix-keys/${encodeURIComponent(chave.id)}`, { method: "DELETE" });
          if (encerrada) return;
          item.remove(); mensagem("Chave excluída.");
          await carregarChaves();
        } catch (erro) { if (!encerrada) mensagem(`${erroTexto(erro)} Atualize as chaves para conferir o resultado.`, true); }
        finally { ocupado = false; excluir.disabled = false; }
      });
      item.append(copiar, excluir); lista.appendChild(item);
    }
  }
  async function carregarHistorico() {
    const lista = el("pix-historico"); lista.replaceChildren();
    const transacoes = await api("transactions/type/PIX");
    if (encerrada) return;
    if (!Array.isArray(transacoes)) throw new Error("Histórico Pix inválido.");
    const recentes = [...transacoes].sort((a,b) => String(b.date).localeCompare(String(a.date)) || Number(b.id)-Number(a.id)).slice(0,10);
    if (!recentes.length) { lista.appendChild(no("p", "Nenhuma transferência Pix registrada.")); return; }
    for (const t of recentes) {
      const enviou = String(t.sender?.id) === String(usuario.id);
      const recebeu = String(t.receiver?.id) === String(usuario.id);
      if (!enviou && !recebeu) throw new Error("Histórico contém transação de outro usuário.");
      const propria = enviou && recebeu;
      const concluida = t.status === "COMPLETED";
      const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(t.date || "");
      const data = partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : "Data não informada";
      const item = no("div", undefined, "pix-linha");
      const status = ({ COMPLETED: "Concluída", PEDING: "Pendente", PENDING: "Pendente", FAILED: "Falhou" })[t.status] || "Status não informado";
      item.append(no("strong", `${propria ? "Mesma conta" : enviou ? "Pix enviado" : "Pix recebido"} · ${concluida && !propria ? enviou ? "− " : "+ " : ""}${moeda(t.value)}`), no("p", `${t.description || "Pix"} · ${data} · ${status}`), no("p", propria ? "Mesma conta" : enviou ? `Para: ${t.receiver?.name || t.destination || "Destino externo"}` : `De: ${t.sender?.name || "Remetente"}`));
      lista.appendChild(item);
    }
  }
  async function atualizarDados() {
    if (ocupado) return;
    ocupado = true; el("pix-atualizar").disabled = true;
    mensagem("Atualizando...");
    const tarefas = [{ id: "pix-chaves", run: carregarChaves }];
    if (pagina === "home") tarefas.push({ id: "pix-historico", run: carregarHistorico });
    try {
      const perfil = await api("users/me");
      if (encerrada) return;
      usuario = perfil;
      renderSaldo();
      const resultados = await Promise.allSettled(tarefas.map(t => t.run()));
      if (encerrada) return;
      const falhas = [];
      resultados.forEach((resultado,i) => { if (resultado.status === "rejected") { el(tarefas[i].id).replaceChildren(no("p", "Consulta indisponível. Tente atualizar.")); falhas.push(erroTexto(resultado.reason)); } });
      mensagem(falhas.length ? falhas.join(" ") : "Dados atualizados.", falhas.length > 0);
    } catch (erro) {
      if (!encerrada) {
        usuario.balance = null; renderSaldo();
        for (const tarefa of tarefas) el(tarefa.id).replaceChildren();
        mensagem(erroTexto(erro), true);
      }
    } finally { ocupado = false; el("pix-atualizar").disabled = false; }
  }
  function prepararChaves() {
    const previa = () => {
      const tipo = el("pix-tipo").value;
      el("pix-chave-previa").textContent = tipo === "ALEATORIA" ? "Uma chave UUID será gerada pelo servidor." : `Chave a cadastrar: ${chaveDoCadastro(tipo) || "Dado ausente no cadastro"}`;
    };
    el("pix-tipo").addEventListener("change", previa); previa();
    el("pix-criar").disabled = false;
    el("pix-chave-form").addEventListener("submit", async e => {
      e.preventDefault(); if (ocupado) return;
      const tipo = el("pix-tipo").value;
      const valor = chaveDoCadastro(tipo);
      if (tipo !== "ALEATORIA" && !valor) { mensagem("Esse dado está ausente no seu cadastro.", true); return; }
      ocupado = true; el("pix-criar").disabled = true;
      try {
        await api("pix-keys", { method: "POST", body: JSON.stringify({ keyType: tipo, keyValue: tipo === "ALEATORIA" ? null : valor }) });
        if (encerrada) return;
        mensagem("Chave cadastrada.");
        await carregarChaves();
      } catch (erro) { if (!encerrada) mensagem(`${erroTexto(erro)} Atualize a lista antes de repetir o cadastro.`, true); }
      finally { ocupado = false; el("pix-criar").disabled = false; }
    });
  }
  function valorDecimal(texto) {
    const partes = /^(\d+)(?:[,.](\d{1,2}))?$/.exec(texto.trim());
    if (!partes) throw new Error("Informe um valor como 25,50, sem separador de milhar.");
    const centavos = Number(partes[1]) * 100 + Number((partes[2] || "").padEnd(2,"0"));
    if (!Number.isSafeInteger(centavos) || centavos <= 0) throw new Error("Informe um valor positivo com até duas casas decimais.");
    return (centavos / 100).toFixed(2);
  }
  async function consultarChave(texto) {
    const chave = texto.trim();
    if (!chave) throw new Error("Informe a chave do destinatário.");
    if (/^000201/.test(chave)) throw new Error("Código Pix Copia e Cola não suportado. Informe uma chave Pix.");
    const dados = await api(`pix-keys/${encodeURIComponent(chave)}`);
    if (!dados?.keyValue || !dados?.titular) throw new Error("Resposta de destinatário inválida.");
    return dados;
  }
  async function prepararTransferencia() {
    let categoriasDisponiveis = [];
    if (bloqueado(lerDraft())) { avisoIncerto(); return; }
    try {
      const categorias = await api(`categories/user/${usuario.id}`);
      if (encerrada) return;
      if (!Array.isArray(categorias)) throw new Error("Categorias inválidas.");
      categoriasDisponiveis = categorias;
      for (const categoria of categorias) {
        const option = no("option", categoria.name); option.value = String(categoria.id);
        el("pix-categoria").appendChild(option);
      }
    } catch (erro) {
      if (encerrada) return;
      mensagem("Categorias indisponíveis. Você pode continuar sem categoria ou recarregar a página.", true);
    }
    el("pix-continuar").disabled = false;
    el("pix-transferir-form").addEventListener("submit", async e => {
      e.preventDefault(); if (ocupado) return;
      if (bloqueado(lerDraft())) { avisoIncerto(); return; }
      ocupado = true; el("pix-continuar").disabled = true;
      try {
        const value = valorDecimal(el("pix-valor").value);
        if (Number(value) > Number(usuario.balance)) throw new Error("Saldo insuficiente para esse valor.");
        const destinatario = await consultarChave(el("pix-chave").value);
        if (encerrada) return;
        const categoriaSelecionada = el("pix-categoria").value;
        const categoryId = categoriaSelecionada ? Number(categoriaSelecionada) : null;
        if (categoryId != null && (!Number.isSafeInteger(categoryId) || categoryId <= 0)) throw new Error("Categoria inválida.");
        const categoryName = categoriasDisponiveis.find(c => c.id === categoryId)?.name || "Sem categoria";
        salvarDraft({ owner: usuario.id, value, categoryId, categoryName, description: el("pix-descricao").value.trim() || "Transferência Pix", destinatario, created: Date.now(), state: "ready" });
        window.location.assign("confirmar.html");
      } catch (erro) { if (!encerrada) mensagem(erroTexto(erro), true); }
      finally { ocupado = false; el("pix-continuar").disabled = false; }
    });
  }
  async function prepararConfirmacao() {
    const draft = lerDraft();
    if (!draft || String(draft.owner) !== String(usuario.id)) { mensagem("Nenhuma transferência preparada para esta conta. Volte e informe a chave e o valor.", true); return; }
    if (bloqueado(draft)) { avisoIncerto(); return; }
    if (draft.state !== "ready" || !draft.destinatario?.keyValue || Date.now() - draft.created > 600000 || !Number.isFinite(draft.created)) { mensagem("Esta confirmação expirou. Volte e prepare a transferência novamente.", true); return; }
    valorDecimal(draft.value);
    const resumo = el("pix-resumo");
    resumo.append(no("h2", "Confira antes de enviar"), no("p", `Destinatário: ${draft.destinatario.titular}`), no("p", `Chave: ${draft.destinatario.keyValue}`), no("p", `Valor: ${moeda(draft.value)}`), no("p", `Descrição: ${draft.description}`), no("p", `Categoria: ${draft.categoryName || "Sem categoria"}`));
    el("pix-confirmar").disabled = false;
    mensagem("A transferência só será enviada ao clicar em Confirmar transferência.");
    el("pix-confirmar").addEventListener("click", async () => {
      if (ocupado) return;
      const atual = lerDraft();
      if (!atual || atual.state !== "ready" || atual.created !== draft.created || String(atual.owner) !== String(usuario.id)) { avisoIncerto(); return; }
      if (Date.now()-draft.created > 600000) { mensagem("Confirmação expirada. Prepare novamente a transferência.", true); el("pix-confirmar").disabled = true; return; }
      ocupado = true; el("pix-confirmar").disabled = true;
      let enviado = false;
      try {
        // Confere a chave novamente antes do envio para detectar mudança do titular.
        const destino = await consultarChave(draft.destinatario.keyValue);
        if (encerrada) return;
        if (destino.titular !== draft.destinatario.titular || destino.keyValue !== draft.destinatario.keyValue || destino.keyType !== draft.destinatario.keyType) throw new Error("Os dados do destinatário mudaram. Volte e consulte novamente.");
        const hoje = new Date();
        const date = `${hoje.getFullYear()}-${String(hoje.getMonth()+1).padStart(2,"0")}-${String(hoje.getDate()).padStart(2,"0")}`;
        draft.state = "sending"; salvarDraft(draft);
        enviado = true;
        const resultado = await api("transactions", { method: "POST", body: JSON.stringify({ description: draft.description, value: draft.value, date, type: "PIX", senderId: usuario.id, pixKey: destino.keyValue, categoryId: draft.categoryId || null }) });
        if (encerrada) return;
        // Uma resposta inesperada pode ocorrer depois do débito; nunca reenviar automaticamente.
        if (!resultado?.id || resultado.status !== "COMPLETED") throw new Error("Resposta inesperada da transferência.");
        draft.state = "done"; salvarDraft(draft);
        mensagem(`Transferência concluída! Transação #${resultado.id}. Consulte o extrato para os detalhes.`);
        el("pix-confirmar").textContent = "Transferência concluída";
        el("pix-cancelar").textContent = "Preparar nova transferência";
        usuario.balance = null; renderSaldo();
        try { const perfil = await api("users/me"); if (!encerrada) { usuario = perfil; renderSaldo(); } } catch { /* O sucesso da transferência não depende da atualização do saldo. */ }
      } catch (erro) {
        if (!enviado || (erro instanceof ApiError && [400,401,403,404,409,422,429].includes(erro.status))) {
          if (enviado) { draft.state = "ready"; salvarDraft(draft); }
          if (!encerrada) { mensagem(erroTexto(erro), true); el("pix-confirmar").disabled = false; }
        } else {
          draft.state = "uncertain"; salvarDraft(draft);
          if (!encerrada) avisoIncerto();
        }
      } finally { ocupado = false; }
    });
  }

  try {
    usuario = await api("users/me");
    if (encerrada) return;
    if (usuario.id == null || typeof usuario.name !== "string") throw new Error("Resposta de usuário inválida.");
    renderSaldo(); mensagem("Dados carregados.");
    if (["home", "keys"].includes(pagina)) {
      el("pix-atualizar").addEventListener("click", atualizarDados);
      if (pagina === "keys") prepararChaves();
      await atualizarDados();
    } else if (["transfer", "paste"].includes(pagina)) await prepararTransferencia();
    else if (pagina === "confirm") await prepararConfirmacao();
  } catch (erro) { if (!encerrada) mensagem(`${erroTexto(erro)} Recarregue a página para tentar novamente.`, true); }
})();
