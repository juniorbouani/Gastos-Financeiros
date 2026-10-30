(async () => {
  const el=id=>document.getElementById(id),page=document.body.dataset.panelPage;
  const token=sessionStorage.getItem("token");let user,busy=false,ended=false,editing=null,number=0,totalPages=0;
  const money=v=>v!=null&&Number.isFinite(Number(v))?Number(v).toLocaleString("pt-BR",{style:"currency",currency:"BRL"}):"—";
  function msg(text,error=false){el("panel-message").textContent=text;el("panel-message").dataset.error=String(error);}
  function node(tag,text,cls=""){const n=document.createElement(tag);n.className=cls;if(text!==undefined)n.textContent=text;return n;}
  function button(text,fn){const b=node("button",text);b.type="button";b.addEventListener("click",()=>run(fn));return b;}
  function clearPasswords(){for(const id of ["current-password","new-password","confirm-password"])if(el(id))el(id).value="";}
  function logout(e){e?.preventDefault();ended=true;clearPasswords();el("panel-list")?.replaceChildren();sessionStorage.removeItem("token");window.location.replace("../index.html");}
  for(const link of document.querySelectorAll(".logout"))link.addEventListener("click",logout);
  window.addEventListener("pageshow",e=>{if(e.persisted)window.location.reload();});
  if(!token){logout();return;}
  class ApiError extends Error{constructor(text,status){super(text);this.status=status;}}
  async function api(path,method="GET",body){const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),15000);try{
    const r=await fetch(`../${path}`,{method,headers:{Authorization:`Bearer ${token}`,...(body?{"Content-Type":"application/json"}:{})},...(body?{body:JSON.stringify(body)}:{}),signal:controller.signal,cache:"no-store"});
    if(r.status===401){logout();throw new ApiError("Sessão inválida ou expirada.",401);}
    if(!r.ok){const d=await r.json().catch(()=>null);const values=d&&typeof d==="object"?Object.values(d).filter(v=>typeof v==="string").join(" "):"";throw new ApiError(r.status<500?d?.message||values||`Requisição recusada (HTTP ${r.status}).`:`Erro no servidor (HTTP ${r.status}).`,r.status);}
    return r.status===204?null:await r.json();
  }finally{clearTimeout(timer);}}
  function errorText(e){return e.name==="AbortError"?"O servidor demorou para responder.":e instanceof TypeError?"A conexão falhou.":e.message;}
  function pagination(){if(page!=="notifications")return;el("page-previous").disabled=busy||ended||number<=0||!totalPages;el("page-next").disabled=busy||ended||number+1>=totalPages;el("only-unread").disabled=busy;el("read-all").disabled=busy;}
  async function run(fn){if(busy||ended)return;busy=true;el("panel-refresh").disabled=true;if(el("budget-month"))el("budget-month").disabled=true;pagination();try{await fn();}catch(e){if(!ended)msg(`${errorText(e)} Atualize os dados para conferir o resultado antes de repetir uma alteração.`,true);}finally{busy=false;el("panel-refresh").disabled=false;if(el("budget-month"))el("budget-month").disabled=false;pagination();}}
  function decimal(text){const m=/^(\d+)(?:[,.](\d{1,2}))?$/.exec(text.trim());if(!m)throw new Error("Informe valor sem separador de milhar, como 500,50.");const c=Number(m[1])*100+Number((m[2]||"").padEnd(2,"0"));if(!Number.isSafeInteger(c)||c<=0)throw new Error("O limite deve ser positivo com até duas casas decimais.");return(c/100).toFixed(2);}
  async function afterWrite(text,refresh){msg(text);try{await refresh();if(!ended)msg(text);}catch(e){if(!ended)msg(`${text} Não foi possível atualizar a tela: ${errorText(e)}`,true);}}
  async function notifications(){
    const list=el("panel-list");list.replaceChildren(node("p","Carregando notificações..."));
    const only=el("only-unread").value==="true";
    const params=new URLSearchParams({onlyUnread:String(only),page:String(number),size:"20"});params.append("sort","dataCriacao,desc");params.append("sort","id,desc");
    let data=await api(`notifications?${params}`);if(ended)return;
    if(!Array.isArray(data.content)||!Number.isInteger(data.totalPages)||!Number.isInteger(data.number))throw new Error("Paginação de notificações inválida.");
    if(data.totalPages>0&&number>=data.totalPages){number=data.totalPages-1;return notifications();}
    number=data.number;totalPages=data.totalPages;list.replaceChildren();
    if(!data.content.length)list.appendChild(node("p",only?"Nenhuma notificação não lida.":"Nenhuma notificação nesta conta."));
    for(const n of data.content){const row=node("article",undefined,`panel-row ${n.lida?"":"unread"}`);row.append(node("strong",n.lida?"Lida":"Não lida"),node("p",n.mensagem),node("p",String(n.dataCriacao||"").replace("T"," ").slice(0,19)));if(!n.lida)row.appendChild(button("Marcar como lida",async()=>{await api(`notifications/${n.id}/read`,"PUT");await afterWrite("Notificação marcada como lida.",notifications);}));list.appendChild(row);}
    el("page-label").textContent=totalPages?`Página ${number+1} de ${totalPages}`:"Nenhuma página";
    try{const count=await api("notifications/unread-count");if(!ended)el("notification-count").textContent=`${count} notificação(ões) não lida(s).`;}catch(e){if(!ended)el("notification-count").textContent="Contagem indisponível.";}
    pagination();
  }
  async function categories(){
    el("panel-list").replaceChildren(node("p","Carregando categorias..."));const data=await api(`categories/user/${user.id}`);if(ended)return;if(!Array.isArray(data))throw new Error("Categorias inválidas.");
    const list=el("panel-list");list.replaceChildren();if(!data.length)list.appendChild(node("p","Nenhuma categoria criada."));
    for(const c of data){const row=node("article",undefined,"panel-row");row.append(node("h2",`${c.icone||""} ${c.name}`),button("Editar",async()=>{editing=c.id;el("category-name").value=c.name;el("category-icon").value=c.icone||"";msg("Edite os campos e salve a categoria.");}),button("Excluir",async()=>{if(!window.confirm(`Excluir a categoria ${c.name}? Categorias usadas em transações ou orçamentos podem não ser removidas.`))return;await api(`categories/${c.id}`,"DELETE");if(editing===c.id)resetCategory();await afterWrite("Categoria excluída.",categories);}));list.appendChild(row);}
  }
  function resetCategory(){editing=null;el("panel-form").reset();el("panel-submit").textContent="Salvar categoria";}
  async function budgetCategories(){const data=await api(`categories/user/${user.id}`);if(ended)return;if(!Array.isArray(data))throw new Error("Categorias inválidas.");const select=el("budget-category");select.replaceChildren();const empty=node("option","Selecione uma categoria");empty.value="";select.appendChild(empty);for(const c of data){const option=node("option",c.name);option.value=String(c.id);select.appendChild(option);}el("panel-submit").disabled=!data.length;}
  async function budgets(){
    const month=el("budget-month").value;if(!/^\d{4}-(0[1-9]|1[0-2])$/.test(month))throw new Error("Selecione um mês válido.");
    el("panel-list").replaceChildren(node("p","Carregando orçamentos..."));const data=await api(`budgets/user/${user.id}?month=${encodeURIComponent(month)}`);if(ended)return;if(!Array.isArray(data))throw new Error("Orçamentos inválidos.");
    const list=el("panel-list");list.replaceChildren();if(!data.length)list.appendChild(node("p","Nenhum orçamento neste mês."));
    for(const b of data){const row=node("article",undefined,"panel-row");row.append(node("h2",`${b.categoria} · ${b.mesReferencia}`),node("p",`Limite: ${money(b.limite)} · Gasto: ${money(b.gasto)} · Restante: ${money(b.restante)}${b.estourado?" · Limite ultrapassado":""}`));
      const label=node("label","Novo limite (R$)");const input=node("input");input.id=`budget-limit-${b.budgetId}`;input.inputMode="decimal";input.value=String(b.limite);label.htmlFor=input.id;
      row.append(label,input,button("Salvar novo limite",async()=>{const limite=decimal(input.value);await api(`budgets/${b.budgetId}/limit`,"PUT",{limite});await afterWrite("Limite atualizado.",budgets);}),button("Excluir orçamento",async()=>{if(!window.confirm(`Excluir o orçamento de ${b.categoria} em ${b.mesReferencia}? Os gastos já registrados permanecem no histórico.`))return;await api(`budgets/${b.budgetId}`,"DELETE");await afterWrite("Orçamento excluído.",budgets);}));list.appendChild(row);}
  }
  async function refresh(){msg("Atualizando...");if(page==="notifications")await notifications();else if(page==="categories")await categories();else if(page==="budgets"){await budgetCategories();await budgets();}else{clearPasswords();msg("Informe a senha atual para continuar.");return;}if(!ended)msg("Dados atualizados.");}
  el("panel-refresh").addEventListener("click",()=>run(refresh));
  try{
    user=await api("users/me");if(ended)return;if(user.id==null)throw new Error("Perfil inválido.");
    if(page==="notifications"){
      el("only-unread").addEventListener("change",()=>run(async()=>{number=0;await refresh();}));
      el("page-previous").addEventListener("click",()=>run(async()=>{number=Math.max(0,number-1);await notifications();}));
      el("page-next").addEventListener("click",()=>run(async()=>{number++;await notifications();}));
      el("read-all").addEventListener("click",()=>run(async()=>{await api("notifications/read-all","PUT");number=0;await afterWrite("Todas as notificações foram marcadas como lidas.",notifications);}));
    }else if(["password","close"].includes(page)){
      el("panel-submit").disabled=false;
      el("panel-form").addEventListener("submit",e=>{e.preventDefault();return run(async()=>{
        const current=el("current-password").value;if(!current)throw new Error("Informe a senha atual.");
        if(page==="password"){
          const password=el("new-password").value;if(password!==el("confirm-password").value)throw new Error("As novas senhas não coincidem.");
          if(password.length<8||new TextEncoder().encode(password).length>72)throw new Error("Use pelo menos 8 caracteres e até 72 bytes em UTF-8.");
          try{await api("users/me/password","PUT",{senhaAtual:current,novaSenha:password});}finally{clearPasswords();}
        }else{
          if(el("close-word").value!=="ENCERRAR")throw new Error("Digite ENCERRAR exatamente como indicado.");
          if(!window.confirm("Encerrar a conta? O acesso será desativado, os cartões cancelados e as chaves Pix removidas. O histórico será preservado."))return;
          try{await api("users/me/close","POST",{senhaAtual:current,confirmacao:"ENCERRAR"});}finally{clearPasswords();}
        }
        if(ended)return;
        sessionStorage.setItem("flowbank.accountMessage",page==="password"?"Senha alterada. Entre novamente com a nova senha.":"Conta encerrada. O acesso foi desativado e o histórico foi preservado.");
        logout();
      });});
    }else if(page==="categories"){
      el("panel-submit").disabled=false;el("category-cancel").addEventListener("click",()=>{if(!busy){resetCategory();msg("Edição cancelada.");}});
      el("panel-form").addEventListener("submit",e=>{e.preventDefault();return run(async()=>{const name=el("category-name").value.trim();if(!name)throw new Error("Informe o nome da categoria.");const icone=el("category-icon").value.trim();await api(editing?`categories/${editing}`:"categories",editing?"PUT":"POST",{name,icone});resetCategory();await afterWrite("Categoria salva.",categories);});});
    }else if(page==="budgets"){
      const now=new Date();el("budget-month").value=`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,"0")}`;
      el("budget-month").addEventListener("change",()=>run(budgets));
      el("panel-form").addEventListener("submit",e=>{e.preventDefault();return run(async()=>{const categoryId=Number(el("budget-category").value);if(!Number.isSafeInteger(categoryId)||categoryId<=0)throw new Error("Selecione uma categoria.");const mesReferencia=el("budget-month").value;if(!/^\d{4}-(0[1-9]|1[0-2])$/.test(mesReferencia))throw new Error("Selecione um mês válido.");const limite=decimal(el("budget-limit").value);await api("budgets","POST",{categoryId,mesReferencia,limite});el("budget-limit").value="";await afterWrite("Orçamento criado.",budgets);});});
    }
    await run(refresh);
  }catch(e){if(!ended)msg(`${errorText(e)} Recarregue a página para tentar novamente.`,true);}
})();
