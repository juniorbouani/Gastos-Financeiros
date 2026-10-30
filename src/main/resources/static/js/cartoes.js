(async () => {
  const el = id => document.getElementById(id);
  const token = sessionStorage.getItem("token");
  const pagina = document.body.dataset.cardsPage;
  const pendingKey = "flowbank.cardsPending";
  let usuario, cartoes = [], selecionado = null, fatura = null;
  let ocupado = false, encerrada = false, saldoVisivel = true;
  const moeda = v => v != null && Number.isFinite(Number(v)) ? Number(v).toLocaleString("pt-BR", {style:"currency",currency:"BRL"}) : "—";
  const data = v => /^(\d{4})-(\d{2})-(\d{2})$/.test(v || "") ? v.split("-").reverse().join("/") : "Não informada";
  function mensagem(texto, erro = false) { el("cards-mensagem").textContent = texto; el("cards-mensagem").dataset.erro = String(erro); }
  function no(tag, texto, classe = "") { const n = document.createElement(tag); n.className = classe; if (texto !== undefined) n.textContent = texto; return n; }
  function botao(texto, acao, disabled = false) { const n = no("button",texto); n.type="button"; n.disabled=disabled; n.addEventListener("click",acao); return n; }
  function sair() {
    encerrada=true;sessionStorage.removeItem("token");
    for (const id of ["cards-lista","cards-faturas","cards-lancamentos","cards-selecionado","cards-fatura-titulo"]) el(id)?.replaceChildren();
    el("cards-saldo").textContent="—";window.location.replace("../index.html");
  }
  document.querySelector(".logout").addEventListener("click",e=>{e.preventDefault();sair();});
  window.addEventListener("pageshow",e=>{if(e.persisted)window.location.reload();});
  if(!token){sair();return;}
  class ApiError extends Error {constructor(texto,status){super(texto);this.status=status;}}
  async function api(caminho, opcoes={}) {
    const controle=new AbortController();const timeout=setTimeout(()=>controle.abort(),15000);
    try {
      const resposta=await fetch(`../${caminho}`,{...opcoes,headers:{Authorization:`Bearer ${token}`,...(opcoes.body?{"Content-Type":"application/json"}:{})},signal:controle.signal,cache:"no-store"});
      if(resposta.status===401){sair();throw new ApiError("Sessão expirada. Entre novamente.",401);}
      if(!resposta.ok){const d=await resposta.json().catch(()=>null);const campos=d&&typeof d==="object"?Object.values(d).filter(v=>typeof v==="string").join(" "):"";throw new ApiError(resposta.status<500?d?.message||campos||`Requisição recusada (HTTP ${resposta.status}).`:`Falha no servidor (HTTP ${resposta.status}).`,resposta.status);}
      return resposta.status===204?null:await resposta.json();
    }finally{clearTimeout(timeout);}
  }
  function erroTexto(e){return e.name==="AbortError"?"O servidor demorou para responder.":e instanceof TypeError?"A conexão falhou.":e.message;}
  function decimal(texto, permiteZero=false){
    const m=/^(\d+)(?:[,.](\d{1,2}))?$/.exec(texto.trim());
    if(!m)throw new Error("Informe um valor como 1000,50, sem separador de milhar.");
    const c=Number(m[1])*100+Number((m[2]||"").padEnd(2,"0"));
    if(!Number.isSafeInteger(c)||c<0||(!permiteZero&&c===0))throw new Error("Informe um valor válido com até duas casas decimais.");
    return(c/100).toFixed(2);
  }
  function saldo(){el("cards-saldo").textContent=saldoVisivel?moeda(usuario?.balance):"••••••••";el("cards-olho").textContent=saldoVisivel?"Ocultar saldo":"Mostrar saldo";}
  el("cards-olho").addEventListener("click",()=>{saldoVisivel=!saldoVisivel;saldo();});
  function pendente(){try{const p=JSON.parse(sessionStorage.getItem(pendingKey)||"null");return p&&String(p.owner)===String(usuario.id)?p:null;}catch{return null;}}
  function mostrarPendencia(){
    const caixa=el("cards-pendencia");caixa.replaceChildren();const p=pendente();if(!p)return;
    caixa.appendChild(no("p",`Resultado ainda não confirmado: ${p.label}. Atualize os cartões, as faturas e o saldo para conferir antes de repetir a operação.`));
    caixa.appendChild(botao("Já conferi os dados: liberar novas operações",()=>{
      if(!window.confirm("Você conferiu o resultado? Repetir uma compra, solicitação ou pagamento pode duplicar a operação."))return;
      sessionStorage.removeItem(pendingKey);mostrarPendencia();mensagem("Novas operações liberadas após sua conferência.");
    }));
  }
  // A atualização visual é feita fora desta função: uma falha de consulta não desfaz uma operação concluída.
  async function alterar(label,caminho,metodo,corpo,validar){
    if(pendente()){mostrarPendencia();throw new Error("Confira a operação pendente antes de enviar outra.");}
    const pendencia={owner:usuario.id,label,created:Date.now()};
    sessionStorage.setItem(pendingKey,JSON.stringify(pendencia));
    try{
      const resultado=await api(caminho,{method:metodo,...(corpo?{body:JSON.stringify(corpo)}:{})});
      if(!validar(resultado))throw new Error("Resposta inesperada após o envio.");
      sessionStorage.removeItem(pendingKey);mostrarPendencia();return resultado;
    }catch(e){
      if(e instanceof ApiError&&[400,401,403,404,409,422,429].includes(e.status))sessionStorage.removeItem(pendingKey);
      if(!encerrada)mostrarPendencia();throw e;
    }
  }
  async function acao(fn){if(ocupado||encerrada)return;ocupado=true;try{await fn();}catch(e){if(!encerrada)mensagem(erroTexto(e),true);}finally{ocupado=false;}}
  async function atualizarPerfil(){const u=await api("users/me");if(!encerrada){if(u.id==null)throw new Error("Perfil inválido.");usuario=u;saldo();}}
  async function aposAlteracao(texto){
    if(encerrada)return;mensagem(texto);
    try{await carregarCartoes();}catch(e){if(!encerrada)mensagem(`${texto} Não foi possível atualizar os dados: ${erroTexto(e)} Use Atualizar cartões.`,true);}
  }
  async function carregarCartoes(){
    await atualizarPerfil();if(encerrada)return;
    const dados=await api(`credit-cards/user/${encodeURIComponent(usuario.id)}`);if(encerrada)return;
    if(!Array.isArray(dados))throw new Error("Lista de cartões inválida.");cartoes=dados;
    const lista=el("cards-lista");lista.replaceChildren();
    if(!cartoes.length){lista.appendChild(no("p","Você ainda não possui cartões. Solicite um cartão para começar."));}
    for(const c of cartoes){
      const item=no("article",undefined,"cards-linha");
      item.append(no("h2",`Cartão ${c.numeroMascarado} · ${c.status}`),no("p",`Titular: ${usuario.name} · Validade: ${data(c.dataExpiracao)} · ${c.descartavel?"Descartável":"Virtual"}`),no("p",`Limite total: ${moeda(c.limiteTotal)} · Disponível: ${moeda(c.limiteDisponivel)}`));
      item.appendChild(botao("Selecionar e ver faturas",()=>acao(()=>selecionarCartao(c))));
      if(c.status!=="CANCELADO")item.appendChild(botao(c.status==="BLOQUEADO"?"Desbloquear":"Bloquear",()=>acao(async()=>{
        const desbloquear=c.status==="BLOQUEADO";if(!window.confirm(`${desbloquear?"Desbloquear":"Bloquear"} o cartão ${c.numeroMascarado}?`))return;
        await alterar(`${desbloquear?"Desbloqueio":"Bloqueio"} do cartão #${c.id}`,`credit-cards/${c.id}/${desbloquear?"unblock":"block"}`,"PUT",null,r=>r?.id===c.id);
        await aposAlteracao("Status do cartão atualizado.");
      })));
      lista.appendChild(item);
    }
    if(selecionado){const atual=cartoes.find(c=>String(c.id)===String(selecionado.id));if(atual)await selecionarCartao(atual);else{selecionado=null;el("cards-detalhes").hidden=true;}}
    mostrarPendencia();
  }
  async function selecionarCartao(c){
    selecionado=c;fatura=null;el("cards-detalhes").hidden=false;el("cards-fatura-detalhes").hidden=true;
    el("cards-selecionado").textContent=`Cartão selecionado: ${c.numeroMascarado}`;
    el("cards-novo-limite").value=String(c.limiteTotal);
    el("cards-faturas").replaceChildren(no("p","Carregando faturas..."));
    try{
      const faturas=await api(`credit-cards/${c.id}/invoices`);if(encerrada)return;
      if(!Array.isArray(faturas))throw new Error("Resposta de faturas inválida.");
      const lista=el("cards-faturas");lista.replaceChildren();
      if(!faturas.length)lista.appendChild(no("p","Nenhuma fatura gerada para este cartão."));
      for(const f of faturas){const item=no("article",undefined,"cards-linha");item.append(no("h3",`Fatura ${f.mesReferencia} · ${f.status}`),no("p",`Vencimento: ${data(f.dataVencimento)}`),no("p",`Total: ${moeda(f.valorTotal)} · Pago: ${moeda(f.valorPago)} · Restante: ${moeda(f.valorRestante)}`),botao("Ver lançamentos e pagar",()=>acao(()=>selecionarFatura(f))));lista.appendChild(item);}
    }catch(e){if(!encerrada)el("cards-faturas").replaceChildren(no("p",`Não foi possível consultar as faturas: ${erroTexto(e)}`));throw e;}
  }
  async function selecionarFatura(f){
    fatura=null;el("cards-fatura-detalhes").hidden=false;el("cards-pagamento-form").hidden=true;
    el("cards-fatura-titulo").textContent=`Fatura ${f.mesReferencia} · Saldo devedor: ${moeda(f.valorRestante)}`;
    el("cards-lancamentos").replaceChildren(no("p","Carregando lançamentos..."));
    try{
      const itens=await api(`credit-cards/invoices/${f.id}/items`);if(encerrada)return;
      if(!Array.isArray(itens))throw new Error("Resposta de lançamentos inválida.");
      const lista=el("cards-lancamentos");lista.replaceChildren();
      if(!itens.length)lista.appendChild(no("p","Nenhum lançamento nesta fatura."));
      for(const i of itens){const item=no("div",undefined,"cards-linha");item.append(no("strong",`${i.descricao} · ${moeda(i.valor)}`),no("p",`${data(i.data)} · Parcela ${i.parcelaAtual}/${i.totalParcelas} · ${i.estornado?"Estornado":"Lançado"} · #${i.id}`));lista.appendChild(item);}
      fatura=f;el("cards-pagamento-form").hidden=Number(f.valorRestante)<=0;el("cards-pagamento-valor").value="";
    }catch(e){if(!encerrada)el("cards-lancamentos").replaceChildren(no("p",`Consulta indisponível: ${erroTexto(e)}`));throw e;}
  }
  function prepararCartoes(){
    el("cards-atualizar").addEventListener("click",()=>acao(async()=>{await carregarCartoes();if(!encerrada)mensagem("Dados atualizados.");}));
    el("cards-limite-form").addEventListener("submit",e=>{e.preventDefault();return acao(async()=>{
      if(!selecionado)throw new Error("Selecione um cartão.");const c=selecionado;const limite=decimal(el("cards-novo-limite").value,true);
      if(!window.confirm(`Alterar o limite total de ${c.numeroMascarado} para ${moeda(limite)}?`))return;
      await alterar(`Alteração de limite do cartão #${c.id}`,`credit-cards/${c.id}/limit`,"PUT",{novoLimite:limite},r=>r?.id===c.id);
      await aposAlteracao("Limite atualizado.");
    });});
    el("cards-compra-form").addEventListener("submit",e=>{e.preventDefault();return acao(async()=>{
      if(!selecionado||selecionado.status!=="ATIVO")throw new Error("Selecione um cartão ativo.");const c=selecionado;
      const valor=decimal(el("cards-compra-valor").value);const parcelas=Number(el("cards-compra-parcelas").value);const descricao=el("cards-compra-descricao").value.trim();
      if(!descricao)throw new Error("Informe a descrição da compra.");if(!Number.isSafeInteger(parcelas)||parcelas<=0)throw new Error("Informe um número inteiro positivo de parcelas.");
      if(Number(valor)>Number(c.limiteDisponivel))throw new Error("Limite disponível insuficiente.");
      if(!window.confirm(`Registrar compra de ${moeda(valor)} em ${parcelas}x no cartão ${c.numeroMascarado}? Descrição: ${descricao}`))return;
      await alterar(`Compra de ${moeda(valor)} no cartão #${c.id}`,`credit-cards/${c.id}/purchases`,"POST",{descricao,valor,totalParcelas:parcelas},r=>r?.id!=null);
      el("cards-compra-form").reset();await aposAlteracao("Compra registrada e faturas geradas pelo servidor.");
    });});
    el("cards-pagar-total").addEventListener("click",()=>{if(fatura)el("cards-pagamento-valor").value=String(fatura.valorRestante);});
    el("cards-pagamento-form").addEventListener("submit",e=>{e.preventDefault();return acao(async()=>{
      if(!fatura)throw new Error("Selecione uma fatura.");const f=fatura;const valor=decimal(el("cards-pagamento-valor").value);
      if(Number(valor)>Number(f.valorRestante))throw new Error("O pagamento excede o saldo devedor da fatura.");
      if(Number(valor)>Number(usuario.balance))throw new Error("Saldo em conta insuficiente.");
      if(!window.confirm(`Pagar ${moeda(valor)} da fatura ${f.mesReferencia}? Esse valor será debitado da sua conta.`))return;
      await alterar(`Pagamento de ${moeda(valor)} da fatura #${f.id}`,`credit-cards/invoices/${f.id}/pay`,"POST",{valor},r=>r?.id===f.id);
      el("cards-pagamento-form").hidden=true;fatura=null;await aposAlteracao("Pagamento concluído. O servidor atualizou o saldo e o limite disponível.");
    });});
  }
  function prepararSolicitacao(){
    let solicitado=false;
    el("cards-solicitar").disabled=false;
    el("cards-request-form").addEventListener("submit",e=>{e.preventDefault();return acao(async()=>{
      if(solicitado)return;
      const limite=decimal(el("cards-limite").value);const descartavel=el("cards-descartavel").value==="true";
      if(!window.confirm(`Solicitar cartão ${descartavel?"descartável":"virtual"} com limite de ${moeda(limite)}?`))return;
      await alterar("Solicitação de novo cartão","credit-cards","POST",{limiteTotal:limite,descartavel},r=>r?.id!=null);
      solicitado=true;
      if(!encerrada){el("cards-solicitar").disabled=true;mensagem("Cartão criado. Abra Meus cartões e faturas para consultá-lo.");window.location.assign("cartoes.html");}
    });});
  }
  try{await atualizarPerfil();if(encerrada)return;mostrarPendencia();if(pagina==="request")prepararSolicitacao();else{prepararCartoes();await acao(carregarCartoes);}if(!encerrada&&!pendente()&&el("cards-mensagem").dataset.erro!=="true")mensagem("Dados carregados.");}
  catch(e){if(!encerrada)mensagem(`${erroTexto(e)} Recarregue a página para tentar novamente.`,true);}
})();
