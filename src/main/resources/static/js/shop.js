(async()=>{
 const el=id=>document.getElementById(id),page=document.body.dataset.shopPage,productId=document.body.dataset.productId;
 const token=sessionStorage.getItem("token");let user,product,cards=[],review=null,busy=false,ended=false,finished=false,pageNumber=0,totalPages=0;
 const money=v=>v!=null&&Number.isFinite(Number(v))?Number(v).toLocaleString("pt-BR",{style:"currency",currency:"BRL"}):"—";
 function msg(text,error=false){el("shop-message").textContent=text;el("shop-message").dataset.error=String(error);}
 function node(tag,text,cls=""){const n=document.createElement(tag);n.className=cls;if(text!==undefined)n.textContent=text;return n;}
 function button(text,fn){const b=node("button",text);b.type="button";b.addEventListener("click",()=>run(fn));return b;}
 function logout(e){e?.preventDefault();ended=true;review=null;for(const id of ["shop-orders","shop-summary","shop-result"])el(id)?.replaceChildren();sessionStorage.removeItem("token");window.location.replace("../index.html");}
 document.querySelector(".logout").addEventListener("click",logout);window.addEventListener("pageshow",e=>{if(e.persisted)window.location.reload();});if(!token){logout();return;}
 class ApiError extends Error{constructor(text,status){super(text);this.status=status;}}
 async function api(path,method="GET",body){const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),15000);try{
  const r=await fetch(`../${path}`,{method,headers:{Authorization:`Bearer ${token}`,...(body?{"Content-Type":"application/json"}:{})},...(body?{body:JSON.stringify(body)}:{}),cache:"no-store",signal:controller.signal});
  if(r.status===401){logout();throw new ApiError("Sessão expirada. Entre novamente.",401);}
  if(!r.ok){const d=await r.json().catch(()=>null);const texts=d&&typeof d==="object"?Object.values(d).filter(v=>typeof v==="string").join(" "):"";throw new ApiError(r.status<500?d?.message||texts||`Requisição recusada (HTTP ${r.status}).`:`Erro no servidor (HTTP ${r.status}).`,r.status);}
  return await r.json();
 }finally{clearTimeout(timer);}}
 function errorText(e){return e.name==="AbortError"?"O servidor demorou para responder.":e instanceof TypeError?"A conexão falhou.":e.message;}
 function controls(){if(page==="catalog"){el("shop-previous").disabled=busy||ended||pageNumber<=0||!totalPages;el("shop-next").disabled=busy||ended||pageNumber+1>=totalPages;el("shop-refresh").disabled=busy;}else{el("shop-confirm").disabled=busy||ended||finished||!review;el("shop-edit").disabled=busy||finished;}}
 async function run(fn){if(busy||ended)return;busy=true;controls();try{await fn();}catch(e){if(!ended)msg(errorText(e),true);}finally{busy=false;controls();}}
 const pendingKey=()=>`flowbank.shopPending.${user.id}`;
 function pending(){try{return JSON.parse(sessionStorage.getItem(pendingKey())||"null");}catch{return null;}}
 function body(p){return {productId:p.productId,cardId:p.cardId,totalParcelas:p.totalParcelas,requestId:p.requestId};}
 function validPending(p){return p&&String(p.owner)===String(user.id)&&typeof p.requestId==="string"&&/^[a-f0-9-]{36}$/i.test(p.requestId)&&typeof p.productId==="string"&&Number.isSafeInteger(p.cardId)&&Number.isInteger(p.totalParcelas)&&p.totalParcelas>=1&&p.totalParcelas<=12;}
 function showPending(){
  const box=el("shop-pending");box.replaceChildren();const p=pending();if(!p)return;
  if(!validPending(p)){box.appendChild(node("p","Há um identificador de pedido inválido armazenado. Confira Meus pedidos antes de iniciar outra compra."));return;}
  box.appendChild(node("p",`Há uma compra aguardando confirmação do resultado: ${p.name} · ${money(p.total)} · ${p.totalParcelas}x. Consulte o resultado ou reenvie a mesma operação com o mesmo identificador.`));
  box.append(button("Consultar resultado",async()=>{try{const result=await api(`shop/orders/request/${encodeURIComponent(p.requestId)}`);await complete(result,p);}catch(e){if(e instanceof ApiError&&e.status===404){msg("Pedido ainda não encontrado. Você pode reenviar a mesma confirmação: o servidor preserva o identificador para impedir duplicidade.",true);return;}throw e;}}),button("Reenviar a mesma confirmação",async()=>{if(!window.confirm(`Confirmar novamente a mesma operação de ${p.name}, ${money(p.total)}, ${p.totalParcelas}x no cartão ${p.cardMask}? O mesmo identificador será usado para evitar outra cobrança.`))return;await checkout(p);}));
 }
 function productTile(p,link=true){
  const tile=node("article",undefined,"shop-tile"),img=node("img");
  if(!/^[A-Za-z0-9_-]+\.jpg$/.test(p.imagem)||!Number.isFinite(Number(p.preco)))throw new Error("Produto inválido.");
  img.src=`../img/${p.imagem}`;img.alt=p.nome;tile.append(img,node("h2",p.nome),node("p",money(p.preco)));
  if(link){if(!/^[A-Za-z0-9_-]+\.html$/.test(p.pagina))throw new Error("Página do produto inválida.");const a=node("a","Ver produto e comprar");a.href=p.pagina;tile.appendChild(a);}return tile;
 }
 async function orders(){
  const list=el("shop-orders");list.replaceChildren(node("p","Carregando pedidos..."));
  const params=new URLSearchParams({page:String(pageNumber),size:"20"});params.append("sort","id,desc");const d=await api(`shop/orders?${params}`);if(ended)return;
  if(!Array.isArray(d.content)||!Number.isInteger(d.totalPages)||!Number.isInteger(d.number))throw new Error("Histórico de pedidos inválido.");
  if(d.totalPages>0&&pageNumber>=d.totalPages){pageNumber=d.totalPages-1;return orders();}
  pageNumber=d.number;totalPages=d.totalPages;list.replaceChildren();if(!d.content.length)list.appendChild(node("p","Você ainda não possui pedidos no Shop."));
  for(const o of d.content){const row=node("article",undefined,"shop-order");row.append(node("h3",`Pedido #${o.id} · ${o.produto}`),node("p",`${money(o.total)} · ${o.totalParcelas}x · Cartão ${o.cartao}`),node("p",`Registrado em ${String(o.createdAt||"").replace("T"," ").slice(0,19)} · Primeira fatura #${o.firstInvoiceId}`));list.appendChild(row);}
  el("shop-page-label").textContent=totalPages?`Página ${pageNumber+1} de ${totalPages}`:"Nenhuma página";
 }
 function installmentText(total,n){const cents=Math.round(Number(total)*100),base=Math.floor(cents/n),last=cents-base*(n-1);return n===1?`1x de ${money(total)}`:base===last?`${n}x de ${money(base/100)}`:`${n-1}x de ${money(base/100)} e última de ${money(last/100)}`;}
 async function complete(result,p){
  if(ended)return;
  if(!result||result.id==null||result.requestId!==p.requestId||result.productId!==p.productId||String(result.cardId)!==String(p.cardId)||result.totalParcelas!==p.totalParcelas)throw new Error("Resposta inesperada do pedido. Consulte o resultado antes de iniciar outra compra.");
  sessionStorage.removeItem(pendingKey());showPending();finished=true;review=null;
  msg(`Compra registrada! Pedido #${result.id}, total ${money(result.total)}. Consulte os lançamentos em Cartões e faturas.`);
  if(page==="product"){
   el("shop-form").hidden=true;el("shop-confirmation").hidden=true;
   const a=node("a","Ver cartões e faturas");a.href="../cartoes/cartoes.html";
   el("shop-result").replaceChildren(node("h2",`Pedido #${result.id} confirmado`),node("p",`${result.produto} · ${money(result.total)} · ${result.totalParcelas}x · ${result.cartao}`),a);
  }else{try{await orders();}catch{if(!ended)msg(`Pedido #${result.id} confirmado. Não foi possível atualizar a lista; use Atualizar pedidos.`,true);}}
 }
 async function checkout(p){
  sessionStorage.setItem(pendingKey(),JSON.stringify(p));showPending();
  try{const result=await api("shop/orders","POST",body(p));await complete(result,p);}
  catch(e){
   const refused=e instanceof ApiError&&[400,401,403,404,422,429].includes(e.status);
   if(refused){sessionStorage.removeItem(pendingKey());if(!ended)showPending();}
   else if(!ended){showPending();msg("O resultado da compra ainda não foi confirmado. Use Consultar resultado ou Reenviar a mesma confirmação. Não crie outro pedido para repetir esta compra.",true);return;}
   throw e;
  }
 }
 async function loadProduct(){
  product=await api(`shop/products/${encodeURIComponent(productId)}`);if(ended)return;el("shop-product").replaceChildren(productTile(product,false));
  const d=await api(`credit-cards/user/${user.id}`);if(ended)return;if(!Array.isArray(d))throw new Error("Lista de cartões inválida.");
  const now=new Date(),today=`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,"0")}-${String(now.getDate()).padStart(2,"0")}`;
  cards=d.filter(c=>c.status==="ATIVO"&&c.dataExpiracao&&c.dataExpiracao>=today);
  const select=el("shop-card");for(const c of cards){const o=node("option",`${c.numeroMascarado} · Disponível ${money(c.limiteDisponivel)}`);o.value=String(c.id);select.appendChild(o);}
  for(let n=1;n<=12;n++){const o=node("option",installmentText(product.preco,n));o.value=String(n);el("shop-installments").appendChild(o);}el("shop-installments").value="1";
  el("shop-review").disabled=!cards.length;if(!cards.length)msg("Você não possui cartão ativo e válido. Solicite ou desbloqueie um cartão em Cartões e faturas.",true);
  el("shop-form").addEventListener("submit",e=>{e.preventDefault();return run(async()=>{
   if(finished)return;if(pending()){showPending();throw new Error("Confira o resultado da compra pendente antes de iniciar outra.");}
   const card=cards.find(c=>String(c.id)===el("shop-card").value),n=Number(el("shop-installments").value);if(!card)throw new Error("Selecione um cartão ativo.");if(!Number.isInteger(n)||n<1||n>12)throw new Error("Selecione de 1 a 12 parcelas.");
   if(Number(card.limiteDisponivel)<Number(product.preco))throw new Error("Limite disponível insuficiente para o total da compra.");
   if(!crypto.randomUUID)throw new Error("Seu navegador precisa oferecer geração segura de identificadores para comprar.");
   review={owner:user.id,productId:product.id,cardId:card.id,totalParcelas:n,requestId:crypto.randomUUID(),name:product.nome,total:product.preco,cardMask:card.numeroMascarado};
   el("shop-summary").replaceChildren(node("p",`Produto: ${product.nome} · Uma unidade`),node("p",`Total: ${money(product.preco)}`),node("p",installmentText(product.preco,n)),node("p",`Cartão: ${card.numeroMascarado}`),node("p","A compra compromete o limite total agora; o pagamento das faturas debita o saldo da conta."));
   el("shop-form").hidden=true;el("shop-confirmation").hidden=false;msg("Confira os dados. Nenhuma compra foi enviada ainda.");
  });});
  el("shop-edit").addEventListener("click",()=>{if(busy||pending()||finished)return;review=null;el("shop-confirmation").hidden=true;el("shop-form").hidden=false;controls();});
  el("shop-confirm").addEventListener("click",()=>run(async()=>{
   if(!review||finished)return;if(pending()){showPending();throw new Error("Consulte o pedido pendente ou reenvie a mesma confirmação.");}
   if(!window.confirm(`Comprar ${review.name} por ${money(review.total)}, em ${review.totalParcelas}x no cartão ${review.cardMask}? A compra será lançada nas faturas do FlowBank.`))return;
   await checkout(review);
  }));
 }
 try{
  user=await api("users/me");if(ended)return;if(user.id==null)throw new Error("Perfil inválido.");showPending();
  if(page==="catalog"){
   const products=await api("shop/products");if(ended)return;if(!Array.isArray(products))throw new Error("Catálogo inválido.");el("shop-products").replaceChildren(...products.map(p=>productTile(p)));
   el("shop-refresh").addEventListener("click",()=>run(async()=>{pageNumber=0;await orders();showPending();msg("Pedidos atualizados.");}));
   el("shop-previous").addEventListener("click",()=>run(async()=>{pageNumber=Math.max(0,pageNumber-1);await orders();}));el("shop-next").addEventListener("click",()=>run(async()=>{pageNumber++;await orders();}));
   await run(orders);
  }else await run(loadProduct);
  if(!ended&&el("shop-message").dataset.error!=="true")msg("Dados carregados.");
 }catch(e){if(!ended)msg(`${errorText(e)} Recarregue a página para tentar novamente.`,true);}
 controls();
})();
