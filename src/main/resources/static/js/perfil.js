(async () => {
  const el=id=>document.getElementById(id);
  const pagina=document.body.dataset.profilePage;
  const editando=pagina==="edit";
  const token=sessionStorage.getItem("token");
  let usuario=null,ocupado=false,encerrada=false,original="",alterado=false;
  const inputs=["perfil-name-input","perfil-phone-input","perfil-email-input","perfil-birth-input"];
  function mensagem(texto,erro=false){el("perfil-mensagem").textContent=texto;el("perfil-mensagem").dataset.erro=String(erro);}
  function campos(){return {name:el("perfil-name-input").value.trim(),numeroTelefone:el("perfil-phone-input").value.replace(/\D/g,""),email:el("perfil-email-input").value.trim(),dataNascimento:el("perfil-birth-input").value};}
  function sujo(){return editando&&usuario&&JSON.stringify(campos())!==original;}
  function sair(evento){
    evento?.preventDefault();if(ocupado)return;
    if(sujo()&&!window.confirm("Sair e descartar as alterações ainda não salvas?"))return;
    encerrada=true;alterado=false;sessionStorage.removeItem("token");
    for(const id of ["perfil-nome","perfil-cpf","perfil-email","perfil-telefone","perfil-nascimento"])if(el(id))el(id).textContent="—";
    if(editando){for(const id of [...inputs,"perfil-cpf-input"])el(id).value="";el("perfil-campos").disabled=true;}
    window.location.replace("../index.html");
  }
  // Logout por sessão expirada não deve ser impedido por edições locais.
  function expirar(){ocupado=false;alterado=false;original=editando&&usuario?JSON.stringify(campos()):"";sair();}
  for(const link of document.querySelectorAll(".logout"))link.addEventListener("click",sair);
  el("perfil-sair")?.addEventListener("click",sair);
  window.addEventListener("pageshow",e=>{if(e.persisted)window.location.reload();});
  window.addEventListener("beforeunload",e=>{if(alterado&&!encerrada){e.preventDefault();e.returnValue="";}});
  if(!token){expirar();return;}
  class ApiError extends Error{constructor(texto,status){super(texto);this.status=status;}}
  async function api(caminho,opcoes={}){
    const controle=new AbortController();const timeout=setTimeout(()=>controle.abort(),15000);
    try{
      const resposta=await fetch(`../${caminho}`,{...opcoes,headers:{Authorization:`Bearer ${token}`,...(opcoes.body?{"Content-Type":"application/json"}:{})},cache:"no-store",signal:controle.signal});
      if(resposta.status===401){expirar();throw new ApiError("Sessão expirada. Entre novamente.",401);}
      if(!resposta.ok){const d=await resposta.json().catch(()=>null);const campos=d&&typeof d==="object"?Object.values(d).filter(v=>typeof v==="string").join(" "):"";throw new ApiError(resposta.status<500?d?.message||campos||`Requisição recusada (HTTP ${resposta.status}).`:`Erro no servidor (HTTP ${resposta.status}).`,resposta.status);}
      return await resposta.json();
    }finally{clearTimeout(timeout);}
  }
  function erroTexto(e){return e.name==="AbortError"?"O servidor demorou para responder.":e instanceof TypeError?"Não foi possível conectar ao servidor.":e.message;}
  const telefone=u=>u.numeroTelefone||u.numeorTelefone||"";
  const cpfFormatado=v=>(v||"").replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/,"$1.$2.$3-$4");
  const dataFormatada=v=>/^(\d{4})-(\d{2})-(\d{2})$/.test(v||"")?v.split("-").reverse().join("/"):"Não informada";
  function validarUsuario(u){if(!u||u.id==null||typeof u.name!=="string")throw new Error("Resposta de perfil inválida.");return u;}
  function render(u){
    usuario=u;
    if(editando){
      el("perfil-name-input").value=u.name||"";el("perfil-cpf-input").value=cpfFormatado(u.cpf);
      el("perfil-phone-input").value=telefone(u);el("perfil-email-input").value=u.email||"";el("perfil-birth-input").value=u.dataNascimento||"";
      original=JSON.stringify(campos());alterado=false;
    }else{
      el("perfil-nome").textContent=u.name;
      if(pagina==="profile"){
        el("perfil-cpf").textContent=cpfFormatado(u.cpf)||"Não informado";el("perfil-email").textContent=u.email||"Não informado";
        el("perfil-telefone").textContent=telefone(u)||"Não informado";el("perfil-nascimento").textContent=dataFormatada(u.dataNascimento);
      }
    }
  }
  function controles(){el("perfil-atualizar").disabled=ocupado;if(editando)el("perfil-campos").disabled=ocupado||encerrada||!usuario;}
  async function carregar(inicial=false){
    if(ocupado||encerrada)return;if(!inicial&&sujo()&&!window.confirm("Recarregar e descartar as alterações ainda não salvas?"))return;
    ocupado=true;controles();mensagem("Carregando seus dados...");
    try{const u=validarUsuario(await api("users/me"));if(!encerrada){render(u);mensagem("Dados atualizados.");}}
    catch(e){if(!encerrada)mensagem(`${erroTexto(e)} Use Recarregar dados para tentar novamente.`,true);}
    finally{ocupado=false;controles();}
  }
  el("perfil-atualizar").addEventListener("click",()=>carregar());
  if(editando){
    for(const id of inputs)el(id).addEventListener("input",()=>{alterado=Boolean(sujo());});
    el("perfil-cancelar").addEventListener("click",()=>{
      if(ocupado||!usuario)return;if(sujo()&&!window.confirm("Descartar as alterações deste formulário?"))return;
      render(usuario);mensagem("Formulário restaurado para os últimos dados carregados.");
    });
    el("perfil-form").addEventListener("submit",async e=>{
      e.preventDefault();if(ocupado||encerrada||!usuario)return;
      if(!sujo()){mensagem("Nenhuma alteração para salvar.");return;}
      const dados=campos();
      if(!dados.name||!dados.numeroTelefone||!dados.email||!dados.dataNascimento){mensagem("Preencha nome, telefone, e-mail e data de nascimento.",true);return;}
      ocupado=true;controles();mensagem("Salvando alterações...");let enviado=false;
      try{
        enviado=true;
        const atualizado=validarUsuario(await api(`users/${encodeURIComponent(usuario.id)}`,{method:"PUT",body:JSON.stringify(dados)}));
        if(encerrada)return;if(String(atualizado.id)!==String(usuario.id))throw new Error("Resposta de usuário inesperada.");
        render(atualizado);mensagem("Dados salvos. Suas chaves Pix já registradas permanecem como estavam.");
      }catch(erro){
        if(!encerrada){const incerto=enviado&&(!(erro instanceof ApiError)||erro.status>=500);mensagem(`${erroTexto(erro)}${incerto?" A alteração pode ter sido salva. Recarregue os dados para conferir antes de editar novamente.":""}`,true);}
      }finally{ocupado=false;controles();}
    });
  }
  await carregar(true);
})();
