(async () => {
  const el=id=>document.getElementById(id);
  const pagina=document.body.dataset.paymentsPage;
  const token=sessionStorage.getItem("token");
  const pendingKey="flowbank.boletoPending";
  let usuario, preparado=null, ocupado=false, encerrada=false, visivel=true;
  const moeda=v=>v!=null&&Number.isFinite(Number(v))?Number(v).toLocaleString("pt-BR",{style:"currency",currency:"BRL"}):"—";
  function mensagem(texto,erro=false){el("pagamentos-mensagem").textContent=texto;el("pagamentos-mensagem").dataset.erro=String(erro);}
  function no(tag,texto,classe=""){const n=document.createElement(tag);n.className=classe;if(texto!==undefined)n.textContent=texto;return n;}
  function sair(){encerrada=true;sessionStorage.removeItem("token");preparado=null;for(const id of ["pagamentos-historico","pagamento-resumo"])el(id)?.replaceChildren();el("pagamentos-saldo").textContent="—";window.location.replace("../../index.html");}
  document.querySelector(".logout").addEventListener("click",e=>{e.preventDefault();sair();});
  window.addEventListener("pageshow",e=>{if(e.persisted)window.location.reload();});
  if(!token){sair();return;}
  class ApiError extends Error{constructor(texto,status){super(texto);this.status=status;}}
  async function api(caminho,opcoes={}){
    const controle=new AbortController();const timeout=setTimeout(()=>controle.abort(),15000);
    try{
      const resposta=await fetch(`../../${caminho}`,{...opcoes,headers:{Authorization:`Bearer ${token}`,...(opcoes.body?{"Content-Type":"application/json"}:{})},cache:"no-store",signal:controle.signal});
      if(resposta.status===401){sair();throw new ApiError("Sessão expirada. Entre novamente.",401);}
      if(!resposta.ok){const d=await resposta.json().catch(()=>null);const campos=d&&typeof d==="object"?Object.values(d).filter(v=>typeof v==="string").join(" "):"";throw new ApiError(resposta.status<500?d?.message||campos||`Requisição recusada (HTTP ${resposta.status}).`:`Falha no servidor (HTTP ${resposta.status}).`,resposta.status);}
      return await resposta.json();
    }finally{clearTimeout(timeout);}
  }
  function erroTexto(e){return e.name==="AbortError"?"O servidor demorou para responder.":e instanceof TypeError?"A conexão falhou.":e.message;}
  function saldo(){el("pagamentos-saldo").textContent=visivel?moeda(usuario?.balance):"••••••••";el("pagamentos-olho").textContent=visivel?"Ocultar saldo":"Mostrar saldo";}
  el("pagamentos-olho").addEventListener("click",()=>{visivel=!visivel;saldo();});
  async function perfil(){const u=await api("users/me");if(encerrada)return;if(u.id==null||typeof u.name!=="string")throw new Error("Perfil inválido.");usuario=u;saldo();}
  function pendente(){try{const p=JSON.parse(sessionStorage.getItem(pendingKey)||"null");return p&&String(p.owner)===String(usuario.id)?p:null;}catch{return null;}}
  function mostrarPendencia(){
    const caixa=el("pagamentos-pendencia");caixa.replaceChildren();const p=pendente();if(!p)return;
    caixa.appendChild(no("p",`Resultado não confirmado para o registro de ${moeda(p.value)}. Consulte o extrato e o saldo antes de repetir. Não há reenvio automático.`));
    const link=no("a","Consultar extrato");link.href="extrato.html";caixa.appendChild(link);
    const liberar=no("button","Já conferi o extrato: liberar novo registro");liberar.type="button";
    liberar.addEventListener("click",()=>{if(!window.confirm("Você conferiu o extrato? Um novo envio poderá gerar outro débito no FlowBank."))return;sessionStorage.removeItem(pendingKey);window.location.replace("transferirPagar.html");});
    caixa.appendChild(liberar);
  }
  function decimal(texto){
    const m=/^(\d+)(?:[,.](\d{1,2}))?$/.exec(texto.trim());if(!m)throw new Error("Informe um valor como 125,50, sem separador de milhar.");
    const c=Number(m[1])*100+Number((m[2]||"").padEnd(2,"0"));if(!Number.isSafeInteger(c)||c<=0)throw new Error("Informe um valor positivo com até duas casas decimais.");return(c/100).toFixed(2);
  }
  async function historico(){
    const lista=el("pagamentos-historico");lista.replaceChildren();
    const dados=await api("transactions/type/BOLETO");if(encerrada)return;if(!Array.isArray(dados))throw new Error("Histórico inválido.");
    const recentes=[...dados].sort((a,b)=>String(b.date).localeCompare(String(a.date))||Number(b.id)-Number(a.id)).slice(0,20);
    if(!recentes.length){lista.appendChild(no("p","Nenhum registro de boleto nesta conta."));return;}
    const itens=[];
    for(const t of recentes){
      const enviou=String(t.sender?.id)===String(usuario.id),recebeu=String(t.receiver?.id)===String(usuario.id);if(!enviou&&!recebeu)throw new Error("Histórico contém transação de outra conta.");
      const propria=enviou&&recebeu;const concluida=t.status==="COMPLETED";
      const status=({COMPLETED:"Registro concluído no FlowBank",PEDING:"Pendente",PENDING:"Pendente",FAILED:"Falhou"})[t.status]||"Status não informado";
      const data=/^(\d{4})-(\d{2})-(\d{2})$/.test(t.date||"")?t.date.split("-").reverse().join("/"):"Data não informada";
      const item=no("article",undefined,"pagamento-linha");
      item.append(no("strong",`${propria?"Mesma conta":enviou?"Saída":"Entrada"} · ${concluida&&!propria?enviou?"− ":"+ ":""}${moeda(t.value)}`),no("p",t.description||"Boleto"),no("p",`${data} · ${status} · #${t.id}`),no("p",`Referência: ${t.destination||"Não informada"}`));itens.push(item);
    }
    lista.replaceChildren(...itens);
  }
  async function atualizar(){
    if(ocupado||encerrada)return;ocupado=true;el("pagamentos-atualizar").disabled=true;mensagem("Atualizando dados...");el("pagamentos-historico").replaceChildren();
    try{await perfil();if(encerrada)return;await historico();if(!encerrada){mostrarPendencia();mensagem("Dados atualizados.");}}
    catch(e){if(!encerrada){usuario.balance=null;saldo();mensagem(erroTexto(e),true);}}
    finally{ocupado=false;el("pagamentos-atualizar").disabled=false;}
  }
  async function prepararFormulario(){
    let categoriasDisponiveis=[];
    if(pendente()){mostrarPendencia();return;}
    try {
      const categorias=await api(`categories/user/${usuario.id}`);
      if(encerrada)return;
      if(!Array.isArray(categorias))throw new Error("Categorias inválidas.");
      categoriasDisponiveis=categorias;
      for(const categoria of categorias){const option=no("option",categoria.name);option.value=String(categoria.id);el("pagamento-categoria").appendChild(option);}
    } catch(erro){if(encerrada)return;mensagem("Categorias indisponíveis. Você pode registrar sem categoria ou recarregar a página.",true);}
    el("pagamento-conferir").disabled=false;
    el("pagamento-form").addEventListener("submit",async e=>{
      e.preventDefault();if(ocupado)return;if(pendente()){mostrarPendencia();return;}
      ocupado=true;el("pagamento-conferir").disabled=true;
      try{
        preparado=null;
        // Remove somente separadores usuais; não aceita letras escondidas no código.
        const codigo=el("pagamento-codigo").value.trim().replace(/[.\s-]/g,"");
        if(!/^\d{1,120}$/.test(codigo))throw new Error("Informe somente os números do código de referência.");
        const descricao=el("pagamento-descricao").value.trim();if(!descricao)throw new Error("Informe uma descrição ou o beneficiário informado no boleto.");
        const valor=decimal(el("pagamento-valor").value);
        await perfil();if(encerrada)return;
        if(Number(valor)>Number(usuario.balance))throw new Error("Saldo insuficiente para esse registro.");
        const categoriaSelecionada=el("pagamento-categoria").value;
        const categoryId=categoriaSelecionada?Number(categoriaSelecionada):null;
        if(categoryId!=null&&(!Number.isSafeInteger(categoryId)||categoryId<=0))throw new Error("Categoria inválida.");
        const categoryName=categoriasDisponiveis.find(c=>c.id===categoryId)?.name||"Sem categoria";
        preparado={owner:usuario.id,categoryId,categoryName,code:codigo,description:descricao,value:valor,created:Date.now()};
        el("pagamento-resumo").replaceChildren(no("p",`Descrição informada: ${descricao}`),no("p",`Código informado: ${codigo}`),no("p",`Valor a debitar no FlowBank: ${moeda(valor)}`),no("p",`Categoria: ${categoryName}`));
        el("pagamento-form").hidden=true;el("pagamento-confirmacao").hidden=false;el("pagamento-confirmar").disabled=false;
        mensagem("Confira os dados. Nenhum débito foi enviado ainda.");
      }catch(erro){if(!encerrada)mensagem(erroTexto(erro),true);}
      finally{ocupado=false;el("pagamento-conferir").disabled=false;}
    });
    el("pagamento-editar").addEventListener("click",()=>{if(ocupado||pendente())return;preparado=null;el("pagamento-confirmacao").hidden=true;el("pagamento-form").hidden=false;el("pagamento-confirmar").disabled=true;});
    el("pagamento-confirmar").addEventListener("click",async()=>{
      if(ocupado||!preparado||encerrada)return;if(pendente()){mostrarPendencia();return;}
      if(String(preparado.owner)!==String(usuario.id)||Date.now()-preparado.created>600000){mensagem("Conferência expirada. Volte e confira os dados novamente.",true);el("pagamento-confirmar").disabled=true;return;}
      if(!window.confirm(`Confirmar débito de ${moeda(preparado.value)} no saldo do FlowBank? Isso registra um BOLETO no projeto e não realiza liquidação bancária.`))return;
      ocupado=true;el("pagamento-confirmar").disabled=true;let enviado=false;
      try{
        await perfil();if(encerrada)return;
        if(Number(preparado.value)>Number(usuario.balance))throw new Error("Saldo insuficiente. Volte e confira o valor.");
        const hoje=new Date();const date=`${hoje.getFullYear()}-${String(hoje.getMonth()+1).padStart(2,"0")}-${String(hoje.getDate()).padStart(2,"0")}`;
        sessionStorage.setItem(pendingKey,JSON.stringify(preparado));enviado=true;
        const resultado=await api("transactions",{method:"POST",body:JSON.stringify({description:preparado.description,value:preparado.value,date,type:"BOLETO",senderId:usuario.id,destination:preparado.code,categoryId:preparado.categoryId})});
        if(encerrada)return;
        if(resultado?.id==null||resultado.status!=="COMPLETED")throw new Error("Resposta inesperada após o envio.");
        sessionStorage.removeItem(pendingKey);preparado=null;mostrarPendencia();el("pagamento-confirmar").textContent="Registro concluído";el("pagamento-editar").disabled=true;
        mensagem(`Registro concluído no FlowBank. Transação #${resultado.id}. Confira o débito no extrato. Este registro não é comprovante de liquidação bancária.`);
        usuario.balance=null;saldo();
        try{await perfil();}catch{/* Uma falha de atualização não desfaz o registro confirmado. */}
      }catch(erro){
        const recusado=erro instanceof ApiError&&[400,401,403,404,409,422,429].includes(erro.status);
        if(!enviado||recusado){if(enviado)sessionStorage.removeItem(pendingKey);if(!encerrada){mensagem(erroTexto(erro),true);el("pagamento-confirmar").disabled=false;}}
        else if(!encerrada){mostrarPendencia();mensagem("Resultado do envio não confirmado. Confira o extrato antes de repetir o registro.",true);}
      }finally{ocupado=false;}
    });
  }
  try{await perfil();if(encerrada)return;mostrarPendencia();if(pagina==="home"){el("pagamentos-atualizar").addEventListener("click",atualizar);await atualizar();}else{await prepararFormulario();if(!pendente()&&el("pagamentos-mensagem").dataset.erro!=="true")mensagem("Preencha os dados para conferir antes de registrar.");}}
  catch(e){if(!encerrada)mensagem(`${erroTexto(e)} Recarregue a página para tentar novamente.`,true);}
})();
